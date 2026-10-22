# Orewise - Implementation Plan

> **Orewise** - *Mine the wisdom in your files.*
> RAG platform that lets small enterprises chat with the knowledge buried in their documents.

Domain: `orewise.io`. Still to check: EUIPO + BOIP trademark (classes 9, 42).

---

## 1. Decisions

| Topic | Decision |
|---|---|
| Deployment | Both: multi-tenant SaaS (EU-hosted) and self-hosted (Docker Compose). Same images, config via env. |
| Backend | Java 21, Spring Boot 4.1, Spring AI 2.0, JobRunr 9 |
| Admin UI | Angular 22 (standalone components, signals) |
| Chat UI | Embeddable Lit web component, two modes: `public` and `intranet` |
| AI provider | Pluggable via Spring AI. Default OpenAI. Ollama as optional local fallback. |
| Ollama | NOT part of self-host stack. Separate, opt-in compose stack. |
| Vector store | OpenSearch 3.x (k-NN HNSW + BM25 hybrid) |
| Tenancy | Single Keycloak realm; tenant = group `/tenants/<slug>`, subgroups -> collections |
| Auth | Keycloak (OIDC), SSO to Microsoft / Google |
| Access control | Group-based collections (Keycloak groups -> collections) |
| Languages / region | Dutch + English, EU hosting |
| First connectors | Manual upload, Dropbox API (personal + shared folders, no Business team folders), mounted-folder (self-host only) |
| Pricing | Document volume: file count + storage cap per tier |
| Quota exceeded | Skip new files (`QUOTA_EXCEEDED`), sync continues, admin notified |
| CI | GitHub Actions |
| MVP timeline | ~10 weeks |

---

## 2. Architecture

```text
 [Widget: public | intranet]   [Admin (Angular)]
              \                     /
               v                   v
        +---------------------------------+      +-----------------+
        | orewise-api (Spring Boot 4)     |<---->| Keycloak (OIDC) |
        | chat SSE, admin, quotas, ACL,   |      +-----------------+
        | widget keys, rate limits        |
        +---------------------------------+
            |            |            |
       PostgreSQL    OpenSearch    S3 / MinIO
            ^
        +---------------------------------+      +-----------------------+
        | orewise-worker (JobRunr)        |----->| OpenAI (default)      |
        | Dropbox / folder sync, Tika,    |      | Ollama (separate,     |
        | chunk, embed, index, quota      |      |  optional stack)      |
        +---------------------------------+      +-----------------------+
                   |
          Dropbox API / mounted folder
```

| Concern | Choice | Why |
|---|---|---|
| Parsing | Apache Tika (`TikaDocumentReader`) | PDF, DOCX, XLSX, PPTX, HTML, mail. OCR later. |
| Jobs | JobRunr on Postgres | Retries + dashboard, no extra broker |
| Metadata | PostgreSQL + Flyway | Tenants, collections, documents, chats, audit |
| Files | S3-compatible (MinIO self-host) | Same code SaaS and self-host |
| Rate limiting | Bucket4j | Per IP / per widget key |

---

## 3. Core design

### 3.1 Multi-tenancy (OpenSearch)

- Shared index, every chunk has `tenant_id` + `collection_ids`.
- Per-tenant filtered alias with routing on `tenant_id` (avoids shard explosion).
- Self-host = single tenant, same code.
- One index per embedding model (dimensions differ). Provider switch triggers background re-embed.

### 3.2 Chunk document

```json
{
  "chunk_id": "...",
  "tenant_id": "...",
  "collection_ids": ["hr", "all"],
  "document_id": "...",
  "title": "...",
  "source_uri": "...",
  "page": 4,
  "heading_path": "Policies > Leave",
  "lang": "nl",
  "text": "...",
  "embedding": [0.01],
  "updated_at": "..."
}
```

`text` gets `dutch` and `english` analyzer sub-fields; language detected per chunk.

### 3.3 Ingestion pipeline

1. Connector / upload -> raw file -> object store (SHA-256 / `content_hash` dedupe).
2. Quota check (doc count + storage). Exceeded -> status `QUOTA_EXCEEDED`, skip.
3. Tika -> text + metadata.
4. Structure-aware chunker (headings / pages), ~500 tokens, ~15% overlap.
5. Batch embed.
6. Upsert to OpenSearch keyed by `document_id`. Delete document -> delete all chunks.

### 3.4 Retrieval + chat

1. Query rewrite (follow-up -> standalone) using chat history.
2. Hybrid search (BM25 + kNN, normalization search pipeline) with ACL filter: `tenant_id` AND `collection_ids IN allowed`. Top 20.
3. Top 5-8 into grounded prompt: answer only from context, cite sources, say "I don't know".
4. Stream via SSE with citations (document + page links).
5. Log question, answer, chunks, tokens, latency.

ACL filter is applied inside the OpenSearch query, never post-filtered.

### 3.5 Connector SPI

```java
public interface SourceConnector {
    String type();
    Stream<SourceItem> listChanges(ConnectorConfig cfg, SyncCursor since);
    InputStream fetch(ConnectorConfig cfg, SourceItem item);
}
```

---

## 4. Connectors (MVP)

### 4.1 Manual upload

Drag and drop in admin UI into a collection.

### 4.2 Dropbox API (personal + shared folders)

- OAuth2 with `token_access_type=offline` -> refresh token, stored AES-GCM encrypted (key from env / Vault).
- SaaS: one registered Orewise Dropbox app. Self-host: customer registers own app.
- Initial sync: `files/list_folder` (recursive) on admin-selected folders, store `cursor`.
- Incremental: `files/list_folder/continue` with cursor.
- Triggers: Dropbox webhook (SaaS) + scheduled poll fallback. Self-host behind firewall: `list_folder/longpoll` or poll.
- Deletes: `DeletedMetadata` -> remove document, chunks, file.
- Skip unchanged files via Dropbox `content_hash`.
- Mapping: Dropbox folder -> Orewise collection (e.g. `/HR` -> `hr`).
- Out of scope: Dropbox Business team folders / team scopes.

### 4.3 Mounted folder (self-host only)

- Scans a directory mounted into the worker container (SMB/NFS share, Dropbox desktop sync folder, local disk).
- Change detection: periodic walk comparing path + size + mtime, hash on change; deletes detected by absence.
- Folder -> collection mapping same as Dropbox.
- Disabled in SaaS.

```yaml
services:
  orewise-worker:
    volumes:
      - /mnt/shared-drive:/data/sources/shared:ro
```

---

## 5. Pricing + quotas

| Tier | Documents | Storage | Widgets | Connectors |
|---|---|---|---|---|
| Basic | 1,000 | 2 GB | 1 intranet + 1 public | Upload + 1 |
| Pro | 10,000 | 20 GB | 5 | Upload + 3 |
| Business | 50,000 | 100 GB | unlimited | all |
| Enterprise / self-host | per license | per license | unlimited | all |

(Numbers are a starting point.)

- 1 document = 1 source file. Storage cap prevents abuse with huge files.
- Counters per tenant in Postgres (`plan`, `doc_limit`, `storage_limit`, usage).
- Enforcement in worker before parsing. Over limit -> file status `QUOTA_EXCEEDED`, skipped; sync continues; admin banner + email. Freed capacity (delete / upgrade) -> skipped files re-queued.
- Self-host licensing: signed JWT (Ed25519) with tier, doc limit, expiry. Offline verification.
- Billing: MVP manual tier assignment in admin. Stripe post-MVP.
- Cost guard: daily token budget per tier.

---

## 6. Widget

```html
<script src="https://cdn.orewise.io/widget.js" type="module"></script>
<orewise-chat widget-key="pk_live_..." mode="public"></orewise-chat>
<orewise-chat widget-key="pk_live_..." mode="intranet"></orewise-chat>
```

| | Public | Intranet |
|---|---|---|
| Identity | Anonymous | Keycloak OIDC PKCE |
| Collections | Only `public`-flagged collections linked to the widget | User groups -> collections |
| Protection | Allowed origins (CORS + server check), per-IP + per-key rate limit, optional Turnstile, daily cost cap | Auth |
| History | Session only, no PII stored | Per user, configurable retention |

Server enforces "public widget -> public collections only" in the OpenSearch filter. The key never grants data access by itself.

```text
widget(id, tenant_id, key, mode, allowed_origins[], collection_ids[], rate_limit, daily_token_cap)
```

---

## 7. AI providers

- Default: OpenAI chat model (e.g. `gpt-4.1-mini` or current equivalent) + `text-embedding-3-small`.
- Fallback: Ollama (`qwen2.5` / `llama3.x` for chat, `bge-m3` for embeddings).
- Ollama runs as a separate stack `deploy/ollama/docker-compose.yml` (optional GPU profile).

```properties
OREWISE_AI_PROVIDER=ollama
SPRING_AI_OLLAMA_BASE_URL=http://ollama-host:11434
```

---

## 8. Security + GDPR

- EU hosting (Scaleway / OVH / Hetzner / Azure / AWS EU region).
- OpenAI EU data residency / zero-retention; Azure OpenAI later.
- DPA, encryption at rest, audit log, per-tenant "delete all data", chat retention settings.
- Prompt-injection mitigation: delimited context, system prompt priority, no tool calls.
- Encrypted connector credentials.

---

## 9. Repository layout

```text
orewise/
  orewise-core/        domain, chunking, OpenSearch mapping, quota service
  orewise-api/         REST + SSE, admin, widget endpoints, rate limiting
  orewise-worker/      JobRunr, ingestion pipeline
  orewise-connectors/  SPI + dropbox + mounted-folder
  orewise-admin/       Angular
  orewise-widget/      Lit web component
  deploy/
    compose/           self-host core stack (no Ollama)
    ollama/            optional separate AI stack
    helm/              SaaS
  eval/                golden Q&A sets (NL + EN)
```

---

## 10. Roadmap (10 weeks)

| Week | Deliverable | Verify |
|---|---|---|
| 1 | Skeleton, Compose stack, Keycloak, CI, tenant model with plan/limits | Stack up, admin login works |
| 2-3 | Upload ingestion: Tika, chunk, embed, OpenSearch; quota enforcement | 1K files indexed; file 1,001 -> `QUOTA_EXCEEDED` |
| 4-5 | Chat: rewrite, hybrid search, ACL filter, SSE, citations | 30-question NL/EN golden set >= 80% |
| 6 | Dropbox connector: OAuth, full + delta sync, webhook, deletes | Add / edit / delete in Dropbox reflected within 1 min |
| 7 | Angular admin: collections, Dropbox setup, usage meters, widgets | Admin onboards tenant end-to-end without API |
| 8 | Widget both modes, allowed origins, rate limits, cost cap | Automated test: public widget cannot see intranet collections |
| 9 | Ollama separate stack, re-embed job, license JWT, mounted-folder connector | Self-host + Ollama answers without OpenAI key; share changes indexed |
| 10 | Hardening, backups, docs, Helm, pilot customer | Pilot live |

### Post-MVP

Stripe billing, Dropbox Business team folders, SharePoint / OneDrive, Google Drive, Confluence / Notion, on-prem agent for shared drives in SaaS, reranker, OCR, unanswered-question analytics, Teams bot.

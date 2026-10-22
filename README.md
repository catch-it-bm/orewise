# Orewise

*Mine the wisdom in your files.* RAG platform for small enterprises. See [PLAN.md](PLAN.md).

## Layout

```text
orewise-core/        domain (tenant, plans), Flyway migrations
orewise-connectors/  connector SPI
orewise-api/         REST API (Spring Boot 4, Keycloak JWT)
orewise-worker/      background jobs (JobRunr)
orewise-admin/       Angular admin UI
deploy/compose/      self-host / dev stack (no Ollama)
deploy/ollama/       optional separate Ollama stack
marketing/           static marketing site (orewise.io), no build step
```

## Prerequisites

- Java 21
- Node 24.15+ (pinned in `.vfox.toml`)
- Docker (for the stack)

## Build and test

```bash
./mvnw -B verify
cd orewise-admin && npm ci && npx ng test --watch=false && npx ng build
```

## Run the full stack

```bash
./mvnw -B package -DskipTests
cd deploy/compose
cp .env.example .env
docker compose up --build
```

| URL | What | Login |
|---|---|---|
| http://localhost:8081 | Admin UI | `platform-admin` / `admin`, `demo-admin` / `demo`, `demo-user` / `demo` |
| http://localhost:8180 | Keycloak console | `admin` / `admin` |
| http://localhost:8000 | JobRunr dashboard | - |
| http://localhost:8080/actuator/health | API health | - |

## Develop locally

```bash
cd deploy/compose
docker compose up postgres keycloak opensearch
```

Then run `OrewiseApiApplication` with profile `dev` (seeds tenant `demo`) and `npm start` in `orewise-admin` (http://localhost:4200, proxies `/api` to 8080).

## Tenants and access

- A tenant is the Keycloak group `/tenants/<slug>`; subgroups (e.g. `/tenants/demo/hr`) will map to collections.
- Realm role `platform-admin` manages tenants and plans; `tenant-admin` manages one tenant.
- Plan limits (documents + storage) live on the `tenant` table, defaults from `Plan`.

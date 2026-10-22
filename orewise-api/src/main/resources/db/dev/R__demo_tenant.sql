INSERT INTO tenant (id, slug, name, plan, doc_limit, storage_limit_bytes, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'demo', 'Demo BV', 'BASIC', 1000, 2147483648, now())
ON CONFLICT (slug) DO NOTHING;

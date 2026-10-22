INSERT INTO tenant (id, slug, name, plan, doc_limit, storage_limit_bytes, monthly_question_limit, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'demo', 'Demo BV', 'BASIC', 2500, 5368709120, 2000, now())
ON CONFLICT (slug) DO NOTHING;

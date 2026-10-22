ALTER TABLE tenant ADD COLUMN monthly_question_limit INTEGER NOT NULL DEFAULT 2000;

UPDATE tenant SET monthly_question_limit = 7500 WHERE plan = 'PRO';
UPDATE tenant SET monthly_question_limit = 20000 WHERE plan IN ('BUSINESS', 'ENTERPRISE');

-- Basic tier was raised from 1,000 docs / 2 GB to 2,500 docs / 5 GB.
UPDATE tenant SET doc_limit = 2500, storage_limit_bytes = 5368709120
WHERE plan = 'BASIC' AND doc_limit = 1000 AND storage_limit_bytes = 2147483648;

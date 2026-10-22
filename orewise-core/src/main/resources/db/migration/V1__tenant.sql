CREATE TABLE tenant (
    id                  UUID         PRIMARY KEY,
    slug                VARCHAR(63)  NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    plan                VARCHAR(32)  NOT NULL,
    doc_limit           INTEGER      NOT NULL,
    storage_limit_bytes BIGINT       NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

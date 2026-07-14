ALTER TABLE content_article
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER updated_at;

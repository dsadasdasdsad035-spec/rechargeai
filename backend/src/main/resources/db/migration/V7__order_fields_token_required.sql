-- AI 账号可选；Session Token 必填且表单项为 text

ALTER TABLE subscription_order
    MODIFY COLUMN target_account_enc VARCHAR(512) NULL COMMENT 'AI 账号标识（加密，可选）';

ALTER TABLE subscription_order
    MODIFY COLUMN account_token_enc TEXT NULL COMMENT 'Session Token（加密）';

UPDATE ai_service_product
SET required_fields_json = JSON_ARRAY(
    JSON_OBJECT('key', 'target_account', 'label', 'AI 账号', 'type', 'text', 'required', false),
    JSON_OBJECT('key', 'account_token', 'label', 'Session Token', 'type', 'text', 'required', true)
);

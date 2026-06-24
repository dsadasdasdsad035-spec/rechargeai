-- 服务类型教程配置 + 订单 Token 字段

CREATE TABLE service_type_config (
    service_type      VARCHAR(64)  NOT NULL PRIMARY KEY,
    display_name      VARCHAR(128) NOT NULL,
    account_tutorial  TEXT         NULL COMMENT '如何获取 AI 账号',
    token_tutorial    TEXT         NULL COMMENT '如何获取 Session Token',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO service_type_config (service_type, display_name, account_tutorial, token_tutorial) VALUES
(
    'CHATGPT',
    'ChatGPT',
    '1. 打开 https://chat.openai.com 并登录\n2. 在设置 → 账户中确认登录邮箱\n3. 将该邮箱填写到「AI 账号」栏',
    '1. 在浏览器登录 ChatGPT 后，按 F12 打开开发者工具\n2. 切到 Application → Cookies → chat.openai.com\n3. 复制 __Secure-next-auth.session-token 的值\n4. 粘贴到「Session Token」栏（请勿泄露给他人）'
),
(
    'CLAUDE',
    'Claude',
    '1. 打开 https://claude.ai 并登录\n2. 在账户设置中确认登录邮箱\n3. 将该邮箱填写到「AI 账号」栏',
    '1. 登录 Claude 后打开开发者工具 → Application → Cookies\n2. 找到 sessionKey 或相关会话 Cookie\n3. 复制完整值到「Session Token」栏'
),
(
    'GENERAL',
    '通用 AI 服务',
    '请填写您在目标 AI 服务上的登录账号（邮箱或用户名）。\n如需帮助，请联系客服。',
    '请按该 AI 服务官方文档获取 Session Token 或 API Key，并填入「Session Token」栏。\n请勿填写登录密码。'
);

ALTER TABLE subscription_order
    ADD COLUMN account_token_enc VARCHAR(512) NULL AFTER target_account_enc;

UPDATE ai_service_product
SET required_fields_json = JSON_ARRAY(
    JSON_OBJECT('key', 'target_account', 'label', 'AI 账号', 'type', 'text', 'required', true),
    JSON_OBJECT('key', 'account_token', 'label', 'Session Token', 'type', 'password', 'required', true)
)
WHERE JSON_LENGTH(required_fields_json) = 1;

-- 示例产品与默认管理员（密码 changeme 由应用启动时 BCrypt 写入，此处仅占位）
INSERT INTO ai_service_product (product_code, name, service_type, official_price, sale_price, currency, period_days, status, required_fields_json, estimated_hours, refund_policy_text, compliance_notice, sort_order)
VALUES (
    'CHATGPT_PLUS',
    'ChatGPT Plus 订阅协助',
    'CHATGPT',
    20.00,
    168.00,
    'CNY',
    30,
    'ON_SHELF',
    '[{"key":"target_account","label":"AI 账号邮箱","type":"email","required":true}]',
    24,
    '履约失败可申请全额退款',
    '本平台不采集第三方 AI 服务密码，订阅操作需用户在官方渠道配合完成。',
    1
);

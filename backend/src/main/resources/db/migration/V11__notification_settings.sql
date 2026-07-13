-- 管理员邮件通知配置：离线客服提醒与订单支付完成提醒

INSERT INTO system_setting (setting_key, setting_value, description)
VALUES
    ('SUPPORT_OFFLINE_NOTIFY_EMAIL', '296629801@qq.com', '离线客服邮件提醒邮箱'),
    ('ORDER_PAID_NOTIFY_EMAIL', '296629801@qq.com', '订单支付完成邮件提醒邮箱'),
    ('ORDER_PAID_NOTIFY_ENABLED', 'true', '订单支付完成邮件提醒开关');

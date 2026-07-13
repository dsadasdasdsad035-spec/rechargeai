-- 支付单拆分订单金额与渠道支付金额：订单可按 USD 展示，虎皮椒渠道按 CNY 收款

ALTER TABLE payment_transaction
    ADD COLUMN currency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '渠道支付币种' AFTER amount,
    ADD COLUMN order_amount DECIMAL(12,2) NULL COMMENT '订单原始金额' AFTER currency,
    ADD COLUMN order_currency VARCHAR(16) NULL COMMENT '订单原始币种' AFTER order_amount,
    ADD COLUMN exchange_rate DECIMAL(18,6) NULL COMMENT '订单币种兑渠道币种汇率' AFTER order_currency;

UPDATE payment_transaction p
JOIN subscription_order o ON p.order_id = o.id
SET p.order_amount = o.amount,
    p.order_currency = o.currency
WHERE p.order_amount IS NULL;

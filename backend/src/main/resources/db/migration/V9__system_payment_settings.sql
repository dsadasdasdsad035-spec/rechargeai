-- 后台动态支付配置：美元兑人民币汇率

CREATE TABLE system_setting (
    setting_key    VARCHAR(64)  NOT NULL PRIMARY KEY,
    setting_value  VARCHAR(128) NOT NULL,
    description    VARCHAR(255) NULL,
    updated_by     BIGINT       NULL,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO system_setting (setting_key, setting_value, description)
VALUES ('USD_TO_CNY_RATE', '7.250000', '美元兑人民币支付汇率');

-- RechargeAi MVP 一期 schema

CREATE TABLE user_account (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_no         VARCHAR(32)  NOT NULL,
    phone           VARCHAR(32)  NULL,
    email           VARCHAR(128) NULL,
    password_hash   VARCHAR(255) NULL,
    nickname        VARCHAR(64)  NULL,
    avatar_url      VARCHAR(512) NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'NORMAL',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_no (user_no),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_service_product (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code         VARCHAR(64)   NOT NULL,
    name                 VARCHAR(128)  NOT NULL,
    service_type         VARCHAR(64)   NOT NULL,
    official_price       DECIMAL(12,2) NULL,
    sale_price           DECIMAL(12,2) NOT NULL,
    currency             VARCHAR(16)   NOT NULL DEFAULT 'CNY',
    period_days          INT           NOT NULL,
    status               VARCHAR(32)   NOT NULL DEFAULT 'OFF_SHELF',
    required_fields_json JSON          NOT NULL,
    estimated_hours      INT           NULL,
    refund_policy_text   TEXT          NULL,
    compliance_notice    TEXT          NULL,
    sort_order           INT           NOT NULL DEFAULT 0,
    created_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE subscription_order (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no             VARCHAR(64)   NOT NULL,
    user_id              BIGINT        NOT NULL,
    product_id           BIGINT        NOT NULL,
    amount               DECIMAL(12,2) NOT NULL,
    currency             VARCHAR(16)   NOT NULL DEFAULT 'CNY',
    target_account_enc   VARCHAR(512)  NOT NULL,
    order_status         VARCHAR(32)   NOT NULL,
    payment_status       VARCHAR(32)   NOT NULL,
    fulfillment_status   VARCHAR(32)   NOT NULL DEFAULT 'NOT_STARTED',
    paid_at              DATETIME(3)   NULL,
    expired_at           DATETIME(3)   NULL,
    completed_at         DATETIME(3)   NULL,
    created_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_created (user_id, created_at),
    KEY idx_user_product_active (user_id, product_id, order_status),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES ai_service_product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_transaction (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_no              VARCHAR(64)   NOT NULL,
    order_id                BIGINT        NOT NULL,
    channel                 VARCHAR(32)   NOT NULL,
    amount                  DECIMAL(12,2) NOT NULL,
    status                  VARCHAR(32)   NOT NULL,
    third_trade_no          VARCHAR(128)  NULL,
    callback_verified       BOOLEAN       NOT NULL DEFAULT FALSE,
    callback_raw_redacted   TEXT          NULL,
    created_at              DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    paid_at                 DATETIME(3)   NULL,
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_third_trade (channel, third_trade_no),
    KEY idx_order_id (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES subscription_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(64)  NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

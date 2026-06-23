-- RechargeAi 二期：履约、退款、RBAC、审计、通知 Outbox

CREATE TABLE admin_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(32)  NOT NULL,
    role_name   VARCHAR(64)  NOT NULL,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_user_role (
    admin_user_id BIGINT NOT NULL,
    role_id       BIGINT NOT NULL,
    PRIMARY KEY (admin_user_id, role_id),
    CONSTRAINT fk_aur_user FOREIGN KEY (admin_user_id) REFERENCES admin_user (id),
    CONSTRAINT fk_aur_role FOREIGN KEY (role_id) REFERENCES admin_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO admin_role (role_code, role_name) VALUES
    ('SUPER_ADMIN', '超级管理员'),
    ('OPS_ADMIN', '运营管理员'),
    ('CUSTOMER_SERVICE', '客服'),
    ('FINANCE', '财务'),
    ('AUDIT_READONLY', '只读审计');

CREATE TABLE fulfillment_task (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no              VARCHAR(64)   NOT NULL,
    order_id             BIGINT        NOT NULL,
    status               VARCHAR(32)   NOT NULL,
    assignee_admin_id    BIGINT        NULL,
    subscription_start   DATETIME(3)   NULL,
    subscription_end     DATETIME(3)   NULL,
    failure_reason       VARCHAR(512)  NULL,
    created_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_task_no (task_no),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_status (status),
    CONSTRAINT fk_ft_order FOREIGN KEY (order_id) REFERENCES subscription_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fulfillment_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT        NOT NULL,
    log_type        VARCHAR(32)   NOT NULL,
    content         TEXT          NOT NULL,
    user_visible    BOOLEAN       NOT NULL DEFAULT FALSE,
    operator_id     BIGINT        NULL,
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_task_id (task_id),
    CONSTRAINT fk_fl_task FOREIGN KEY (task_id) REFERENCES fulfillment_task (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refund_request (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_no         VARCHAR(64)   NOT NULL,
    order_id          BIGINT        NOT NULL,
    user_id           BIGINT        NOT NULL,
    amount            DECIMAL(12,2) NOT NULL,
    status            VARCHAR(32)   NOT NULL,
    apply_reason      VARCHAR(512)  NOT NULL,
    review_comment    VARCHAR(512)  NULL,
    reviewer_admin_id BIGINT        NULL,
    channel_refund_no VARCHAR(128)  NULL,
    created_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_order_id (order_id),
    KEY idx_status (status),
    CONSTRAINT fk_rr_order FOREIGN KEY (order_id) REFERENCES subscription_order (id),
    CONSTRAINT fk_rr_user FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_operation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id     BIGINT        NOT NULL,
    operation_type  VARCHAR(64)   NOT NULL,
    target_type     VARCHAR(64)   NOT NULL,
    target_id       VARCHAR(64)   NOT NULL,
    before_value    TEXT          NULL,
    after_value     TEXT          NULL,
    reason          VARCHAR(512)  NULL,
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_operator (operator_id),
    KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_event (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type   VARCHAR(64)   NOT NULL,
    payload_json JSON          NOT NULL,
    status       VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    retry_count  INT           NOT NULL DEFAULT 0,
    next_run_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_status_next (status, next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

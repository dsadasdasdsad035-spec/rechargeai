-- RechargeAi 在线客服：会话与消息留痕

CREATE TABLE support_session (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_no          VARCHAR(64)   NOT NULL,
    user_id             BIGINT        NOT NULL,
    subject             VARCHAR(128)  NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    last_message        VARCHAR(512)  NULL,
    unread_user_count   INT           NOT NULL DEFAULT 0,
    unread_admin_count  INT           NOT NULL DEFAULT 0,
    last_message_at     DATETIME(3)   NULL,
    created_at          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_support_session_no (session_no),
    KEY idx_support_session_user (user_id, updated_at),
    KEY idx_support_session_status (status, updated_at),
    CONSTRAINT fk_support_session_user FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE support_message (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id     BIGINT       NOT NULL,
    sender_type    VARCHAR(16)  NOT NULL,
    sender_id      BIGINT       NOT NULL,
    content        TEXT         NOT NULL,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_support_message_session (session_id, created_at),
    CONSTRAINT fk_support_message_session FOREIGN KEY (session_id) REFERENCES support_session (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

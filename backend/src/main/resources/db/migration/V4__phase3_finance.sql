-- RechargeAi 三期：平台资金账本、提现、收款账户

CREATE TABLE platform_ledger_entry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_no        VARCHAR(64)   NOT NULL,
    entry_type      VARCHAR(32)   NOT NULL COMMENT 'SETTLE/REFUND/WITHDRAW_FREEZE/WITHDRAW_RELEASE/WITHDRAW_COMPLETE',
    amount          DECIMAL(12,2) NOT NULL COMMENT '正数入账、负数出账（影响可提现余额）',
    balance_after   DECIMAL(12,2) NOT NULL COMMENT '记账后可用余额',
    ref_type        VARCHAR(32)   NOT NULL COMMENT 'ORDER/REFUND/WITHDRAWAL',
    ref_id          VARCHAR(64)   NOT NULL,
    order_id        BIGINT        NULL,
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_entry_no (entry_no),
    UNIQUE KEY uk_entry_idempotent (entry_type, ref_type, ref_id),
    KEY idx_order_id (order_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payout_account (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_name    VARCHAR(128)  NOT NULL COMMENT '户名',
    bank_name       VARCHAR(128)  NOT NULL COMMENT '开户行',
    account_no_enc  VARCHAR(512)  NOT NULL COMMENT '账号 AES 加密',
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE withdrawal_request (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    withdrawal_no           VARCHAR(64)   NOT NULL,
    amount                  DECIMAL(12,2) NOT NULL,
    actual_amount           DECIMAL(12,2) NULL,
    status                  VARCHAR(32)   NOT NULL,
    payout_account_id       BIGINT        NOT NULL,
    applicant_admin_id      BIGINT        NOT NULL,
    approver_admin_id       BIGINT        NULL,
    payout_confirmer_id     BIGINT        NULL,
    external_voucher_no     VARCHAR(128)  NULL,
    remark                  VARCHAR(512)  NULL,
    reject_reason           VARCHAR(512)  NULL,
    variance_reason         VARCHAR(512)  NULL,
    applied_at              DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    approved_at             DATETIME(3)   NULL,
    paid_at                 DATETIME(3)   NULL,
    created_at              DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_withdrawal_no (withdrawal_no),
    KEY idx_status (status),
    KEY idx_applicant (applicant_admin_id),
    CONSTRAINT fk_wr_payout FOREIGN KEY (payout_account_id) REFERENCES payout_account (id),
    CONSTRAINT fk_wr_applicant FOREIGN KEY (applicant_admin_id) REFERENCES admin_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 账本写入串行化：固定单行锁，避免并发 FOR UPDATE 最新流水导致死锁

CREATE TABLE platform_ledger_lock (
    id         TINYINT      NOT NULL PRIMARY KEY,
    updated_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO platform_ledger_lock (id) VALUES (1);

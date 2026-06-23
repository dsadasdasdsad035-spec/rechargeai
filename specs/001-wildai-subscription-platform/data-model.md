# Data Model: RechargeAi 订阅助手平台

**Date**: 2026-06-17  
**Phase 1 Tables**: `user_account`, `ai_service_product`, `subscription_order`, `payment_transaction`, `admin_user`（一期简化后台账号）

**Phase 2+ Tables** (designed, not implemented in Phase 1): `fulfillment_task`, `fulfillment_log`, `refund_request`, `admin_operation_log`, `outbox_event`

---

## Entity Relationship (Phase 1)

```text
user_account 1──* subscription_order *──1 ai_service_product
subscription_order 1──* payment_transaction
admin_user (standalone, Phase 1 basic auth)
```

---

## 1. user_account（用户账户）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主键 |
| user_no | VARCHAR(32) | UNIQUE, NOT NULL | 用户编号 |
| phone | VARCHAR(32) | UNIQUE, NULL | 手机号 |
| email | VARCHAR(128) | UNIQUE, NULL | 邮箱 |
| password_hash | VARCHAR(255) | NULL | BCrypt；手机验证码注册可为空 |
| nickname | VARCHAR(64) | NULL | 昵称 |
| avatar_url | VARCHAR(512) | NULL | 头像 URL |
| status | VARCHAR(32) | NOT NULL, DEFAULT 'NORMAL' | NORMAL / DISABLED / LOCKED |
| created_at | DATETIME(3) | NOT NULL | UTC |
| updated_at | DATETIME(3) | NOT NULL | UTC |

**Validation**:
- `phone` 与 `email` 至少一项非空
- `status != NORMAL` 时禁止创建订单/支付（FR-004）

**Indexes**: `uk_user_no`, `uk_phone`, `uk_email`

---

## 2. ai_service_product（AI 服务产品）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| product_code | VARCHAR(64) | UNIQUE, NOT NULL | 产品编码 |
| name | VARCHAR(128) | NOT NULL | 展示名称 |
| service_type | VARCHAR(64) | NOT NULL | CHATGPT / CLAUDE / GEMINI 等 |
| official_price | DECIMAL(12,2) | NULL | 官方参考价 |
| sale_price | DECIMAL(12,2) | NOT NULL | 平台售价 |
| currency | VARCHAR(16) | NOT NULL, DEFAULT 'CNY' | 币种 |
| period_days | INT | NOT NULL | 服务周期天数 |
| status | VARCHAR(32) | NOT NULL | ON_SHELF / OFF_SHELF |
| required_fields_json | JSON | NOT NULL | 下单必填字段配置 |
| estimated_hours | INT | NULL | 预计处理时长（小时，展示用） |
| refund_policy_text | TEXT | NULL | 退款政策文案 |
| compliance_notice | TEXT | NULL | 合规提示 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 排序 |
| created_at | DATETIME(3) | NOT NULL | |
| updated_at | DATETIME(3) | NOT NULL | |

**required_fields_json 示例**:
```json
[
  { "key": "target_account", "label": "AI 账号邮箱", "type": "email", "required": true }
]
```

---

## 3. subscription_order（订阅订单）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| order_no | VARCHAR(64) | UNIQUE, NOT NULL | 订单号 |
| user_id | BIGINT | FK, NOT NULL | 用户 |
| product_id | BIGINT | FK, NOT NULL | 产品 |
| amount | DECIMAL(12,2) | NOT NULL | 订单金额 |
| currency | VARCHAR(16) | NOT NULL | 币种 |
| target_account_enc | VARCHAR(512) | NOT NULL | AI 账号标识（加密） |
| order_status | VARCHAR(32) | NOT NULL | 见状态机 |
| payment_status | VARCHAR(32) | NOT NULL | 见状态机 |
| fulfillment_status | VARCHAR(32) | NOT NULL, DEFAULT 'NOT_STARTED' | 一期固定 NOT_STARTED |
| paid_at | DATETIME(3) | NULL | 支付时间 |
| expired_at | DATETIME(3) | NULL | 待支付过期时间（创建+15min） |
| completed_at | DATETIME(3) | NULL | 完成时间（二期） |
| created_at | DATETIME(3) | NOT NULL | |
| updated_at | DATETIME(3) | NOT NULL | |

**Indexes**: `uk_order_no`, `idx_user_created`, `idx_user_product_active`（用于重复订购校验）

### order_status（一期）

```text
CREATED → WAIT_PAY → PAID
WAIT_PAY → CLOSED (超时/取消)
```

二期扩展：`PAID → FULFILLING → SUCCESS | FAILED → REFUNDING → REFUNDED`

### payment_status（一期）

```text
UNPAID → PAYING → PAID
UNPAID/PAYING → PAY_FAILED
```

二期扩展：`PAID → REFUNDING → REFUNDED`

### 重复订购规则

存在 `user_id + product_id` 且 `order_status IN ('WAIT_PAY','PAID')` 的订单 → 拒绝新单（FR-011）

---

## 4. payment_transaction（支付流水）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| payment_no | VARCHAR(64) | UNIQUE, NOT NULL | 支付流水号 |
| order_id | BIGINT | FK, UNIQUE(一期主支付) | 订单 |
| channel | VARCHAR(32) | NOT NULL | WECHAT / ALIPAY |
| amount | DECIMAL(12,2) | NOT NULL | 支付金额 |
| status | VARCHAR(32) | NOT NULL | UNPAID/PAYING/PAID/PAY_FAILED |
| third_trade_no | VARCHAR(128) | UNIQUE, NULL | 渠道交易号（幂等键） |
| callback_verified | BOOLEAN | DEFAULT FALSE | 回调验签结果 |
| callback_raw_redacted | TEXT | NULL | 脱敏回调原文 |
| created_at | DATETIME(3) | NOT NULL | |
| paid_at | DATETIME(3) | NULL | |

**幂等**: `channel + third_trade_no` 重复回调直接返回成功，不重复更新（SC-004）

---

## 5. admin_user（管理账号，一期简化）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| username | VARCHAR(64) | UNIQUE | 登录名 |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt |
| display_name | VARCHAR(64) | NULL | 显示名 |
| status | VARCHAR(32) | NOT NULL | ACTIVE / DISABLED |
| created_at | DATETIME(3) | NOT NULL | |
| updated_at | DATETIME(3) | NOT NULL | |

一期无 RBAC 表；所有登录管理员可访问产品/用户/订单只读模块。

---

## Phase 2 预留实体（仅设计）

### fulfillment_task

支付成功后创建；状态：`PENDING → PROCESSING → SUCCESS | FAILED | WAIT_USER | WAIT_ADMIN`

### refund_request

仅 `fulfillment_status = FAILED` 时可创建；金额 = 订单实付全额。

### admin_operation_log

强制改状态、退款、封禁等敏感操作审计（二期 RBAC 启用）。

---

## 脱敏展示规则（API 层）

| 数据 | 规则 |
|------|------|
| 手机号 | `138****1234` |
| 邮箱 | `m***@example.com` |
| AI 账号 | 保留首尾各 2 字符，中间 `*` |
| 渠道交易号 | 仅后 6 位 |

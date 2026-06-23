# Implementation Plan: RechargeAi 订阅助手平台

**Branch**: `001-wildai-subscription-platform` | **Date**: 2026-06-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-wildai-subscription-platform/spec.md`

## Summary

构建 AI 服务订阅助手平台全生命周期能力，分四期交付。**一期（当前实现焦点）**：用户注册登录、浏览产品、下单支付、交易记录，以及后台产品/用户管理与订单只读查看。**二期**：履约任务、退款、通知、RBAC/审计。**三期**：运营统计、平台资金账本与后台提现（仅 `order_status = SUCCESS` 订单可结算）。采用 **Java 21 + Spring Boot 3 模块化单体** + **Vue 3 双前端**，MySQL 持久化，微信/支付宝支付回调幂等。

## Technical Context

**Language/Version**: Java 21（后端）, TypeScript 5.x（前端）  
**Primary Dependencies**: Spring Boot 3.3.x, Spring Data JPA, Spring Security, Flyway, Vue 3, Vite, Pinia, Element Plus  
**Storage**: MySQL 8（主库）, Redis 7（限流/可选分布式锁）  
**Testing**: JUnit 5, Mockito, MockMvc, Testcontainers  
**Target Platform**: Linux 服务器 / Docker；用户 H5 + 管理端 Web  
**Project Type**: Web 应用（backend + frontend-user + frontend-admin）  
**Performance Goals**: 交易记录列表 P95 < 2s（SC-002）；100 并发下单无明显失败（SC-005）  
**Constraints**: 不采集第三方 AI 密码；一期后台订单只读；重复订购一律拒绝；可提现仅含 `SUCCESS` 订单  
**Scale/Scope**: 一期约 15–20 API；二期 +25 API；三期资金模块 +10 API；4 张一期表 + 二期 5 表 + 三期 3 表

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Constitution ratified | ⚠️ PASS (waived) | `.specify/memory/constitution.md` 仍为模板，以 spec 澄清 + 合规边界为 gate |
| 合规：不存第三方密码 | ✅ PASS | FR-010/FR-025，加密存储 AI 账号标识 |
| 一期范围不膨胀 | ✅ PASS | 澄清锁定：一期无履约/退款/通知/审计/提现 |
| 测试可验证 | ✅ PASS | quickstart 与 SC-001~014 对应集成路径 |
| 复杂度可控 | ✅ PASS | 模块化单体，分阶段 Flyway 迁移 |

**Post-Design Re-check**: ✅ 通过 — 一期仅 4 业务表 + `admin_user`；二期/三期表在独立迁移脚本中引入，不提前实现。

## Project Structure

### Documentation (this feature)

```text
specs/001-wildai-subscription-platform/
├── plan.md              # 本文件
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── api-user-phase1.openapi.yaml
│   ├── api-admin-phase1.openapi.yaml
│   └── api-admin-phase3.openapi.yaml   # 三期资金/提现（设计）
└── tasks.md             # /speckit-tasks 输出
```

### Source Code (repository root)

```text
backend/
├── pom.xml
└── src/main/java/com/wildai/
    ├── WildAiApplication.java
    ├── common/          # 响应体、异常、脱敏、幂等
    ├── auth/            # 注册、登录、JWT
    ├── product/         # 产品 CRUD、上下架
    ├── order/           # 订单创建、列表、详情、超时关闭
    ├── payment/         # 支付单、渠道适配、回调
    ├── admin/           # 管理端 API、种子账号
    ├── user/            # 用户资料、封禁
    ├── fulfillment/   # 二期：履约任务与日志
    ├── refund/          # 二期：退款申请与渠道退款
    ├── notify/          # 二期：通知与 Outbox
    └── finance/         # 三期：账本、提现、收款账户
└── src/main/resources/
    ├── application.yml
    └── db/migration/    # V1 phase1, V2 phase2, V3 phase3

frontend-user/
├── src/pages/           # 产品列表、下单、transaction-record
└── ...

frontend-admin/
├── src/views/           # 产品、订单、用户；二期履约/退款；三期资金管理
└── ...

docker-compose.yml
```

**Structure Decision**: 绿field 三工程；后端按 `com.wildai.*` 分包的单体应用，二期/三期在同一仓库扩展模块，避免微服务过早拆分。

## Phase 0: Research

已完成 → [research.md](./research.md)（含 2026-06-23 澄清结论）

关键决策：
- 模块化单体 + MySQL 8 + JPA
- 一期不建 fulfillment/refund/finance 表
- JWT 认证；管理端一期无 RBAC
- 支付回调幂等键 `channel + thirdTradeNo`
- 订单履约成功 `order_status = SUCCESS`（非 `COMPLETED`）
- 可提现/运营收入仅统计 `SUCCESS` 订单
- 提现收款账户必选 `PayoutAccount` 预设列表
- 退款审核：运营管理员与财务同等权限

## Phase 1: Design & Contracts

已完成产物：
- [data-model.md](./data-model.md) — 实体、状态机、索引、脱敏、三期表设计
- [contracts/api-user-phase1.openapi.yaml](./contracts/api-user-phase1.openapi.yaml)
- [contracts/api-admin-phase1.openapi.yaml](./contracts/api-admin-phase1.openapi.yaml)
- [contracts/api-admin-phase3.openapi.yaml](./contracts/api-admin-phase3.openapi.yaml) — 三期设计契约
- [quickstart.md](./quickstart.md)

### 一期模块实现要点

| 模块 | 一期职责 | 排除 |
|------|----------|------|
| auth | 手机/邮箱注册登录、JWT、限流 | Google OAuth |
| product | CRUD、上下架、规则展示 | — |
| order | 创建、列表、详情、15min 超时关闭、重复订购校验 | 履约状态变更 |
| payment | 微信/支付宝下单、回调验签幂等 | 退款 |
| admin | 产品/用户写、订单只读+导出 | 改订单状态、RBAC、资金 |
| user | 资料、封禁 | — |

### 状态机（一期，已实现）

**订单**: `WAIT_PAY → PAID | CLOSED`  
**支付**: `UNPAID → PAYING → PAID | PAY_FAILED`  
**履约**: 固定 `NOT_STARTED`

### 状态机（二期，设计）

**订单**: `PAID → FULFILLING → SUCCESS | FAILED → REFUNDING → REFUNDED`  
**履约任务**: `PENDING → PROCESSING → SUCCESS | FAILED | WAIT_USER | WAIT_ADMIN`  
**退款**: 仅 `FAILED` 订单可申请；运营/财务可审批

### 状态机（三期，设计）

**账本**: 订单 `SUCCESS` 时 `SETTLE`；退款 `REFUND`；提现冻结/释放/完成  
**提现单**: `PENDING_APPROVAL → APPROVED | REJECTED | CANCELLED → COMPLETED | PAYOUT_TIMEOUT`  
**可提现**: `SUCCESS` 订单实收 - 已退款 - 提现冻结 - 已提现

### 定时任务

| 任务 | 频率 | 阶段 | 说明 |
|------|------|------|------|
| closeExpiredOrders | 1 min | 一期 | `WAIT_PAY` 过期 → `CLOSED` |
| reconcilePendingPayments | 5 min | 一期 | `PAYING` 补偿查单（可选） |
| notifyOutboxDispatcher | 30 s | 二期 | Outbox 通知重试 |
| withdrawalPayoutTimeoutCheck | 1 h | 三期 | `APPROVED` 超 7 天 → `PAYOUT_TIMEOUT` |

### 安全

- BCrypt 密码；AES 加密 `target_account`、收款账号
- 回调验签 + 金额校验
- 管理端与用户端 JWT 分离（`aud` 区分）
- 二期 RBAC；提现职责分离（财务申请、超管审批、财务打款确认）
- API 限流：登录、验证码、创建订单、提现申请

## Phase 2 规划预览（不在本期实现）

- Flyway `V2__phase2_*.sql`：`fulfillment_task`、`fulfillment_log`、`refund_request`、`admin_operation_log`、`outbox_event`、RBAC 表
- `fulfillment` / `refund` / `notify` 模块
- 支付成功创建履约任务；`order_status` 流转至 `FULFILLING`
- 渠道退款；运营+财务退款审批
- 契约：`api-user-phase2.openapi.yaml`、`api-admin-phase2.openapi.yaml`

## Phase 3 规划预览（不在本期实现）

- Flyway `V3__phase3_finance.sql`：`platform_ledger_entry`、`withdrawal_request`、`payout_account`
- `finance` 模块：账本写入监听（`SUCCESS`/`REFUND`/提现事件）、资金概览、提现流程
- 管理端「资金管理」页面；`PayoutAccount` 超管维护
- 契约：[api-admin-phase3.openapi.yaml](./contracts/api-admin-phase3.openapi.yaml)
- 实现顺序：账本事件 → 概览 API → 提现 CRUD/审批 → 导出与超时告警

## Complexity Tracking

> 无 Constitution 违规需 justification。

| 项 | 说明 |
|----|------|
| 三工程 | 用户端/管理端/后端职责清晰 |
| 支付双渠道 | FR-016，抽象 `PaymentChannelAdapter` |
| 分阶段 Flyway | 避免一期表膨胀，账本与提现延至三期 |

## Implementation Order（建议）

### 一期（当前）

1. 基础设施：docker-compose、Flyway V1、通用响应/异常  
2. auth + user  
3. product  
4. order + 超时任务  
5. payment + 回调幂等  
6. frontend-user 下单流 + `/transaction-record`  
7. admin 产品/用户/订单只读 + 导出  
8. 集成测试 + quickstart 验证  

### 二期

1. RBAC + 审计日志  
2. fulfillment 模块 + 支付成功钩子  
3. refund 模块 + 渠道退款  
4. notify Outbox  
5. 管理端履约/退款页面 + 用户端进度/退款入口  

### 三期

1. `platform_ledger_entry` + 订单 `SUCCESS`/退款事件监听器  
2. `payout_account` CRUD（超管）  
3. `withdrawal_request` 申请/审批/打款  
4. 资金概览 + 运营统计（`SUCCESS` 收入口径）  
5. 导出、超时告警、对账测试（SC-011~014）  

## Artifacts Generated

| 文件 | 状态 |
|------|------|
| plan.md | ✅ 2026-06-23 更新 |
| research.md | ✅ 2026-06-23 更新 |
| data-model.md | ✅ |
| quickstart.md | ✅ |
| contracts/api-user-phase1.openapi.yaml | ✅ |
| contracts/api-admin-phase1.openapi.yaml | ✅ |
| contracts/api-admin-phase3.openapi.yaml | ✅ 新增 |

**Suggested next command**: `/speckit-tasks`（一期剩余任务）或按二期/三期规划拆分新 feature 目录

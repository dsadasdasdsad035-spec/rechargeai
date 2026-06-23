# Implementation Plan: RechargeAi 订阅助手平台

**Branch**: `001-wildai-subscription-platform` | **Date**: 2026-06-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-wildai-subscription-platform/spec.md`

## Summary

构建 AI 服务订阅助手平台的 **MVP 一期订单闭环**：用户注册登录、浏览产品、下单支付、交易记录，以及后台产品/用户管理与订单只读查看。采用 **Java 21 + Spring Boot 3 模块化单体** + **Vue 3 双前端**，MySQL 持久化，微信/支付宝支付回调幂等。一期支付成功后订单止于 `PAID`，不创建履约任务（澄清结论）。

## Technical Context

**Language/Version**: Java 21（后端）, TypeScript 5.x（前端）  
**Primary Dependencies**: Spring Boot 3.3.x, Spring Data JPA, Spring Security, Flyway, Vue 3, Vite, Pinia, Element Plus  
**Storage**: MySQL 8（主库）, Redis 7（限流/可选分布式锁）  
**Testing**: JUnit 5, Mockito, MockMvc, Testcontainers  
**Target Platform**: Linux 服务器 / Docker；用户 H5 + 管理端 Web  
**Project Type**: Web 应用（backend + frontend-user + frontend-admin）  
**Performance Goals**: 交易记录列表 P95 < 2s（SC-002）；100 并发下单无明显失败（SC-005）  
**Constraints**: 不采集第三方 AI 密码；一期后台订单只读；重复订购一律拒绝  
**Scale/Scope**: MVP 一期约 15–20 个 API；4 张核心业务表；2 个前端应用

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Constitution ratified | ⚠️ PASS (waived) | `.specify/memory/constitution.md` 仍为模板，以 spec 澄清 + 设计文档合规边界为 gate |
| 合规：不存第三方密码 | ✅ PASS | FR-010/FR-025，加密存储 AI 账号标识 |
| 一期范围不膨胀 | ✅ PASS | 澄清锁定：无履约/退款/通知/审计 |
| 测试可验证 | ✅ PASS | quickstart 与 SC-001~010 对应集成路径 |
| 复杂度可控 | ✅ PASS | 模块化单体，无微服务 |

**Post-Design Re-check**: ✅ 通过 — 一期仅 4 表 + 简化 admin_user，无二期表提前实现。

## Project Structure

### Documentation (this feature)

```text
specs/001-wildai-subscription-platform/
├── plan.md              # 本文件
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/           # Phase 1 OpenAPI
│   ├── api-user-phase1.openapi.yaml
│   └── api-admin-phase1.openapi.yaml
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
    └── user/            # 用户资料、封禁
└── src/main/resources/
    ├── application.yml
    └── db/migration/    # Flyway V1 phase1

frontend-user/
├── src/
│   ├── pages/           # 产品列表、下单、transaction-record
│   ├── components/
│   ├── services/        # API client
│   └── stores/
└── tests/

frontend-admin/
├── src/
│   ├── views/           # 产品、订单(只读)、用户
│   └── ...
└── tests/

docker-compose.yml       # mysql, redis, optional nginx
```

**Structure Decision**: 绿field 三工程结构；后端按设计文档 `com.wildai.*` 分包的单体应用，便于二期在同一仓库扩展 fulfillment/refund 模块。

## Phase 0: Research

已完成 → [research.md](./research.md)

关键决策：
- 模块化单体 + MySQL 8 + JPA
- 一期不建 fulfillment/refund 表
- JWT 认证；管理端一期无 RBAC
- 支付回调幂等键 `channel + thirdTradeNo`

## Phase 1: Design & Contracts

已完成产物：
- [data-model.md](./data-model.md) — 实体、状态机、索引、脱敏规则
- [contracts/api-user-phase1.openapi.yaml](./contracts/api-user-phase1.openapi.yaml)
- [contracts/api-admin-phase1.openapi.yaml](./contracts/api-admin-phase1.openapi.yaml)
- [quickstart.md](./quickstart.md)

### 一期模块实现要点

| 模块 | 一期职责 | 排除 |
|------|----------|------|
| auth | 手机/邮箱注册登录、JWT、限流 | Google OAuth |
| product | CRUD、上下架、规则展示 | — |
| order | 创建、列表、详情、15min 超时关闭、重复订购校验 | 履约状态变更 |
| payment | 微信/支付宝下单、回调验签幂等 | 退款 |
| admin | 产品/用户写、订单只读+导出 | 改订单状态、RBAC |
| user | 资料、封禁 | — |

### 状态机（一期）

**订单**: `CREATED → WAIT_PAY → PAID | CLOSED`  
**支付**: `UNPAID → PAYING → PAID | PAY_FAILED`  
**履约**: 固定 `NOT_STARTED`（字段预留）

### 定时任务

| 任务 | 频率 | 说明 |
|------|------|------|
| closeExpiredOrders | 1 min | WAIT_PAY 且 expired_at 过期 → CLOSED |
| reconcilePendingPayments | 5 min | PAYING 超时查单（可选一期末尾） |

### 安全

- BCrypt 密码；AES 加密 `target_account`
- 回调验签 + 金额校验
- 管理端与用户端 JWT 签发分离（claim `aud` 区分）
- API 限流：登录、验证码、创建订单

## Phase 2 规划预览（不在本期实现）

- `fulfillment_task` / `fulfillment_log` + Outbox
- `refund_request` + 渠道退款（仅履约失败、全额）
- 通知服务（站内信/短信/邮件）
- RBAC + `admin_operation_log`
- 运营统计

## Complexity Tracking

> 无 Constitution 违规需 justification。

| 项 | 说明 |
|----|------|
| 三工程 | 用户端/管理端/后端职责清晰，符合 Web 应用惯例 |
| 支付双渠道 | 规格 FR-016 要求，抽象 `PaymentChannelAdapter` |

## Implementation Order（建议）

1. **基础设施**: docker-compose、Flyway V1、通用响应/异常
2. **auth + user**: 注册登录 JWT
3. **product**: 产品 CRUD + 用户端列表/详情
4. **order**: 创建、重复校验、超时任务
5. **payment**: 渠道适配、回调、幂等
6. **frontend-user**: 下单流 + `/transaction-record`
7. **admin**: 产品/用户/订单只读 + 导出
8. **集成测试 + quickstart 验证**

## Artifacts Generated

| 文件 | 状态 |
|------|------|
| plan.md | ✅ |
| research.md | ✅ |
| data-model.md | ✅ |
| quickstart.md | ✅ |
| contracts/* | ✅ |

**Suggested next command**: `/speckit-tasks`

# Research: WildAI 订阅助手平台

**Date**: 2026-06-17  
**Feature**: `001-wildai-subscription-platform`  
**Scope**: MVP 一期（订单闭环）

## 1. 架构风格

**Decision**: 模块化单体（Modular Monolith），单 Spring Boot 应用按领域分包，用户端与管理端分离前端工程。

**Rationale**:
- 规格澄清明确一期为订单闭环，团队规模与流量预期适合单体起步
- 设计文档已规划 `com.wildai.*` 模块边界，便于二期拆分为微服务
- 降低早期运维与分布式事务复杂度

**Alternatives considered**:
- 微服务：过早引入服务网格、分布式事务与 Outbox 跨服务复杂度
- 全栈 Next.js：与设计文档 Java/Spring 技术栈及企业交付习惯不一致

---

## 2. 后端技术栈

**Decision**: Java 21 + Spring Boot 3.3.x + Spring Data JPA + MySQL 8 + Redis 7

**Rationale**:
- 与设计文档（DDD V1.0）一致
- JPA 适合订单/支付等领域模型与状态机
- MySQL 8 在企业环境部署成熟；金额用 `DECIMAL(12,2)`

**Alternatives considered**:
- PostgreSQL：功能更强但团队/运维环境以 MySQL 为主时可二期评估
- MyBatis：复杂报表场景可局部引入 QueryDSL/SQL，一期 JPA 足够

---

## 3. 前端技术栈

**Decision**:
- 用户端：`Vue 3` + `TypeScript` + `Vite` + `Pinia` + 移动端优先 H5 布局
- 管理端：`Vue 3` + `TypeScript` + `Element Plus`

**Rationale**:
- 设计文档推荐 Vue 3 生态，Element Plus 适合后台表格/筛选/导出
- 用户端 `/transaction-record` 需响应式卡片列表，Vue 3 组合式 API 便于维护

**Alternatives considered**:
- React：可行但无强制需求，保持与设计文档一致

---

## 4. 认证方案

**Decision**:
- 用户端：JWT（Access Token 短有效期 + Refresh Token）
- 管理端：JWT + 一期简单账号密码（细粒度 RBAC 二期）

**Rationale**:
- 规格明确一期后台仅基础登录鉴权
- 无状态 JWT 便于 H5 与 API 分离部署

**Alternatives considered**:
- Session Cookie：H5 跨域与多端场景 JWT 更灵活

---

## 5. 支付集成

**Decision**: 微信支付 Native/JSAPI + 支付宝当面付/电脑网站支付；回调验签 + 幂等键（`channel + thirdTradeNo`）

**Rationale**:
- 规格 FR-016~FR-018 要求双渠道、二维码/等价入口、幂等回调
- 一期支付成功后订单状态置 `PAID`，**不**创建履约任务（澄清结论）

**Alternatives considered**:
- Mock 支付：dev/test 环境使用，生产接沙箱/正式渠道

---

## 6. 订单超时关闭

**Decision**: Spring `@Scheduled` 或 Redis 延迟队列，每分钟扫描 `WAIT_PAY` 且 `expired_at < now` 的订单，置 `CLOSED`

**Rationale**:
- FR-012 要求 15 分钟未支付自动关闭
- 一期无 MQ 硬性要求；Redis 可选用于分布式锁避免重复关闭

**Alternatives considered**:
- 纯 DB 定时任务：最简单，一期足够

---

## 7. 敏感数据存储

**Decision**:
- 平台密码：BCrypt
- AI 账号标识：AES-256-GCM 加密存储，展示层脱敏
- 支付回调原文：入库脱敏（掩码密钥/卡号段）

**Rationale**:
- 规格 FR-002、FR-015 与设计文档安全章节一致
- **禁止**采集第三方 AI 服务密码

---

## 8. 重复订购校验

**Decision**: 创建订单前查询 `user_id + product_id` 是否存在 `order_status IN (WAIT_PAY, PAID)` 且未 `CLOSED`/`REFUNDED` 的订单，存在则拒绝（409）

**Rationale**:
- 澄清结论：一律拒绝重复订购，不按产品配置续费

---

## 9. 一期数据表范围

**Decision**: 一期实现 `user_account`、`ai_service_product`、`subscription_order`、`payment_transaction`；**不建** `fulfillment_*`、`refund_request`、`admin_operation_log` 表（二期迁移脚本预留）

**Rationale**:
- 严格对齐澄清后的一期边界
- `subscription_order` 仍保留 `fulfillment_status` 字段，一期固定为 `NOT_STARTED` 或 null

---

## 10. 部署与本地开发

**Decision**: Docker Compose 编排 MySQL + Redis + backend + 双前端；Nginx 反向代理 `/api` 与静态资源

**Rationale**:
- 绿field 项目需可复现本地环境
- quickstart.md 提供一键启动路径

---

## 11. 测试策略

**Decision**:
- 单元测试：JUnit 5 + Mockito（领域服务、状态机、幂等）
- 集成测试：Testcontainers MySQL + MockMvc API
- 契约测试：支付回调验签与重复回调场景（SC-004）

**Rationale**:
- Constitution 模板未 ratify，采用设计文档测试章节作为基线

---

## 12. 已消除的 NEEDS CLARIFICATION

| 原未知项 | 结论 |
|----------|------|
| MVP 范围 | 一期仅订单闭环（澄清 Session 2026-06-17） |
| 重复订购 | 一律拒绝 |
| 后台订单 | 只读 + 导出 |
| 退款 | 二期；仅履约失败 + 全额 |
| 数据库选型 | MySQL 8 |
| 架构 | 模块化单体 |

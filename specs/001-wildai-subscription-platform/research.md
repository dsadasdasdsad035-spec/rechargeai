# Research: RechargeAi 订阅助手平台

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

---

## 13. 可提现余额与订单状态（Session 2026-06-23）

**Decision**: 仅 `order_status = SUCCESS`（履约成功）订单实收计入可提现池；`PAID`/履约中、退款中均不计入。

**Rationale**:
- 防止履约完成前出金，保护用户资金安全
- 与现有 `data-model.md` 二期状态机 `SUCCESS` 枚举一致（代码一期尚未实现该状态）

**Alternatives considered**:
- 全部 `PAID` 即可提现：资金风险高，已拒绝
- T+N 天自动结算：与履约结果脱钩，已拒绝

---

## 14. 退款审批角色（Session 2026-06-23）

**Decision**: 运营管理员与财务角色均可审核退款（同等权限）；客服与只读审计不可审批。

**Rationale**: 中小团队运营需参与售后；仍通过 RBAC 限制客服/审计。

**Alternatives considered**:
- 仅财务：职责过窄，运营无法闭环售后

---

## 15. 提现收款账户（Session 2026-06-23）

**Decision**: 提现申请必须从超管维护的 `PayoutAccount` 预设列表选择；不支持临时手填。

**Rationale**: 降低录错账号风险；超管集中维护收款信息。

**Alternatives considered**:
- 每次手填：灵活但易错
- 预设+手填并存：规范性与灵活性折中，用户选择预设唯一

---

## 16. 运营统计收入口径（Session 2026-06-23）

**Decision**: FR-036 收入指标仅统计 `order_status = SUCCESS` 订单，与资金概览/可提现口径一致。

**Rationale**: 避免运营看板与财务可提现金额不一致。

**Alternatives considered**:
- 同时展示 PAID 总额与 SUCCESS 结算额：信息更丰富，用户选择仅 SUCCESS

---

## 17. 订单 vs 提现状态枚举（Session 2026-06-23）

**Decision**: 订单履约成功使用 `order_status = SUCCESS`；`COMPLETED` 仅用于提现单等业务对象。

**Rationale**: 对齐 `data-model.md` 与一期代码已有 `PAID`/`CLOSED` 命名风格；避免跨实体枚举冲突。

**Alternatives considered**:
- 订单也用 `COMPLETED`：与 data-model 及现有设计不一致

---

## 18. 平台资金与提现实现（三期）

**Decision**:
- 事件驱动账本：`SETTLE`（SUCCESS）、`REFUND`、`WITHDRAW_*`
- 三期手动打款登记，不对接渠道提现 API（四期评估）
- 财务申请 → 超管审批 → 财务确认打款

**Rationale**: 虎皮椒等聚合渠道无统一提现 API；人工登记满足合规留痕。

**Alternatives considered**:
- 渠道自动提现：三期复杂度高，延至四期

---

## 19. 渠道退款失败重试（Session 2026-06-23）

**Decision**: 审核通过调用渠道退款后，失败或超时无结果时系统自动重试最多 **3 次**（指数退避）；仍失败则 `RefundRequest.status = CHANNEL_REFUND_FAILED`，运营/财务可人工重试；订单/支付保持退款中，禁止乐观标已退款。

**Rationale**: 与通知 Outbox 重试模式一致，降低运营盯单成本，同时保留人工兜底。

**Alternatives considered**:
- 纯手动重试：运维负担高
- 回退待审核：重复审批无意义
- 乐观已退款：资金对账风险高

---

## 20. 无邮箱用户的通知策略（Session 2026-06-23）

**Decision**: 站内信必达；邮件尽力而为。用户未绑定有效邮箱时仅发站内信，跳过邮件且不报错、不阻断业务。

**Rationale**: 手机号注册为主场景，强制绑邮箱会抬高转化摩擦；站内信已覆盖用户端触达。

**Alternatives considered**:
- 强制绑邮箱：转化成本高
- 改发短信：超出二期 FR-031 范围

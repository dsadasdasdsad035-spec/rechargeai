# Tasks: RechargeAi 订阅助手平台

**Input**: Design documents from `/specs/001-wildai-subscription-platform/`  
**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/ ✅  
**Tests**: 规格未要求 TDD，本任务列表不含测试任务（集成验证见 Polish 阶段 quickstart）

**Organization**: 一期 US1/US2/US5/US6/US7 已完成；本文件含 **二期（US3/4/8/9/10）** 与 **三期（US11/12/13）** 待办任务

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: 用户故事标签（US1–US7 一期范围）

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 三工程骨架与本地开发环境

- [x] T001 创建仓库根目录 `docker-compose.yml`（MySQL 8 + Redis 7 服务定义）
- [x] T002 [P] 初始化 Spring Boot 工程 `backend/pom.xml`（Java 21、Spring Boot 3.3、JPA、Security、Flyway、Redis）
- [x] T003 [P] 创建启动类 `backend/src/main/java/com/wildai/WildAiApplication.java`
- [x] T004 [P] 初始化用户端 `frontend-user/`（Vite + Vue 3 + TypeScript + Pinia + Vue Router）
- [x] T005 [P] 初始化管理端 `frontend-admin/`（Vite + Vue 3 + TypeScript + Element Plus + Vue Router）
- [x] T006 [P] 添加后端配置模板 `backend/src/main/resources/application.yml` 与 `application-local.example.yml`
- [x] T007 [P] 配置用户端 API 代理 `frontend-user/vite.config.ts`（`/api` → `localhost:8080`）
- [x] T008 [P] 配置管理端 API 代理 `frontend-admin/vite.config.ts`（`/admin/api` → `localhost:8080`）

---

## Phase 2: Foundational（阻塞性前置）

**Purpose**: 所有用户故事依赖的公共能力 —— **完成前不得开始用户故事**

- [x] T009 编写 Flyway 一期 schema `backend/src/main/resources/db/migration/V1__phase1_schema.sql`（user_account、ai_service_product、subscription_order、payment_transaction、admin_user）
- [x] T010 [P] 实现统一响应体 `backend/src/main/java/com/wildai/common/dto/ApiResponse.java`
- [x] T011 [P] 实现全局异常处理 `backend/src/main/java/com/wildai/common/exception/GlobalExceptionHandler.java`
- [x] T012 [P] 实现业务错误码枚举 `backend/src/main/java/com/wildai/common/exception/ErrorCode.java`
- [x] T013 [P] 实现脱敏工具 `backend/src/main/java/com/wildai/common/util/DesensitizeUtil.java`
- [x] T014 [P] 实现 AES 加密工具 `backend/src/main/java/com/wildai/common/util/AesEncryptUtil.java`（AI 账号加密存储）
- [x] T015 实现 JWT 工具与配置 `backend/src/main/java/com/wildai/common/security/JwtTokenProvider.java`
- [x] T016 配置 Spring Security 过滤器链 `backend/src/main/java/com/wildai/common/security/SecurityConfig.java`（区分 user/admin `aud`）
- [x] T017 [P] 配置 Redis 连接 `backend/src/main/java/com/wildai/common/config/RedisConfig.java`
- [x] T018 [P] 实现限流组件 `backend/src/main/java/com/wildai/common/ratelimit/RateLimitService.java`（登录/验证码/下单）
- [x] T019 编写 admin 种子数据 `backend/src/main/resources/db/migration/V2__seed_admin.sql`
- [x] T020 [P] 实现用户端 HTTP 客户端 `frontend-user/src/services/http.ts`（Bearer、401 刷新）
- [x] T021 [P] 实现管理端 HTTP 客户端 `frontend-admin/src/services/http.ts`

**Checkpoint**: 基础架构就绪 —— 可开始用户故事实现

---

## Phase 3: User Story 5 - 用户注册与账户管理（Priority: P2）**（一期）**

**Goal**: 手机号/邮箱注册登录、JWT、资料管理、封禁拦截

**Independent Test**: 新用户注册登录后可访问受保护 API；DISABLED 用户创建订单返回 403

### Implementation

- [x] T022 [P] [US5] 创建实体 `backend/src/main/java/com/wildai/auth/domain/UserAccount.java`
- [x] T023 [P] [US5] 创建 Repository `backend/src/main/java/com/wildai/auth/repository/UserAccountRepository.java`
- [x] T024 [US5] 实现验证码服务（Mock/Redis）`backend/src/main/java/com/wildai/auth/service/VerifyCodeService.java`
- [x] T025 [US5] 实现 AuthService `backend/src/main/java/com/wildai/auth/service/AuthService.java`（注册、登录、刷新 Token）
- [x] T026 [US5] 实现 AuthController `backend/src/main/java/com/wildai/auth/web/AuthController.java`（`/api/auth/*`）
- [x] T027 [US5] 实现 UserService `backend/src/main/java/com/wildai/user/service/UserService.java`（资料更新、二次验证）
- [x] T028 [US5] 实现 UserController `backend/src/main/java/com/wildai/user/web/UserController.java`（`/api/users/me`）
- [x] T029 [P] [US5] 实现账户状态拦截 `backend/src/main/java/com/wildai/user/security/AccountStatusChecker.java`
- [x] T030 [P] [US5] 创建 auth store `frontend-user/src/stores/auth.ts`
- [x] T031 [P] [US5] 创建登录/注册页 `frontend-user/src/pages/LoginPage.vue` 与 `RegisterPage.vue`
- [x] T032 [US5] 配置用户端路由守卫 `frontend-user/src/router/index.ts`

**Checkpoint**: US5 可独立验证注册登录流程

---

## Phase 4: User Story 6 - 后台产品管理（Priority: P3）**（一期）**

**Goal**: 管理员配置产品；用户端浏览上架产品详情与合规提示

**Independent Test**: 后台上架产品后用户端可见；下架后用户端不可下单

### Implementation

- [x] T033 [P] [US6] 创建实体 `backend/src/main/java/com/wildai/product/domain/AiServiceProduct.java`
- [x] T034 [P] [US6] 创建 Repository `backend/src/main/java/com/wildai/product/repository/AiServiceProductRepository.java`
- [x] T035 [US6] 实现 ProductService `backend/src/main/java/com/wildai/product/service/ProductService.java`（上下架、required_fields 校验）
- [x] T036 [US6] 实现用户端 ProductController `backend/src/main/java/com/wildai/product/web/ProductController.java`（`GET /api/products`）
- [x] T037 [US6] 实现管理端 AdminProductController `backend/src/main/java/com/wildai/admin/web/AdminProductController.java`（CRUD + shelf）
- [x] T038 [P] [US6] 创建产品 DTO `backend/src/main/java/com/wildai/product/dto/ProductDetailDto.java`
- [x] T039 [P] [US6] 创建产品 API `frontend-user/src/services/productApi.ts`
- [x] T040 [P] [US6] 创建产品列表页 `frontend-user/src/pages/ProductListPage.vue`
- [x] T041 [US6] 创建产品详情页 `frontend-user/src/pages/ProductDetailPage.vue`（规则、退款说明、合规提示）
- [x] T042 [P] [US6] 创建管理端产品 API `frontend-admin/src/services/productApi.ts`
- [x] T043 [US6] 创建管理端产品视图 `frontend-admin/src/views/ProductListView.vue` 与 `ProductFormView.vue`

**Checkpoint**: US6 可独立验证产品上下架与用户端展示

---

## Phase 5: User Story 1 - 浏览并订购 AI 订阅服务（Priority: P1）**（一期）** 🎯 MVP 核心

**Goal**: 登录用户选产品、填 AI 账号、下单、微信/支付宝支付，支付成功后订单 `PAID`

**Independent Test**: 完成支付后交易记录可见「已支付」订单；重复订购返回 409；15 分钟未支付自动 CLOSED

### Implementation

- [x] T044 [P] [US1] 创建订单实体 `backend/src/main/java/com/wildai/order/domain/SubscriptionOrder.java`
- [x] T045 [P] [US1] 创建支付流水实体 `backend/src/main/java/com/wildai/payment/domain/PaymentTransaction.java`
- [x] T046 [P] [US1] 创建 OrderRepository `backend/src/main/java/com/wildai/order/repository/SubscriptionOrderRepository.java`
- [x] T047 [P] [US1] 创建 PaymentRepository `backend/src/main/java/com/wildai/payment/repository/PaymentTransactionRepository.java`
- [x] T048 [US1] 实现重复订购校验 `backend/src/main/java/com/wildai/order/service/DuplicateOrderChecker.java`
- [x] T049 [US1] 实现 OrderService `backend/src/main/java/com/wildai/order/service/OrderService.java`（创建、状态流转、expired_at）
- [x] T050 [US1] 定义支付渠道接口 `backend/src/main/java/com/wildai/payment/channel/PaymentChannelAdapter.java`
- [x] T051 [P] [US1] 实现 Mock 支付适配器 `backend/src/main/java/com/wildai/payment/channel/MockPaymentAdapter.java`
- [x] T052 [P] [US1] 实现微信支付适配器 `backend/src/main/java/com/wildai/payment/channel/WechatPaymentAdapter.java`
- [x] T053 [P] [US1] 实现支付宝适配器 `backend/src/main/java/com/wildai/payment/channel/AlipayPaymentAdapter.java`
- [x] T054 [US1] 实现 PaymentService `backend/src/main/java/com/wildai/payment/service/PaymentService.java`（创建支付单、回调幂等）
- [x] T055 [US1] 实现 OrderController `backend/src/main/java/com/wildai/order/web/OrderController.java`（`POST /api/orders`）
- [x] T056 [US1] 实现 PayController `backend/src/main/java/com/wildai/order/web/OrderPayController.java`（`POST /api/orders/{orderNo}/pay`）
- [x] T057 [US1] 实现支付回调 `backend/src/main/java/com/wildai/payment/web/PaymentCallbackController.java`（微信/支付宝 notify）
- [x] T058 [US1] 实现订单超时关闭定时任务 `backend/src/main/java/com/wildai/order/job/OrderExpireJob.java`
- [x] T059 [P] [US1] 创建订单 API `frontend-user/src/services/orderApi.ts`
- [x] T060 [US1] 创建下单页 `frontend-user/src/pages/CheckoutPage.vue`（动态 required_fields、禁止密码字段）
- [x] T061 [US1] 创建支付页 `frontend-user/src/pages/PaymentPage.vue`（二维码/支付参数展示）

**Checkpoint**: US1 可独立验证完整下单支付闭环

---

## Phase 6: User Story 2 - 查看交易记录与订单详情（Priority: P1）**（一期）**

**Goal**: `/transaction-record` 列表筛选、详情脱敏、空状态引导

**Independent Test**: 用户只看自己的订单；敏感字段脱敏；一期详情无履约日志/退款入口

### Implementation

- [x] T062 [US2] 扩展 OrderService 列表查询 `backend/src/main/java/com/wildai/order/service/OrderQueryService.java`（分页、多条件筛选）
- [x] T063 [US2] 扩展 OrderController `backend/src/main/java/com/wildai/order/web/OrderController.java`（`GET /api/orders`、`GET /api/orders/{orderNo}`）
- [x] T064 [US2] 实现订单详情 DTO 脱敏组装 `backend/src/main/java/com/wildai/order/dto/OrderDetailDto.java`
- [x] T065 [P] [US2] 创建交易记录页 `frontend-user/src/pages/TransactionRecordPage.vue`（路由 `/transaction-record`）
- [x] T066 [P] [US2] 创建订单详情组件 `frontend-user/src/components/OrderDetailDrawer.vue`
- [x] T067 [US2] 实现列表筛选与空状态 `frontend-user/src/pages/TransactionRecordPage.vue`（状态/时间/服务类型）
- [x] T068 [P] [US2] 移动端卡片布局样式 `frontend-user/src/pages/TransactionRecordPage.vue`

**Checkpoint**: US2 可独立验证交易记录与详情

---

## Phase 7: User Story 7 - 后台订单只读查看（Priority: P3）**（一期）**

**Goal**: 后台订单列表、详情、CSV 导出；**无**状态变更入口

**Independent Test**: 管理员可筛选导出；界面与 API 均不提供改状态操作

### Implementation

- [x] T069 [US7] 实现 AdminAuthController `backend/src/main/java/com/wildai/admin/web/AdminAuthController.java`（`/admin/api/auth/login`）
- [x] T070 [US7] 实现 AdminOrderQueryService `backend/src/main/java/com/wildai/admin/service/AdminOrderQueryService.java`
- [x] T071 [US7] 实现 AdminOrderController `backend/src/main/java/com/wildai/admin/web/AdminOrderController.java`（list/detail/export 只读）
- [x] T072 [US7] 实现 CSV 导出 `backend/src/main/java/com/wildai/admin/service/OrderExportService.java`
- [x] T073 [P] [US7] 创建管理端订单 API `frontend-admin/src/services/orderApi.ts`
- [x] T074 [US7] 创建订单只读列表 `frontend-admin/src/views/OrderListView.vue`（无编辑/改状态按钮）
- [x] T075 [US7] 创建订单详情只读页 `frontend-admin/src/views/OrderDetailView.vue`
- [x] T076 [US7] 实现管理端用户管理 `backend/src/main/java/com/wildai/admin/web/AdminUserController.java` + `frontend-admin/src/views/UserListView.vue`（封禁/解封，支撑 US5 验收）

**Checkpoint**: US7 + 用户封禁构成一期后台闭环

---

## Phase 8: Polish & Cross-Cutting（横切收尾）

**Purpose**: 非功能需求、文档与一期验收

- [x] T077 [P] 补充 Actuator 健康检查 `backend/src/main/resources/application.yml`（`/actuator/health`）
- [ ] T078 实现支付补偿查单任务（可选）`backend/src/main/java/com/wildai/payment/job/PaymentReconcileJob.java`
- [x] T079 [P] 补充 README `README.md`（指向 quickstart.md）
- [x] T080 按 `specs/001-wildai-subscription-platform/quickstart.md` 执行一期验收并记录结果（2026-06-18 API 冒烟通过：注册/登录→下单→Mock 支付→PAID→重复订购 409→后台只读）
- [x] T081 [P] 更新 `.cursor/rules/specify-rules.mdc` 构建命令为实际可运行状态

**Checkpoint**: 一期交付完成（SC-010）；二期起从 Phase 9 开始

---

## Phase 9: Foundational 二期（阻塞性前置）

**Purpose**: RBAC、审计、履约/退款/通知表结构；**完成前不得开始二期用户故事**

- [x] T082 编写 Flyway 二期 schema `backend/src/main/resources/db/migration/V3__phase2_schema.sql`（fulfillment_task、fulfillment_log、refund_request、admin_operation_log、outbox_event、RBAC 表）
- [x] T083 [P] 创建履约任务实体 `backend/src/main/java/com/wildai/fulfillment/domain/FulfillmentTask.java`
- [x] T084 [P] 创建履约日志实体 `backend/src/main/java/com/wildai/fulfillment/domain/FulfillmentLog.java`
- [x] T085 [P] 创建退款申请实体 `backend/src/main/java/com/wildai/refund/domain/RefundRequest.java`
- [x] T086 [P] 创建审计日志实体 `backend/src/main/java/com/wildai/admin/domain/AdminOperationLog.java`
- [x] T087 [P] 创建 Outbox 实体 `backend/src/main/java/com/wildai/notify/domain/OutboxEvent.java`
- [x] T088 实现 RBAC 服务 `backend/src/main/java/com/wildai/admin/service/RbacService.java`（超管/运营/客服/财务/只读审计）
- [x] T089 实现审计日志服务 `backend/src/main/java/com/wildai/admin/service/AuditLogService.java`
- [x] T090 扩展 SecurityConfig 方法级权限 `backend/src/main/java/com/wildai/common/security/SecurityConfig.java`（`@PreAuthorize`）
- [x] T091 支付成功钩子创建履约任务 `backend/src/main/java/com/wildai/payment/service/PaymentService.java`（`order_status`→`FULFILLING`）

**Checkpoint**: 二期基础表与服务就绪

---

## Phase 10: User Story 3 - 履约进度跟踪与补充资料（Priority: P2）**（二期）**

**Goal**: 用户查看履约进度、补充资料；支付后收到处理中通知

**Independent Test**: 支付成功后订单进入履约中；等待用户时可提交资料；详情可见脱敏履约日志

### Implementation

- [x] T092 [US3] 实现 FulfillmentService `backend/src/main/java/com/wildai/fulfillment/service/FulfillmentService.java`（状态机、用户可见日志）
- [x] T093 [US3] 实现用户端履约 API `backend/src/main/java/com/wildai/fulfillment/web/FulfillmentController.java`（补充资料、确认）
- [x] T094 [US3] 扩展 OrderDetailDto 含履约状态与日志 `backend/src/main/java/com/wildai/order/dto/OrderDetailDto.java`
- [x] T095 [US3] 实现通知 Outbox 派发 `backend/src/main/java/com/wildai/notify/service/NotifyService.java` + `notify/job/OutboxDispatchJob.java`
- [x] T096 [P] [US3] 扩展订单详情页履约区块 `frontend-user/src/components/OrderDetailDrawer.vue`
- [x] T097 [P] [US3] 创建补充资料表单组件 `frontend-user/src/components/FulfillmentSupplementForm.vue`

**Checkpoint**: US3 可独立验证用户侧履约跟踪

---

## Phase 11: User Story 8 - 后台订单与履约任务处理（Priority: P3）**（二期）**

**Goal**: 客服分派履约任务、登记订阅时间、标记成功/失败（`order_status`→`SUCCESS`/`FAILED`）

**Independent Test**: 标记成功后订单 `SUCCESS` 且用户收到通知；标记失败后可申请退款

### Implementation

- [x] T098 [US8] 实现 AdminFulfillmentService `backend/src/main/java/com/wildai/fulfillment/service/AdminFulfillmentService.java`（分派、备注、成功/失败）
- [x] T099 [US8] 实现管理端履约 API `backend/src/main/java/com/wildai/admin/web/AdminFulfillmentController.java`
- [x] T100 [US8] 成功时写入 `completed_at` 并置 `order_status=SUCCESS` `backend/src/main/java/com/wildai/fulfillment/service/AdminFulfillmentService.java`
- [x] T101 [P] [US8] 创建履约任务列表页 `frontend-admin/src/views/FulfillmentListView.vue`
- [x] T102 [P] [US8] 创建履约处理详情页 `frontend-admin/src/views/FulfillmentDetailView.vue`（登记订阅起止时间）

**Checkpoint**: US8 可独立验证后台履约闭环

---

## Phase 12: User Story 4 - 申请退款（Priority: P2）**（二期）**

**Goal**: 用户仅在履约失败订单发起全额退款申请

**Independent Test**: `FAILED` 可提交；其他状态拒绝；重复申请拒绝

### Implementation

- [x] T103 [US4] 实现 RefundService 用户侧 `backend/src/main/java/com/wildai/refund/service/RefundService.java`（申请校验 FR-027/030）
- [x] T104 [US4] 实现用户端退款 API `backend/src/main/java/com/wildai/refund/web/RefundController.java`
- [x] T105 [P] [US4] 订单详情增加退款入口 `frontend-user/src/components/OrderDetailDrawer.vue`（仅 FAILED）
- [x] T106 [P] [US4] 创建退款申请页/抽屉 `frontend-user/src/components/RefundApplyForm.vue`

**Checkpoint**: US4 可独立验证用户退款申请

---

## Phase 13: User Story 9 - 后台退款审核（Priority: P3）**（二期）**

**Goal**: 运营管理员与财务审核退款、发起渠道退款、跟踪结果

**Independent Test**: 审批通过后渠道退款完成；驳回展示意见；客服/审计不可审批

### Implementation

- [x] T107 [US9] 实现 AdminRefundService `backend/src/main/java/com/wildai/refund/service/AdminRefundService.java`（审核、渠道退款、审计）
- [x] T108 [US9] 实现管理端退款 API `backend/src/main/java/com/wildai/admin/web/AdminRefundController.java`
- [x] T109 [US9] 扩展支付适配器退款 `backend/src/main/java/com/wildai/payment/channel/PaymentChannelAdapter.java`（refund 方法）
- [x] T110 [P] [US9] 创建退款管理列表 `frontend-admin/src/views/RefundListView.vue`
- [x] T111 [P] [US9] 创建退款审核详情 `frontend-admin/src/views/RefundDetailView.vue`

**Checkpoint**: US9 可独立验证退款审核与渠道退款

---

## Phase 14: User Story 10 - 后台权限与审计（Priority: P4）**（二期）**

**Goal**: 角色权限控制；敏感操作 100% 审计可追溯

**Independent Test**: 只读审计无法改状态；强制改状态/退款/封禁有审计记录

### Implementation

- [x] T112 [US10] 实现管理员与角色 CRUD `backend/src/main/java/com/wildai/admin/web/AdminRoleController.java`
- [x] T113 [US10] 实现审计日志查询 API `backend/src/main/java/com/wildai/admin/web/AdminAuditLogController.java`
- [x] T114 [US10] 订单/支付/履约强制改状态入口（含审计）`backend/src/main/java/com/wildai/admin/web/AdminOrderMutationController.java`
- [x] T115 [P] [US10] 创建角色管理页 `frontend-admin/src/views/RoleListView.vue`
- [x] T116 [P] [US10] 创建审计日志页 `frontend-admin/src/views/AuditLogListView.vue`
- [x] T117 [US10] 编写二期契约 `specs/001-wildai-subscription-platform/contracts/api-admin-phase2.openapi.yaml`

**Checkpoint**: US10 完成二期安全基线

---

## Phase 15: Foundational 三期（阻塞性前置）

**Purpose**: 资金账本与提现表；监听 `SUCCESS`/退款事件

- [x] T118 编写 Flyway 三期 schema `backend/src/main/resources/db/migration/V4__phase3_finance.sql`（platform_ledger_entry、withdrawal_request、payout_account）
- [x] T119 [P] 创建账本流水实体 `backend/src/main/java/com/wildai/finance/domain/PlatformLedgerEntry.java`
- [x] T120 [P] 创建提现单实体 `backend/src/main/java/com/wildai/finance/domain/WithdrawalRequest.java`
- [x] T121 [P] 创建收款账户实体 `backend/src/main/java/com/wildai/finance/domain/PayoutAccount.java`
- [x] T122 实现 LedgerService `backend/src/main/java/com/wildai/finance/service/LedgerService.java`（SETTLE/REFUND/WITHDRAW_* 事件）
- [x] T123 订阅订单 SUCCESS 结算 `backend/src/main/java/com/wildai/finance/listener/OrderSettledLedgerListener.java`
- [x] T124 订阅退款完成扣账 `backend/src/main/java/com/wildai/finance/listener/RefundCompletedLedgerListener.java`

**Checkpoint**: 三期账本基础就绪

---

## Phase 16: User Story 11 - 平台资金概览（Priority: P3）**（三期）**

**Goal**: 财务查看已结算实收、可提现余额、账本明细

**Independent Test**: 概览数字与 `SUCCESS` 订单/退款抽样对账误差为 0（SC-011）

### Implementation

- [x] T125 [US11] 实现 FinanceOverviewService `backend/src/main/java/com/wildai/finance/service/FinanceOverviewService.java`
- [x] T126 [US11] 实现资金 API `backend/src/main/java/com/wildai/admin/web/AdminFinanceController.java`（overview + ledger）
- [x] T127 [US11] 实现运营统计 SUCCESS 收入 `backend/src/main/java/com/wildai/admin/service/AdminStatsService.java`（对齐 FR-036）
- [x] T128 [P] [US11] 创建资金管理概览页 `frontend-admin/src/views/FinanceOverviewView.vue`
- [x] T129 [P] [US11] 创建账本流水页 `frontend-admin/src/views/LedgerListView.vue`

**Checkpoint**: US11 可独立验证资金概览

---

## Phase 17: User Story 12 - 提现申请与审批（Priority: P3）**（三期）**

**Goal**: 财务从 `PayoutAccount` 发起提现；超管审批；职责分离

**Independent Test**: 驳回释放冻结；同人不可自审；无启用账户不可申请

### Implementation

- [x] T130 [US12] 实现 PayoutAccountService `backend/src/main/java/com/wildai/finance/service/PayoutAccountService.java`（超管 CRUD）
- [x] T131 [US12] 实现 WithdrawalService `backend/src/main/java/com/wildai/finance/service/WithdrawalService.java`（申请/审批/冻结余额）
- [x] T132 [US12] 实现收款账户 API `backend/src/main/java/com/wildai/admin/web/AdminPayoutAccountController.java`
- [x] T133 [US12] 实现提现 API `backend/src/main/java/com/wildai/admin/web/AdminWithdrawalController.java`（create/approve/reject/cancel）
- [x] T134 [P] [US12] 创建收款账户管理页 `frontend-admin/src/views/PayoutAccountListView.vue`
- [x] T135 [P] [US12] 创建提现申请/列表页 `frontend-admin/src/views/WithdrawalListView.vue`

**Checkpoint**: US12 可独立验证提现申请与审批

---

## Phase 18: User Story 13 - 打款确认与对账（Priority: P3）**（三期）**

**Goal**: 财务登记打款凭证；超时告警；导出

**Independent Test**: 打款完成后冻结转已提现；全流程审计可追溯（SC-012）

### Implementation

- [x] T136 [US13] 实现打款确认 `backend/src/main/java/com/wildai/finance/service/WithdrawalService.java`（confirmPayout、差额原因）
- [x] T137 [US13] 实现提现导出 `backend/src/main/java/com/wildai/admin/service/WithdrawalExportService.java`
- [x] T138 [US13] 实现打款超时任务 `backend/src/main/java/com/wildai/finance/job/WithdrawalPayoutTimeoutJob.java`
- [x] T139 [P] [US13] 创建打款确认对话框 `frontend-admin/src/components/WithdrawalPayoutDialog.vue`
- [x] T140 [US13] 三期验收脚本记录 `specs/001-wildai-subscription-platform/quickstart.md`（补充 US11–13 验证步骤）

**Checkpoint**: US13 完成三期资金提现闭环

---

## Phase 19: Polish 二期/三期（横切收尾）

- [x] T141 [P] 补充二期集成测试 `backend/src/test/java/com/wildai/integration/Phase2FlowIT.java`（履约→失败→退款）
- [x] T142 [P] 补充三期账本并发测试 `backend/src/test/java/com/wildai/finance/LedgerConcurrencyTest.java`（SC-013）
- [x] T143 更新 `specs/001-wildai-subscription-platform/contracts/api-admin-phase3.openapi.yaml` 与实现对齐
- [x] T144 按 quickstart 执行二期/三期端到端验收并记录结果

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup (Phase 1) → Foundational 一期 (Phase 2)
  → US5 → US6 → US1 → US2 → US7 → Polish 一期 (Phase 8) ✅

Foundational 二期 (Phase 9) [BLOCKS 二期/三期故事]
  → US3 → US8（履约后台）
  → US4 → US9（退款，依赖 US8 失败路径）
  → US10（RBAC/审计，可与 US9 并行部分 UI）

Foundational 三期 (Phase 15) [BLOCKS 三期故事，依赖二期 SUCCESS/退款]
  → US11 → US12 → US13 → Polish 二期/三期 (Phase 19)
```

### User Story Dependencies

| 故事 | 依赖 | 说明 |
|------|------|------|
| US5 | Foundational 一期 | 认证基础 |
| US6 | US5 | 管理端登录 |
| US1 | US5 + US6 | 下单支付 |
| US2 | US1 | 交易记录 |
| US7 | US1 | 后台只读 |
| US3 | Foundational 二期 | 用户履约跟踪 |
| US8 | US3 | 后台处理履约 |
| US4 | US8（FAILED） | 用户退款申请 |
| US9 | US4 + US10 部分 | 退款审核需 RBAC |
| US10 | Foundational 二期 | 权限与审计 |
| US11 | Foundational 三期 + US8 SUCCESS | 资金概览 |
| US12 | US11 + PayoutAccount | 提现申请 |
| US13 | US12 | 打款确认 |

### Parallel Opportunities

**Setup 阶段可并行**: T002–T008  
**Foundational 可并行**: T010–T014、T017–T018、T020–T021  
**US1 支付适配器可并行**: T051–T053  
**US2 前端可并行**: T065–T066、T068  
**跨故事并行（Foundational 完成后）**: US5 与 US6 后端可由不同开发者并行（注意 T019 admin seed）

### Parallel Example: US1

```bash
# 并行创建实体与 Repository
T044: backend/.../order/domain/SubscriptionOrder.java
T045: backend/.../payment/domain/PaymentTransaction.java
T046: backend/.../order/repository/SubscriptionOrderRepository.java
T047: backend/.../payment/repository/PaymentTransactionRepository.java

# 并行实现支付渠道适配器
T051: MockPaymentAdapter.java
T052: WechatPaymentAdapter.java
T053: AlipayPaymentAdapter.java
```

---

## Implementation Strategy

### MVP First（最小可演示）

1. Phase 1–2: Setup + Foundational  
2. Phase 3: US5（能登录）  
3. Phase 4: US6（有产品）  
4. Phase 5: US1（能下单支付）→ **STOP 可演示 MVP**  
5. Phase 6–7: US2 + US7 → 完整一期  
6. Phase 8: Polish + quickstart 验收

### 一期交付标准（SC-010）✅

用户可完成：**注册 → 浏览 → 下单支付 → 查看交易记录 → 后台订单只读管理**

### 二期交付标准

用户可完成：**支付 → 履约跟踪 → 失败退款 → 后台处理与审核**

### 三期交付标准（SC-011~014）

财务可完成：**SUCCESS 结算对账 → 提现申请 → 超管审批 → 打款登记**

### 任务统计

| 阶段 | 任务数 | 状态 |
|------|--------|------|
| Setup | 8 | ✅ |
| Foundational 一期 | 13 | ✅ |
| US5 | 11 | ✅ |
| US6 | 11 | ✅ |
| US1 | 18 | ✅ |
| US2 | 7 | ✅ |
| US7 | 8 | ✅ |
| Polish 一期 | 5 | 4/5（T078 可选待办） |
| Foundational 二期 | 10 | ✅ |
| US3 | 6 | ✅ |
| US8 | 5 | ✅ |
| US4 | 4 | ✅ |
| US9 | 5 | ✅ |
| US10 | 6 | ✅ |
| Foundational 三期 | 7 | 待办 |
| US11 | 5 | 待办 |
| US12 | 6 | 待办 |
| US13 | 5 | 待办 |
| Polish 二期/三期 | 4 | 待办 |
| **合计** | **144** | **100 完成 / 44 待办** |

### 按用户故事任务数（待办）

| 故事 | 待办任务 |
|------|----------|
| US11 | 5 |
| US12 | 6 |
| US13 | 5 |
| Foundational 三期 + Polish | 16 |

---

## Notes

- 所有任务描述含目标文件路径，便于 LLM 直接执行
- 一期支付成功后 **不** 创建履约任务（`fulfillment_status = NOT_STARTED`）；二期 T091 起创建履约任务
- 履约成功订单 `order_status = SUCCESS` 后计入可提现（三期 SETTLE）
- 提现必选 `PayoutAccount` 预设账户
- 重复订购一律拒绝（FR-011；二期含 `FULFILLING`）
- 后台订单一期 **只读**（FR-033a）；二期起 AdminOrderMutation
- 建议每完成一个 Checkpoint 提交一次 Git

**Suggested next command**: `/speckit-implement`（从 T082 Foundational 二期开始）或继续完成可选 T078

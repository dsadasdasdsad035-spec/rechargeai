package com.wildai.fulfillment.service;

import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.admin.service.AuditLogService;
import com.wildai.admin.service.RbacService;
import com.wildai.common.dto.PageResult;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.fulfillment.domain.FulfillmentLog;
import com.wildai.fulfillment.domain.FulfillmentTask;
import com.wildai.fulfillment.dto.AdminFulfillmentLogDto;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.AddNoteRequest;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.AssignRequest;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.MarkFailedRequest;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.MarkSuccessRequest;
import com.wildai.fulfillment.dto.AdminFulfillmentRequests.WaitUserRequest;
import com.wildai.fulfillment.dto.FulfillmentTaskDetailDto;
import com.wildai.fulfillment.dto.FulfillmentTaskSummaryDto;
import com.wildai.fulfillment.repository.FulfillmentLogRepository;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.finance.event.OrderSettledEvent;
import com.wildai.notify.service.NotifyService;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.payment.dto.PaymentAmountSnapshot;
import com.wildai.payment.service.PaymentAmountService;
import com.wildai.product.repository.AiServiceProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
public class AdminFulfillmentService {

    private static final Set<String> TERMINAL_STATUSES = Set.of("SUCCESS", "FAILED", "CANCELLED");

    private final FulfillmentTaskRepository taskRepo;
    private final FulfillmentLogRepository logRepo;
    private final SubscriptionOrderRepository orderRepo;
    private final AiServiceProductRepository productRepo;
    private final PaymentAmountService paymentAmountService;
    private final AdminUserRepository adminUserRepo;
    private final RbacService rbacService;
    private final AuditLogService auditLogService;
    private final NotifyService notifyService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminFulfillmentService(FulfillmentTaskRepository taskRepo,
                                   FulfillmentLogRepository logRepo,
                                   SubscriptionOrderRepository orderRepo,
                                   AiServiceProductRepository productRepo,
                                   PaymentAmountService paymentAmountService,
                                   AdminUserRepository adminUserRepo,
                                   RbacService rbacService,
                                   AuditLogService auditLogService,
                                   NotifyService notifyService,
                                   ApplicationEventPublisher eventPublisher) {
        this.taskRepo = taskRepo;
        this.logRepo = logRepo;
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.paymentAmountService = paymentAmountService;
        this.adminUserRepo = adminUserRepo;
        this.rbacService = rbacService;
        this.auditLogService = auditLogService;
        this.notifyService = notifyService;
        this.eventPublisher = eventPublisher;
    }

    public PageResult<FulfillmentTaskSummaryDto> listTasks(String status, Long assigneeAdminId,
                                                           String orderNo, int pageNo, int pageSize) {
        var page = taskRepo.searchAdminTasks(status, assigneeAdminId, orderNo, PageRequest.of(pageNo - 1, pageSize));
        var items = page.getContent().stream().map(this::toSummary).toList();
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), items);
    }

    public FulfillmentTaskDetailDto getTaskDetail(String taskNo) {
        FulfillmentTask task = requireTask(taskNo);
        return toDetail(task);
    }

    @Transactional
    public void assign(String taskNo, Long operatorId, AssignRequest req) {
        requireManagePermission(operatorId);
        FulfillmentTask task = requireTask(taskNo);
        requireActive(task);

        adminUserRepo.findById(req.assigneeAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "处理人不存在"));

        String before = task.getStatus();
        task.setAssigneeAdminId(req.assigneeAdminId());
        if ("PENDING".equals(task.getStatus())) {
            task.setStatus("PROCESSING");
        }
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);

        syncOrderFulfillmentStatus(task.getOrderId(), task.getStatus());
        appendAdminLog(task, operatorId, "ASSIGN",
                "任务已分派给 " + resolveAdminName(req.assigneeAdminId()), true);

        auditLogService.log(operatorId, "FULFILLMENT_ASSIGN", "FULFILLMENT_TASK", taskNo,
                before, task.getStatus(), "assignee=" + req.assigneeAdminId());
    }

    @Transactional
    public void addNote(String taskNo, Long operatorId, AddNoteRequest req) {
        requireManagePermission(operatorId);
        FulfillmentTask task = requireTask(taskNo);
        requireActive(task);
        if (req.content() == null || req.content().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "备注内容不能为空");
        }
        boolean visible = req.userVisible() == null || req.userVisible();
        appendAdminLog(task, operatorId, "ADMIN_NOTE", req.content().trim(), visible);
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);
    }

    @Transactional
    public void requestUserAction(String taskNo, Long operatorId, WaitUserRequest req) {
        requireManagePermission(operatorId);
        FulfillmentTask task = requireTask(taskNo);
        requireActive(task);

        String before = task.getStatus();
        task.setStatus("WAIT_USER");
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);

        syncOrderFulfillmentStatus(task.getOrderId(), "WAIT_USER");
        String instruction = req.instruction() != null && !req.instruction().isBlank()
                ? req.instruction().trim()
                : "请按指引完成操作后提交确认或补充资料";
        appendAdminLog(task, operatorId, "WAIT_USER", instruction, true);

        auditLogService.log(operatorId, "FULFILLMENT_WAIT_USER", "FULFILLMENT_TASK", taskNo,
                before, "WAIT_USER", instruction);
    }

    @Transactional
    public void markSuccess(String taskNo, Long operatorId, MarkSuccessRequest req) {
        requireManagePermission(operatorId);
        FulfillmentTask task = requireTask(taskNo);
        requireActive(task);
        if (req.subscriptionStart() == null || req.subscriptionEnd() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请登记订阅起止时间");
        }
        if (!req.subscriptionEnd().isAfter(req.subscriptionStart())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订阅结束时间须晚于开始时间");
        }

        SubscriptionOrder order = requireOrder(task.getOrderId());
        String beforeTask = task.getStatus();
        String beforeOrder = order.getOrderStatus();

        task.setStatus("SUCCESS");
        task.setSubscriptionStart(req.subscriptionStart());
        task.setSubscriptionEnd(req.subscriptionEnd());
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);

        Instant now = Instant.now();
        order.setOrderStatus("SUCCESS");
        order.setFulfillmentStatus("SUCCESS");
        order.setCompletedAt(now);
        order.setUpdatedAt(now);
        orderRepo.save(order);

        String remark = req.remark() != null && !req.remark().isBlank()
                ? req.remark().trim()
                : "履约成功，订阅已生效";
        appendAdminLog(task, operatorId, "SUCCESS", remark, true);

        auditLogService.log(operatorId, "FULFILLMENT_SUCCESS", "FULFILLMENT_TASK", taskNo,
                beforeTask + "/" + beforeOrder, "SUCCESS", remark);

        notifyService.publish("ORDER_FULFILLMENT_SUCCESS", Map.of(
                "orderNo", order.getOrderNo(),
                "userId", order.getUserId(),
                "taskNo", task.getTaskNo(),
                "subscriptionStart", req.subscriptionStart().toString(),
                "subscriptionEnd", req.subscriptionEnd().toString()
        ));

        PaymentAmountSnapshot amount = paymentAmountService.resolve(order);
        eventPublisher.publishEvent(new OrderSettledEvent(
                order.getOrderNo(), order.getId(), amount.settlementAmount()));
    }

    @Transactional
    public void markFailed(String taskNo, Long operatorId, MarkFailedRequest req) {
        requireManagePermission(operatorId);
        FulfillmentTask task = requireTask(taskNo);
        requireActive(task);
        if (req.failureReason() == null || req.failureReason().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写失败原因");
        }

        SubscriptionOrder order = requireOrder(task.getOrderId());
        String beforeTask = task.getStatus();
        String beforeOrder = order.getOrderStatus();
        String reason = req.failureReason().trim();

        task.setStatus("FAILED");
        task.setFailureReason(reason);
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);

        order.setOrderStatus("FAILED");
        order.setFulfillmentStatus("FAILED");
        order.setUpdatedAt(Instant.now());
        orderRepo.save(order);

        String content = "履约失败：" + reason;
        if (req.remark() != null && !req.remark().isBlank()) {
            content += "（" + req.remark().trim() + "）";
        }
        appendAdminLog(task, operatorId, "FAILED", content, true);

        auditLogService.log(operatorId, "FULFILLMENT_FAILED", "FULFILLMENT_TASK", taskNo,
                beforeTask + "/" + beforeOrder, "FAILED", reason);

        notifyService.publish("ORDER_FULFILLMENT_FAILED", Map.of(
                "orderNo", order.getOrderNo(),
                "userId", order.getUserId(),
                "taskNo", task.getTaskNo(),
                "failureReason", reason
        ));
    }

    private void requireManagePermission(Long adminId) {
        if (!rbacService.canManageFulfillment(adminId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色不可操作履约任务");
        }
    }

    private FulfillmentTask requireTask(String taskNo) {
        return taskRepo.findByTaskNo(taskNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "履约任务不存在"));
    }

    private SubscriptionOrder requireOrder(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "关联订单不存在"));
    }

    private void requireActive(FulfillmentTask task) {
        if (TERMINAL_STATUSES.contains(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已结束，不可变更");
        }
    }

    private void syncOrderFulfillmentStatus(Long orderId, String fulfillmentStatus) {
        SubscriptionOrder order = requireOrder(orderId);
        order.setFulfillmentStatus(fulfillmentStatus);
        if ("PROCESSING".equals(fulfillmentStatus) && "FULFILLING".equals(order.getOrderStatus())) {
            // 保持履约中
        }
        order.setUpdatedAt(Instant.now());
        orderRepo.save(order);
    }

    private void appendAdminLog(FulfillmentTask task, Long operatorId, String logType,
                                String content, boolean userVisible) {
        FulfillmentLog log = new FulfillmentLog();
        log.setTaskId(task.getId());
        log.setLogType(logType);
        log.setContent(content);
        log.setUserVisible(userVisible);
        log.setOperatorId(operatorId);
        logRepo.save(log);
    }

    private FulfillmentTaskSummaryDto toSummary(FulfillmentTask task) {
        SubscriptionOrder order = requireOrder(task.getOrderId());
        String productName = productRepo.findById(order.getProductId()).map(p -> p.getName()).orElse("-");
        return new FulfillmentTaskSummaryDto(
                task.getTaskNo(),
                order.getOrderNo(),
                productName,
                task.getStatus(),
                task.getAssigneeAdminId(),
                resolveAdminName(task.getAssigneeAdminId()),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private FulfillmentTaskDetailDto toDetail(FulfillmentTask task) {
        SubscriptionOrder order = requireOrder(task.getOrderId());
        String productName = productRepo.findById(order.getProductId()).map(p -> p.getName()).orElse("-");
        PaymentAmountSnapshot amount = paymentAmountService.resolve(order);
        var logs = logRepo.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream()
                .map(l -> new AdminFulfillmentLogDto(
                        l.getLogType(),
                        l.getContent(),
                        l.isUserVisible(),
                        l.getOperatorId(),
                        resolveAdminName(l.getOperatorId()),
                        l.getCreatedAt()))
                .toList();
        return new FulfillmentTaskDetailDto(
                task.getTaskNo(),
                order.getOrderNo(),
                order.getUserId(),
                productName,
                order.getAmount(),
                order.getCurrency(),
                amount.paidAmount(),
                amount.paidCurrency(),
                amount.exchangeRate(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                order.getFulfillmentStatus(),
                task.getStatus(),
                task.getAssigneeAdminId(),
                resolveAdminName(task.getAssigneeAdminId()),
                task.getSubscriptionStart(),
                task.getSubscriptionEnd(),
                task.getFailureReason(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                logs
        );
    }

    private String resolveAdminName(Long adminId) {
        if (adminId == null) {
            return null;
        }
        return adminUserRepo.findById(adminId)
                .map(a -> a.getDisplayName() != null ? a.getDisplayName() : a.getUsername())
                .orElse("未知");
    }
}

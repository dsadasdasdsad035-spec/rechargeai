package com.wildai.admin.service;

import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.fulfillment.domain.FulfillmentTask;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.admin.dto.ForceOrderStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AdminOrderMutationService {

    private final SubscriptionOrderRepository orderRepo;
    private final FulfillmentTaskRepository fulfillmentTaskRepo;
    private final RbacService rbacService;
    private final AuditLogService auditLogService;

    public AdminOrderMutationService(SubscriptionOrderRepository orderRepo,
                                     FulfillmentTaskRepository fulfillmentTaskRepo,
                                     RbacService rbacService,
                                     AuditLogService auditLogService) {
        this.orderRepo = orderRepo;
        this.fulfillmentTaskRepo = fulfillmentTaskRepo;
        this.rbacService = rbacService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void forceOrderStatus(String orderNo, Long operatorId, ForceOrderStatusRequest req) {
        if (!rbacService.canForceOrderStatus(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色不可强制改状态");
        }
        if (req.orderStatus() == null && req.paymentStatus() == null && req.fulfillmentStatus() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少指定一项状态变更");
        }

        SubscriptionOrder order = orderRepo.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));

        String before = snapshot(order);
        if (req.orderStatus() != null) {
            order.setOrderStatus(req.orderStatus());
        }
        if (req.paymentStatus() != null) {
            order.setPaymentStatus(req.paymentStatus());
        }
        if (req.fulfillmentStatus() != null) {
            order.setFulfillmentStatus(req.fulfillmentStatus());
            syncFulfillmentTask(order.getId(), req.fulfillmentStatus());
        }
        order.setUpdatedAt(Instant.now());
        orderRepo.save(order);

        String after = snapshot(order);
        auditLogService.log(operatorId, "ORDER_FORCE_STATUS", "SUBSCRIPTION_ORDER", orderNo,
                before, after, req.reason().trim());
    }

    private void syncFulfillmentTask(Long orderId, String fulfillmentStatus) {
        Optional<FulfillmentTask> taskOpt = fulfillmentTaskRepo.findByOrderId(orderId);
        if (taskOpt.isEmpty()) {
            return;
        }
        FulfillmentTask task = taskOpt.get();
        String mapped = mapFulfillmentToTaskStatus(fulfillmentStatus);
        if (mapped != null) {
            task.setStatus(mapped);
            task.setUpdatedAt(Instant.now());
            fulfillmentTaskRepo.save(task);
        }
    }

    private String mapFulfillmentToTaskStatus(String fulfillmentStatus) {
        return switch (fulfillmentStatus) {
            case "PENDING" -> "PENDING";
            case "PROCESSING" -> "PROCESSING";
            case "WAIT_USER" -> "WAIT_USER";
            case "WAIT_ADMIN" -> "WAIT_ADMIN";
            case "SUCCESS" -> "SUCCESS";
            case "FAILED" -> "FAILED";
            default -> null;
        };
    }

    private String snapshot(SubscriptionOrder order) {
        return "order=" + order.getOrderStatus()
                + ",payment=" + order.getPaymentStatus()
                + ",fulfillment=" + order.getFulfillmentStatus();
    }
}

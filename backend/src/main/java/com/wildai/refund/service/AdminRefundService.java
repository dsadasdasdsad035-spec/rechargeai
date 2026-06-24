package com.wildai.refund.service;

import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.admin.service.AuditLogService;
import com.wildai.admin.service.RbacService;
import com.wildai.common.dto.PageResult;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.finance.event.RefundCompletedEvent;
import com.wildai.notify.service.NotifyService;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.payment.channel.ChannelRefundResult;
import com.wildai.payment.channel.PaymentChannelAdapter;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.payment.repository.PaymentTransactionRepository;
import com.wildai.product.repository.AiServiceProductRepository;
import com.wildai.refund.domain.RefundRequest;
import com.wildai.refund.dto.AdminRefundDetailDto;
import com.wildai.refund.dto.AdminRefundSummaryDto;
import com.wildai.refund.dto.RefundReviewRequest;
import com.wildai.refund.repository.RefundRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminRefundService {

    private static final Logger log = LoggerFactory.getLogger(AdminRefundService.class);
    private static final int MAX_CHANNEL_RETRIES = 3;

    private final RefundRequestRepository refundRepo;
    private final SubscriptionOrderRepository orderRepo;
    private final PaymentTransactionRepository paymentRepo;
    private final AiServiceProductRepository productRepo;
    private final AdminUserRepository adminUserRepo;
    private final RbacService rbacService;
    private final AuditLogService auditLogService;
    private final NotifyService notifyService;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, PaymentChannelAdapter> adapters;

    public AdminRefundService(RefundRequestRepository refundRepo,
                              SubscriptionOrderRepository orderRepo,
                              PaymentTransactionRepository paymentRepo,
                              AiServiceProductRepository productRepo,
                              AdminUserRepository adminUserRepo,
                              RbacService rbacService,
                              AuditLogService auditLogService,
                              NotifyService notifyService,
                              ApplicationEventPublisher eventPublisher,
                              java.util.List<PaymentChannelAdapter> adapterList) {
        this.refundRepo = refundRepo;
        this.orderRepo = orderRepo;
        this.paymentRepo = paymentRepo;
        this.productRepo = productRepo;
        this.adminUserRepo = adminUserRepo;
        this.rbacService = rbacService;
        this.auditLogService = auditLogService;
        this.notifyService = notifyService;
        this.eventPublisher = eventPublisher;
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(PaymentChannelAdapter::channel, Function.identity()));
    }

    public PageResult<AdminRefundSummaryDto> listRefunds(String status, String orderNo, String refundNo,
                                                         int pageNo, int pageSize) {
        var page = refundRepo.searchAdminRefunds(status, orderNo, refundNo, PageRequest.of(pageNo - 1, pageSize));
        var items = page.getContent().stream().map(this::toSummary).toList();
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), items);
    }

    public AdminRefundDetailDto getDetail(String refundNo) {
        RefundRequest refund = requireRefund(refundNo);
        return toDetail(refund);
    }

    @Transactional
    public AdminRefundDetailDto approve(String refundNo, Long adminId, RefundReviewRequest req) {
        requireApprover(adminId);
        RefundRequest refund = requireRefund(refundNo);
        if (!"PENDING".equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待审核退款可审批通过");
        }

        SubscriptionOrder order = requireOrder(refund.getOrderId());
        PaymentTransaction payment = requirePaidPayment(order.getId());

        String before = refund.getStatus();
        refund.setStatus("APPROVED");
        refund.setReviewerAdminId(adminId);
        if (req.reviewComment() != null && !req.reviewComment().isBlank()) {
            refund.setReviewComment(req.reviewComment().trim());
        }
        refund.setUpdatedAt(Instant.now());
        refundRepo.save(refund);

        auditLogService.log(adminId, "REFUND_APPROVE", "REFUND_REQUEST", refundNo,
                before, "APPROVED", refund.getReviewComment());

        markRefunding(order, payment);
        refund.setStatus("REFUNDING");
        refund.setUpdatedAt(Instant.now());
        refundRepo.save(refund);

        ChannelRefundResult result = invokeChannelRefundWithRetry(payment, refund);
        if (result.success()) {
            completeRefund(refund, order, payment, result.channelRefundNo(), adminId);
        } else {
            markChannelRefundFailed(refund, order, payment, result.errorMessage(), adminId);
        }

        return toDetail(refundRepo.findByRefundNo(refundNo).orElseThrow());
    }

    @Transactional
    public AdminRefundDetailDto reject(String refundNo, Long adminId, RefundReviewRequest req) {
        requireApprover(adminId);
        RefundRequest refund = requireRefund(refundNo);
        if (!"PENDING".equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待审核退款可驳回");
        }
        if (req.reviewComment() == null || req.reviewComment().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回须填写审核意见");
        }

        String before = refund.getStatus();
        refund.setStatus("REJECTED");
        refund.setReviewerAdminId(adminId);
        refund.setReviewComment(req.reviewComment().trim());
        refund.setUpdatedAt(Instant.now());
        refundRepo.save(refund);

        SubscriptionOrder order = requireOrder(refund.getOrderId());
        auditLogService.log(adminId, "REFUND_REJECT", "REFUND_REQUEST", refundNo,
                before, "REJECTED", refund.getReviewComment());

        notifyService.publish("ORDER_REFUND_REJECTED", Map.of(
                "orderNo", order.getOrderNo(),
                "userId", order.getUserId(),
                "refundNo", refund.getRefundNo(),
                "reviewComment", refund.getReviewComment()
        ));

        return toDetail(refund);
    }

    @Transactional
    public AdminRefundDetailDto retryChannelRefund(String refundNo, Long adminId) {
        requireApprover(adminId);
        RefundRequest refund = requireRefund(refundNo);
        if (!"CHANNEL_REFUND_FAILED".equals(refund.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅渠道退款失败单可重试");
        }

        SubscriptionOrder order = requireOrder(refund.getOrderId());
        PaymentTransaction payment = requirePaidPayment(order.getId());

        markRefunding(order, payment);
        refund.setStatus("REFUNDING");
        refund.setUpdatedAt(Instant.now());
        refundRepo.save(refund);

        ChannelRefundResult result = invokeChannelRefundWithRetry(payment, refund);
        if (result.success()) {
            completeRefund(refund, order, payment, result.channelRefundNo(), adminId);
        } else {
            markChannelRefundFailed(refund, order, payment, result.errorMessage(), adminId);
        }

        auditLogService.log(adminId, "REFUND_CHANNEL_RETRY", "REFUND_REQUEST", refundNo,
                "CHANNEL_REFUND_FAILED", refund.getStatus(), result.errorMessage());

        return toDetail(refundRepo.findByRefundNo(refundNo).orElseThrow());
    }

    private ChannelRefundResult invokeChannelRefundWithRetry(PaymentTransaction payment, RefundRequest refund) {
        PaymentChannelAdapter adapter = adapters.get(payment.getChannel());
        if (adapter == null) {
            return ChannelRefundResult.fail("支付渠道不可用: " + payment.getChannel());
        }

        ChannelRefundResult last = ChannelRefundResult.fail("未知错误");
        for (int attempt = 1; attempt <= MAX_CHANNEL_RETRIES; attempt++) {
            last = adapter.refund(
                    payment.getPaymentNo(),
                    payment.getThirdTradeNo(),
                    refund.getRefundNo(),
                    refund.getAmount());
            if (last.success()) {
                return last;
            }
            log.warn("渠道退款失败 refundNo={} attempt={} reason={}",
                    refund.getRefundNo(), attempt, last.errorMessage());
            if (attempt < MAX_CHANNEL_RETRIES) {
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ChannelRefundResult.fail("退款重试被中断");
                }
            }
        }
        return last;
    }

    private void completeRefund(RefundRequest refund, SubscriptionOrder order,
                                PaymentTransaction payment, String channelRefundNo, Long adminId) {
        refund.setStatus("COMPLETED");
        refund.setChannelRefundNo(channelRefundNo);
        refund.setUpdatedAt(Instant.now());
        refundRepo.save(refund);

        order.setOrderStatus("REFUNDED");
        order.setPaymentStatus("REFUNDED");
        order.setUpdatedAt(Instant.now());
        orderRepo.save(order);

        payment.setStatus("REFUNDED");
        paymentRepo.save(payment);

        auditLogService.log(adminId, "REFUND_COMPLETED", "REFUND_REQUEST", refund.getRefundNo(),
                "REFUNDING", "COMPLETED", "channelRefundNo=" + channelRefundNo);

        notifyService.publish("ORDER_REFUND_COMPLETED", Map.of(
                "orderNo", order.getOrderNo(),
                "userId", order.getUserId(),
                "refundNo", refund.getRefundNo(),
                "amount", refund.getAmount().toPlainString()
        ));

        eventPublisher.publishEvent(new RefundCompletedEvent(
                refund.getRefundNo(), order.getId(), refund.getAmount()));
    }

    private void markChannelRefundFailed(RefundRequest refund, SubscriptionOrder order,
                                         PaymentTransaction payment, String reason, Long adminId) {
        refund.setStatus("CHANNEL_REFUND_FAILED");
        refund.setUpdatedAt(Instant.now());
        refundRepo.save(refund);

        order.setOrderStatus("REFUNDING");
        order.setPaymentStatus("REFUNDING");
        order.setUpdatedAt(Instant.now());
        orderRepo.save(order);

        payment.setStatus("REFUNDING");
        paymentRepo.save(payment);

        auditLogService.log(adminId, "REFUND_CHANNEL_FAILED", "REFUND_REQUEST", refund.getRefundNo(),
                "REFUNDING", "CHANNEL_REFUND_FAILED", reason);
    }

    private void markRefunding(SubscriptionOrder order, PaymentTransaction payment) {
        order.setOrderStatus("REFUNDING");
        order.setPaymentStatus("REFUNDING");
        order.setUpdatedAt(Instant.now());
        orderRepo.save(order);

        payment.setStatus("REFUNDING");
        paymentRepo.save(payment);
    }

    private void requireApprover(Long adminId) {
        if (!rbacService.canApproveRefund(adminId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色不可审批退款");
        }
    }

    private RefundRequest requireRefund(String refundNo) {
        return refundRepo.findByRefundNo(refundNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "退款单不存在"));
    }

    private SubscriptionOrder requireOrder(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "关联订单不存在"));
    }

    private PaymentTransaction requirePaidPayment(Long orderId) {
        PaymentTransaction payment = paymentRepo.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "支付单不存在"));
        if (!"PAID".equals(payment.getStatus()) && !"REFUNDING".equals(payment.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付单状态不支持退款");
        }
        return payment;
    }

    private AdminRefundSummaryDto toSummary(RefundRequest refund) {
        SubscriptionOrder order = requireOrder(refund.getOrderId());
        return new AdminRefundSummaryDto(
                refund.getRefundNo(),
                order.getOrderNo(),
                refund.getUserId(),
                refund.getAmount(),
                refund.getStatus(),
                refund.getApplyReason(),
                refund.getCreatedAt()
        );
    }

    private AdminRefundDetailDto toDetail(RefundRequest refund) {
        SubscriptionOrder order = requireOrder(refund.getOrderId());
        String productName = productRepo.findById(order.getProductId()).map(p -> p.getName()).orElse("-");
        String channel = paymentRepo.findByOrderId(order.getId()).map(PaymentTransaction::getChannel).orElse("-");
        return new AdminRefundDetailDto(
                refund.getRefundNo(),
                order.getOrderNo(),
                refund.getUserId(),
                productName,
                channel,
                refund.getAmount(),
                refund.getStatus(),
                refund.getApplyReason(),
                refund.getReviewComment(),
                refund.getReviewerAdminId(),
                resolveAdminName(refund.getReviewerAdminId()),
                refund.getChannelRefundNo(),
                refund.getCreatedAt(),
                refund.getUpdatedAt()
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

package com.wildai.refund.service;

import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.payment.dto.PaymentAmountSnapshot;
import com.wildai.payment.service.PaymentAmountService;
import com.wildai.refund.domain.RefundRequest;
import com.wildai.refund.dto.RefundApplyRequest;
import com.wildai.refund.dto.RefundRequestDto;
import com.wildai.refund.repository.RefundRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RefundService {

    /** 进行中的退款状态，存在则拒绝重复申请（FR-030） */
    private static final List<String> ACTIVE_REFUND_STATUSES = List.of(
            "PENDING", "APPROVED", "REFUNDING", "CHANNEL_REFUND_FAILED"
    );

    private final RefundRequestRepository refundRepo;
    private final SubscriptionOrderRepository orderRepo;
    private final PaymentAmountService paymentAmountService;

    public RefundService(RefundRequestRepository refundRepo, SubscriptionOrderRepository orderRepo,
                         PaymentAmountService paymentAmountService) {
        this.refundRepo = refundRepo;
        this.orderRepo = orderRepo;
        this.paymentAmountService = paymentAmountService;
    }

    @Transactional
    public RefundRequestDto apply(String orderNo, Long userId, RefundApplyRequest req) {
        SubscriptionOrder order = orderRepo.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));

        validateRefundable(order);

        RefundRequest refund = new RefundRequest();
        PaymentAmountSnapshot amount = paymentAmountService.resolve(order);
        refund.setRefundNo("RF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        refund.setOrderId(order.getId());
        refund.setUserId(userId);
        refund.setAmount(amount.settlementAmount());
        refund.setStatus("PENDING");
        refund.setApplyReason(req.applyReason().trim());
        refund = refundRepo.save(refund);

        return toDto(refund, order);
    }

    public RefundRequestDto getLatestForOrder(String orderNo, Long userId) {
        SubscriptionOrder order = orderRepo.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        return refundRepo.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                .map(r -> toDto(r, order))
                .orElse(null);
    }

    private void validateRefundable(SubscriptionOrder order) {
        if (!"FAILED".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "仅履约失败订单可申请退款");
        }
        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "订单支付状态不支持退款");
        }
        if (refundRepo.existsByOrderIdAndStatusIn(order.getId(), ACTIVE_REFUND_STATUSES)) {
            throw new BusinessException(ErrorCode.REFUND_DUPLICATE, "已有进行中的退款申请");
        }
    }

    RefundRequestDto toDto(RefundRequest refund, SubscriptionOrder order) {
        PaymentAmountSnapshot amount = paymentAmountService.resolve(order);
        return new RefundRequestDto(
                refund.getRefundNo(),
                order.getOrderNo(),
                refund.getAmount(),
                amount.settlementCurrency(),
                refund.getStatus(),
                refund.getApplyReason(),
                refund.getReviewComment(),
                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}

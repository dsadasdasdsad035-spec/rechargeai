package com.wildai.order.service;

import com.wildai.common.dto.PageResult;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.common.util.AesEncryptUtil;
import com.wildai.common.util.DesensitizeUtil;
import com.wildai.fulfillment.dto.FulfillmentLogDto;
import com.wildai.fulfillment.repository.FulfillmentLogRepository;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.refund.repository.RefundRequestRepository;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.dto.OrderDetailDto;
import com.wildai.order.dto.OrderSummaryDto;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.payment.repository.PaymentTransactionRepository;
import com.wildai.product.repository.AiServiceProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderQueryService {

    private final SubscriptionOrderRepository orderRepo;
    private final AiServiceProductRepository productRepo;
    private final PaymentTransactionRepository paymentRepo;
    private final AesEncryptUtil aesEncryptUtil;
    private final FulfillmentTaskRepository fulfillmentTaskRepo;
    private final FulfillmentLogRepository fulfillmentLogRepo;
    private final RefundRequestRepository refundRequestRepo;

    public OrderQueryService(SubscriptionOrderRepository orderRepo, AiServiceProductRepository productRepo,
                             PaymentTransactionRepository paymentRepo, AesEncryptUtil aesEncryptUtil,
                             FulfillmentTaskRepository fulfillmentTaskRepo,
                             FulfillmentLogRepository fulfillmentLogRepo,
                             RefundRequestRepository refundRequestRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.paymentRepo = paymentRepo;
        this.aesEncryptUtil = aesEncryptUtil;
        this.fulfillmentTaskRepo = fulfillmentTaskRepo;
        this.fulfillmentLogRepo = fulfillmentLogRepo;
        this.refundRequestRepo = refundRequestRepo;
    }

    public PageResult<OrderSummaryDto> listUserOrders(Long userId, String orderStatus, String paymentStatus,
                                                      Instant start, Instant end, int pageNo, int pageSize) {
        var page = orderRepo.searchUserOrders(userId, orderStatus, paymentStatus, start, end, PageRequest.of(pageNo - 1, pageSize));
        Map<Long, String> names = productRepo.findAll().stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));
        var items = page.getContent().stream().map(o -> toSummary(o, names.get(o.getProductId()))).toList();
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), items);
    }

    public OrderDetailDto getUserOrderDetail(String orderNo, Long userId) {
        SubscriptionOrder order = orderRepo.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return toDetail(order);
    }

    public PageResult<OrderSummaryDto> listAdminOrders(String orderNo, Long userId, String orderStatus,
                                                       String paymentStatus, Instant start, Instant end,
                                                       int pageNo, int pageSize) {
        var page = orderRepo.searchAdminOrders(orderNo, userId, orderStatus, paymentStatus, start, end,
                PageRequest.of(pageNo - 1, pageSize));
        Map<Long, String> names = productRepo.findAll().stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));
        var items = page.getContent().stream().map(o -> toSummary(o, names.get(o.getProductId()))).toList();
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), items);
    }

    public OrderDetailDto getAdminOrderDetail(String orderNo) {
        SubscriptionOrder order = orderRepo.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return toDetail(order);
    }

    private OrderSummaryDto toSummary(SubscriptionOrder o, String productName) {
        return new OrderSummaryDto(o.getOrderNo(), productName, o.getAmount(), o.getCurrency(),
                o.getOrderStatus(), o.getPaymentStatus(), o.getFulfillmentStatus(),
                o.getCreatedAt(), o.getPaidAt());
    }

    private OrderDetailDto toDetail(SubscriptionOrder order) {
        String productName = productRepo.findById(order.getProductId()).map(p -> p.getName()).orElse("-");
        String account = DesensitizeUtil.account(aesEncryptUtil.decrypt(order.getTargetAccountEnc()));
        String tradeMasked = paymentRepo.findByOrderId(order.getId())
                .map(PaymentTransaction::getThirdTradeNo)
                .map(DesensitizeUtil::tradeNo)
                .orElse(null);

        var taskOpt = fulfillmentTaskRepo.findByOrderId(order.getId());
        String taskNo = null;
        String taskStatus = null;
        Instant subStart = null;
        Instant subEnd = null;
        List<FulfillmentLogDto> logs = Collections.emptyList();
        if (taskOpt.isPresent()) {
            var task = taskOpt.get();
            taskNo = task.getTaskNo();
            taskStatus = task.getStatus();
            subStart = task.getSubscriptionStart();
            subEnd = task.getSubscriptionEnd();
            logs = fulfillmentLogRepo.findByTaskIdAndUserVisibleTrueOrderByCreatedAtAsc(task.getId()).stream()
                    .map(l -> new FulfillmentLogDto(l.getLogType(), l.getContent(), l.getCreatedAt()))
                    .toList();
        }

        String refundNo = null;
        String refundStatus = null;
        var refundOpt = refundRequestRepo.findFirstByOrderIdOrderByCreatedAtDesc(order.getId());
        if (refundOpt.isPresent()) {
            var refund = refundOpt.get();
            refundNo = refund.getRefundNo();
            refundStatus = refund.getStatus();
        }

        return new OrderDetailDto(order.getOrderNo(), order.getProductId(), productName,
                order.getAmount(), order.getCurrency(), account,
                order.getOrderStatus(), order.getPaymentStatus(), order.getFulfillmentStatus(),
                tradeMasked, order.getCreatedAt(), order.getPaidAt(), order.getExpiredAt(),
                taskNo, taskStatus, subStart, subEnd, logs, refundNo, refundStatus);
    }
}

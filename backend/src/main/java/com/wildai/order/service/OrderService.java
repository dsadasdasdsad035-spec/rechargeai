package com.wildai.order.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.common.util.AesEncryptUtil;
import com.wildai.fulfillment.service.FulfillmentCreationService;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.dto.CreateOrderRequest;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.product.domain.AiServiceProduct;
import com.wildai.product.service.ProductService;
import com.wildai.user.security.AccountStatusChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class OrderService {

    private final SubscriptionOrderRepository orderRepo;
    private final ProductService productService;
    private final DuplicateOrderChecker duplicateOrderChecker;
    private final AesEncryptUtil aesEncryptUtil;
    private final AccountStatusChecker accountStatusChecker;
    private final FulfillmentCreationService fulfillmentCreationService;
    private final WildAiProperties properties;

    public OrderService(SubscriptionOrderRepository orderRepo, ProductService productService,
                        DuplicateOrderChecker duplicateOrderChecker, AesEncryptUtil aesEncryptUtil,
                        AccountStatusChecker accountStatusChecker,
                        FulfillmentCreationService fulfillmentCreationService,
                        WildAiProperties properties) {
        this.orderRepo = orderRepo;
        this.productService = productService;
        this.duplicateOrderChecker = duplicateOrderChecker;
        this.aesEncryptUtil = aesEncryptUtil;
        this.accountStatusChecker = accountStatusChecker;
        this.fulfillmentCreationService = fulfillmentCreationService;
        this.properties = properties;
    }

    @Transactional
    public SubscriptionOrder createOrder(Long userId, CreateOrderRequest req) {
        accountStatusChecker.ensureCanTrade(userId);
        AiServiceProduct product = productService.requireOnShelf(req.productId());
        duplicateOrderChecker.check(userId, product.getId());
        productService.validateOrderFields(product, req.fields());

        String targetAccount = req.fields().getOrDefault("target_account", req.fields().get("targetAccount"));
        if (targetAccount == null || targetAccount.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI 账号标识不能为空");
        }

        SubscriptionOrder order = new SubscriptionOrder();
        order.setOrderNo("O" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setAmount(product.getSalePrice());
        order.setCurrency(product.getCurrency());
        order.setTargetAccountEnc(aesEncryptUtil.encrypt(targetAccount));
        String accountToken = req.fields().get("account_token");
        if (accountToken != null && !accountToken.isBlank()) {
            order.setAccountTokenEnc(aesEncryptUtil.encrypt(accountToken.trim()));
        }
        order.setOrderStatus("WAIT_PAY");
        order.setPaymentStatus("UNPAID");
        order.setFulfillmentStatus("NOT_STARTED");
        order.setExpiredAt(Instant.now().plus(properties.getOrder().getExpireMinutes(), ChronoUnit.MINUTES));
        return orderRepo.save(order);
    }

    public SubscriptionOrder requireOwned(String orderNo, Long userId) {
        return orderRepo.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
    }

    public SubscriptionOrder requireByOrderNo(String orderNo) {
        return orderRepo.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
    }

    @Transactional
    public void markPaid(SubscriptionOrder order) {
        fulfillmentCreationService.createFromPaidOrder(order);
    }

    @Transactional
    public void closeExpiredOrders() {
        var expired = orderRepo.findByOrderStatusAndExpiredAtBefore("WAIT_PAY", Instant.now());
        for (SubscriptionOrder order : expired) {
            order.setOrderStatus("CLOSED");
            order.setPaymentStatus("PAY_FAILED");
        }
        orderRepo.saveAll(expired);
    }
}

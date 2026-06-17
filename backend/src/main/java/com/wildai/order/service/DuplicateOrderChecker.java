package com.wildai.order.service;

import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.order.repository.SubscriptionOrderRepository;
import org.springframework.stereotype.Service;

@Service
public class DuplicateOrderChecker {

    private final SubscriptionOrderRepository orderRepo;

    public DuplicateOrderChecker(SubscriptionOrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    public void check(Long userId, Long productId) {
        if (!orderRepo.findActiveByUserAndProduct(userId, productId).isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_DUPLICATE, "存在进行中的同产品订单，请先处理现有订单");
        }
    }
}

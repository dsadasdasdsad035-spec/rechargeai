package com.wildai.admin.service;

import com.wildai.order.repository.SubscriptionOrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 运营统计，收入口径与资金概览一致：仅 SUCCESS 订单 */
@Service
public class AdminStatsService {

    private final SubscriptionOrderRepository orderRepo;

    public AdminStatsService(SubscriptionOrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    public BigDecimal getSettledRevenue() {
        return orderRepo.sumAmountByOrderStatus("SUCCESS");
    }

    public long countOrdersByStatus(String orderStatus) {
        return orderRepo.countByOrderStatus(orderStatus);
    }
}

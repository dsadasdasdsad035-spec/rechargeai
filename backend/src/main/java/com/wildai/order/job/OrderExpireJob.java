package com.wildai.order.job;

import com.wildai.order.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpireJob {

    private final OrderService orderService;

    public OrderExpireJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = 60_000)
    public void closeExpiredOrders() {
        orderService.closeExpiredOrders();
    }
}

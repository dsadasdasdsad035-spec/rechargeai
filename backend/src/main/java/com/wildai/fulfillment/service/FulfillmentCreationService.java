package com.wildai.fulfillment.service;

import com.wildai.fulfillment.domain.FulfillmentLog;
import com.wildai.fulfillment.domain.FulfillmentTask;
import com.wildai.fulfillment.repository.FulfillmentLogRepository;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.notify.service.NotifyService;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 支付成功后创建履约任务并推进订单进入履约中状态。
 */
@Service
public class FulfillmentCreationService {

    private final FulfillmentTaskRepository taskRepo;
    private final FulfillmentLogRepository logRepo;
    private final SubscriptionOrderRepository orderRepo;
    private final NotifyService notifyService;

    public FulfillmentCreationService(FulfillmentTaskRepository taskRepo,
                                      FulfillmentLogRepository logRepo,
                                      SubscriptionOrderRepository orderRepo,
                                      NotifyService notifyService) {
        this.taskRepo = taskRepo;
        this.logRepo = logRepo;
        this.orderRepo = orderRepo;
        this.notifyService = notifyService;
    }

    @Transactional
    public FulfillmentTask createFromPaidOrder(SubscriptionOrder order) {
        return taskRepo.findByOrderId(order.getId()).orElseGet(() -> {
            FulfillmentTask task = new FulfillmentTask();
            task.setTaskNo("FT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
            task.setOrderId(order.getId());
            task.setStatus("PENDING");
            task = taskRepo.save(task);

            FulfillmentLog log = new FulfillmentLog();
            log.setTaskId(task.getId());
            log.setLogType("SYSTEM");
            log.setContent("支付成功，履约任务已创建，等待客服处理");
            log.setUserVisible(true);
            logRepo.save(log);

            order.setOrderStatus("FULFILLING");
            order.setPaymentStatus("PAID");
            order.setFulfillmentStatus("PENDING");
            if (order.getPaidAt() == null) {
                order.setPaidAt(Instant.now());
            }
            order.setUpdatedAt(Instant.now());
            orderRepo.save(order);

            notifyService.publish("ORDER_FULFILLMENT_STARTED", Map.of(
                    "orderNo", order.getOrderNo(),
                    "userId", order.getUserId(),
                    "taskNo", task.getTaskNo()
            ));

            return task;
        });
    }
}

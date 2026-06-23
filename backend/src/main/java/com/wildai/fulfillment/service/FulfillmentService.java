package com.wildai.fulfillment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.fulfillment.domain.FulfillmentLog;
import com.wildai.fulfillment.domain.FulfillmentTask;
import com.wildai.fulfillment.dto.FulfillmentSupplementRequest;
import com.wildai.fulfillment.repository.FulfillmentLogRepository;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class FulfillmentService {

    private final FulfillmentTaskRepository taskRepo;
    private final FulfillmentLogRepository logRepo;
    private final SubscriptionOrderRepository orderRepo;

    public FulfillmentService(FulfillmentTaskRepository taskRepo,
                              FulfillmentLogRepository logRepo,
                              SubscriptionOrderRepository orderRepo) {
        this.taskRepo = taskRepo;
        this.logRepo = logRepo;
        this.orderRepo = orderRepo;
    }

    public FulfillmentTask requireUserTask(String orderNo, Long userId) {
        SubscriptionOrder order = orderRepo.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        return taskRepo.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "履约任务不存在"));
    }

    @Transactional
    public void submitSupplement(String orderNo, Long userId, FulfillmentSupplementRequest req) {
        FulfillmentTask task = requireUserTask(orderNo, userId);
        if (!"WAIT_USER".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不可提交补充资料");
        }
        appendUserLog(task, "USER_SUPPLEMENT", "用户补充资料：" + req.content());
        resumeProcessing(task);
    }

    @Transactional
    public void confirmUserAction(String orderNo, Long userId) {
        FulfillmentTask task = requireUserTask(orderNo, userId);
        if (!"WAIT_USER".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不可确认");
        }
        appendUserLog(task, "USER_CONFIRM", "用户已完成自主确认，等待客服继续处理");
        resumeProcessing(task);
    }

    private void resumeProcessing(FulfillmentTask task) {
        task.setStatus("PROCESSING");
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);

        SubscriptionOrder order = orderRepo.findById(task.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        order.setFulfillmentStatus("PROCESSING");
        order.setUpdatedAt(Instant.now());
        orderRepo.save(order);
    }

    private void appendUserLog(FulfillmentTask task, String logType, String content) {
        FulfillmentLog log = new FulfillmentLog();
        log.setTaskId(task.getId());
        log.setLogType(logType);
        log.setContent(content);
        log.setUserVisible(true);
        logRepo.save(log);
    }
}

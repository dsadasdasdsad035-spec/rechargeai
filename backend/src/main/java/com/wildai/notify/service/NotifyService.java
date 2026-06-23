package com.wildai.notify.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.notify.domain.OutboxEvent;
import com.wildai.notify.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public NotifyService(OutboxEventRepository outboxRepo, ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publish(String eventType, Map<String, Object> payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        try {
            event.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("通知载荷序列化失败", e);
        }
        event.setStatus("PENDING");
        event.setNextRunAt(Instant.now());
        outboxRepo.save(event);
    }

    @Transactional
    public void dispatchDueEvents() {
        var due = outboxRepo.findDuePending(Instant.now());
        for (OutboxEvent event : due) {
            try {
                log.info("派发通知事件 type={} payload={}", event.getEventType(), event.getPayloadJson());
                event.setStatus("SENT");
            } catch (Exception ex) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setStatus(event.getRetryCount() >= 5 ? "FAILED" : "PENDING");
                event.setNextRunAt(Instant.now().plusSeconds(30L * event.getRetryCount()));
                log.warn("通知派发失败 id={} retry={}", event.getId(), event.getRetryCount(), ex);
            }
            event.setUpdatedAt(Instant.now());
            outboxRepo.save(event);
        }
    }
}

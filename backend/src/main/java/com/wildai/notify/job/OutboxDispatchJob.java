package com.wildai.notify.job;

import com.wildai.notify.service.NotifyService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatchJob {

    private final NotifyService notifyService;

    public OutboxDispatchJob(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Scheduled(fixedRate = 30_000)
    public void dispatch() {
        notifyService.dispatchDueEvents();
    }
}

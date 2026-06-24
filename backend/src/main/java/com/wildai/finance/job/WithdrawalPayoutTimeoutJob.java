package com.wildai.finance.job;

import com.wildai.common.config.WildAiProperties;
import com.wildai.finance.service.WithdrawalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class WithdrawalPayoutTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalPayoutTimeoutJob.class);

    private final WithdrawalService withdrawalService;
    private final WildAiProperties properties;

    public WithdrawalPayoutTimeoutJob(WithdrawalService withdrawalService, WildAiProperties properties) {
        this.withdrawalService = withdrawalService;
        this.properties = properties;
    }

    @Scheduled(fixedRate = 3600_000)
    public void markOverduePayouts() {
        int days = properties.getFinance().getPayoutTimeoutDays();
        Instant deadline = Instant.now().minus(days, ChronoUnit.DAYS);
        int count = withdrawalService.markPayoutTimeout(deadline);
        if (count > 0) {
            log.info("打款超时标记完成，共 {} 笔", count);
        }
    }
}

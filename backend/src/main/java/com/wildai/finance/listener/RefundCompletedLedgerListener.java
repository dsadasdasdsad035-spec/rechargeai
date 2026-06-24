package com.wildai.finance.listener;

import com.wildai.finance.domain.LedgerEntryType;
import com.wildai.finance.event.RefundCompletedEvent;
import com.wildai.finance.repository.PlatformLedgerEntryRepository;
import com.wildai.finance.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RefundCompletedLedgerListener {

    private static final Logger log = LoggerFactory.getLogger(RefundCompletedLedgerListener.class);

    private final LedgerService ledgerService;
    private final PlatformLedgerEntryRepository entryRepo;

    public RefundCompletedLedgerListener(LedgerService ledgerService,
                                         PlatformLedgerEntryRepository entryRepo) {
        this.ledgerService = ledgerService;
        this.entryRepo = entryRepo;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onRefundCompleted(RefundCompletedEvent event) {
        // 仅已结算（SUCCESS）订单退款才扣减可提现池；履约失败退款从未入账 SETTLE
        if (!entryRepo.existsByEntryTypeAndOrderId(LedgerEntryType.SETTLE, event.orderId())) {
            log.info("订单未结算，跳过退款账本扣账: orderId={}, refundNo={}", event.orderId(), event.refundNo());
            return;
        }
        log.info("退款完成扣账: refundNo={}, amount={}", event.refundNo(), event.amount());
        ledgerService.recordRefund(event.refundNo(), event.orderId(), event.amount());
    }
}

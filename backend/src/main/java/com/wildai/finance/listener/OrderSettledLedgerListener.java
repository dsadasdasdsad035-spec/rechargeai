package com.wildai.finance.listener;

import com.wildai.finance.event.OrderSettledEvent;
import com.wildai.finance.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderSettledLedgerListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSettledLedgerListener.class);

    private final LedgerService ledgerService;

    public OrderSettledLedgerListener(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOrderSettled(OrderSettledEvent event) {
        log.info("订单结算入账: orderNo={}, amount={}", event.orderNo(), event.amount());
        ledgerService.recordSettle(event.orderNo(), event.orderId(), event.amount());
    }
}

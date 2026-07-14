package com.wildai.seo.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class SitemapVersion {

    private final AtomicLong version = new AtomicLong();

    public long current() {
        return version.get();
    }

    public void invalidate() {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    version.incrementAndGet();
                }
            });
            return;
        }
        version.incrementAndGet();
    }
}

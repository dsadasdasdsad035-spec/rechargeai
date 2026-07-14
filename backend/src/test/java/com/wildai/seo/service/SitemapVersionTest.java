package com.wildai.seo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

class SitemapVersionTest {

    private final SitemapVersion sitemapVersion = new SitemapVersion();

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionSynchronizationManager.setCurrentTransactionName(null);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
    }

    @Test
    void invalidatesImmediatelyWithoutTransaction() {
        sitemapVersion.invalidate();

        assertThat(sitemapVersion.current()).isEqualTo(1L);
    }

    @Test
    void invalidatesOnlyAfterTransactionCommit() {
        beginSynchronizedTransaction();

        sitemapVersion.invalidate();

        assertThat(sitemapVersion.current()).isZero();
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.getFirst().afterCommit();
        assertThat(sitemapVersion.current()).isEqualTo(1L);
    }

    @Test
    void doesNotInvalidateAfterTransactionRollback() {
        beginSynchronizedTransaction();

        sitemapVersion.invalidate();

        assertThat(sitemapVersion.current()).isZero();
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(sitemapVersion.current()).isZero();
    }

    private void beginSynchronizedTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }
}

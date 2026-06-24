package com.wildai.finance.service;

import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.finance.domain.LedgerEntryType;
import com.wildai.finance.domain.LedgerRefType;
import com.wildai.finance.domain.PlatformLedgerEntry;
import com.wildai.finance.repository.PlatformLedgerEntryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final PlatformLedgerEntryRepository entryRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public LedgerService(PlatformLedgerEntryRepository entryRepo) {
        this.entryRepo = entryRepo;
    }

    @Transactional
    public void recordSettle(String orderNo, Long orderId, BigDecimal amount) {
        recordEntry(LedgerEntryType.SETTLE, amount, LedgerRefType.ORDER, orderNo, orderId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordRefund(String refundNo, Long orderId, BigDecimal amount) {
        recordEntry(LedgerEntryType.REFUND, amount.negate(), LedgerRefType.REFUND, refundNo, orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordWithdrawFreeze(String withdrawalNo, BigDecimal amount) {
        recordEntry(LedgerEntryType.WITHDRAW_FREEZE, amount.negate(),
                LedgerRefType.WITHDRAWAL, withdrawalNo, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordWithdrawRelease(String withdrawalNo, BigDecimal amount) {
        recordEntry(LedgerEntryType.WITHDRAW_RELEASE, amount,
                LedgerRefType.WITHDRAWAL, withdrawalNo, null);
    }

    /** 打款完成：可用余额不变，仅记录流水 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordWithdrawComplete(String withdrawalNo) {
        recordEntry(LedgerEntryType.WITHDRAW_COMPLETE, BigDecimal.ZERO,
                LedgerRefType.WITHDRAWAL, withdrawalNo, null);
    }

    public BigDecimal getAvailableBalance() {
        return entryRepo.sumAllAmountsNative();
    }

    private void recordEntry(String entryType, BigDecimal amount, String refType,
                             String refId, Long orderId) {
        if (entryRepo.existsByEntryTypeAndRefTypeAndRefId(entryType, refType, refId)) {
            log.info("账本流水已存在，跳过重复记账: type={}, ref={}/{}", entryType, refType, refId);
            return;
        }
        if (amount == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "记账金额不能为空");
        }

        entityManager.flush();
        entityManager.clear();
        BigDecimal current = entryRepo.sumAllAmountsNative();
        BigDecimal balanceAfter = current.add(amount);
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "可用余额不足，无法记账");
        }

        PlatformLedgerEntry entry = new PlatformLedgerEntry();
        entry.setEntryNo(generateEntryNo());
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        entry.setRefType(refType);
        entry.setRefId(refId);
        entry.setOrderId(orderId);
        entryRepo.save(entry);
        entryRepo.flush();

        log.info("账本记账: no={}, type={}, amount={}, balanceAfter={}, ref={}/{}",
                entry.getEntryNo(), entryType, amount, balanceAfter, refType, refId);
    }

    private static String generateEntryNo() {
        return "LE" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}

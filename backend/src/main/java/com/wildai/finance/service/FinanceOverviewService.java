package com.wildai.finance.service;

import com.wildai.admin.service.AdminStatsService;
import com.wildai.admin.service.RbacService;
import com.wildai.common.dto.PageResult;
import com.wildai.finance.domain.LedgerEntryType;
import com.wildai.finance.domain.PlatformLedgerEntry;
import com.wildai.finance.dto.FinanceOverviewDto;
import com.wildai.finance.dto.LedgerEntryDto;
import com.wildai.finance.repository.PlatformLedgerEntryRepository;
import com.wildai.finance.repository.WithdrawalRequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class FinanceOverviewService {

    private static final Set<String> FROZEN_STATUSES = Set.of("PENDING_APPROVAL", "APPROVED");

    private final RbacService rbacService;
    private final AdminStatsService statsService;
    private final LedgerService ledgerService;
    private final PlatformLedgerEntryRepository entryRepo;
    private final WithdrawalRequestRepository withdrawalRepo;

    public FinanceOverviewService(RbacService rbacService,
                                  AdminStatsService statsService,
                                  LedgerService ledgerService,
                                  PlatformLedgerEntryRepository entryRepo,
                                  WithdrawalRequestRepository withdrawalRepo) {
        this.rbacService = rbacService;
        this.statsService = statsService;
        this.ledgerService = ledgerService;
        this.entryRepo = entryRepo;
        this.withdrawalRepo = withdrawalRepo;
    }

    public FinanceOverviewDto getOverview(Long adminId) {
        rbacService.requireViewFinance(adminId);

        BigDecimal settledRevenue = statsService.getSettledRevenue();
        BigDecimal refundSum = entryRepo.sumAmountByEntryType(LedgerEntryType.REFUND);
        BigDecimal totalRefunded = refundSum.abs();
        BigDecimal availableBalance = ledgerService.getAvailableBalance();
        BigDecimal frozenForWithdrawal = withdrawalRepo.sumAmountByStatusIn(FROZEN_STATUSES);
        BigDecimal totalWithdrawn = withdrawalRepo.sumCompletedWithdrawn();

        return new FinanceOverviewDto(
                settledRevenue, totalRefunded, availableBalance, frozenForWithdrawal, totalWithdrawn);
    }

    public PageResult<LedgerEntryDto> listLedger(Long adminId, String entryType,
                                                  Instant startTime, Instant endTime,
                                                  int pageNo, int pageSize) {
        rbacService.requireViewFinance(adminId);
        var page = entryRepo.search(entryType, startTime, endTime, PageRequest.of(pageNo - 1, pageSize));
        List<LedgerEntryDto> items = page.getContent().stream().map(this::toDto).toList();
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), items);
    }

    private LedgerEntryDto toDto(PlatformLedgerEntry e) {
        return new LedgerEntryDto(
                e.getEntryNo(), e.getEntryType(), e.getAmount(), e.getBalanceAfter(),
                e.getRefType(), e.getRefId(), e.getOrderId(), e.getCreatedAt());
    }
}

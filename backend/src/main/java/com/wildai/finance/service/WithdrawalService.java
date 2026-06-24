package com.wildai.finance.service;

import com.wildai.admin.domain.AdminUser;
import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.admin.service.AuditLogService;
import com.wildai.admin.service.RbacService;
import com.wildai.common.config.WildAiProperties;
import com.wildai.common.dto.PageResult;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.finance.domain.PayoutAccount;
import com.wildai.finance.domain.WithdrawalRequest;
import com.wildai.finance.domain.WithdrawalStatus;
import com.wildai.finance.dto.*;
import com.wildai.finance.repository.PayoutAccountRepository;
import com.wildai.finance.repository.WithdrawalRequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WithdrawalService {

    private static final Set<String> PENDING_STATUSES = Set.of(
            WithdrawalStatus.PENDING_APPROVAL, WithdrawalStatus.APPROVED);

    /** 串行化提现冻结，避免并发超额扣减可提现余额 */
    private static final ReentrantLock LEDGER_WRITE_LOCK = new ReentrantLock();

    private final WithdrawalRequestRepository withdrawalRepo;
    private final PayoutAccountRepository payoutAccountRepo;
    private final PayoutAccountService payoutAccountService;
    private final LedgerService ledgerService;
    private final RbacService rbacService;
    private final AuditLogService auditLogService;
    private final AdminUserRepository adminUserRepo;
    private final WildAiProperties properties;

    public WithdrawalService(WithdrawalRequestRepository withdrawalRepo,
                           PayoutAccountRepository payoutAccountRepo,
                           PayoutAccountService payoutAccountService,
                           LedgerService ledgerService,
                           RbacService rbacService,
                           AuditLogService auditLogService,
                           AdminUserRepository adminUserRepo,
                           WildAiProperties properties) {
        this.withdrawalRepo = withdrawalRepo;
        this.payoutAccountRepo = payoutAccountRepo;
        this.payoutAccountService = payoutAccountService;
        this.ledgerService = ledgerService;
        this.rbacService = rbacService;
        this.auditLogService = auditLogService;
        this.adminUserRepo = adminUserRepo;
        this.properties = properties;
    }

    public PageResult<WithdrawalSummaryDto> list(Long adminId, String status, String withdrawalNo,
                                                  int pageNo, int pageSize) {
        rbacService.requireViewFinance(adminId);
        var page = withdrawalRepo.search(status, withdrawalNo, PageRequest.of(pageNo - 1, pageSize));
        Map<Long, AdminUser> admins = loadAdminMap();
        Map<Long, PayoutAccount> accounts = loadAccountMap(page.getContent());
        List<WithdrawalSummaryDto> items = page.getContent().stream()
                .map(w -> toSummary(w, admins, accounts)).toList();
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), items);
    }

    public WithdrawalDetailDto getDetail(Long adminId, String withdrawalNo) {
        rbacService.requireViewFinance(adminId);
        return toDetail(requireWithdrawal(withdrawalNo));
    }

    public WithdrawalDetailDto create(Long adminId, CreateWithdrawalRequest req) {
        rbacService.requireManageFinance(adminId);
        BigDecimal amount = req.amount();
        var finance = properties.getFinance();

        if (amount.compareTo(finance.getMinWithdrawAmount()) < 0) {
            throw new BusinessException(ErrorCode.WITHDRAW_NOT_ALLOWED,
                    "最低单笔提现金额为 ¥" + finance.getMinWithdrawAmount());
        }

        Instant dayStart = startOfTodayShanghai();
        BigDecimal todaySum = withdrawalRepo.sumTodayAppliedByApplicant(adminId, dayStart);
        if (todaySum.add(amount).compareTo(finance.getMaxDailyWithdrawAmount()) > 0) {
            throw new BusinessException(ErrorCode.WITHDRAW_LIMIT, "超出每日提现上限");
        }

        if (withdrawalRepo.countByStatusIn(PENDING_STATUSES) >= finance.getMaxPendingWithdrawals()) {
            throw new BusinessException(ErrorCode.WITHDRAW_LIMIT, "待处理提现单已达上限");
        }

        PayoutAccount account = payoutAccountService.requireEnabledAccount(req.payoutAccountId());

        return withLedgerWriteLock(() -> {
            String withdrawalNo = generateWithdrawalNo();
            ledgerService.recordWithdrawFreeze(withdrawalNo, amount);

            WithdrawalRequest w = new WithdrawalRequest();
            w.setWithdrawalNo(withdrawalNo);
            w.setAmount(amount);
            w.setStatus(WithdrawalStatus.PENDING_APPROVAL);
            w.setPayoutAccountId(account.getId());
            w.setApplicantAdminId(adminId);
            if (req.remark() != null && !req.remark().isBlank()) {
                w.setRemark(req.remark().trim());
            }
            w.setAppliedAt(Instant.now());
            withdrawalRepo.save(w);

            auditLogService.log(adminId, "WITHDRAW_APPLY", "WITHDRAWAL", w.getWithdrawalNo(),
                    null, WithdrawalStatus.PENDING_APPROVAL, "amount=" + amount);

            return toDetail(w);
        });
    }

    private <T> T withLedgerWriteLock(java.util.concurrent.Callable<T> action) {
        try {
            if (!LEDGER_WRITE_LOCK.tryLock(30, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "账本繁忙，请稍后重试");
            }
            try {
                return action.call();
            } finally {
                LEDGER_WRITE_LOCK.unlock();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.BAD_REQUEST, "账本操作被中断");
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "提现申请失败: " + ex.getMessage());
        }
    }

    @Transactional
    public WithdrawalDetailDto approve(Long adminId, String withdrawalNo) {
        if (!rbacService.isSuperAdmin(adminId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可审批提现");
        }
        WithdrawalRequest w = requireWithdrawal(withdrawalNo);
        if (!WithdrawalStatus.PENDING_APPROVAL.equals(w.getStatus())) {
            throw new BusinessException(ErrorCode.WITHDRAW_NOT_ALLOWED, "当前状态不可审批");
        }
        if (w.getApplicantAdminId().equals(adminId)) {
            throw new BusinessException(ErrorCode.WITHDRAW_NOT_ALLOWED, "不可审批自己发起的提现");
        }

        String before = w.getStatus();
        w.setStatus(WithdrawalStatus.APPROVED);
        w.setApproverAdminId(adminId);
        w.setApprovedAt(Instant.now());
        w.setUpdatedAt(Instant.now());
        withdrawalRepo.save(w);

        auditLogService.log(adminId, "WITHDRAW_APPROVE", "WITHDRAWAL", withdrawalNo,
                before, WithdrawalStatus.APPROVED, null);

        return toDetail(w);
    }

    @Transactional
    public WithdrawalDetailDto reject(Long adminId, String withdrawalNo, RejectWithdrawalRequest req) {
        if (!rbacService.isSuperAdmin(adminId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可驳回提现");
        }
        WithdrawalRequest w = requireWithdrawal(withdrawalNo);
        if (!WithdrawalStatus.PENDING_APPROVAL.equals(w.getStatus())) {
            throw new BusinessException(ErrorCode.WITHDRAW_NOT_ALLOWED, "当前状态不可驳回");
        }

        String before = w.getStatus();
        w.setStatus(WithdrawalStatus.REJECTED);
        w.setRejectReason(req.reason().trim());
        w.setApproverAdminId(adminId);
        w.setApprovedAt(Instant.now());
        w.setUpdatedAt(Instant.now());
        withdrawalRepo.save(w);

        ledgerService.recordWithdrawRelease(w.getWithdrawalNo(), w.getAmount());

        auditLogService.log(adminId, "WITHDRAW_REJECT", "WITHDRAWAL", withdrawalNo,
                before, WithdrawalStatus.REJECTED, req.reason());

        return toDetail(w);
    }

    @Transactional
    public WithdrawalDetailDto cancel(Long adminId, String withdrawalNo) {
        WithdrawalRequest w = requireWithdrawal(withdrawalNo);
        if (!WithdrawalStatus.PENDING_APPROVAL.equals(w.getStatus())) {
            throw new BusinessException(ErrorCode.WITHDRAW_NOT_ALLOWED, "仅待审批状态可取消");
        }
        boolean isApplicant = w.getApplicantAdminId().equals(adminId);
        if (!isApplicant && !rbacService.isSuperAdmin(adminId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权取消该提现单");
        }

        String before = w.getStatus();
        w.setStatus(WithdrawalStatus.CANCELLED);
        w.setUpdatedAt(Instant.now());
        withdrawalRepo.save(w);

        ledgerService.recordWithdrawRelease(w.getWithdrawalNo(), w.getAmount());

        auditLogService.log(adminId, "WITHDRAW_CANCEL", "WITHDRAWAL", withdrawalNo,
                before, WithdrawalStatus.CANCELLED, null);

        return toDetail(w);
    }

    @Transactional
    public WithdrawalDetailDto confirmPayout(Long adminId, String withdrawalNo, ConfirmPayoutRequest req) {
        rbacService.requireManageFinance(adminId);
        WithdrawalRequest w = requireWithdrawal(withdrawalNo);
        if (!WithdrawalStatus.APPROVED.equals(w.getStatus())
                && !WithdrawalStatus.PAYOUT_TIMEOUT.equals(w.getStatus())) {
            throw new BusinessException(ErrorCode.WITHDRAW_NOT_ALLOWED, "当前状态不可确认打款");
        }

        if (req.actualAmount().compareTo(w.getAmount()) != 0
                && (req.varianceReason() == null || req.varianceReason().isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "实际打款金额与申请不一致时需填写差额原因");
        }

        String before = w.getStatus();
        w.setActualAmount(req.actualAmount());
        w.setPaidAt(req.paidAt());
        w.setExternalVoucherNo(req.externalVoucherNo().trim());
        w.setVarianceReason(req.varianceReason());
        w.setPayoutConfirmerId(adminId);
        w.setStatus(WithdrawalStatus.COMPLETED);
        w.setUpdatedAt(Instant.now());
        withdrawalRepo.save(w);

        ledgerService.recordWithdrawComplete(w.getWithdrawalNo());

        auditLogService.log(adminId, "WITHDRAW_PAYOUT", "WITHDRAWAL", withdrawalNo,
                before, WithdrawalStatus.COMPLETED,
                "actual=" + req.actualAmount() + ", voucher=" + req.externalVoucherNo());

        return toDetail(w);
    }

    @Transactional
    public int markPayoutTimeout(Instant deadline) {
        List<WithdrawalRequest> overdue = withdrawalRepo.findByStatusAndApprovedAtBefore(
                WithdrawalStatus.APPROVED, deadline);
        for (WithdrawalRequest w : overdue) {
            String before = w.getStatus();
            w.setStatus(WithdrawalStatus.PAYOUT_TIMEOUT);
            w.setUpdatedAt(Instant.now());
            withdrawalRepo.save(w);
            auditLogService.log(null, "WITHDRAW_PAYOUT_TIMEOUT", "WITHDRAWAL", w.getWithdrawalNo(),
                    before, WithdrawalStatus.PAYOUT_TIMEOUT, "系统自动标记打款超时");
        }
        return overdue.size();
    }

    public List<WithdrawalRequest> listAllForExport(String status) {
        if (status == null || status.isBlank()) {
            return withdrawalRepo.findAll();
        }
        return withdrawalRepo.search(status, null, PageRequest.of(0, 10_000)).getContent();
    }

    private WithdrawalRequest requireWithdrawal(String withdrawalNo) {
        return withdrawalRepo.findByWithdrawalNo(withdrawalNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "提现单不存在"));
    }

    private WithdrawalSummaryDto toSummary(WithdrawalRequest w, Map<Long, AdminUser> admins,
                                           Map<Long, PayoutAccount> accounts) {
        PayoutAccount account = accounts.get(w.getPayoutAccountId());
        String accountSummary = account != null
                ? payoutAccountService.formatAccountSummary(account) : "账户#" + w.getPayoutAccountId();
        return new WithdrawalSummaryDto(
                w.getWithdrawalNo(),
                w.getAmount(),
                w.getActualAmount(),
                w.getStatus(),
                w.getPayoutAccountId(),
                accountSummary,
                w.getApplicantAdminId(),
                displayName(admins.get(w.getApplicantAdminId())),
                w.getAppliedAt()
        );
    }

    private WithdrawalDetailDto toDetail(WithdrawalRequest w) {
        Map<Long, AdminUser> admins = loadAdminMap();
        String accountSummary = payoutAccountRepo.findById(w.getPayoutAccountId())
                .map(payoutAccountService::formatAccountSummary)
                .orElse("账户#" + w.getPayoutAccountId());
        return new WithdrawalDetailDto(
                w.getWithdrawalNo(),
                w.getAmount(),
                w.getActualAmount(),
                w.getStatus(),
                w.getPayoutAccountId(),
                accountSummary,
                w.getApplicantAdminId(),
                displayName(admins.get(w.getApplicantAdminId())),
                w.getApproverAdminId(),
                displayName(w.getApproverAdminId() != null ? admins.get(w.getApproverAdminId()) : null),
                w.getPayoutConfirmerId(),
                displayName(w.getPayoutConfirmerId() != null ? admins.get(w.getPayoutConfirmerId()) : null),
                w.getExternalVoucherNo(),
                w.getRemark(),
                w.getRejectReason(),
                w.getVarianceReason(),
                w.getAppliedAt(),
                w.getApprovedAt(),
                w.getPaidAt()
        );
    }

    private Map<Long, AdminUser> loadAdminMap() {
        return adminUserRepo.findAll().stream()
                .collect(Collectors.toMap(AdminUser::getId, Function.identity()));
    }

    private Map<Long, PayoutAccount> loadAccountMap(List<WithdrawalRequest> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = items.stream().map(WithdrawalRequest::getPayoutAccountId).collect(Collectors.toSet());
        return payoutAccountRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(PayoutAccount::getId, Function.identity()));
    }

    private static String displayName(AdminUser user) {
        if (user == null) {
            return "-";
        }
        return user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName() : user.getUsername();
    }

    private static Instant startOfTodayShanghai() {
        return ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .toLocalDate().atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
    }

    private static String generateWithdrawalNo() {
        return "WD" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}

package com.wildai.finance.service;

import com.wildai.admin.service.RbacService;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.common.util.AesEncryptUtil;
import com.wildai.finance.domain.PayoutAccount;
import com.wildai.finance.dto.PayoutAccountCreateRequest;
import com.wildai.finance.dto.PayoutAccountSummaryDto;
import com.wildai.finance.dto.PayoutAccountUpdateRequest;
import com.wildai.finance.repository.PayoutAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PayoutAccountService {

    private final PayoutAccountRepository accountRepo;
    private final RbacService rbacService;
    private final AesEncryptUtil aesEncryptUtil;

    public PayoutAccountService(PayoutAccountRepository accountRepo,
                                  RbacService rbacService,
                                  AesEncryptUtil aesEncryptUtil) {
        this.accountRepo = accountRepo;
        this.rbacService = rbacService;
        this.aesEncryptUtil = aesEncryptUtil;
    }

    public List<PayoutAccountSummaryDto> list(Long adminId, boolean enabledOnly) {
        rbacService.requireViewFinance(adminId);
        List<PayoutAccount> accounts = enabledOnly
                ? accountRepo.findByEnabledTrueOrderByIdAsc()
                : accountRepo.findAll();
        if (enabledOnly || !rbacService.isSuperAdmin(adminId)) {
            return accounts.stream().filter(PayoutAccount::isEnabled).map(this::toSummary).toList();
        }
        return accounts.stream().map(this::toSummary).toList();
    }

    /** 财务创建提现时可选的启用账户 */
    public List<PayoutAccountSummaryDto> listEnabledForWithdraw(Long adminId) {
        rbacService.requireManageFinance(adminId);
        return accountRepo.findByEnabledTrueOrderByIdAsc().stream().map(this::toSummary).toList();
    }

    @Transactional
    public PayoutAccountSummaryDto create(Long adminId, PayoutAccountCreateRequest req) {
        rbacService.requireManageAdmins(adminId);
        PayoutAccount account = new PayoutAccount();
        account.setAccountName(req.accountName().trim());
        account.setBankName(req.bankName().trim());
        account.setAccountNoEnc(aesEncryptUtil.encrypt(req.accountNo().trim()));
        account.setEnabled(true);
        accountRepo.save(account);
        return toSummary(account);
    }

    @Transactional
    public PayoutAccountSummaryDto update(Long adminId, Long id, PayoutAccountUpdateRequest req) {
        rbacService.requireManageAdmins(adminId);
        PayoutAccount account = requireAccount(id);
        if (req.accountName() != null && !req.accountName().isBlank()) {
            account.setAccountName(req.accountName().trim());
        }
        if (req.bankName() != null && !req.bankName().isBlank()) {
            account.setBankName(req.bankName().trim());
        }
        if (req.accountNo() != null && !req.accountNo().isBlank()) {
            account.setAccountNoEnc(aesEncryptUtil.encrypt(req.accountNo().trim()));
        }
        if (req.enabled() != null) {
            account.setEnabled(req.enabled());
        }
        account.setUpdatedAt(Instant.now());
        accountRepo.save(account);
        return toSummary(account);
    }

    public PayoutAccount requireEnabledAccount(Long id) {
        PayoutAccount account = requireAccount(id);
        if (!account.isEnabled()) {
            throw new BusinessException(ErrorCode.PAYOUT_ACCOUNT_INVALID, "收款账户已停用");
        }
        return account;
    }

    public String formatAccountSummary(PayoutAccount account) {
        return account.getBankName() + " " + account.getAccountName() + " ****" + maskLast4(account);
    }

    private PayoutAccount requireAccount(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "收款账户不存在"));
    }

    private PayoutAccountSummaryDto toSummary(PayoutAccount account) {
        return new PayoutAccountSummaryDto(
                account.getId(),
                account.getAccountName(),
                account.getBankName(),
                maskLast4(account),
                account.isEnabled()
        );
    }

    private String maskLast4(PayoutAccount account) {
        try {
            String plain = aesEncryptUtil.decrypt(account.getAccountNoEnc());
            if (plain.length() <= 4) {
                return plain;
            }
            return plain.substring(plain.length() - 4);
        } catch (Exception e) {
            return "****";
        }
    }
}

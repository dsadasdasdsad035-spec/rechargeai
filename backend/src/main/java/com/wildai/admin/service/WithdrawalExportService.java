package com.wildai.admin.service;

import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.finance.domain.PayoutAccount;
import com.wildai.finance.domain.WithdrawalRequest;
import com.wildai.finance.repository.PayoutAccountRepository;
import com.wildai.finance.service.PayoutAccountService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WithdrawalExportService {

    private final AdminUserRepository adminUserRepo;
    private final PayoutAccountRepository payoutAccountRepo;
    private final PayoutAccountService payoutAccountService;

    public WithdrawalExportService(AdminUserRepository adminUserRepo,
                                   PayoutAccountRepository payoutAccountRepo,
                                   PayoutAccountService payoutAccountService) {
        this.adminUserRepo = adminUserRepo;
        this.payoutAccountRepo = payoutAccountRepo;
        this.payoutAccountService = payoutAccountService;
    }

    public String exportCsv(java.util.List<WithdrawalRequest> withdrawals) {
        Map<Long, String> adminNames = adminUserRepo.findAll().stream()
                .collect(Collectors.toMap(
                        u -> u.getId(),
                        u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername()));
        Map<Long, PayoutAccount> accounts = payoutAccountRepo.findAll().stream()
                .collect(Collectors.toMap(PayoutAccount::getId, Function.identity()));

        StringBuilder sb = new StringBuilder(
                "withdrawalNo,amount,actualAmount,status,applicant,payoutAccount,appliedAt,paidAt,voucher\n");
        for (WithdrawalRequest w : withdrawals) {
            PayoutAccount acc = accounts.get(w.getPayoutAccountId());
            String accSummary = acc != null ? payoutAccountService.formatAccountSummary(acc) : "-";
            sb.append(w.getWithdrawalNo()).append(',')
                    .append(w.getAmount()).append(',')
                    .append(w.getActualAmount() != null ? w.getActualAmount() : "").append(',')
                    .append(w.getStatus()).append(',')
                    .append(adminNames.getOrDefault(w.getApplicantAdminId(), "-")).append(',')
                    .append("\"").append(accSummary.replace("\"", "\"\"")).append("\"").append(',')
                    .append(w.getAppliedAt()).append(',')
                    .append(w.getPaidAt() != null ? w.getPaidAt() : "").append(',')
                    .append(w.getExternalVoucherNo() != null ? w.getExternalVoucherNo() : "").append('\n');
        }
        return sb.toString();
    }
}

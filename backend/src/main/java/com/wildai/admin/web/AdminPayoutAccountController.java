package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import com.wildai.finance.dto.PayoutAccountCreateRequest;
import com.wildai.finance.dto.PayoutAccountSummaryDto;
import com.wildai.finance.dto.PayoutAccountUpdateRequest;
import com.wildai.finance.service.PayoutAccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/payout-accounts")
public class AdminPayoutAccountController {

    private final PayoutAccountService payoutAccountService;

    public AdminPayoutAccountController(PayoutAccountService payoutAccountService) {
        this.payoutAccountService = payoutAccountService;
    }

    @GetMapping
    public ApiResponse<List<PayoutAccountSummaryDto>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean enabledOnly) {
        return ApiResponse.ok(payoutAccountService.list(principal.id(), enabledOnly));
    }

    @PostMapping
    public ApiResponse<PayoutAccountSummaryDto> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PayoutAccountCreateRequest req) {
        return ApiResponse.ok(payoutAccountService.create(principal.id(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<PayoutAccountSummaryDto> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody PayoutAccountUpdateRequest req) {
        return ApiResponse.ok(payoutAccountService.update(principal.id(), id, req));
    }
}

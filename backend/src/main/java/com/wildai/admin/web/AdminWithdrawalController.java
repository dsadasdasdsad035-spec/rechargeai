package com.wildai.admin.web;

import com.wildai.admin.service.RbacService;
import com.wildai.admin.service.WithdrawalExportService;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.common.security.UserPrincipal;
import com.wildai.finance.dto.*;
import com.wildai.finance.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/withdrawals")
public class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;
    private final WithdrawalExportService exportService;
    private final RbacService rbacService;

    public AdminWithdrawalController(WithdrawalService withdrawalService,
                                     WithdrawalExportService exportService,
                                     RbacService rbacService) {
        this.withdrawalService = withdrawalService;
        this.exportService = exportService;
        this.rbacService = rbacService;
    }

    @GetMapping
    public ApiResponse<PageResult<WithdrawalSummaryDto>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String withdrawalNo,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(withdrawalService.list(
                principal.id(), status, withdrawalNo, pageNo, pageSize));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestParam(required = false) String status) {
        rbacService.requireViewFinance(principal.id());
        String csv = exportService.exportCsv(withdrawalService.listAllForExport(status));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=withdrawals.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/{withdrawalNo}")
    public ApiResponse<WithdrawalDetailDto> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable String withdrawalNo) {
        return ApiResponse.ok(withdrawalService.getDetail(principal.id(), withdrawalNo));
    }

    @PostMapping
    public ApiResponse<WithdrawalDetailDto> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody CreateWithdrawalRequest req) {
        return ApiResponse.ok(withdrawalService.create(principal.id(), req));
    }

    @PostMapping("/{withdrawalNo}/approve")
    public ApiResponse<WithdrawalDetailDto> approve(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable String withdrawalNo) {
        return ApiResponse.ok(withdrawalService.approve(principal.id(), withdrawalNo));
    }

    @PostMapping("/{withdrawalNo}/reject")
    public ApiResponse<WithdrawalDetailDto> reject(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable String withdrawalNo,
                                                   @Valid @RequestBody RejectWithdrawalRequest req) {
        return ApiResponse.ok(withdrawalService.reject(principal.id(), withdrawalNo, req));
    }

    @PostMapping("/{withdrawalNo}/cancel")
    public ApiResponse<WithdrawalDetailDto> cancel(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable String withdrawalNo) {
        return ApiResponse.ok(withdrawalService.cancel(principal.id(), withdrawalNo));
    }

    @PostMapping("/{withdrawalNo}/confirm-payout")
    public ApiResponse<WithdrawalDetailDto> confirmPayout(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String withdrawalNo,
            @Valid @RequestBody ConfirmPayoutRequest req) {
        return ApiResponse.ok(withdrawalService.confirmPayout(principal.id(), withdrawalNo, req));
    }
}

package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.common.security.UserPrincipal;
import com.wildai.finance.dto.FinanceOverviewDto;
import com.wildai.finance.dto.LedgerEntryDto;
import com.wildai.finance.service.FinanceOverviewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/admin/api/finance")
public class AdminFinanceController {

    private final FinanceOverviewService financeOverviewService;

    public AdminFinanceController(FinanceOverviewService financeOverviewService) {
        this.financeOverviewService = financeOverviewService;
    }

    @GetMapping("/overview")
    public ApiResponse<FinanceOverviewDto> overview(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(financeOverviewService.getOverview(principal.id()));
    }

    @GetMapping("/ledger")
    public ApiResponse<PageResult<LedgerEntryDto>> ledger(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(financeOverviewService.listLedger(
                principal.id(), entryType, startTime, endTime, pageNo, pageSize));
    }
}

package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.common.security.UserPrincipal;
import com.wildai.refund.dto.AdminRefundDetailDto;
import com.wildai.refund.dto.AdminRefundSummaryDto;
import com.wildai.refund.dto.RefundReviewRequest;
import com.wildai.refund.service.AdminRefundService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/refunds")
public class AdminRefundController {

    private final AdminRefundService refundService;

    public AdminRefundController(AdminRefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminRefundSummaryDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String refundNo,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(refundService.listRefunds(status, orderNo, refundNo, pageNo, pageSize));
    }

    @GetMapping("/{refundNo}")
    public ApiResponse<AdminRefundDetailDto> detail(@PathVariable String refundNo) {
        return ApiResponse.ok(refundService.getDetail(refundNo));
    }

    @PostMapping("/{refundNo}/approve")
    public ApiResponse<AdminRefundDetailDto> approve(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable String refundNo,
                                                     @Valid @RequestBody RefundReviewRequest req) {
        return ApiResponse.ok(refundService.approve(refundNo, principal.id(), req));
    }

    @PostMapping("/{refundNo}/reject")
    public ApiResponse<AdminRefundDetailDto> reject(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable String refundNo,
                                                    @Valid @RequestBody RefundReviewRequest req) {
        return ApiResponse.ok(refundService.reject(refundNo, principal.id(), req));
    }

    @PostMapping("/{refundNo}/retry-channel")
    public ApiResponse<AdminRefundDetailDto> retryChannel(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable String refundNo) {
        return ApiResponse.ok(refundService.retryChannelRefund(refundNo, principal.id()));
    }
}

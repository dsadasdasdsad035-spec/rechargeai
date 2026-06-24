package com.wildai.refund.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import com.wildai.refund.dto.RefundApplyRequest;
import com.wildai.refund.dto.RefundRequestDto;
import com.wildai.refund.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderNo}/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ApiResponse<RefundRequestDto> apply(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable String orderNo,
                                               @Valid @RequestBody RefundApplyRequest req) {
        return ApiResponse.ok(refundService.apply(orderNo, principal.id(), req));
    }

    @GetMapping
    public ApiResponse<RefundRequestDto> latest(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable String orderNo) {
        RefundRequestDto dto = refundService.getLatestForOrder(orderNo, principal.id());
        return ApiResponse.ok(dto);
    }
}

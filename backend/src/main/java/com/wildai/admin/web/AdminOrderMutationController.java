package com.wildai.admin.web;

import com.wildai.admin.dto.ForceOrderStatusRequest;
import com.wildai.admin.service.AdminOrderMutationService;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/api/orders")
public class AdminOrderMutationController {

    private final AdminOrderMutationService mutationService;

    public AdminOrderMutationController(AdminOrderMutationService mutationService) {
        this.mutationService = mutationService;
    }

    @PostMapping("/{orderNo}/force-status")
    public ApiResponse<Map<String, String>> forceStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable String orderNo,
                                                        @Valid @RequestBody ForceOrderStatusRequest req) {
        mutationService.forceOrderStatus(orderNo, principal.id(), req);
        return ApiResponse.ok(Map.of("message", "状态已强制更新"));
    }
}

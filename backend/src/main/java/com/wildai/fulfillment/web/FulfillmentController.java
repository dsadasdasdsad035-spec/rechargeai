package com.wildai.fulfillment.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import com.wildai.fulfillment.dto.FulfillmentSupplementRequest;
import com.wildai.fulfillment.service.FulfillmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders/{orderNo}/fulfillment")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping("/supplement")
    public ApiResponse<Map<String, String>> supplement(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable String orderNo,
                                                       @Valid @RequestBody FulfillmentSupplementRequest req) {
        fulfillmentService.submitSupplement(orderNo, principal.id(), req);
        return ApiResponse.ok(Map.of("status", "PROCESSING"));
    }

    @PostMapping("/confirm")
    public ApiResponse<Map<String, String>> confirm(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable String orderNo) {
        fulfillmentService.confirmUserAction(orderNo, principal.id());
        return ApiResponse.ok(Map.of("status", "PROCESSING"));
    }
}

package com.wildai.order.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import com.wildai.payment.service.PaymentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderPayController {

    private final PaymentService paymentService;

    public OrderPayController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderNo}/pay")
    public ApiResponse<Map<String, String>> pay(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable String orderNo,
                                                @RequestBody Map<String, String> body) {
        return ApiResponse.ok(paymentService.createPayment(orderNo, principal.id(), body.get("channel")));
    }
}

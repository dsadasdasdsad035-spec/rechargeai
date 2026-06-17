package com.wildai.payment.web;

import com.wildai.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentCallbackController {

    private final PaymentService paymentService;

    public PaymentCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/wechat/notify")
    public Map<String, String> wechatNotify(@RequestParam Map<String, String> params, @RequestBody(required = false) String body) {
        paymentService.handleCallback("WECHAT", params, body);
        return Map.of("code", "SUCCESS");
    }

    @PostMapping("/alipay/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        paymentService.handleCallback("ALIPAY", params, null);
        return "success";
    }

    @PostMapping("/mock/notify")
    public Map<String, String> mockNotify(@RequestParam Map<String, String> params) {
        Map<String, String> all = new HashMap<>(params);
        all.putIfAbsent("paymentNo", params.get("paymentNo"));
        paymentService.handleCallback("MOCK", all, null);
        return Map.of("code", "SUCCESS");
    }
}

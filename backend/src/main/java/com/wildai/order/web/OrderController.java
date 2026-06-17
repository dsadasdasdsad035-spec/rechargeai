package com.wildai.order.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import com.wildai.order.dto.CreateOrderRequest;
import com.wildai.order.dto.OrderDetailDto;
import com.wildai.order.dto.OrderSummaryDto;
import com.wildai.common.dto.PageResult;
import com.wildai.order.service.OrderQueryService;
import com.wildai.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    public OrderController(OrderService orderService, OrderQueryService orderQueryService) {
        this.orderService = orderService;
        this.orderQueryService = orderQueryService;
    }

    @PostMapping
    public ApiResponse<Map<String, String>> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody CreateOrderRequest req) {
        var order = orderService.createOrder(principal.id(), req);
        return ApiResponse.ok(Map.of("orderNo", order.getOrderNo()));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderSummaryDto>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(orderQueryService.listUserOrders(
                principal.id(), orderStatus, paymentStatus, startTime, endTime, pageNo, pageSize));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetailDto> detail(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable String orderNo) {
        return ApiResponse.ok(orderQueryService.getUserOrderDetail(orderNo, principal.id()));
    }
}

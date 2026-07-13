package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.order.dto.AdminOrderDetailDto;
import com.wildai.order.dto.OrderDetailDto;
import com.wildai.order.dto.OrderSummaryDto;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.order.service.OrderQueryService;
import com.wildai.admin.service.OrderExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/admin/api/orders")
public class AdminOrderController {

    private final OrderQueryService orderQueryService;
    private final SubscriptionOrderRepository orderRepo;
    private final OrderExportService exportService;

    public AdminOrderController(OrderQueryService orderQueryService, SubscriptionOrderRepository orderRepo,
                                OrderExportService exportService) {
        this.orderQueryService = orderQueryService;
        this.orderRepo = orderRepo;
        this.exportService = exportService;
    }

    @GetMapping
    public ApiResponse<PageResult<OrderSummaryDto>> list(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(orderQueryService.listAdminOrders(
                blankToNull(orderNo), userId, blankToNull(orderStatus), blankToNull(paymentStatus),
                startTime, endTime, pageNo, pageSize));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<AdminOrderDetailDto> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderQueryService.getAdminOrderDetail(orderNo));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        var orders = orderRepo.searchAdminOrdersForExport(
                blankToNull(orderNo), userId, blankToNull(orderStatus), blankToNull(paymentStatus), startTime, endTime);
        String csv = exportService.exportCsv(orders);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

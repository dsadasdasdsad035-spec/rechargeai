package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.product.dto.ServiceTypeConfigUpdateRequest;
import com.wildai.product.dto.ServiceTypeGuideDto;
import com.wildai.product.service.ServiceTypeConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/service-types")
public class AdminServiceTypeController {

    private final ServiceTypeConfigService serviceTypeConfigService;

    public AdminServiceTypeController(ServiceTypeConfigService serviceTypeConfigService) {
        this.serviceTypeConfigService = serviceTypeConfigService;
    }

    @GetMapping
    public ApiResponse<List<ServiceTypeGuideDto>> list() {
        return ApiResponse.ok(serviceTypeConfigService.listAll());
    }

    @PutMapping("/{serviceType}")
    public ApiResponse<ServiceTypeGuideDto> update(
            @PathVariable String serviceType,
            @RequestBody ServiceTypeConfigUpdateRequest req) {
        return ApiResponse.ok(serviceTypeConfigService.update(serviceType, req));
    }
}

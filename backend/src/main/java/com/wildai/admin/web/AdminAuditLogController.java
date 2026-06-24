package com.wildai.admin.web;

import com.wildai.admin.dto.AuditLogDto;
import com.wildai.admin.service.AdminAuditLogQueryService;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.common.security.UserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/admin/api/audit-logs")
public class AdminAuditLogController {

    private final AdminAuditLogQueryService auditLogQueryService;

    public AdminAuditLogController(AdminAuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping
    public ApiResponse<PageResult<AuditLogDto>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(auditLogQueryService.search(
                principal.id(), operatorId, operationType, targetType, targetId,
                startTime, endTime, pageNo, pageSize));
    }
}

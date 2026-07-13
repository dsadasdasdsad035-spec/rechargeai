package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.common.security.UserPrincipal;
import com.wildai.support.dto.SupportMessageDto;
import com.wildai.support.dto.SupportSendMessageRequest;
import com.wildai.support.dto.SupportSessionDto;
import com.wildai.support.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/support/sessions")
public class AdminSupportController {

    private final SupportService supportService;

    public AdminSupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping
    public ApiResponse<PageResult<SupportSessionDto>> list(@RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(supportService.listAdminSessions(pageNo, pageSize));
    }

    @GetMapping("/{sessionNo}/messages")
    public ApiResponse<PageResult<SupportMessageDto>> messages(@PathVariable String sessionNo,
                                                               @RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.ok(supportService.getAdminMessages(sessionNo, pageNo, pageSize));
    }

    @PostMapping("/{sessionNo}/messages")
    public ApiResponse<SupportMessageDto> send(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable String sessionNo,
                                               @Valid @RequestBody SupportSendMessageRequest req) {
        return ApiResponse.ok(supportService.sendAdminMessage(principal.id(), sessionNo, req));
    }
}

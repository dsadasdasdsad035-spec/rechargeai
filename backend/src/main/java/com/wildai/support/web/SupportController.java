package com.wildai.support.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.common.security.UserPrincipal;
import com.wildai.support.dto.SupportCreateSessionRequest;
import com.wildai.support.dto.SupportMessageDto;
import com.wildai.support.dto.SupportSendMessageRequest;
import com.wildai.support.dto.SupportSessionDto;
import com.wildai.support.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support/sessions")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping
    public ApiResponse<SupportSessionDto> create(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody SupportCreateSessionRequest req) {
        return ApiResponse.ok(supportService.createSession(principal.id(), req));
    }

    @GetMapping
    public ApiResponse<PageResult<SupportSessionDto>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(supportService.listUserSessions(principal.id(), pageNo, pageSize));
    }

    @GetMapping("/{sessionNo}/messages")
    public ApiResponse<PageResult<SupportMessageDto>> messages(@AuthenticationPrincipal UserPrincipal principal,
                                                               @PathVariable String sessionNo,
                                                               @RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.ok(supportService.getUserMessages(principal.id(), sessionNo, pageNo, pageSize));
    }

    @PostMapping("/{sessionNo}/messages")
    public ApiResponse<SupportMessageDto> send(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable String sessionNo,
                                               @Valid @RequestBody SupportSendMessageRequest req) {
        return ApiResponse.ok(supportService.sendUserMessage(principal.id(), sessionNo, req));
    }
}

package com.wildai.user.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import com.wildai.user.dto.UserProfileDto;
import com.wildai.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getProfile(principal.id()));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileDto> update(@AuthenticationPrincipal UserPrincipal principal,
                                              @RequestBody Map<String, String> body) {
        return ApiResponse.ok(userService.updateProfile(principal.id(), body.get("nickname"), body.get("avatarUrl")));
    }
}

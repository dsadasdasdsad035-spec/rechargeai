package com.wildai.admin.web;

import com.wildai.auth.domain.UserAccount;
import com.wildai.auth.repository.UserAccountRepository;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/users")
public class AdminUserController {

    private final UserAccountRepository userRepo;

    public AdminUserController(UserAccountRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping
    public ApiResponse<List<UserAccount>> list() {
        return ApiResponse.ok(userRepo.findAll());
    }

    @PostMapping("/{id}/status")
    public ApiResponse<UserAccount> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        UserAccount user = userRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.setStatus(body.get("status"));
        return ApiResponse.ok(userRepo.save(user));
    }
}

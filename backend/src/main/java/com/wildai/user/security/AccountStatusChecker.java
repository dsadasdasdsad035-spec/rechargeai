package com.wildai.user.security;

import com.wildai.user.service.UserService;
import org.springframework.stereotype.Component;

/**
 * 下单前校验用户账户是否可交易（非 DISABLED）。
 */
@Component
public class AccountStatusChecker {

    private final UserService userService;

    public AccountStatusChecker(UserService userService) {
        this.userService = userService;
    }

    public void ensureCanTrade(Long userId) {
        userService.ensureCanTrade(userId);
    }
}

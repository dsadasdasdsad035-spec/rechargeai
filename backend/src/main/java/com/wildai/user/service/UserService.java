package com.wildai.user.service;

import com.wildai.auth.domain.UserAccount;
import com.wildai.auth.repository.UserAccountRepository;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.common.util.DesensitizeUtil;
import com.wildai.user.dto.UserProfileDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserAccountRepository userRepo;

    public UserService(UserAccountRepository userRepo) {
        this.userRepo = userRepo;
    }

    public UserProfileDto getProfile(Long userId) {
        UserAccount user = userRepo.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return toDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, String nickname, String avatarUrl) {
        UserAccount user = userRepo.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (nickname != null) user.setNickname(nickname);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
        return toDto(user);
    }

    public void ensureCanTrade(Long userId) {
        UserAccount user = userRepo.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!"NORMAL".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private UserProfileDto toDto(UserAccount user) {
        return new UserProfileDto(
                user.getId(), user.getUserNo(),
                DesensitizeUtil.phone(user.getPhone()),
                DesensitizeUtil.email(user.getEmail()),
                user.getNickname(), user.getAvatarUrl(), user.getStatus());
    }
}

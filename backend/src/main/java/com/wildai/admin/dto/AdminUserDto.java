package com.wildai.admin.dto;

import java.util.List;

public record AdminUserDto(
        Long id,
        String username,
        String displayName,
        String status,
        List<String> roleCodes
) {}

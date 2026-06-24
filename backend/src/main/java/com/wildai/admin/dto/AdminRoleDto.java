package com.wildai.admin.dto;

import java.util.List;

public record AdminRoleDto(
        Long id,
        String roleCode,
        String roleName
) {}

package com.wildai.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAdminRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 6, max = 64) String password,
        @Size(max = 64) String displayName,
        List<String> roleCodes
) {}

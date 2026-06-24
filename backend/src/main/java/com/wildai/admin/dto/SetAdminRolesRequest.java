package com.wildai.admin.dto;

import java.util.List;

public record SetAdminRolesRequest(
        List<String> roleCodes
) {}

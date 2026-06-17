package com.wildai.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, String userNo) {}

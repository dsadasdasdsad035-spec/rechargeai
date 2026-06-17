package com.wildai.auth.dto;

public record LoginRequest(String phone, String email, String password, String verifyCode) {}

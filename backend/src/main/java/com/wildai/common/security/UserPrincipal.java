package com.wildai.common.security;

public record UserPrincipal(Long id, String identifier, String audience) {
}

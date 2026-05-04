package com.weaver.weaver_backend.dto.request.auth;


import com.weaver.weaver_backend.common.AuthProvider;

public record LoginViaOAuthRequest(String email, AuthProvider provider, String providerId) {
}

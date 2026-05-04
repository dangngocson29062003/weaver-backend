package com.weaver.weaver_backend.common;

public enum AuthProvider {
    LOCAL,
    GOOGLE;
    public static AuthProvider from(String registrationId) {
        return AuthProvider.valueOf(registrationId.toUpperCase());
    }
}

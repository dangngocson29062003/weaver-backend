package com.weaver.weaver_backend.security.oauth2;

public interface OAuth2UserInfo {
    String getEmail();
    String getProviderId();
    String getName();
}

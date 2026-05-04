package com.weaver.weaver_backend.security.oauth2;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo get(String provider,
                                     Map<String, Object> attributes) {

        return switch (provider) {
            case "google" -> new GoogleUserInfo(attributes);
            default -> throw new RuntimeException("Unsupported provider");
        };
    }
}

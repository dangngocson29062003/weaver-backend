package com.weaver.weaver_backend.constant;

import org.springframework.beans.factory.annotation.Value;

public final class JWTConstant {
    @Value("${app.web-url}")
    public static String JWT_ISSUER;
    public static final String TOKEN_TYPE = "type";
}

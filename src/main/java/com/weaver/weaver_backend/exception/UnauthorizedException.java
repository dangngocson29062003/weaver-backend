package com.weaver.weaver_backend.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message) {
        super(401, HttpStatus.UNAUTHORIZED, message);
    }
}

package com.weaver.weaver_backend.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException{
    public ForbiddenException(String message) {
        super(403, HttpStatus.FORBIDDEN, message);
    }
}

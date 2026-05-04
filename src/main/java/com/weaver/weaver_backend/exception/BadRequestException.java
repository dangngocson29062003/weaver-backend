package com.weaver.weaver_backend.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends AppException{
    public BadRequestException(String message) {
        super(400, HttpStatus.FORBIDDEN, message);
    }
}

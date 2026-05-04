package com.weaver.weaver_backend.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AppException{
    public NotFoundException(String message) {
        super(404, HttpStatus.NOT_FOUND, message);
    }
}

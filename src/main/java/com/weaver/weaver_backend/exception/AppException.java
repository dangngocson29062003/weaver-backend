package com.weaver.weaver_backend.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class AppException extends RuntimeException{
    private final int status;
    private final HttpStatus httpStatus;

    public AppException(int status, HttpStatus httpStatus, String message) {
        super(message);
        this.status = status;
        this.httpStatus = httpStatus;
    }
}

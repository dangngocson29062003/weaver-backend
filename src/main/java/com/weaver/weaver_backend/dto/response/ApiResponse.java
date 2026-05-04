package com.weaver.weaver_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int status;
    private HttpStatus httpStatus;
    private String message;
    private T data;
    private Instant timestamp;
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .httpStatus(HttpStatus.OK)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }
    public static ApiResponse<Void> error(
            int status,
            HttpStatus httpStatus,
            String message
    ){
        return ApiResponse.<Void>builder()
                .status(status)
                .httpStatus(httpStatus)
                .message(message)
                .data(null)
                .timestamp(Instant.now())
                .build();

    }
    public static <T> ApiResponse<T> error(
            int status,
            HttpStatus httpStatus,
            String message,
            T data
    ){
        return ApiResponse.<T>builder()
                .status(status)
                .httpStatus(httpStatus)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }
}

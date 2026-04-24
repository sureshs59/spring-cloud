package com.example.mulitpleapi.dto;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Builder
public class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true).data(data)
                .timestamp(LocalDateTime.now()).build();
    }
    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.<T>builder()
                .success(false).message(msg)
                .timestamp(LocalDateTime.now()).build();
    }
}

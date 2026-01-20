package com.thunder11.fitline_be.common.response;

import lombok.Getter;

@Getter
public class ApiResponse <T>{
    private final int status;
    private final String code;
    private final String message;
    private final T data;

    public ApiResponse(int status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> of(int status, String code, String message, T data) {
        return new ApiResponse<>(status, code, message, data);
    }

    public static ApiResponse<Void> of(int status, String code, String message) {
        return new ApiResponse<>(status, code, message, null);
    }

}

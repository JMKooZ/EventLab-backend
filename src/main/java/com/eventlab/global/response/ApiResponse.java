package com.eventlab.global.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<T>(true, data, message);
    }

    public static ApiResponse<Void> error(String message){
        return new ApiResponse<>(false, null, message);
    }
}

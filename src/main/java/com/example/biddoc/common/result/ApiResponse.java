package com.example.biddoc.common.result;

import com.example.biddoc.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, System.currentTimeMillis());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                System.currentTimeMillis()
        );
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
                errorCode.getCode(),
                message,
                null,
                System.currentTimeMillis()
        );
    }
}

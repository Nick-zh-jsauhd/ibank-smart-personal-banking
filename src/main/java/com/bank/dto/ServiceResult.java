package com.bank.dto;

public class ServiceResult<T> {
    private final boolean success;
    private final String message;
    private final T data;

    private ServiceResult(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ServiceResult<T> success(String message, T data) {
        return new ServiceResult<T>(true, message, data);
    }

    public static <T> ServiceResult<T> failure(String message) {
        return new ServiceResult<T>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}

package com.zinhao.kikoeru.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 结果包装类，用于 LiveData 返回数据和状态
 * @param <T> 数据类型
 */
public class Result<T> {

    public enum Status {
        LOADING,    // 加载中
        SUCCESS,    // 成功
        ERROR       // 失败
    }

    @NonNull
    private final Status status;

    @Nullable
    private final T data;

    @Nullable
    private final String message;

    @Nullable
    private final Throwable error;

    private Result(@NonNull Status status, @Nullable T data, @Nullable String message, @Nullable Throwable error) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.error = error;
    }

    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null, null);
    }

    public static <T> Result<T> loading(@Nullable T data) {
        return new Result<>(Status.LOADING, data, null, null);
    }

    public static <T> Result<T> success(@NonNull T data) {
        return new Result<>(Status.SUCCESS, data, null, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(Status.ERROR, null, message, null);
    }

    public static <T> Result<T> error(String message, Throwable error) {
        return new Result<>(Status.ERROR, null, message, error);
    }

    public static <T> Result<T> error(String message, @Nullable T data) {
        return new Result<>(Status.ERROR, data, message, null);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public Throwable getError() {
        return error;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    @NonNull
    @Override
    public String toString() {
        return "Result{" +
                "status=" + status +
                ", data=" + data +
                ", message='" + message + '\'' +
                ", error=" + error +
                '}';
    }
}

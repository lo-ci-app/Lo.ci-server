package com.teamloci.loci.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.teamloci.loci.global.exception.code.BaseErrorCode;
import com.teamloci.loci.global.exception.code.GeneralSuccessCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"timestamp", "isSuccess", "code", "message", "result"})
public class CustomResponse<T> {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private final T result;

    public static <T> CustomResponse<T> ok(T result) {
        return new CustomResponse<>(
                true,
                GeneralSuccessCode.OK.getCode(),
                GeneralSuccessCode.OK.getMessage(),
                result
        );
    }

    public static <T> CustomResponse<T> created(T result) {
        return new CustomResponse<>(
                true,
                GeneralSuccessCode.CREATED.getCode(),
                GeneralSuccessCode.CREATED.getMessage(),
                result
        );
    }

    public static <T> CustomResponse<T> onFailure(BaseErrorCode code) {
        return new CustomResponse<>(
                false,
                code.getCode(),
                code.getMessage(),
                null
        );
    }

    public static <T> CustomResponse<T> onFailure(BaseErrorCode code, String message) {
        return new CustomResponse<>(
                false,
                code.getCode(),
                message,
                null
        );
    }
}
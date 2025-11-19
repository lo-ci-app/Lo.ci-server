package com.teamloci.loci.global.exception;

import com.teamloci.loci.global.exception.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final BaseErrorCode code;

    public CustomException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode;
    }

    public CustomException(BaseErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode;
    }
}
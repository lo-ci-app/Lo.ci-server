package com.teamloci.loci.global.exception;

import com.teamloci.loci.global.exception.code.BaseErrorCode;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.response.CustomResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomResponse<Void>> handleCustomException(CustomException ex) {
        BaseErrorCode code = ex.getCode();
        log.warn("[CustomException]: {}", code.getMessage());

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(CustomResponse.onFailure(code));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {

        BaseErrorCode code = ErrorCode.INVALID_REQUEST;

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s : %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        if (errorMessage.isEmpty()) errorMessage = code.getMessage();

        log.warn("[InvalidRequest]: {}", errorMessage);

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(CustomResponse.onFailure(code, errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomResponse<Void>> handleAllException(Exception ex) {
        log.error("[Exception]: {}", ex.getMessage(), ex);

        BaseErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CustomResponse.onFailure(errorCode));
    }
}
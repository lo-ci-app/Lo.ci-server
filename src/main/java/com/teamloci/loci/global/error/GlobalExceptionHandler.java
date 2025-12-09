package com.teamloci.loci.global.error;

import com.teamloci.loci.global.common.CustomResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CustomResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("[DataIntegrityViolation]: {}", ex.getMessage());
        BaseErrorCode code = ErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(CustomResponse.onFailure(code, "데이터 무결성 위반(중복 데이터 등)이 발생했습니다."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<CustomResponse<Void>> handleDataAccessException(DataAccessException ex) {
        log.error("[DataAccessException]: {}", ex.getMessage());
        BaseErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(CustomResponse.onFailure(code, "데이터베이스 처리 중 오류가 발생했습니다."));
    }
}
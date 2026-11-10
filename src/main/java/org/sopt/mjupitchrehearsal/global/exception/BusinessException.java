package org.sopt.mjupitchrehearsal.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생시키는 예외. GlobalExceptionHandler가 BaseResponse로 변환합니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

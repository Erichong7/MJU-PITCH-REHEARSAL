package org.mju.mjupitchrehearsal.global.exception;

import java.util.Map;
import lombok.Getter;

/**
 * 필드별 검증 실패. 응답 data에 { 필드: 메시지 }로 내려갑니다.
 */
@Getter
public class ValidationException extends BusinessException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super(ErrorCode.INVALID_INPUT);
        this.errors = errors;
    }
}

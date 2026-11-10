package org.mju.mjupitchrehearsal.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 모든 API의 공통 응답 형식. code는 HTTP 상태 코드 문자열입니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(boolean success, String code, String message, T data) {

    public static <T> BaseResponse<T> ok(T data) {
        return of(HttpStatus.OK, "요청에 성공했습니다.", data);
    }

    public static <T> BaseResponse<T> created(T data) {
        return of(HttpStatus.CREATED, "생성에 성공했습니다.", data);
    }

    public static <T> BaseResponse<T> of(HttpStatus status, String message, T data) {
        return new BaseResponse<>(true, String.valueOf(status.value()), message, data);
    }

    public static BaseResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static BaseResponse<Void> fail(ErrorCode errorCode, String message) {
        return new BaseResponse<>(false, String.valueOf(errorCode.getStatus().value()), message, null);
    }
}

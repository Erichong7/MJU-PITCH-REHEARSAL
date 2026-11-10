package org.mju.mjupitchrehearsal.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드. 도메인별 에러는 이 enum에 섹션을 나눠 추가합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "업로드한 파일이 올바르지 않습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기는 25MB를 넘을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    CONFLICT(HttpStatus.CONFLICT, "현재 상태에서 처리할 수 없는 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 서비스 호출에 실패했습니다."),
    STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "녹음 파일 저장에 실패했습니다."),

    // Practice
    PRACTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 연습입니다."),
    PRACTICE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 연습에만 녹음할 수 있습니다."),

    // Attempt
    ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 녹음입니다."),
    ATTEMPT_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 녹음만 조회할 수 있습니다."),
    AUDIO_REQUIRED(HttpStatus.BAD_REQUEST, "녹음 파일이 없습니다."),
    UNSUPPORTED_AUDIO(HttpStatus.BAD_REQUEST, "m4a(AAC) 녹음 파일만 업로드할 수 있습니다."),
    AUDIO_TOO_LONG(HttpStatus.BAD_REQUEST, "녹음이 너무 깁니다."),
    TRANSCRIPT_NOT_READY(HttpStatus.CONFLICT, "아직 전사가 끝나지 않았습니다."),
    TRANSCRIPT_LOCKED(HttpStatus.CONFLICT, "분석 중에는 전사를 수정할 수 없습니다."),
    AUDIO_URL_INVALID(HttpStatus.FORBIDDEN, "재생 링크가 만료되었거나 올바르지 않습니다."),

    // Feedback
    FEEDBACK_NOT_READY(HttpStatus.CONFLICT, "아직 분석이 끝나지 않았습니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 피드백입니다."),
    FEEDBACK_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 피드백만 다시 연습할 수 있습니다."),

    // Retry
    RETRY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 재연습입니다."),
    RETRY_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 재연습만 조회할 수 있습니다."),
    RETRY_LOCKED(HttpStatus.CONFLICT, "녹음을 다시 분석하는 중에는 재연습할 수 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}

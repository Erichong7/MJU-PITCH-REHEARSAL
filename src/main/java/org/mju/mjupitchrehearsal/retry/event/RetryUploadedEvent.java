package org.mju.mjupitchrehearsal.retry.event;

/** 재연습 업로드가 커밋된 뒤 전사·비교를 백그라운드에서 시작하기 위한 이벤트 */
public record RetryUploadedEvent(Long retryId) {
}

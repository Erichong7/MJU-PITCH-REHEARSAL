package org.mju.mjupitchrehearsal.attempt.event;

/** 녹음 업로드가 DB에 커밋된 뒤 백그라운드 분석을 시작하기 위한 이벤트 */
public record AttemptUploadedEvent(Long attemptId) {
}

package org.mju.mjupitchrehearsal.attempt.event;

/** 전사 수정이 커밋된 뒤 STT 없이 분석만 다시 실행하기 위한 이벤트 */
public record TranscriptEditedEvent(Long attemptId) {
}

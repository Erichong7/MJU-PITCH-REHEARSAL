package org.sopt.mjupitchrehearsal.attempt.entity;

/**
 * 녹음·재연습 처리 상태. UPLOADED → TRANSCRIBING → ANALYZING → COMPLETED, 실패 시 FAILED.
 */
public enum AttemptStatus {
    UPLOADED,
    TRANSCRIBING,
    ANALYZING,
    COMPLETED,
    FAILED;

    public boolean isInProgress() {
        return this == UPLOADED || this == TRANSCRIBING || this == ANALYZING;
    }
}

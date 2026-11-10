package org.mju.mjupitchrehearsal.infra.llm.dto;

import org.mju.mjupitchrehearsal.feedback.entity.FeedbackType;
import org.mju.mjupitchrehearsal.practice.entity.Mode;

/**
 * 비교 받기 입력.
 *
 * @param mode       PRESENTATION / INTERVIEW
 * @param feedback   원래 피드백 (1차 구간에 대한 지적)
 * @param beforeText 1차 발언 (피드백 구간 전사)
 * @param afterText  2차 발언 (재녹음 전사)
 */
public record ComparisonRequest(
        Mode mode,
        OriginalFeedback feedback,
        String beforeText,
        String afterText
) {

    public record OriginalFeedback(FeedbackType type, String quote, String reason, String suggestion) {
    }
}

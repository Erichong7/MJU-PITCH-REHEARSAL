package org.mju.mjupitchrehearsal.retry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;

/**
 * ⑭ 재연습 결과. status가 COMPLETED가 아니면 after.text·after.audioUrl·comparison은 null (폴링 겸용).
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RetryResultResponse(
        Long retryId,
        Long feedbackId,
        Integer retryNo,
        AttemptStatus status,
        FailedStep failedStep,
        Before before,
        After after,
        List<Comparison> comparison
) {

    /** 1차 구간 */
    public record Before(Long startMs, Long endMs, Long durationMs, String text) {
    }

    /** 재녹음 */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record After(Long durationMs, String text, String audioUrl) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Comparison(String criterion, String before, String after, boolean resolved, String afterQuote) {
    }
}

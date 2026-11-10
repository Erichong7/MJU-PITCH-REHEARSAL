package org.mju.mjupitchrehearsal.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** ⑨ 피드백 조회 응답. strength는 없으면 null로 내려갑니다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FeedbackListResponse(
        Long attemptId,
        Summary summary,
        Metrics metrics,
        List<Issue> issues,
        Strength strength
) {

    /** overMs: 목표 시간 대비 초과(+) / 미달(-) */
    public record Summary(Long durationMs, Long timeLimitMs, Long overMs) {
    }

    public record Metrics(Double syllablesPerSec, List<FastSegment> fastSegments, List<LongPause> longPauses) {
    }

    public record FastSegment(String segmentId, Long startMs, Double syllablesPerSec) {
    }

    public record LongPause(Long startMs, Long pauseMs) {
    }

    public record Issue(Long feedbackId, Integer rank, String type, String segmentId, Long startMs, Long endMs,
                        String quote, String reason, String suggestion) {
    }

    public record Strength(String segmentId, Long startMs, Long endMs, String quote, String reason) {
    }
}

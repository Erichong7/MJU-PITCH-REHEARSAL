package org.mju.mjupitchrehearsal.infra.llm.dto;

import org.mju.mjupitchrehearsal.practice.entity.Mode;
import java.util.List;

/**
 * 피드백 받기 입력.
 *
 * @param mode      PRESENTATION / INTERVIEW (프롬프트 템플릿 선택용)
 * @param condition 발표 조건
 * @param metrics   말한 방식 수치 (서버가 순수 Java로 계산)
 * @param segments  구간별 전사, 순서대로
 */
public record FeedbackRequest(
        Mode mode,
        Condition condition,
        Metrics metrics,
        List<Segment> segments
) {

    /**
     * @param purpose      발표 목적 (nullable)
     * @param audience     청중 (nullable)
     * @param timeLimitSec 제한 시간(초)
     * @param keyPoints    꼭 전달할 내용 (없으면 빈 리스트)
     */
    public record Condition(String purpose, String audience, Integer timeLimitSec, List<String> keyPoints) {
    }

    /**
     * @param durationMs      발표 시간
     * @param syllablesPerSec 평균 말 속도 (초당 음절)
     * @param fastSegments    본인 평균보다 빨라진 구간
     * @param longPauses      1초 이상 멈춤
     */
    public record Metrics(Long durationMs, Double syllablesPerSec,
                          List<FastSegment> fastSegments, List<LongPause> longPauses) {
    }

    public record FastSegment(String segmentId, Double syllablesPerSec) {
    }

    /** segmentId: 멈춤 직후 시작하는 구간 */
    public record LongPause(String segmentId, Long pauseMs) {
    }

    /** segmentId: seg_01, seg_02 … */
    public record Segment(String segmentId, String text) {
    }
}

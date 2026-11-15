package org.mju.mjupitchrehearsal.infra.llm.dto;

import java.util.List;

/**
 * 전문 용어 교정 입력. STT가 소리 나는 대로 잘못 적은 전문 용어만 고치기 위한 것입니다.
 *
 * @param terms    발표자가 미리 알려 준 전문 용어
 * @param segments 구간별 전사, 순서대로
 */
public record TermCorrectionRequest(
        List<String> terms,
        List<FeedbackRequest.Segment> segments
) {
}

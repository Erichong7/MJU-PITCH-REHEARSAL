package org.sopt.mjupitchrehearsal.infra.llm.dto;

import java.util.List;

/**
 * 비교 받기 출력. 개선 효과를 단정하지 않고 항목별 변화만 담습니다.
 *
 * @param items 평가 항목별 비교 (피드백 type에서 파생한 항목)
 */
public record ComparisonResult(
        List<Item> items
) {

    /**
     * @param criterion  평가 항목 (예: 효과의 근거)
     * @param before     1차 상태
     * @param after      2차 상태
     * @param resolved   지적이 반영됐는지
     * @param afterQuote 2차 발언 인용, 재녹음 전사에 그대로 있는 문자열 (nullable)
     */
    public record Item(String criterion, String before, String after, boolean resolved, String afterQuote) {
    }
}

package org.mju.mjupitchrehearsal.infra.llm.dto;

import org.mju.mjupitchrehearsal.feedback.entity.FeedbackType;
import java.util.List;

/**
 * 피드백 받기 출력. 점수와 시간 값은 없습니다.
 *
 * @param issues   고칠 곳 후보 (중요한 순서). 서버가 검증 후 최대 3개만 저장
 * @param strength 잘 전달된 부분 1개 (nullable)
 */
public record FeedbackResult(
        List<Issue> issues,
        Strength strength
) {

    /**
     * @param segmentId  구간 ID (요청에 있던 값)
     * @param type       피드백 유형
     * @param quote      해당 구간 전사에 그대로 있는 실제 발언
     * @param reason     이해하기 어려운 이유
     * @param suggestion 수정 방향
     */
    public record Issue(String segmentId, FeedbackType type, String quote, String reason, String suggestion) {
    }

    public record Strength(String segmentId, String quote, String reason) {
    }
}

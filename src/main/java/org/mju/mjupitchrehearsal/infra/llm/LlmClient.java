package org.mju.mjupitchrehearsal.infra.llm;

import org.mju.mjupitchrehearsal.infra.llm.dto.ComparisonRequest;
import org.mju.mjupitchrehearsal.infra.llm.dto.ComparisonResult;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackRequest;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackResult;

/**
 * 홍정우(분석 파이프라인)와 이수영(AI 연결·프롬프트)이 만나는 유일한 지점.
 *
 * <p>함수 이름과 주고받는 값(dto 패키지)의 모양은 2단계에서 확정하고 이후 바꾸지 않습니다.
 * 구현체는 설정값 {@code llm.provider}로 고릅니다 (fake: 가짜 AI, 그 외: 진짜 AI).
 *
 * <ul>
 *   <li>LLM은 시간(ms)을 응답하지 않습니다. segmentId만 돌려주고 재생 위치는 서버가 전사 타임스탬프로 연결합니다.</li>
 *   <li>quote 검증, 최대 3개 제한, 순위 부여는 호출하는 쪽(feedback 패키지)이 합니다.</li>
 *   <li>호출 실패 시 {@code BusinessException(ErrorCode.EXTERNAL_API_ERROR)}를 던집니다.</li>
 * </ul>
 */
public interface LlmClient {

    /** 피드백 받기: 발표 조건·지표·구간별 전사 → 고칠 곳 목록 + 잘된 부분 */
    FeedbackResult requestFeedback(FeedbackRequest request);

    /** 비교 받기: 1차 발언·2차 발언·원래 피드백 → 항목별로 나아졌는지 */
    ComparisonResult requestComparison(ComparisonRequest request);
}

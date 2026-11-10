package org.sopt.mjupitchrehearsal.infra.llm;

import org.sopt.mjupitchrehearsal.feedback.entity.FeedbackType;
import org.sopt.mjupitchrehearsal.infra.llm.dto.ComparisonRequest;
import org.sopt.mjupitchrehearsal.infra.llm.dto.ComparisonResult;
import org.sopt.mjupitchrehearsal.infra.llm.dto.FeedbackRequest;
import org.sopt.mjupitchrehearsal.infra.llm.dto.FeedbackResult;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 가짜 AI. 진짜 AI 코드가 나오기 전까지 항상 같은 모양의 답을 돌려줍니다.
 * 받은 구간의 실제 전사로 quote를 만들어서 서버의 인용 검증을 통과합니다.
 *
 * <p>진짜 AI로 바꾸려면 {@code llm.provider}를 fake가 아닌 값으로 설정합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "fake", matchIfMissing = true)
public class FakeLlmClient implements LlmClient {

    private static final List<FeedbackType> TYPES = List.of(
            FeedbackType.NO_EVIDENCE, FeedbackType.MISSING_EXPLANATION, FeedbackType.STRUCTURE);

    @Override
    public FeedbackResult requestFeedback(FeedbackRequest request) {
        log.info("[FakeLlm] requestFeedback segments={}", request.segments().size());
        List<FeedbackRequest.Segment> segments = request.segments();
        if (segments.isEmpty()) {
            return new FeedbackResult(List.of(), null);
        }

        List<FeedbackResult.Issue> issues = new ArrayList<>();
        int count = Math.min(TYPES.size(), segments.size());
        for (int i = 0; i < count; i++) {
            FeedbackRequest.Segment segment = segments.get(segments.size() - 1 - i);
            issues.add(new FeedbackResult.Issue(
                    segment.segmentId(),
                    TYPES.get(i),
                    segment.text(),
                    "[가짜 AI] " + TYPES.get(i).getDescription() + " 예시 피드백입니다.",
                    "[가짜 AI] 이 부분을 구체적인 근거와 함께 다시 말해 보세요."
            ));
        }

        FeedbackRequest.Segment first = segments.get(0);
        FeedbackResult.Strength strength = new FeedbackResult.Strength(
                first.segmentId(), first.text(), "[가짜 AI] 도입이 명확합니다.");

        return new FeedbackResult(issues, strength);
    }

    @Override
    public ComparisonResult requestComparison(ComparisonRequest request) {
        log.info("[FakeLlm] requestComparison type={}", request.feedback().type());
        String criterion = request.feedback().type() == null
                ? "전달력"
                : request.feedback().type().getDescription();
        boolean changed = request.afterText() != null && !request.afterText().equals(request.beforeText());

        return new ComparisonResult(List.of(new ComparisonResult.Item(
                criterion,
                "[가짜 AI] 1차: 지적된 상태",
                changed ? "[가짜 AI] 2차: 내용이 바뀜" : "[가짜 AI] 2차: 그대로",
                changed,
                changed ? request.afterText() : null
        )));
    }
}

package org.mju.mjupitchrehearsal.feedback.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptMetric;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;
import org.mju.mjupitchrehearsal.attempt.repository.AttemptMetricRepository;
import org.mju.mjupitchrehearsal.attempt.service.AttemptService;
import org.mju.mjupitchrehearsal.feedback.dto.FeedbackListResponse;
import org.mju.mjupitchrehearsal.feedback.entity.Feedback;
import org.mju.mjupitchrehearsal.feedback.entity.FeedbackKind;
import org.mju.mjupitchrehearsal.feedback.repository.FeedbackRepository;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackQueryService {

    private final AttemptService attemptService;
    private final AttemptMetricRepository metricRepository;
    private final FeedbackRepository feedbackRepository;

    /** ⑨ 피드백 조회. 분석이 끝나지 않았으면 409 */
    @Transactional(readOnly = true)
    public FeedbackListResponse getFeedbacks(Long attemptId) {
        Attempt attempt = attemptService.getOwnedAttempt(attemptId);
        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.FEEDBACK_NOT_READY);
        }

        AttemptMetric metric = metricRepository.findByAttemptId(attemptId).orElse(null);
        long durationMs = metric != null ? metric.getDurationMs()
                : attempt.getDurationMs() != null ? attempt.getDurationMs() : 0L;
        long timeLimitMs = attempt.getPractice().getTimeLimitSec() * 1000L;

        List<Feedback> feedbacks = feedbackRepository.findAllWithSegmentByAttemptId(attemptId);
        List<FeedbackListResponse.Issue> issues = feedbacks.stream()
                .filter(f -> f.getKind() == FeedbackKind.ISSUE)
                .map(f -> {
                    TranscriptSegment s = f.getSegment();
                    return new FeedbackListResponse.Issue(f.getId(), f.getRank(), f.getType().name(),
                            s.getSegmentKey(), s.getStartMs(), s.getEndMs(),
                            f.getQuote(), f.getReason(), f.getSuggestion());
                })
                .toList();
        FeedbackListResponse.Strength strength = feedbacks.stream()
                .filter(f -> f.getKind() == FeedbackKind.STRENGTH)
                .findFirst()
                .map(f -> new FeedbackListResponse.Strength(f.getSegment().getSegmentKey(),
                        f.getSegment().getStartMs(), f.getSegment().getEndMs(), f.getQuote(), f.getReason()))
                .orElse(null);

        return new FeedbackListResponse(
                attemptId,
                new FeedbackListResponse.Summary(durationMs, timeLimitMs, durationMs - timeLimitMs),
                toMetrics(metric),
                issues,
                strength);
    }

    private FeedbackListResponse.Metrics toMetrics(AttemptMetric metric) {
        if (metric == null) {
            return new FeedbackListResponse.Metrics(0.0, List.of(), List.of());
        }
        return new FeedbackListResponse.Metrics(
                metric.getSyllablesPerSec(),
                metric.getFastSegments().stream()
                        .map(f -> new FeedbackListResponse.FastSegment(f.getSegmentKey(), f.getStartMs(),
                                f.getSyllablesPerSec()))
                        .toList(),
                metric.getLongPauses().stream()
                        .map(p -> new FeedbackListResponse.LongPause(p.getStartMs(), p.getPauseMs()))
                        .toList());
    }
}

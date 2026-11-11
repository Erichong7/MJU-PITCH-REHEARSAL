package org.mju.mjupitchrehearsal.feedback.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;
import org.mju.mjupitchrehearsal.attempt.repository.AttemptRepository;
import org.mju.mjupitchrehearsal.attempt.repository.TranscriptSegmentRepository;
import org.mju.mjupitchrehearsal.feedback.entity.Feedback;
import org.mju.mjupitchrehearsal.feedback.entity.FeedbackKind;
import org.mju.mjupitchrehearsal.feedback.repository.FeedbackRepository;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackResult;
import org.mju.mjupitchrehearsal.retry.repository.RetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * AI 답을 검증해서 피드백으로 저장합니다.
 *
 * <ul>
 *   <li>segmentId가 이 녹음의 실제 구간이어야 함</li>
 *   <li>quote가 그 구간 전사에 그대로 있어야 함 (없으면 버림)</li>
 *   <li>통과한 고칠 곳은 최대 3개, 순서대로 rank 1~3 / 잘된 부분은 1개</li>
 *   <li>재생 시간은 AI가 아니라 구간의 startMs·endMs를 씀 (Feedback → segment 연결)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackSaveService {

    public static final int MAX_ISSUES = 3;

    private final AttemptRepository attemptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final FeedbackRepository feedbackRepository;
    private final RetryRepository retryRepository;

    @Transactional
    public void replaceFeedbacks(Long attemptId, FeedbackResult result) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));

        deleteExisting(attemptId);

        Map<String, TranscriptSegment> segments = segmentRepository.findAllByAttemptIdOrderBySeqAsc(attemptId)
                .stream()
                .collect(Collectors.toMap(TranscriptSegment::getSegmentKey, Function.identity()));

        int rank = 0;
        int dropped = 0;
        Set<String> seen = new HashSet<>();
        List<FeedbackResult.Issue> issues = result.issues() == null ? List.of() : result.issues();
        for (FeedbackResult.Issue issue : issues) {
            if (rank >= MAX_ISSUES) {
                break;
            }
            if (issue == null) {
                dropped++;
                continue;
            }
            TranscriptSegment segment = segments.get(issue.segmentId());
            if (segment == null || issue.type() == null || !StringUtils.hasText(issue.reason())
                    || !StringUtils.hasText(issue.suggestion())
                    || !QuoteMatcher.contains(segment.getText(), issue.quote())
                    || !seen.add(issue.segmentId() + "|" + issue.type())) {
                dropped++;
                log.warn("[Feedback] dropped issue attemptId={} segmentId={} quote={}",
                        attemptId, issue.segmentId(), issue.quote());
                continue;
            }
            rank++;
            feedbackRepository.save(Feedback.builder()
                    .attempt(attempt)
                    .segment(segment)
                    .kind(FeedbackKind.ISSUE)
                    .type(issue.type())
                    .quote(issue.quote().strip())
                    .reason(issue.reason())
                    .suggestion(issue.suggestion())
                    .rank(rank)
                    .build());
        }

        FeedbackResult.Strength strength = result.strength();
        boolean strengthSaved = false;
        if (strength != null) {
            TranscriptSegment segment = segments.get(strength.segmentId());
            if (segment != null && StringUtils.hasText(strength.reason())
                    && QuoteMatcher.contains(segment.getText(), strength.quote())) {
                feedbackRepository.save(Feedback.builder()
                        .attempt(attempt)
                        .segment(segment)
                        .kind(FeedbackKind.STRENGTH)
                        .quote(strength.quote().strip())
                        .reason(strength.reason())
                        .build());
                strengthSaved = true;
            } else {
                log.warn("[Feedback] dropped strength attemptId={} segmentId={}", attemptId, strength.segmentId());
            }
        }
        log.info("[Feedback] saved attemptId={} issues={} dropped={} strength={}",
                attemptId, rank, dropped, strengthSaved);
    }

    /** 다시 분석할 때: 기존 피드백과 그 피드백으로 한 재연습을 지우고 새로 저장합니다. */
    private void deleteExisting(Long attemptId) {
        retryRepository.deleteAll(retryRepository.findAllByFeedbackAttemptId(attemptId));
        retryRepository.flush();
        feedbackRepository.deleteAllByAttemptId(attemptId);
    }
}

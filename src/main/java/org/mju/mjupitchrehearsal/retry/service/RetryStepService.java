package org.mju.mjupitchrehearsal.retry.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;
import org.mju.mjupitchrehearsal.feedback.entity.Feedback;
import org.mju.mjupitchrehearsal.feedback.service.QuoteMatcher;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.mju.mjupitchrehearsal.infra.llm.dto.ComparisonRequest;
import org.mju.mjupitchrehearsal.infra.llm.dto.ComparisonResult;
import org.mju.mjupitchrehearsal.retry.entity.ComparisonItem;
import org.mju.mjupitchrehearsal.retry.entity.Retry;
import org.mju.mjupitchrehearsal.retry.repository.RetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 재연습 파이프라인의 DB 작업. 단계마다 따로 커밋해서 ⑭에 바로 보이게 합니다. */
@Service
@RequiredArgsConstructor
public class RetryStepService {

    private static final int MAX_ITEMS = 3;

    private final RetryRepository retryRepository;

    @Transactional
    public void changeStatus(Long retryId, AttemptStatus status) {
        getRetry(retryId).changeStatus(status);
    }

    @Transactional
    public void fail(Long retryId, FailedStep step) {
        retryRepository.findById(retryId).ifPresent(r -> r.fail(step));
    }

    @Transactional(readOnly = true)
    public SttInput getSttInput(Long retryId) {
        Retry retry = getRetry(retryId);
        return new SttInput(retry.getAudioKey(),
                List.copyOf(retry.getFeedback().getAttempt().getPractice().getTermWords()));
    }

    @Transactional
    public void saveTranscript(Long retryId, String transcript) {
        getRetry(retryId).updateTranscript(transcript);
    }

    /** AI에 보낼 비교 요청: 원래 피드백, 1차 구간 글, 재녹음 글 */
    @Transactional(readOnly = true)
    public ComparisonRequest buildComparisonRequest(Long retryId) {
        Retry retry = getRetry(retryId);
        Feedback feedback = retry.getFeedback();
        return new ComparisonRequest(
                feedback.getAttempt().getPractice().getMode(),
                new ComparisonRequest.OriginalFeedback(feedback.getType(), feedback.getQuote(),
                        feedback.getReason(), feedback.getSuggestion()),
                feedback.getSegment().getText(),
                retry.getTranscript());
    }

    /**
     * afterQuote가 재녹음 전사에 없으면 비웁니다 (항목 자체는 남김).
     * 근거 인용이 없으면 "나아짐"을 인정하지 않고 resolved=false로 저장합니다.
     */
    @Transactional
    public void complete(Long retryId, ComparisonResult result) {
        Retry retry = getRetry(retryId);
        List<ComparisonResult.Item> items = result.items() == null ? List.of() : result.items();
        List<ComparisonItem> comparisons = items.stream()
                .filter(Objects::nonNull)
                .filter(i -> StringUtils.hasText(i.criterion()))
                .limit(MAX_ITEMS)
                .map(i -> {
                    String afterQuote = QuoteMatcher.contains(retry.getTranscript(), i.afterQuote())
                            ? i.afterQuote().strip() : null;
                    return new ComparisonItem(
                            i.criterion(),
                            nullToEmpty(i.before()),
                            nullToEmpty(i.after()),
                            i.resolved() && afterQuote != null,
                            afterQuote);
                })
                .toList();
        retry.complete(comparisons);
    }

    private Retry getRetry(Long retryId) {
        return retryRepository.findById(retryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETRY_NOT_FOUND));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public record SttInput(String audioKey, List<String> terms) {
    }
}

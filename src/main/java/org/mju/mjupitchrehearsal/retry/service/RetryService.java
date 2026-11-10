package org.mju.mjupitchrehearsal.retry.service;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;
import org.mju.mjupitchrehearsal.attempt.support.AudioFiles;
import org.mju.mjupitchrehearsal.feedback.entity.Feedback;
import org.mju.mjupitchrehearsal.feedback.entity.FeedbackKind;
import org.mju.mjupitchrehearsal.feedback.repository.FeedbackRepository;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.mju.mjupitchrehearsal.infra.storage.AudioStorage;
import org.mju.mjupitchrehearsal.retry.dto.RetryResultResponse;
import org.mju.mjupitchrehearsal.retry.dto.RetryUploadResponse;
import org.mju.mjupitchrehearsal.retry.entity.Retry;
import org.mju.mjupitchrehearsal.retry.event.RetryUploadedEvent;
import org.mju.mjupitchrehearsal.retry.repository.RetryRepository;
import org.mju.mjupitchrehearsal.user.service.CurrentUserProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RetryService {

    private static final long RETRY_MAX_MS = 90 * 1000L;
    private static final Duration PLAYBACK_TTL = Duration.ofMinutes(10);

    private final FeedbackRepository feedbackRepository;
    private final RetryRepository retryRepository;
    private final AudioStorage audioStorage;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    /** ⑬ 구간 재연습 업로드. 비교는 커밋 뒤 백그라운드에서 진행하고 ⑭로 확인합니다. */
    @Transactional
    public RetryUploadResponse upload(Long feedbackId, MultipartFile audio) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .filter(f -> f.getKind() == FeedbackKind.ISSUE)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));
        if (!feedback.getAttempt().getPractice().getUser().getId().equals(currentUserProvider.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FEEDBACK_FORBIDDEN);
        }
        if (feedback.getAttempt().getStatus().isInProgress()) {
            throw new BusinessException(ErrorCode.RETRY_LOCKED);
        }

        AudioFiles.M4a m4a = AudioFiles.readM4a(audio);
        if (m4a.durationMs() > RETRY_MAX_MS) {
            throw new BusinessException(ErrorCode.AUDIO_TOO_LONG, "재연습 녹음은 90초를 넘을 수 없습니다.");
        }

        String audioKey = "retries/%d/%s.m4a".formatted(feedbackId, UUID.randomUUID());
        audioStorage.save(audioKey, m4a.content());

        int retryNo = (int) retryRepository.countByFeedbackId(feedbackId) + 1;
        Retry retry = retryRepository.save(Retry.builder()
                .feedback(feedback)
                .retryNo(retryNo)
                .audioKey(audioKey)
                .durationMs(m4a.durationMs())
                .build());

        eventPublisher.publishEvent(new RetryUploadedEvent(retry.getId()));
        return new RetryUploadResponse(retry.getId(), retry.getRetryNo(), retry.getStatus());
    }

    /** ⑭ 재연습 결과(1차·2차 비교) 조회 */
    @Transactional(readOnly = true)
    public RetryResultResponse getResult(Long retryId) {
        Retry retry = retryRepository.findById(retryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETRY_NOT_FOUND));
        Feedback feedback = retry.getFeedback();
        if (!feedback.getAttempt().getPractice().getUser().getId().equals(currentUserProvider.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.RETRY_FORBIDDEN);
        }

        TranscriptSegment segment = feedback.getSegment();
        RetryResultResponse.Before before = new RetryResultResponse.Before(
                segment.getStartMs(), segment.getEndMs(), segment.getEndMs() - segment.getStartMs(), segment.getText());

        boolean completed = retry.getStatus() == AttemptStatus.COMPLETED;
        RetryResultResponse.After after = new RetryResultResponse.After(
                retry.getDurationMs(),
                completed ? retry.getTranscript() : null,
                completed ? audioStorage.createPlaybackUrl(retry.getAudioKey(), PLAYBACK_TTL).url() : null);

        return new RetryResultResponse(
                retry.getId(),
                feedback.getId(),
                retry.getRetryNo(),
                retry.getStatus(),
                retry.getFailedStep(),
                before,
                after,
                completed
                        ? retry.getComparisons().stream()
                                .map(c -> new RetryResultResponse.Comparison(c.getCriterion(), c.getBefore(),
                                        c.getAfter(), c.isResolved(), c.getAfterQuote()))
                                .toList()
                        : null);
    }
}

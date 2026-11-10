package org.mju.mjupitchrehearsal.retry.service;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;
import org.mju.mjupitchrehearsal.infra.llm.LlmClient;
import org.mju.mjupitchrehearsal.infra.llm.dto.ComparisonResult;
import org.mju.mjupitchrehearsal.infra.stt.SttClient;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;
import org.mju.mjupitchrehearsal.retry.event.RetryUploadedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 재연습 백그라운드 흐름: UPLOADED → TRANSCRIBING(재녹음 전사) → ANALYZING(1차·2차 비교) → COMPLETED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryPipeline {

    private final RetryStepService steps;
    private final SttClient sttClient;
    private final LlmClient llmClient;

    @Async("analysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploaded(RetryUploadedEvent event) {
        Long retryId = event.retryId();
        long start = System.currentTimeMillis();
        log.info("[Retry] start retryId={}", retryId);

        try {
            steps.changeStatus(retryId, AttemptStatus.TRANSCRIBING);
            RetryStepService.SttInput input = steps.getSttInput(retryId);
            SttResult stt = sttClient.transcribe(input.audioKey(), input.terms());
            String transcript = stt.segments().stream()
                    .map(SttResult.Segment::text)
                    .collect(Collectors.joining(" "));
            steps.saveTranscript(retryId, transcript);
        } catch (Exception e) {
            log.error("[Retry] transcribe failed retryId={}", retryId, e);
            steps.fail(retryId, FailedStep.TRANSCRIBE);
            return;
        }

        try {
            steps.changeStatus(retryId, AttemptStatus.ANALYZING);
            ComparisonResult result = llmClient.requestComparison(steps.buildComparisonRequest(retryId));
            steps.complete(retryId, result);
        } catch (Exception e) {
            log.error("[Retry] compare failed retryId={}", retryId, e);
            steps.fail(retryId, FailedStep.ANALYZE);
            return;
        }
        log.info("[Retry] completed retryId={} totalMs={}", retryId, System.currentTimeMillis() - start);
    }
}

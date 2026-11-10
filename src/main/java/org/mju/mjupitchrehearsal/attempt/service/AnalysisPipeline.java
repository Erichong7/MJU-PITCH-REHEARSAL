package org.mju.mjupitchrehearsal.attempt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;
import org.mju.mjupitchrehearsal.attempt.event.AttemptUploadedEvent;
import org.mju.mjupitchrehearsal.infra.llm.LlmClient;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackResult;
import org.mju.mjupitchrehearsal.infra.stt.SttClient;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 업로드 후 백그라운드에서 도는 분석 흐름.
 * UPLOADED → TRANSCRIBING(STT) → ANALYZING(AI) → COMPLETED, 실패하면 FAILED + 실패한 단계.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisPipeline {

    private final AnalysisStepService steps;
    private final SttClient sttClient;
    private final LlmClient llmClient;

    @Async("analysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploaded(AttemptUploadedEvent event) {
        run(event.attemptId());
    }

    public void run(Long attemptId) {
        log.info("[Analysis] start attemptId={}", attemptId);

        // 1. 음성 → 글
        try {
            steps.changeStatus(attemptId, AttemptStatus.TRANSCRIBING);
            AnalysisStepService.SttInput input = steps.getSttInput(attemptId);
            SttResult result = sttClient.transcribe(input.audioKey(), input.terms());
            steps.saveSegments(attemptId, result);
        } catch (Exception e) {
            log.error("[Analysis] transcribe failed attemptId={}", attemptId, e);
            steps.fail(attemptId, FailedStep.TRANSCRIBE);
            return;
        }

        // 2. 지표 계산 + AI 피드백
        try {
            steps.changeStatus(attemptId, AttemptStatus.ANALYZING);
            steps.calculateMetrics(attemptId);
            FeedbackResult result = llmClient.requestFeedback(steps.buildFeedbackRequest(attemptId));
            // TODO(5단계): 인용 검증(quote가 해당 구간에 있는지) 후 Feedback 저장, 최대 3개
            log.info("[Analysis] feedback received attemptId={} issues={}", attemptId, result.issues().size());
        } catch (Exception e) {
            log.error("[Analysis] analyze failed attemptId={}", attemptId, e);
            steps.fail(attemptId, FailedStep.ANALYZE);
            return;
        }

        steps.changeStatus(attemptId, AttemptStatus.COMPLETED);
        log.info("[Analysis] completed attemptId={}", attemptId);
    }
}

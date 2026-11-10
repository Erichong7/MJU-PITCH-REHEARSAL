package org.mju.mjupitchrehearsal.attempt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;
import org.mju.mjupitchrehearsal.attempt.event.AttemptUploadedEvent;
import org.mju.mjupitchrehearsal.attempt.event.TranscriptEditedEvent;
import org.mju.mjupitchrehearsal.feedback.service.FeedbackSaveService;
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
 * UPLOADED → TRANSCRIBING(STT) → ANALYZING(지표 + AI) → COMPLETED, 실패하면 FAILED + 실패한 단계.
 * 전사를 고친 경우(⑧)에는 STT를 건너뛰고 ANALYZING부터 다시 실행합니다.
 * 단계별 걸린 시간을 로그로 남깁니다 (9단계 시간·비용 측정용).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisPipeline {

    private final AnalysisStepService steps;
    private final SttClient sttClient;
    private final LlmClient llmClient;
    private final FeedbackSaveService feedbackSaveService;

    @Async("analysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploaded(AttemptUploadedEvent event) {
        long start = System.currentTimeMillis();
        log.info("[Analysis] start attemptId={}", event.attemptId());
        if (transcribe(event.attemptId()) && analyze(event.attemptId())) {
            log.info("[Analysis] completed attemptId={} totalMs={}", event.attemptId(), System.currentTimeMillis() - start);
        }
    }

    @Async("analysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranscriptEdited(TranscriptEditedEvent event) {
        long start = System.currentTimeMillis();
        log.info("[Analysis] re-analyze (transcript edited) attemptId={}", event.attemptId());
        if (analyze(event.attemptId())) {
            log.info("[Analysis] completed attemptId={} totalMs={}", event.attemptId(), System.currentTimeMillis() - start);
        }
    }

    /** 1. 음성 → 글 */
    private boolean transcribe(Long attemptId) {
        long start = System.currentTimeMillis();
        try {
            steps.changeStatus(attemptId, AttemptStatus.TRANSCRIBING);
            AnalysisStepService.SttInput input = steps.getSttInput(attemptId);
            SttResult result = sttClient.transcribe(input.audioKey(), input.terms());
            steps.saveSegments(attemptId, result);
            log.info("[Analysis] transcribed attemptId={} segments={} elapsedMs={}",
                    attemptId, result.segments().size(), System.currentTimeMillis() - start);
            return true;
        } catch (Exception e) {
            log.error("[Analysis] transcribe failed attemptId={}", attemptId, e);
            steps.fail(attemptId, FailedStep.TRANSCRIBE);
            return false;
        }
    }

    /** 2. 지표 계산 + AI 피드백 + 검증 후 저장 */
    private boolean analyze(Long attemptId) {
        long start = System.currentTimeMillis();
        try {
            steps.changeStatus(attemptId, AttemptStatus.ANALYZING);
            steps.calculateMetrics(attemptId);
            FeedbackResult result = llmClient.requestFeedback(steps.buildFeedbackRequest(attemptId));
            log.info("[Analysis] feedback received attemptId={} issues={} elapsedMs={}", attemptId,
                    result.issues() == null ? 0 : result.issues().size(), System.currentTimeMillis() - start);
            feedbackSaveService.replaceFeedbacks(attemptId, result);
            steps.changeStatus(attemptId, AttemptStatus.COMPLETED);
            return true;
        } catch (Exception e) {
            log.error("[Analysis] analyze failed attemptId={}", attemptId, e);
            steps.fail(attemptId, FailedStep.ANALYZE);
            return false;
        }
    }
}

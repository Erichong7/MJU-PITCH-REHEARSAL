package org.mju.mjupitchrehearsal.attempt.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;
import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;
import org.mju.mjupitchrehearsal.attempt.repository.AttemptRepository;
import org.mju.mjupitchrehearsal.attempt.repository.TranscriptSegmentRepository;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackRequest;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;
import org.mju.mjupitchrehearsal.practice.entity.Practice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석 파이프라인의 DB 작업. 단계마다 트랜잭션을 따로 커밋해서 ⑤ 상태 조회에 바로 보이게 합니다.
 * (외부 API 호출은 트랜잭션 밖 {@link AnalysisPipeline}에서 합니다.)
 */
@Service
@RequiredArgsConstructor
public class AnalysisStepService {

    private final AttemptRepository attemptRepository;
    private final TranscriptSegmentRepository segmentRepository;

    @Transactional
    public void changeStatus(Long attemptId, AttemptStatus status) {
        getAttempt(attemptId).changeStatus(status);
    }

    @Transactional
    public void fail(Long attemptId, FailedStep step) {
        getAttempt(attemptId).fail(step);
    }

    /** STT 입력: 녹음 위치와 전문 용어 */
    @Transactional(readOnly = true)
    public SttInput getSttInput(Long attemptId) {
        Attempt attempt = getAttempt(attemptId);
        return new SttInput(attempt.getAudioKey(), List.copyOf(attempt.getPractice().getTermWords()));
    }

    /** STT 결과를 seg_01, seg_02 … 구간으로 저장합니다. 다시 실행하면 기존 구간을 지우고 새로 저장합니다. */
    @Transactional
    public void saveSegments(Long attemptId, SttResult result) {
        Attempt attempt = getAttempt(attemptId);
        segmentRepository.deleteAllByAttemptId(attemptId);
        List<SttResult.Segment> segments = result.segments();
        for (int i = 0; i < segments.size(); i++) {
            SttResult.Segment s = segments.get(i);
            segmentRepository.save(TranscriptSegment.builder()
                    .attempt(attempt)
                    .seq(i)
                    .segmentKey(TranscriptSegment.toSegmentKey(i))
                    .startMs(s.startMs())
                    .endMs(s.endMs())
                    .text(s.text())
                    .build());
        }
    }

    /**
     * AI에 보낼 주문서. 말 속도·멈춤 지표는 4단계에서 계산해 채웁니다.
     */
    @Transactional(readOnly = true)
    public FeedbackRequest buildFeedbackRequest(Long attemptId) {
        Attempt attempt = getAttempt(attemptId);
        Practice practice = attempt.getPractice();

        FeedbackRequest.Condition condition = new FeedbackRequest.Condition(
                practice.getPurpose(),
                practice.getAudience(),
                practice.getTimeLimitSec(),
                List.copyOf(practice.getKeyPoints()),
                List.copyOf(practice.getTermWords()));

        // TODO(4단계): 말 속도, 빨라진 구간, 1초 이상 멈춤 계산
        FeedbackRequest.Metrics metrics = new FeedbackRequest.Metrics(
                attempt.getDurationMs(), null, List.of(), List.of());

        List<FeedbackRequest.Segment> segments = segmentRepository.findAllByAttemptIdOrderBySeqAsc(attemptId)
                .stream()
                .map(s -> new FeedbackRequest.Segment(s.getSegmentKey(), s.getText()))
                .toList();

        return new FeedbackRequest(practice.getMode(), condition, metrics, segments);
    }

    private Attempt getAttempt(Long attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));
    }

    public record SttInput(String audioKey, List<String> terms) {
    }
}

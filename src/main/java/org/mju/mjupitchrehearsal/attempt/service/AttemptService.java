package org.mju.mjupitchrehearsal.attempt.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.attempt.dto.AttemptStatusResponse;
import org.mju.mjupitchrehearsal.attempt.dto.AudioUrlResponse;
import org.mju.mjupitchrehearsal.attempt.dto.AttemptUploadResponse;
import org.mju.mjupitchrehearsal.attempt.dto.TranscriptEditRequest;
import org.mju.mjupitchrehearsal.attempt.dto.TranscriptEditResponse;
import org.mju.mjupitchrehearsal.attempt.dto.TranscriptResponse;
import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;
import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;
import org.mju.mjupitchrehearsal.attempt.event.AttemptUploadedEvent;
import org.mju.mjupitchrehearsal.attempt.event.TranscriptEditedEvent;
import org.mju.mjupitchrehearsal.attempt.repository.AttemptRepository;
import org.mju.mjupitchrehearsal.attempt.repository.TranscriptSegmentRepository;
import org.mju.mjupitchrehearsal.attempt.support.AudioFiles;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.mju.mjupitchrehearsal.global.exception.ValidationException;
import org.mju.mjupitchrehearsal.infra.storage.AudioStorage;
import org.mju.mjupitchrehearsal.practice.entity.Mode;
import org.mju.mjupitchrehearsal.practice.entity.Practice;
import org.mju.mjupitchrehearsal.practice.repository.PracticeRepository;
import org.mju.mjupitchrehearsal.user.service.CurrentUserProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private static final long PRESENTATION_MAX_MS = 5 * 60 * 1000L;
    private static final long INTERVIEW_MAX_MS = 2 * 60 * 1000L;
    private static final Duration PLAYBACK_TTL = Duration.ofMinutes(10);

    private final PracticeRepository practiceRepository;
    private final AttemptRepository attemptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final AudioStorage audioStorage;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    /** ④ 녹음 업로드. 저장 후 바로 응답하고, 분석은 커밋 뒤 백그라운드에서 시작합니다. */
    @Transactional
    public AttemptUploadResponse upload(Long practiceId, MultipartFile audio) {
        Practice practice = practiceRepository.findById(practiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRACTICE_NOT_FOUND));
        if (!practice.getUser().getId().equals(currentUserProvider.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.PRACTICE_FORBIDDEN);
        }

        AudioFiles.M4a m4a = AudioFiles.readM4a(audio);
        byte[] content = m4a.content();
        long durationMs = m4a.durationMs();
        validateLength(practice.getMode(), durationMs);

        String audioKey = "attempts/%d/%s.m4a".formatted(practiceId, UUID.randomUUID());
        audioStorage.save(audioKey, content);

        int attemptNo = (int) attemptRepository.countByPracticeId(practiceId) + 1;
        Attempt attempt = attemptRepository.save(Attempt.builder()
                .practice(practice)
                .attemptNo(attemptNo)
                .audioKey(audioKey)
                .durationMs(durationMs)
                .build());

        eventPublisher.publishEvent(new AttemptUploadedEvent(attempt.getId()));
        return AttemptUploadResponse.from(attempt);
    }

    /** ⑤ 처리 상태 조회 */
    @Transactional(readOnly = true)
    public AttemptStatusResponse getStatus(Long attemptId) {
        return AttemptStatusResponse.from(getOwnedAttempt(attemptId));
    }

    /** ⑦ 전사 조회. 전사가 끝나기 전(업로드됨·전사 중·전사 실패)에는 409 */
    @Transactional(readOnly = true)
    public TranscriptResponse getTranscript(Long attemptId) {
        Attempt attempt = getOwnedAttempt(attemptId);
        if (!hasTranscript(attempt)) {
            throw new BusinessException(ErrorCode.TRANSCRIPT_NOT_READY);
        }
        List<TranscriptResponse.SegmentDto> segments = segmentRepository.findAllByAttemptIdOrderBySeqAsc(attemptId)
                .stream()
                .map(TranscriptResponse.SegmentDto::from)
                .toList();
        return new TranscriptResponse(attemptId, segments);
    }

    /**
     * ⑧ 전사 수정. 글만 바꾸고(시간은 그대로) 커밋 뒤 STT 없이 지표·피드백만 다시 만듭니다.
     */
    @Transactional
    public TranscriptEditResponse editTranscript(Long attemptId, TranscriptEditRequest request) {
        Attempt attempt = getOwnedAttempt(attemptId);
        if (attempt.getStatus().isInProgress()) {
            throw new BusinessException(ErrorCode.TRANSCRIPT_LOCKED);
        }
        if (!hasTranscript(attempt)) {
            throw new BusinessException(ErrorCode.TRANSCRIPT_NOT_READY);
        }

        Map<String, TranscriptSegment> segments = segmentRepository.findAllByAttemptIdOrderBySeqAsc(attemptId)
                .stream()
                .collect(Collectors.toMap(TranscriptSegment::getSegmentKey, Function.identity()));

        Map<String, String> errors = new LinkedHashMap<>();
        for (int i = 0; i < request.segments().size(); i++) {
            if (!segments.containsKey(request.segments().get(i).segmentId())) {
                errors.put("segments[%d].segmentId".formatted(i), "해당 녹음에 없는 구간입니다.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        request.segments().forEach(edit -> segments.get(edit.segmentId()).editText(edit.text().strip()));
        attempt.changeStatus(AttemptStatus.ANALYZING);
        eventPublisher.publishEvent(new TranscriptEditedEvent(attemptId));
        return new TranscriptEditResponse(attemptId, attempt.getStatus());
    }

    /** ⑩ 녹음 재생 링크 (10분 유효). 앱은 피드백의 startMs로 이동해서 재생합니다. */
    @Transactional(readOnly = true)
    public AudioUrlResponse getAudioUrl(Long attemptId) {
        Attempt attempt = getOwnedAttempt(attemptId);
        AudioStorage.PlaybackUrl url = audioStorage.createPlaybackUrl(attempt.getAudioKey(), PLAYBACK_TTL);
        return new AudioUrlResponse(url.url(), url.expiresAt(), attempt.getDurationMs());
    }

    /** 본인 녹음인지 확인하고 돌려줍니다. 다른 서비스(피드백 등)에서도 사용합니다. */
    @Transactional(readOnly = true)
    public Attempt getOwnedAttempt(Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));
        if (!attempt.getPractice().getUser().getId().equals(currentUserProvider.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.ATTEMPT_FORBIDDEN);
        }
        return attempt;
    }

    private boolean hasTranscript(Attempt attempt) {
        return switch (attempt.getStatus()) {
            case UPLOADED, TRANSCRIBING -> false;
            case FAILED -> attempt.getFailedStep() != FailedStep.TRANSCRIBE;
            default -> true;
        };
    }

    private void validateLength(Mode mode, long durationMs) {
        if (mode == Mode.INTERVIEW && durationMs > INTERVIEW_MAX_MS) {
            throw new BusinessException(ErrorCode.AUDIO_TOO_LONG, "면접 녹음은 2분을 넘을 수 없습니다.");
        }
        if (mode == Mode.PRESENTATION && durationMs > PRESENTATION_MAX_MS) {
            throw new BusinessException(ErrorCode.AUDIO_TOO_LONG, "발표 녹음은 5분을 넘을 수 없습니다.");
        }
    }
}

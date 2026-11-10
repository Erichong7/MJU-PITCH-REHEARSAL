package org.mju.mjupitchrehearsal.attempt.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.attempt.dto.AttemptStatusResponse;
import org.mju.mjupitchrehearsal.attempt.dto.AttemptUploadResponse;
import org.mju.mjupitchrehearsal.attempt.dto.TranscriptResponse;
import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;
import org.mju.mjupitchrehearsal.attempt.event.AttemptUploadedEvent;
import org.mju.mjupitchrehearsal.attempt.repository.AttemptRepository;
import org.mju.mjupitchrehearsal.attempt.repository.TranscriptSegmentRepository;
import org.mju.mjupitchrehearsal.attempt.support.M4aDurationReader;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
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

        byte[] content = readAudio(audio);
        long durationMs = M4aDurationReader.readDurationMs(content)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_AUDIO));
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

    private byte[] readAudio(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new BusinessException(ErrorCode.AUDIO_REQUIRED);
        }
        String name = audio.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".m4a")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_AUDIO);
        }
        try {
            return audio.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
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

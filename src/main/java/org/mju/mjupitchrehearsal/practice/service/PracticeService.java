package org.mju.mjupitchrehearsal.practice.service;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.global.exception.ValidationException;
import org.mju.mjupitchrehearsal.practice.dto.PracticeCreateRequest;
import org.mju.mjupitchrehearsal.practice.dto.PracticeCreateResponse;
import org.mju.mjupitchrehearsal.practice.entity.Mode;
import org.mju.mjupitchrehearsal.practice.entity.Practice;
import org.mju.mjupitchrehearsal.practice.repository.PracticeRepository;
import org.mju.mjupitchrehearsal.user.service.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PracticeService {

    private static final int PRESENTATION_MIN_SEC = 30;
    private static final int PRESENTATION_MAX_SEC = 300;
    private static final int INTERVIEW_MIN_SEC = 30;
    private static final int INTERVIEW_MAX_SEC = 120;

    private final PracticeRepository practiceRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public PracticeCreateResponse create(PracticeCreateRequest request) {
        validateByMode(request);

        PracticeCreateRequest.Condition c = request.condition();
        boolean interview = request.mode() == Mode.INTERVIEW;
        Practice practice = Practice.builder()
                .user(currentUserProvider.getCurrentUser())
                .mode(request.mode())
                .title(request.title().strip())
                .purpose(c.purpose())
                .audience(c.audience())
                .timeLimitSec(c.timeLimitSec())
                .keyPoints(c.keyPoints())
                .jobField(interview ? c.jobField() : null)
                .interviewType(interview ? c.interviewType() : null)
                .experience(interview ? c.experience() : null)
                .build();

        if (request.terms() != null) {
            request.terms().stream().map(String::strip).distinct().forEach(practice::addTerm);
        }

        return new PracticeCreateResponse(practiceRepository.save(practice).getId());
    }

    /** 모드에 따라 필수 조건과 제한 시간 범위가 다릅니다. */
    private void validateByMode(PracticeCreateRequest request) {
        PracticeCreateRequest.Condition c = request.condition();
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.mode() == Mode.PRESENTATION) {
            if (!StringUtils.hasText(c.purpose())) {
                errors.put("condition.purpose", "발표 목적은 필수입니다.");
            }
            if (outOfRange(c.timeLimitSec(), PRESENTATION_MIN_SEC, PRESENTATION_MAX_SEC)) {
                errors.put("condition.timeLimitSec", "발표 제한 시간은 30~300초입니다.");
            }
        } else {
            if (!StringUtils.hasText(c.jobField())) {
                errors.put("condition.jobField", "지원 분야는 필수입니다.");
            }
            if (c.interviewType() == null) {
                errors.put("condition.interviewType", "면접 유형은 필수입니다.");
            }
            if (outOfRange(c.timeLimitSec(), INTERVIEW_MIN_SEC, INTERVIEW_MAX_SEC)) {
                errors.put("condition.timeLimitSec", "면접 제한 시간은 30~120초입니다.");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private boolean outOfRange(Integer value, int min, int max) {
        return value == null || value < min || value > max;
    }

}

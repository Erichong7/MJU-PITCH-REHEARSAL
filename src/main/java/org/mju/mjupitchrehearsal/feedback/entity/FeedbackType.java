package org.mju.mjupitchrehearsal.feedback.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedbackType {
    NO_EVIDENCE("근거 부족"),
    MISSING_EXPLANATION("설명 누락"),
    LIKELY_QUESTION("되물을 만한 곳"),
    STRUCTURE("구성");

    private final String description;
}

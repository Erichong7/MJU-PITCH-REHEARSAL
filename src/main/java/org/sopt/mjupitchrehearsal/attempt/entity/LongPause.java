package org.sopt.mjupitchrehearsal.attempt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 1초 이상 멈춤. segmentKey는 멈춤 직후 시작하는 구간입니다. */
@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LongPause {

    @Column(length = 20)
    private String segmentKey;

    @Column(nullable = false)
    private Long startMs;

    @Column(nullable = false)
    private Long pauseMs;
}

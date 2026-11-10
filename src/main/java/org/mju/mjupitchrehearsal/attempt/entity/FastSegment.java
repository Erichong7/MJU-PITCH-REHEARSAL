package org.mju.mjupitchrehearsal.attempt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 본인 평균보다 빨라진 구간 */
@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FastSegment {

    @Column(nullable = false, length = 20)
    private String segmentKey;

    @Column(nullable = false)
    private Long startMs;

    @Column(nullable = false)
    private Double syllablesPerSec;
}

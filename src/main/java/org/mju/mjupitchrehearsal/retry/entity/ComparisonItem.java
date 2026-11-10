package org.mju.mjupitchrehearsal.retry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 1차·2차 비교의 평가 항목 1개 */
@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComparisonItem {

    @Column(nullable = false)
    private String criterion;

    @Column(name = "before_state", nullable = false, columnDefinition = "TEXT")
    private String before;

    @Column(name = "after_state", nullable = false, columnDefinition = "TEXT")
    private String after;

    @Column(nullable = false)
    private boolean resolved;

    @Column(columnDefinition = "TEXT")
    private String afterQuote;
}

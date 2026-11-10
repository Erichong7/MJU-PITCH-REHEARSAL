package org.mju.mjupitchrehearsal.attempt.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 말한 방식 수치. 순수 Java로 계산하고, 전사를 고치면 다시 계산합니다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttemptMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false, unique = true)
    private Attempt attempt;

    @Column(nullable = false)
    private Long durationMs;

    @Column(nullable = false)
    private Double syllablesPerSec;

    @ElementCollection
    @CollectionTable(name = "attempt_fast_segment", joinColumns = @JoinColumn(name = "attempt_metric_id"))
    @OrderColumn(name = "seq")
    private List<FastSegment> fastSegments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "attempt_long_pause", joinColumns = @JoinColumn(name = "attempt_metric_id"))
    @OrderColumn(name = "seq")
    private List<LongPause> longPauses = new ArrayList<>();

    public AttemptMetric(Attempt attempt, Long durationMs, Double syllablesPerSec,
                         List<FastSegment> fastSegments, List<LongPause> longPauses) {
        this.attempt = attempt;
        update(durationMs, syllablesPerSec, fastSegments, longPauses);
    }

    public void update(Long durationMs, Double syllablesPerSec,
                       List<FastSegment> fastSegments, List<LongPause> longPauses) {
        this.durationMs = durationMs;
        this.syllablesPerSec = syllablesPerSec;
        this.fastSegments.clear();
        this.fastSegments.addAll(fastSegments);
        this.longPauses.clear();
        this.longPauses.addAll(longPauses);
    }
}

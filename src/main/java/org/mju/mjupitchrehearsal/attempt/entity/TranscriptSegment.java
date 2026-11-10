package org.mju.mjupitchrehearsal.attempt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전사 구간. segmentKey(seg_01…)를 LLM에 넘기고, 재생 위치는 startMs·endMs(STT 실제 시간)로만 정합니다.
 */
@Getter
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "segment_key"}),
        indexes = @Index(columnList = "attempt_id, seq")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TranscriptSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    /** 구간 순서 (0부터) */
    @Column(nullable = false)
    private Integer seq;

    /** seg_01, seg_02 … */
    @Column(name = "segment_key", nullable = false, length = 20)
    private String segmentKey;

    @Column(nullable = false)
    private Long startMs;

    @Column(nullable = false)
    private Long endMs;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /** 사용자가 전사를 고쳤는지 */
    @Column(nullable = false)
    private boolean edited;

    @Builder
    private TranscriptSegment(Attempt attempt, Integer seq, String segmentKey, Long startMs, Long endMs, String text) {
        this.attempt = attempt;
        this.seq = seq;
        this.segmentKey = segmentKey;
        this.startMs = startMs;
        this.endMs = endMs;
        this.text = text;
        this.edited = false;
    }

    /** 글만 고칠 수 있고 시간은 고칠 수 없습니다. */
    public void editText(String text) {
        this.text = text;
        this.edited = true;
    }

    public static String toSegmentKey(int seq) {
        return String.format("seg_%02d", seq + 1);
    }
}

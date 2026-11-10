package org.mju.mjupitchrehearsal.feedback.entity;

import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피드백 카드. quote는 연결된 구간 전사에 실제로 있는 문자열이어야 합니다 (서버가 검증 후 저장).
 * 재생 위치는 segment의 startMs·endMs를 사용합니다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id", nullable = false)
    private TranscriptSegment segment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackKind kind;

    /** ISSUE일 때만 값이 있습니다. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FeedbackType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String quote;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** ISSUE일 때만 값이 있습니다. */
    @Column(columnDefinition = "TEXT")
    private String suggestion;

    /** 고칠 순서 1~3. STRENGTH는 null */
    @Column(name = "`rank`")
    private Integer rank;

    @Builder
    private Feedback(Attempt attempt, TranscriptSegment segment, FeedbackKind kind, FeedbackType type,
                     String quote, String reason, String suggestion, Integer rank) {
        this.attempt = attempt;
        this.segment = segment;
        this.kind = kind;
        this.type = type;
        this.quote = quote;
        this.reason = reason;
        this.suggestion = suggestion;
        this.rank = rank;
    }
}

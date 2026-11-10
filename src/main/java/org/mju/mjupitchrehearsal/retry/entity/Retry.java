package org.mju.mjupitchrehearsal.retry.entity;

import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;
import org.mju.mjupitchrehearsal.feedback.entity.Feedback;
import org.mju.mjupitchrehearsal.global.entity.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피드백 구간 재연습 1회와 1차·2차 비교 결과.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Retry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @Column(nullable = false)
    private String audioKey;

    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FailedStep failedStep;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @ElementCollection
    @CollectionTable(name = "retry_comparison", joinColumns = @JoinColumn(name = "retry_id"))
    @OrderColumn(name = "seq")
    private List<ComparisonItem> comparisons = new ArrayList<>();

    @Builder
    private Retry(Feedback feedback, String audioKey, Long durationMs) {
        this.feedback = feedback;
        this.audioKey = audioKey;
        this.durationMs = durationMs;
        this.status = AttemptStatus.UPLOADED;
    }

    public void changeStatus(AttemptStatus status) {
        this.status = status;
        this.failedStep = null;
    }

    public void fail(FailedStep failedStep) {
        this.status = AttemptStatus.FAILED;
        this.failedStep = failedStep;
    }

    public void updateTranscript(String transcript) {
        this.transcript = transcript;
    }

    public void complete(List<ComparisonItem> comparisons) {
        this.comparisons.clear();
        this.comparisons.addAll(comparisons);
        changeStatus(AttemptStatus.COMPLETED);
    }
}

package org.mju.mjupitchrehearsal.attempt.entity;

import org.mju.mjupitchrehearsal.global.entity.BaseTimeEntity;
import org.mju.mjupitchrehearsal.practice.entity.Practice;
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
 * 녹음 1회. 처리 상태가 바뀐 시각은 updatedAt을 사용합니다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attempt extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practice_id", nullable = false)
    private Practice practice;

    /** S3 객체 키 */
    @Column(nullable = false)
    private String audioKey;

    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FailedStep failedStep;

    @Builder
    private Attempt(Practice practice, String audioKey, Long durationMs) {
        this.practice = practice;
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

    public void updateDuration(Long durationMs) {
        this.durationMs = durationMs;
    }
}

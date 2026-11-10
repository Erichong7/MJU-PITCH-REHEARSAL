package org.sopt.mjupitchrehearsal.practice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전문 용어. STT(Whisper) 호출 시 prompt로 넘겨 인식률을 높입니다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practice_id", nullable = false)
    private Practice practice;

    @Column(nullable = false, length = 30)
    private String word;

    Term(Practice practice, String word) {
        this.practice = practice;
        this.word = word;
    }
}

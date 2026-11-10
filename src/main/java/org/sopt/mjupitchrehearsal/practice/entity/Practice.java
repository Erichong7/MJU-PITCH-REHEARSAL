package org.sopt.mjupitchrehearsal.practice.entity;

import org.sopt.mjupitchrehearsal.global.entity.BaseTimeEntity;
import org.sopt.mjupitchrehearsal.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상황 설정 1건. 발표 조건(purpose·audience·timeLimitSec·keyPoints)과
 * 면접 조건(jobField·interviewType·experience)을 함께 가지며, mode에 따라 쓰는 값이 다릅니다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Practice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mode mode;

    @Column(nullable = false, length = 50)
    private String title;

    // 발표 조건
    @Column(length = 100)
    private String purpose;

    @Column(length = 50)
    private String audience;

    @Column(nullable = false)
    private Integer timeLimitSec;

    @ElementCollection
    @CollectionTable(name = "practice_key_point", joinColumns = @JoinColumn(name = "practice_id"))
    @OrderColumn(name = "seq")
    @Column(name = "content", nullable = false)
    private List<String> keyPoints = new ArrayList<>();

    // 면접 조건 (MVP 이후)
    private String jobField;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private InterviewType interviewType;

    @Column(length = 200)
    private String experience;

    @OneToMany(mappedBy = "practice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Term> terms = new ArrayList<>();

    @Builder
    private Practice(User user, Mode mode, String title, String purpose, String audience, Integer timeLimitSec,
                     List<String> keyPoints, String jobField, InterviewType interviewType, String experience) {
        this.user = user;
        this.mode = mode;
        this.title = title;
        this.purpose = purpose;
        this.audience = audience;
        this.timeLimitSec = timeLimitSec;
        if (keyPoints != null) {
            this.keyPoints.addAll(keyPoints);
        }
        this.jobField = jobField;
        this.interviewType = interviewType;
        this.experience = experience;
    }

    public void addTerm(String word) {
        terms.add(new Term(this, word));
    }

    public List<String> getTermWords() {
        return terms.stream().map(Term::getWord).toList();
    }
}

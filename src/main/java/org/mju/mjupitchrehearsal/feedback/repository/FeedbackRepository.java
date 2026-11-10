package org.mju.mjupitchrehearsal.feedback.repository;

import java.util.List;
import org.mju.mjupitchrehearsal.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /** 구간과 함께 조회 (ISSUE는 rank 순서) */
    @Query("select f from Feedback f join fetch f.segment where f.attempt.id = :attemptId order by f.rank asc")
    List<Feedback> findAllWithSegmentByAttemptId(@Param("attemptId") Long attemptId);

    List<Feedback> findAllByAttemptId(Long attemptId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Feedback f where f.attempt.id = :attemptId")
    void deleteAllByAttemptId(@Param("attemptId") Long attemptId);
}

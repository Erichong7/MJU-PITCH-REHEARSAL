package org.sopt.mjupitchrehearsal.feedback.repository;

import org.sopt.mjupitchrehearsal.feedback.entity.Feedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findAllByAttemptId(Long attemptId);

    void deleteAllByAttemptId(Long attemptId);
}

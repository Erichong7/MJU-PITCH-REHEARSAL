package org.mju.mjupitchrehearsal.retry.repository;

import java.util.List;
import org.mju.mjupitchrehearsal.retry.entity.Retry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetryRepository extends JpaRepository<Retry, Long> {

    List<Retry> findAllByFeedbackAttemptId(Long attemptId);

    long countByFeedbackId(Long feedbackId);
}

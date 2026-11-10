package org.sopt.mjupitchrehearsal.attempt.repository;

import org.sopt.mjupitchrehearsal.attempt.entity.AttemptMetric;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptMetricRepository extends JpaRepository<AttemptMetric, Long> {

    Optional<AttemptMetric> findByAttemptId(Long attemptId);
}

package org.mju.mjupitchrehearsal.attempt.repository;

import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    long countByPracticeId(Long practiceId);
}

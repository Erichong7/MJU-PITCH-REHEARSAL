package org.sopt.mjupitchrehearsal.attempt.repository;

import org.sopt.mjupitchrehearsal.attempt.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
}

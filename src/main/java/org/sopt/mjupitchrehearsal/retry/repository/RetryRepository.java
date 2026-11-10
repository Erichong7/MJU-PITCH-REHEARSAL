package org.sopt.mjupitchrehearsal.retry.repository;

import org.sopt.mjupitchrehearsal.retry.entity.Retry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetryRepository extends JpaRepository<Retry, Long> {
}

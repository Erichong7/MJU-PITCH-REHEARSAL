package org.mju.mjupitchrehearsal.retry.repository;

import org.mju.mjupitchrehearsal.retry.entity.Retry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetryRepository extends JpaRepository<Retry, Long> {
}

package org.mju.mjupitchrehearsal.practice.repository;

import org.mju.mjupitchrehearsal.practice.entity.Practice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeRepository extends JpaRepository<Practice, Long> {
}

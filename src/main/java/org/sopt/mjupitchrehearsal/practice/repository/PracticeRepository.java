package org.sopt.mjupitchrehearsal.practice.repository;

import org.sopt.mjupitchrehearsal.practice.entity.Practice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeRepository extends JpaRepository<Practice, Long> {
}

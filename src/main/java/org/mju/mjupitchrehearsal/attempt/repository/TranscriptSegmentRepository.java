package org.mju.mjupitchrehearsal.attempt.repository;

import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    List<TranscriptSegment> findAllByAttemptIdOrderBySeqAsc(Long attemptId);

    Optional<TranscriptSegment> findByAttemptIdAndSegmentKey(Long attemptId, String segmentKey);
}

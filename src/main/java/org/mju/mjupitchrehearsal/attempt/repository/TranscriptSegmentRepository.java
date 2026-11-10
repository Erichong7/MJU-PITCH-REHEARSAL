package org.mju.mjupitchrehearsal.attempt.repository;

import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    List<TranscriptSegment> findAllByAttemptIdOrderBySeqAsc(Long attemptId);

    Optional<TranscriptSegment> findByAttemptIdAndSegmentKey(Long attemptId, String segmentKey);

    /** 바로 DELETE 쿼리를 실행합니다 (같은 트랜잭션에서 새 구간을 저장할 때 unique 충돌 방지). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TranscriptSegment s where s.attempt.id = :attemptId")
    void deleteAllByAttemptId(@Param("attemptId") Long attemptId);
}

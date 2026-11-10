package org.mju.mjupitchrehearsal.attempt.dto;

import java.util.List;
import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;

public record TranscriptResponse(Long attemptId, List<SegmentDto> segments) {

    public record SegmentDto(String segmentId, Long startMs, Long endMs, String text, boolean edited) {

        public static SegmentDto from(TranscriptSegment s) {
            return new SegmentDto(s.getSegmentKey(), s.getStartMs(), s.getEndMs(), s.getText(), s.isEdited());
        }
    }
}

package org.mju.mjupitchrehearsal.attempt.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;
import org.mju.mjupitchrehearsal.attempt.entity.FailedStep;

/** failedStep은 FAILED일 때만 값이 있고, 그 외에는 null로 내려갑니다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AttemptStatusResponse(
        Long attemptId,
        AttemptStatus status,
        FailedStep failedStep,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt
) {

    public static AttemptStatusResponse from(Attempt attempt) {
        return new AttemptStatusResponse(attempt.getId(), attempt.getStatus(), attempt.getFailedStep(),
                attempt.getUpdatedAt());
    }
}

package org.mju.mjupitchrehearsal.attempt.dto;

import org.mju.mjupitchrehearsal.attempt.entity.Attempt;
import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;

public record AttemptUploadResponse(Long attemptId, Integer attemptNo, AttemptStatus status) {

    public static AttemptUploadResponse from(Attempt attempt) {
        return new AttemptUploadResponse(attempt.getId(), attempt.getAttemptNo(), attempt.getStatus());
    }
}

package org.mju.mjupitchrehearsal.attempt.dto;

import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;

public record TranscriptEditResponse(Long attemptId, AttemptStatus status) {
}

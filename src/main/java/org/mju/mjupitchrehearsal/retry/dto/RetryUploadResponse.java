package org.mju.mjupitchrehearsal.retry.dto;

import org.mju.mjupitchrehearsal.attempt.entity.AttemptStatus;

public record RetryUploadResponse(Long retryId, Integer retryNo, AttemptStatus status) {
}

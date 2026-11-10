package org.mju.mjupitchrehearsal.attempt.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record AudioUrlResponse(
        String audioUrl,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime expiresAt,
        Long durationMs
) {
}

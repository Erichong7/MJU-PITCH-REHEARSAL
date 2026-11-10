package org.mju.mjupitchrehearsal.attempt.support;

import java.io.IOException;
import java.util.Locale;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드한 녹음 파일 검사 (④ 녹음, ⑬ 재연습 공통). m4a만 받고 길이는 파일 헤더에서 읽습니다.
 */
public final class AudioFiles {

    private AudioFiles() {
    }

    /** 파일이 있고 m4a인지 확인한 뒤 내용과 길이를 돌려줍니다. */
    public static M4a readM4a(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new BusinessException(ErrorCode.AUDIO_REQUIRED);
        }
        String name = audio.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".m4a")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_AUDIO);
        }
        byte[] content;
        try {
            content = audio.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        long durationMs = M4aDurationReader.readDurationMs(content)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_AUDIO));
        return new M4a(content, durationMs);
    }

    public record M4a(byte[] content, long durationMs) {
    }
}

package org.mju.mjupitchrehearsal.infra.storage;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 저장소의 재생 링크(⑩)가 가리키는 주소. S3로 바꾸면 사라집니다.
 * Range 요청을 지원해서 앱이 startMs로 바로 이동(seek)할 수 있습니다.
 */
@Hidden
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalAudioController {

    private static final MediaType AUDIO_MP4 = MediaType.parseMediaType("audio/mp4");

    private final LocalAudioStorage storage;

    @GetMapping("/api/v1/local-audio")
    public ResponseEntity<Resource> play(@RequestParam String key,
                                         @RequestParam long expires,
                                         @RequestParam String signature) {
        if (!storage.verify(key, expires, signature)) {
            throw new BusinessException(ErrorCode.AUDIO_URL_INVALID);
        }
        return ResponseEntity.ok().contentType(AUDIO_MP4).body(storage.load(key));
    }
}

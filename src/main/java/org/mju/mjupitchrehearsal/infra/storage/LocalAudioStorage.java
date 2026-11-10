package org.mju.mjupitchrehearsal.infra.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 내 컴퓨터 디스크에 저장하는 임시 저장소. S3가 준비되기 전까지 사용합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalAudioStorage implements AudioStorage {

    private final Path root;

    public LocalAudioStorage(@Value("${storage.local.dir:./uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public void save(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            log.info("[LocalStorage] saved {} ({} bytes)", target, content.length);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public Resource load(String key) {
        Path target = resolve(key);
        if (!Files.exists(target)) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "녹음 파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(target);
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return target;
    }
}

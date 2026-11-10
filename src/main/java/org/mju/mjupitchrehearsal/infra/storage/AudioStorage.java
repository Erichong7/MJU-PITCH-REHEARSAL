package org.mju.mjupitchrehearsal.infra.storage;

import org.springframework.core.io.Resource;

/**
 * 녹음 파일 저장소. 지금은 로컬 디스크({@link LocalAudioStorage}), S3 준비 후 S3 구현으로 교체합니다.
 * 교체는 설정값 {@code storage.type}으로 합니다.
 */
public interface AudioStorage {

    /**
     * @param key     저장 위치 (예: attempts/12/uuid.m4a)
     * @param content 파일 내용
     */
    void save(String key, byte[] content);

    Resource load(String key);
}

package org.mju.mjupitchrehearsal.infra.stt;

import java.util.List;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;

/**
 * 음성 → 글 변환(STT). 4단계에서 Whisper 구현을 추가하고 설정값 {@code stt.provider}로 교체합니다.
 */
public interface SttClient {

    /**
     * @param audioKey 저장소의 녹음 파일 위치
     * @param terms    전문 용어 (인식률을 높이기 위해 STT에 힌트로 전달)
     */
    SttResult transcribe(String audioKey, List<String> terms);
}

package org.mju.mjupitchrehearsal.infra.stt;

import java.util.List;
import org.mju.mjupitchrehearsal.global.util.FakeDelay;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 가짜 STT. 녹음 내용과 관계없이 항상 같은 전사를 돌려줍니다.
 * {@code fake.delay-ms}만큼 기다려서 ⑤ 상태 조회에서 TRANSCRIBING을 볼 수 있게 합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "stt.provider", havingValue = "fake", matchIfMissing = true)
public class FakeSttClient implements SttClient {

    private static final List<SttResult.Segment> SEGMENTS = List.of(
            new SttResult.Segment(0, 4200, "안녕하세요, 빈자리맵 팀의 발표를 시작하겠습니다."),
            new SttResult.Segment(4500, 10800, "학생들은 빈 강의실을 찾기 위해 평균 십오 분을 씁니다."),
            new SttResult.Segment(12300, 17600, "저희 서비스는 시간표 데이터로 지금 비어 있는 강의실을 바로 보여 줍니다."),
            new SttResult.Segment(18000, 22400, "이 시장은 엄청 큽니다."),
            new SttResult.Segment(22900, 27500, "앞으로 다른 학교로도 확장할 계획입니다.")
    );

    private final long delayMs;

    public FakeSttClient(@Value("${fake.delay-ms:2000}") long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public SttResult transcribe(String audioKey, List<String> terms) {
        log.info("[FakeStt] transcribe audioKey={} terms={}", audioKey, terms);
        FakeDelay.sleep(delayMs);
        return new SttResult(SEGMENTS);
    }
}

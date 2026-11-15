package org.mju.mjupitchrehearsal.infra.stt.dto;

import java.util.List;

/**
 * STT 결과. 문장(구간) 단위로 시작·끝 시간과 글을 담습니다.
 */
public record SttResult(List<Segment> segments) {

    /**
     * @param pauses 문장 안에서 1초 이상 멈춘 곳. 문장과 문장 사이 멈춤은 구간 간격으로 따로 계산합니다.
     */
    public record Segment(long startMs, long endMs, String text, List<Pause> pauses) {

        public Segment {
            pauses = pauses == null ? List.of() : List.copyOf(pauses);
        }

        public Segment(long startMs, long endMs, String text) {
            this(startMs, endMs, text, List.of());
        }
    }

    /** startMs: 멈춤이 시작된 시각(추정), pauseMs: 멈춘 길이 */
    public record Pause(long startMs, long pauseMs) {
    }
}

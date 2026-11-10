package org.mju.mjupitchrehearsal.infra.stt.dto;

import java.util.List;

/**
 * STT 결과. 문장(구간) 단위로 시작·끝 시간과 글을 담습니다.
 */
public record SttResult(List<Segment> segments) {

    public record Segment(long startMs, long endMs, String text) {
    }
}

package org.mju.mjupitchrehearsal.attempt.entity;

/**
 * 한 구간(문장) 안에서 멈춘 곳. STT 단어 시간으로 찾고, 전사를 고쳐도 바뀌지 않습니다.
 *
 * @param startMs 멈춤이 시작된 시각(추정)
 * @param pauseMs 멈춘 길이
 */
public record InnerPause(long startMs, long pauseMs) {
}

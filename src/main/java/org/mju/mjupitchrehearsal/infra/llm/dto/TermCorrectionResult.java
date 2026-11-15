package org.mju.mjupitchrehearsal.infra.llm.dto;

import java.util.List;

/**
 * 전문 용어 교정 출력. 서버가 검증한 뒤 통과한 것만 반영합니다.
 *
 * @param corrections 고칠 곳 목록 (없으면 빈 리스트)
 */
public record TermCorrectionResult(
        List<Correction> corrections
) {

    /**
     * @param segmentId 구간 ID (요청에 있던 값)
     * @param from      그 구간 전사에 그대로 있는 잘못 적힌 부분
     * @param to        from을 대신할 글. terms 중 하나를 포함
     */
    public record Correction(String segmentId, String from, String to) {
    }
}

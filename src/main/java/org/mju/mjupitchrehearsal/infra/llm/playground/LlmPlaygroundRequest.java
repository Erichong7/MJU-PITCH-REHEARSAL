package org.mju.mjupitchrehearsal.infra.llm.playground;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 연습용 주소 요청. 전사 글만 넣어도 되고, 조건은 비워도 됩니다.
 */
public record LlmPlaygroundRequest(
        @Schema(description = "전사 글. 문장(. ? !)이나 줄바꿈 단위로 seg_01…로 나뉩니다.",
                example = "안녕하세요, 빈자리맵 팀입니다. 학생들은 빈 강의실을 찾는 데 시간을 많이 씁니다. 이 시장은 엄청 큽니다. RAG로 추천 정확도를 높였습니다.")
        @NotBlank(message = "전사 글은 필수입니다.")
        String transcript,

        @Schema(example = "빈자리맵 서비스 소개") String purpose,
        @Schema(example = "교수님과 학생") String audience,
        @Schema(example = "180") Integer timeLimitSec,
        @Schema(example = "[\"빈 강의실을 바로 찾는다\"]") List<String> keyPoints,
        @Schema(example = "[\"RAG\"]") List<String> terms,
        @Schema(description = "비우면 resources/prompts/feedback-system.txt 사용") String systemPrompt
) {
}

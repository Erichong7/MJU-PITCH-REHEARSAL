package org.mju.mjupitchrehearsal.practice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.mju.mjupitchrehearsal.practice.entity.InterviewType;
import org.mju.mjupitchrehearsal.practice.entity.Mode;

/**
 * ① 연습 생성 요청. 모드별 필수 조건은 PracticeService에서 한 번 더 검증합니다.
 */
public record PracticeCreateRequest(
        @Schema(example = "PRESENTATION")
        @NotNull(message = "모드는 필수입니다.")
        Mode mode,

        @Schema(example = "캡스톤 중간 발표")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 50, message = "제목은 50자 이내입니다.")
        String title,

        @NotNull(message = "조건은 필수입니다.")
        @Valid
        Condition condition,

        @Schema(example = "[\"빈자리맵\", \"STT\"]")
        @Size(max = 20, message = "전문 용어는 최대 20개입니다.")
        List<@NotBlank(message = "전문 용어는 빈 값일 수 없습니다.")
             @Size(max = 30, message = "전문 용어는 각 30자 이내입니다.") String> terms
) {

    public record Condition(
            @Schema(example = "빈자리맵 서비스 소개")
            @Size(max = 100, message = "발표 목적은 100자 이내입니다.")
            String purpose,

            @Schema(example = "교수님과 학생")
            @Size(max = 50, message = "청중은 50자 이내입니다.")
            String audience,

            @Schema(example = "180")
            @NotNull(message = "제한 시간은 필수입니다.")
            Integer timeLimitSec,

            @Schema(example = "[\"빈 강의실을 바로 찾는다\"]")
            @Size(max = 5, message = "꼭 전달할 내용은 최대 5개입니다.")
            List<@NotBlank(message = "꼭 전달할 내용은 빈 값일 수 없습니다.") String> keyPoints,

            @Schema(description = "INTERVIEW 필수", example = "백엔드 개발")
            @Size(max = 50, message = "지원 분야는 50자 이내입니다.")
            String jobField,

            @Schema(description = "INTERVIEW 필수")
            InterviewType interviewType,

            @Schema(example = "캡스톤 서버 담당")
            @Size(max = 200, message = "강조할 경험은 200자 이내입니다.")
            String experience
    ) {
    }
}

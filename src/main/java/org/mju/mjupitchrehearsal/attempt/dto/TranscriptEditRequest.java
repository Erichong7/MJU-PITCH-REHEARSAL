package org.mju.mjupitchrehearsal.attempt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** ⑧ 전사 수정 요청. 글만 고칠 수 있고 시간은 고칠 수 없습니다. */
public record TranscriptEditRequest(
        @NotEmpty(message = "수정할 구간이 없습니다.")
        List<@Valid Segment> segments
) {

    public record Segment(
            @Schema(example = "seg_04")
            @NotBlank(message = "구간 ID는 필수입니다.")
            String segmentId,

            @Schema(example = "이 시장은 국내에서만 3조 원 규모입니다.")
            @NotBlank(message = "고친 텍스트는 필수입니다.")
            @Size(max = 500, message = "텍스트는 500자 이내입니다.")
            String text
    ) {
    }
}

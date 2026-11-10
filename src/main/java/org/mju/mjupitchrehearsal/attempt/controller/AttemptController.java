package org.mju.mjupitchrehearsal.attempt.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.attempt.dto.AttemptStatusResponse;
import org.mju.mjupitchrehearsal.attempt.dto.AttemptUploadResponse;
import org.mju.mjupitchrehearsal.attempt.dto.TranscriptResponse;
import org.mju.mjupitchrehearsal.attempt.service.AttemptService;
import org.mju.mjupitchrehearsal.global.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "녹음·분석")
@RestController
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @Operation(summary = "④ 녹음 업로드 (분석 시작)",
            description = "m4a(AAC), 최대 25MB, 발표 5분·면접 2분 이내. 응답 후 분석은 백그라운드에서 진행되며 ⑤로 확인합니다.")
    @PostMapping(value = "/api/v1/practices/{practiceId}/attempts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<AttemptUploadResponse>> upload(
            @PathVariable Long practiceId,
            @RequestPart(value = "audio", required = false) MultipartFile audio) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.of(HttpStatus.CREATED, "녹음 업로드 성공", attemptService.upload(practiceId, audio)));
    }

    @Operation(summary = "⑤ 처리 상태 조회", description = "앱은 2초 간격으로 폴링하고 COMPLETED/FAILED에서 멈춥니다.")
    @GetMapping("/api/v1/attempts/{attemptId}/status")
    public BaseResponse<AttemptStatusResponse> getStatus(@PathVariable Long attemptId) {
        return BaseResponse.of(HttpStatus.OK, "처리 상태 조회 성공", attemptService.getStatus(attemptId));
    }

    @Operation(summary = "⑦ 전사 조회", description = "구간 단위 전사와 타임스탬프. 전사가 끝나기 전에는 409")
    @GetMapping("/api/v1/attempts/{attemptId}/transcript")
    public BaseResponse<TranscriptResponse> getTranscript(@PathVariable Long attemptId) {
        return BaseResponse.of(HttpStatus.OK, "전사 조회 성공", attemptService.getTranscript(attemptId));
    }
}

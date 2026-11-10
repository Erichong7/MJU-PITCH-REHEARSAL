package org.mju.mjupitchrehearsal.retry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.global.response.BaseResponse;
import org.mju.mjupitchrehearsal.retry.dto.RetryResultResponse;
import org.mju.mjupitchrehearsal.retry.dto.RetryUploadResponse;
import org.mju.mjupitchrehearsal.retry.service.RetryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "재연습·비교")
@RestController
@RequiredArgsConstructor
public class RetryController {

    private final RetryService retryService;

    @Operation(summary = "⑬ 구간 재연습 업로드",
            description = "피드백이 짚은 구간만 다시 녹음(m4a, 최대 90초). 비교는 백그라운드에서 진행되고 ⑭로 확인합니다.")
    @PostMapping(value = "/api/v1/feedbacks/{feedbackId}/retries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<RetryUploadResponse>> upload(
            @PathVariable Long feedbackId,
            @RequestPart(value = "audio", required = false) MultipartFile audio) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.of(HttpStatus.CREATED, "재연습 업로드 성공", retryService.upload(feedbackId, audio)));
    }

    @Operation(summary = "⑭ 재연습 결과(1차·2차 비교) 조회",
            description = "status가 COMPLETED가 아니면 comparison은 null (폴링 겸용)")
    @GetMapping("/api/v1/retries/{retryId}")
    public BaseResponse<RetryResultResponse> getResult(@PathVariable Long retryId) {
        return BaseResponse.of(HttpStatus.OK, "재연습 결과 조회 성공", retryService.getResult(retryId));
    }
}

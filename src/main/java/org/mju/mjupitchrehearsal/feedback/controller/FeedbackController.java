package org.mju.mjupitchrehearsal.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.feedback.dto.FeedbackListResponse;
import org.mju.mjupitchrehearsal.feedback.service.FeedbackQueryService;
import org.mju.mjupitchrehearsal.global.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "피드백")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackQueryService feedbackQueryService;

    @Operation(summary = "⑨ 분석 결과(피드백) 조회",
            description = "인용 검증을 통과한 고칠 곳 최대 3개 + 잘된 부분 1개. startMs·endMs는 서버가 전사 타임스탬프로 채움. 분석 전 409")
    @GetMapping("/api/v1/attempts/{attemptId}/feedbacks")
    public BaseResponse<FeedbackListResponse> getFeedbacks(@PathVariable Long attemptId) {
        return BaseResponse.of(HttpStatus.OK, "피드백 조회 성공", feedbackQueryService.getFeedbacks(attemptId));
    }
}

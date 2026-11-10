package org.mju.mjupitchrehearsal.practice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.global.response.BaseResponse;
import org.mju.mjupitchrehearsal.practice.dto.PracticeCreateRequest;
import org.mju.mjupitchrehearsal.practice.dto.PracticeCreateResponse;
import org.mju.mjupitchrehearsal.practice.service.PracticeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "연습 설정")
@RestController
@RequestMapping("/api/v1/practices")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @Operation(summary = "① 연습 생성 (상황 설정)", description = "mode에 따라 condition 필수 값이 다릅니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<PracticeCreateResponse>> create(@Valid @RequestBody PracticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.of(HttpStatus.CREATED, "연습 생성 성공", practiceService.create(request)));
    }
}

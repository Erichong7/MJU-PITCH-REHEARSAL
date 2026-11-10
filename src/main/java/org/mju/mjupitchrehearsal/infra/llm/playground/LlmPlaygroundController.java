package org.mju.mjupitchrehearsal.infra.llm.playground;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.mju.mjupitchrehearsal.global.response.BaseResponse;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackRequest;
import org.mju.mjupitchrehearsal.infra.llm.openai.OpenAiChatClient;
import org.mju.mjupitchrehearsal.practice.entity.Mode;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 프롬프트를 다듬기 위한 연습용 주소. local 프로필(내 컴퓨터)에서만 열리고 배포 서버에는 없습니다.
 * 녹음·DB 없이 전사 글만 넣으면 GPT가 준 답을 그대로 보여 줍니다.
 */
@Profile("local")
@Tag(name = "개발용 (local 전용)")
@RestController
@RequestMapping("/api/v1/dev/llm")
@RequiredArgsConstructor
public class LlmPlaygroundController {

    private static final String FEEDBACK_PROMPT = "prompts/feedback-system.txt";

    private final OpenAiChatClient openAiChatClient;
    private final JsonMapper jsonMapper;

    @Operation(summary = "피드백 프롬프트 시험", description = "전사 글 → seg_01… 구간 → GPT 답(JSON)을 그대로 보여 줍니다. OPENAI_API_KEY 필요.")
    @PostMapping("/feedback")
    public BaseResponse<LlmPlaygroundResponse> feedback(@Valid @RequestBody LlmPlaygroundRequest request) {
        FeedbackRequest feedbackRequest = new FeedbackRequest(
                Mode.PRESENTATION,
                new FeedbackRequest.Condition(request.purpose(), request.audience(), request.timeLimitSec(),
                        nullToEmpty(request.keyPoints()), nullToEmpty(request.terms())),
                new FeedbackRequest.Metrics(null, null, List.of(), List.of()),
                splitSegments(request.transcript()));

        String systemPrompt = StringUtils.hasText(request.systemPrompt())
                ? request.systemPrompt()
                : loadPrompt(FEEDBACK_PROMPT);
        String userMessage = jsonMapper.writeValueAsString(feedbackRequest);

        OpenAiChatClient.ChatResult result = openAiChatClient.chat(systemPrompt, userMessage);
        return BaseResponse.ok(new LlmPlaygroundResponse(
                feedbackRequest, toJsonOrText(result.content()), result.model(),
                result.promptTokens(), result.completionTokens(), result.elapsedMs()));
    }

    /** 문장 끝(. ? !)이나 줄바꿈에서 나눕니다. 실제 서비스에서는 STT가 준 구간을 그대로 씁니다. */
    private List<FeedbackRequest.Segment> splitSegments(String transcript) {
        List<FeedbackRequest.Segment> segments = new ArrayList<>();
        for (String sentence : transcript.split("(?<=[.?!])\\s+|\\n+")) {
            if (StringUtils.hasText(sentence)) {
                segments.add(new FeedbackRequest.Segment(
                        String.format("seg_%02d", segments.size() + 1), sentence.strip()));
            }
        }
        return segments;
    }

    private String loadPrompt(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "프롬프트 파일을 읽을 수 없습니다: " + path);
        }
    }

    /** GPT 답이 JSON이면 보기 좋게 객체로, 아니면 글자 그대로 돌려줍니다. */
    private Object toJsonOrText(String content) {
        try {
            JsonNode node = jsonMapper.readTree(content);
            return node;
        } catch (RuntimeException e) {
            return content;
        }
    }

    private static List<String> nullToEmpty(List<String> list) {
        return list == null ? List.of() : list;
    }

    public record LlmPlaygroundResponse(
            FeedbackRequest sentToAi,
            Object aiAnswer,
            String model,
            int promptTokens,
            int completionTokens,
            long elapsedMs
    ) {
    }
}

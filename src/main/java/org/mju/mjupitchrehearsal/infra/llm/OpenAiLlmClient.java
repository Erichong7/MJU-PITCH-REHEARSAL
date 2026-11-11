package org.mju.mjupitchrehearsal.infra.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.mju.mjupitchrehearsal.infra.llm.dto.ComparisonRequest;
import org.mju.mjupitchrehearsal.infra.llm.dto.ComparisonResult;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackRequest;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackResult;
import org.mju.mjupitchrehearsal.infra.llm.openai.OpenAiChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * 실제 GPT로 피드백·비교를 받습니다.
 *
 * <p>여기서는 GPT 답을 JSON으로 바꾸는 것까지만 합니다. 항목별 검증(구간·인용·최대 3개)은
 * 저장하는 쪽(FeedbackSaveService, RetryStepService)에서 하고, 잘못된 항목은 그 항목만 버립니다.
 * 항목 하나 때문에 분석 전체가 실패하지 않게 하기 위해서입니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public class OpenAiLlmClient implements LlmClient {

    private final OpenAiChatClient openAiChatClient;
    private final JsonMapper jsonMapper;

    public OpenAiLlmClient(OpenAiChatClient openAiChatClient,
                           JsonMapper jsonMapper) {
        this.openAiChatClient = openAiChatClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public FeedbackResult requestFeedback(FeedbackRequest request) {
        return callAi("prompts/feedback-system.txt", request, FeedbackResult.class);
    }

    @Override
    public ComparisonResult requestComparison(ComparisonRequest request) {
        return callAi("prompts/comparison-system.txt", request, ComparisonResult.class);
    }

    private <T> T callAi(String promptPath, Object request,
                         Class<T> resultType) {
        String systemPrompt = loadPrompt(promptPath);
        String userMessage = jsonMapper.writeValueAsString(request);

        OpenAiChatClient.ChatResult response =
                openAiChatClient.chat(systemPrompt, userMessage);

        if (response == null
                || !StringUtils.hasText(response.content())) {
            throw invalidResponse("GPT 응답이 비어 있습니다.");
        }

        T result;
        try {
            result = jsonMapper.readValue(response.content(), resultType);
        } catch (RuntimeException e) {
            log.warn("[OpenAiLlm] JSON 변환 실패 prompt={} content={}",
                    promptPath, response.content(), e);
            throw invalidResponse("GPT 응답을 변환할 수 없습니다.");
        }

        if (result == null) {
            throw invalidResponse("GPT 응답에 결과가 없습니다.");
        }
        return result;
    }

    private String loadPrompt(String path) {
        try {
            return new ClassPathResource(path)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "프롬프트 파일을 읽을 수 없습니다: " + path
            );
        }
    }

    private BusinessException invalidResponse(String message) {
        return new BusinessException(
                ErrorCode.EXTERNAL_API_ERROR,
                message
        );
    }
}

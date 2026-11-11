package org.mju.mjupitchrehearsal.infra.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        FeedbackResult result = callAi(
                "prompts/feedback-system.txt",
                request,
                FeedbackResult.class
        );

        validateFeedback(result);
        return result;
    }

    @Override
    public ComparisonResult requestComparison(ComparisonRequest request) {
        ComparisonResult result = callAi(
                "prompts/comparison-system.txt",
                request,
                ComparisonResult.class
        );

        validateComparison(result, request.afterText());
        return result;
    }

    private <T> T callAi(String promptPath, Object request,
                         Class<T> resultType) {
        String systemPrompt = loadPrompt(promptPath);

        try {
            String userMessage = jsonMapper.writeValueAsString(request);

            OpenAiChatClient.ChatResult response =
                    openAiChatClient.chat(systemPrompt, userMessage);

            if (response == null
                    || !StringUtils.hasText(response.content())) {
                throw invalidResponse("GPT 응답이 비어 있습니다.");
            }

            T result = jsonMapper.readValue(response.content(), resultType);

            if (result == null) {
                throw invalidResponse("GPT 응답에 결과가 없습니다.");
            }

            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw invalidResponse(
                    "GPT 요청 또는 응답을 변환할 수 없습니다."
            );
        }
    }

    private void validateFeedback(FeedbackResult result) {
        if (result.issues() == null || result.strength() == null) {
            throw invalidResponse(
                    "GPT 피드백 응답에 필수 항목이 없습니다."
            );
        }

        if (result.issues().size() > 3) {
            throw invalidResponse(
                    "GPT 피드백의 고칠 부분이 3개를 초과했습니다."
            );
        }

        for (FeedbackResult.Issue issue : result.issues()) {
            if (issue == null
                    || !StringUtils.hasText(issue.segmentId())
                    || issue.type() == null
                    || !StringUtils.hasText(issue.quote())
                    || !StringUtils.hasText(issue.reason())
                    || !StringUtils.hasText(issue.suggestion())) {
                throw invalidResponse(
                        "GPT 피드백 항목의 필수 값이 누락됐습니다."
                );
            }
        }

        FeedbackResult.Strength strength = result.strength();

        if (!StringUtils.hasText(strength.segmentId())
                || !StringUtils.hasText(strength.quote())
                || !StringUtils.hasText(strength.reason())) {
            throw invalidResponse(
                    "GPT의 잘된 부분에 필수 값이 누락됐습니다."
            );
        }
    }

    private void validateComparison(ComparisonResult result,
                                    String afterText) {
        if (result.items() == null
                || result.items().isEmpty()
                || result.items().size() > 3) {
            throw invalidResponse(
                    "GPT 비교 항목은 1~3개여야 합니다."
            );
        }

        for (ComparisonResult.Item item : result.items()) {
            if (item == null
                    || !StringUtils.hasText(item.criterion())
                    || !StringUtils.hasText(item.before())
                    || !StringUtils.hasText(item.after())) {
                throw invalidResponse(
                        "GPT 비교 항목의 필수 값이 누락됐습니다."
                );
            }

            if (item.resolved()
                    && (!StringUtils.hasText(item.afterQuote())
                    || afterText == null
                    || !afterText.contains(item.afterQuote()))) {
                throw invalidResponse(
                        "GPT의 개선 판단을 뒷받침하는 실제 인용이 없습니다."
                );
            }
        }
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
package org.mju.mjupitchrehearsal.infra.llm.openai;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenAI(GPT) Chat Completions 호출. 답은 항상 JSON 문자열로 받습니다(response_format=json_object).
 *
 * <p>프롬프트를 만들고 답을 FeedbackResult 등으로 바꾸는 일은 이 클래스를 쓰는 쪽(진짜 LlmClient)이 합니다.
 */
@Slf4j
@Component
public class OpenAiChatClient {

    private static final String BASE_URL = "https://api.openai.com/v1";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    public OpenAiChatClient(JsonMapper jsonMapper,
                            @Value("${openai.api-key:}") String apiKey,
                            @Value("${openai.model:gpt-4o-mini}") String model,
                            @Value("${openai.timeout-sec:60}") int timeoutSec) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(timeoutSec * 1000);
        this.restClient = RestClient.builder().baseUrl(BASE_URL).requestFactory(factory).build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * @param systemPrompt 역할·규칙·출력 형식
     * @param userMessage  이번 요청 내용 (보통 주문서 JSON)
     * @return GPT가 돌려준 JSON 문자열 그대로
     */
    public ChatResult chat(String systemPrompt, String userMessage) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "OPENAI_API_KEY가 설정되지 않았습니다. .env를 확인해 주세요.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)));

        long start = System.currentTimeMillis();
        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode root = jsonMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asString();
            JsonNode usage = root.path("usage");
            ChatResult result = new ChatResult(model, content,
                    usage.path("prompt_tokens").asInt(), usage.path("completion_tokens").asInt(),
                    System.currentTimeMillis() - start);
            log.info("[OpenAI] model={} promptTokens={} completionTokens={} elapsedMs={}",
                    model, result.promptTokens(), result.completionTokens(), result.elapsedMs());
            return result;
        } catch (RestClientResponseException e) {
            log.error("[OpenAI] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "OpenAI 호출 실패: " + e.getStatusCode());
        } catch (RestClientException e) {
            log.error("[OpenAI] request failed", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "OpenAI 호출 실패: " + e.getMessage());
        }
    }

    /** 비용 측정(9단계)을 위해 토큰 수와 걸린 시간도 함께 돌려줍니다. */
    public record ChatResult(String model, String content, int promptTokens, int completionTokens, long elapsedMs) {
    }
}

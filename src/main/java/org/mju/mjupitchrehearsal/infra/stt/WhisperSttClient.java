package org.mju.mjupitchrehearsal.infra.stt;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.mju.mjupitchrehearsal.infra.storage.AudioStorage;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenAI Whisper로 녹음을 글로 바꿉니다. 구간(문장)마다 시작·끝 시간을 받습니다.
 * 사용하려면 {@code STT_PROVIDER=whisper}와 {@code OPENAI_API_KEY}를 설정합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "stt.provider", havingValue = "whisper")
public class WhisperSttClient implements SttClient {

    /** Whisper prompt는 224토큰까지라 용어가 너무 많으면 앞에서부터 자릅니다. */
    private static final int MAX_PROMPT_CHARS = 400;

    private final RestClient restClient;
    private final AudioStorage audioStorage;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    public WhisperSttClient(AudioStorage audioStorage,
                            JsonMapper jsonMapper,
                            @Value("${openai.api-key:}") String apiKey,
                            @Value("${stt.whisper.model:whisper-1}") String model,
                            @Value("${stt.whisper.timeout-sec:120}") int timeoutSec) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(timeoutSec * 1000);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(factory)
                .build();
        this.audioStorage = audioStorage;
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public SttResult transcribe(String audioKey, List<String> terms) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        Resource audio = audioStorage.load(audioKey);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", audio);
        form.add("model", model);
        form.add("language", "ko");
        form.add("response_format", "verbose_json");
        form.add("timestamp_granularities[]", "segment");
        String prompt = buildPrompt(terms);
        if (StringUtils.hasText(prompt)) {
            form.add("prompt", prompt);
        }

        long start = System.currentTimeMillis();
        try {
            String response = restClient.post()
                    .uri("/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            SttResult result = parse(response);
            log.info("[Whisper] audioKey={} segments={} elapsedMs={}",
                    audioKey, result.segments().size(), System.currentTimeMillis() - start);
            return result;
        } catch (RestClientResponseException e) {
            log.error("[Whisper] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Whisper 호출 실패: " + e.getStatusCode());
        } catch (RestClientException e) {
            log.error("[Whisper] request failed", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Whisper 호출 실패: " + e.getMessage());
        }
    }

    /** 전문 용어를 문장처럼 넘기면 Whisper가 그 표기를 따라 씁니다. */
    private String buildPrompt(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return null;
        }
        String joined = String.join(", ", terms);
        return joined.length() > MAX_PROMPT_CHARS ? joined.substring(0, MAX_PROMPT_CHARS) : joined;
    }

    private SttResult parse(String response) {
        JsonNode root = jsonMapper.readTree(response);
        List<SttResult.Segment> segments = new ArrayList<>();
        for (JsonNode node : root.path("segments")) {
            String text = node.path("text").asString().strip();
            if (text.isEmpty()) {
                continue;
            }
            long startMs = Math.round(node.path("start").asDouble() * 1000);
            long endMs = Math.round(node.path("end").asDouble() * 1000);
            segments.add(new SttResult.Segment(startMs, Math.max(startMs, endMs), text));
        }
        return new SttResult(segments);
    }
}

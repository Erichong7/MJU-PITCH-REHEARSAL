package org.mju.mjupitchrehearsal.infra.stt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.infra.llm.LlmClient;
import org.mju.mjupitchrehearsal.infra.llm.dto.FeedbackRequest;
import org.mju.mjupitchrehearsal.infra.llm.dto.TermCorrectionRequest;
import org.mju.mjupitchrehearsal.infra.llm.dto.TermCorrectionResult;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;
import org.springframework.stereotype.Component;

/**
 * STT가 소리 나는 대로 잘못 적은 전문 용어를 고칩니다. ("레디스로" → "Redis로", "에스쿠로" → "에스크로")
 *
 * <p>Whisper prompt에 용어를 넘겨도 한글 표기로 적는 경우가 많고, 틀린 용어는 엉뚱한 피드백으로 이어집니다.
 * 그래서 STT 직후 GPT에게 고칠 곳만 받고, 아래 조건을 모두 만족하는 것만 반영합니다.
 * <ul>
 *   <li>있는 구간이고, from이 그 구간 글에 그대로 있다</li>
 *   <li>to에 전문 용어 중 하나가 들어 있다 (용어와 상관없는 교정은 버림)</li>
 *   <li>실제로 글자가 바뀌고, from이 {@value #MAX_FROM_LENGTH}자 이내다 (문장을 통째로 바꾸지 않음)</li>
 * </ul>
 * 시간은 건드리지 않습니다(⑧ 전사 수정과 같은 방식). 교정이 실패해도 원래 전사로 계속 진행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SttTermCorrector {

    static final int MAX_FROM_LENGTH = 20;
    /** to 앞뒤에 덧붙은 조사 등을 찾을 최대 글자 수 */
    private static final int MAX_OVERLAP = 3;

    private final LlmClient llmClient;

    public SttResult correct(SttResult stt, List<String> terms) {
        if (terms == null || terms.isEmpty() || stt.segments().isEmpty()) {
            return stt;
        }
        try {
            List<FeedbackRequest.Segment> segments = new ArrayList<>();
            for (int i = 0; i < stt.segments().size(); i++) {
                segments.add(new FeedbackRequest.Segment(segmentId(i), stt.segments().get(i).text()));
            }
            TermCorrectionResult result =
                    llmClient.requestTermCorrection(new TermCorrectionRequest(terms, segments));
            return apply(stt, terms, result);
        } catch (Exception e) {
            log.warn("[TermCorrection] 실패해서 원래 전사로 진행 cause={}", e.getMessage());
            return stt;
        }
    }

    static SttResult apply(SttResult stt, List<String> terms, TermCorrectionResult result) {
        if (result == null || result.corrections() == null || result.corrections().isEmpty()) {
            return stt;
        }
        List<String> texts = new ArrayList<>(stt.segments().stream().map(SttResult.Segment::text).toList());
        int applied = 0;
        for (TermCorrectionResult.Correction c : result.corrections()) {
            int index = indexOf(c == null ? null : c.segmentId(), texts.size());
            if (index < 0 || !isValid(c, texts.get(index), terms)) {
                log.info("[TermCorrection] 버림 {}", c);
                continue;
            }
            String text = texts.get(index);
            int at = text.indexOf(c.from());
            String before = text.substring(0, at);
            String after = text.substring(at + c.from().length());
            texts.set(index, before + trimOverlap(c.from(), c.to(), before, after) + after);
            log.info("[TermCorrection] {} \"{}\" → \"{}\"", c.segmentId(), c.from(), c.to());
            applied++;
        }
        if (applied == 0) {
            return stt;
        }
        List<SttResult.Segment> corrected = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            SttResult.Segment s = stt.segments().get(i);
            corrected.add(new SttResult.Segment(s.startMs(), s.endMs(), texts.get(i), s.pauses()));
        }
        return new SttResult(corrected);
    }

    private static boolean isValid(TermCorrectionResult.Correction c, String text, List<String> terms) {
        if (c.from() == null || c.to() == null || c.from().isBlank() || c.to().isBlank()) {
            return false;
        }
        if (c.from().equals(c.to()) || c.from().length() > MAX_FROM_LENGTH || !text.contains(c.from())) {
            return false;
        }
        String to = normalize(c.to());
        return terms.stream().map(SttTermCorrector::normalize).anyMatch(t -> !t.isEmpty() && to.contains(t));
    }

    /**
     * GPT가 from 바깥의 글자를 to에 덧붙인 경우 겹치는 부분을 뺍니다.
     * 예: "벡터디비에" 중 from="벡터디비", to="벡터 DB에" → 그대로 바꾸면 "벡터 DB에에"
     */
    static String trimOverlap(String from, String to, String before, String after) {
        String result = to;
        for (int k = Math.min(MAX_OVERLAP, after.length()); k >= 1; k--) {
            String head = after.substring(0, k);
            if (!head.isBlank() && result.endsWith(head) && !from.endsWith(head)
                    && result.length() > head.length()) {
                result = result.substring(0, result.length() - k);
                break;
            }
        }
        for (int k = Math.min(MAX_OVERLAP, before.length()); k >= 1; k--) {
            String tail = before.substring(before.length() - k);
            if (!tail.isBlank() && result.startsWith(tail) && !from.startsWith(tail)
                    && result.length() > tail.length()) {
                result = result.substring(k);
                break;
            }
        }
        return result;
    }

    /** 대소문자·띄어쓰기 차이는 같은 용어로 봄 ("벡터 DB" = "벡터DB") */
    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    }

    private static String segmentId(int index) {
        return String.format("seg_%02d", index + 1);
    }

    private static int indexOf(String segmentId, int size) {
        if (segmentId == null || !segmentId.matches("seg_\\d+")) {
            return -1;
        }
        int index = Integer.parseInt(segmentId.substring(4)) - 1;
        return index >= 0 && index < size ? index : -1;
    }
}

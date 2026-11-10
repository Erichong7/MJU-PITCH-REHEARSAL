package org.mju.mjupitchrehearsal.feedback.service;

import org.springframework.util.StringUtils;

/**
 * AI가 준 인용(quote)이 실제 전사에 있는지 확인합니다.
 * 띄어쓰기·줄바꿈 차이와 앞뒤 따옴표는 무시하고, 글자는 그대로 있어야 통과합니다.
 */
public final class QuoteMatcher {

    private QuoteMatcher() {
    }

    public static boolean contains(String transcript, String quote) {
        if (!StringUtils.hasText(transcript) || !StringUtils.hasText(quote)) {
            return false;
        }
        String q = normalize(stripQuotes(quote));
        return !q.isEmpty() && normalize(transcript).contains(q);
    }

    /** 공백을 모두 없애서 비교합니다 (Whisper·AI의 띄어쓰기 차이 흡수). */
    static String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }

    private static String stripQuotes(String text) {
        return text.strip().replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "");
    }
}

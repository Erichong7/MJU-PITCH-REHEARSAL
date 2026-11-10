package org.mju.mjupitchrehearsal.attempt.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mju.mjupitchrehearsal.attempt.entity.FastSegment;
import org.mju.mjupitchrehearsal.attempt.entity.LongPause;
import org.mju.mjupitchrehearsal.attempt.entity.TranscriptSegment;

/**
 * 말한 방식 수치를 계산합니다. AI 없이 전사와 타임스탬프만 사용합니다.
 *
 * <ul>
 *   <li>말 속도: 음절 수 ÷ 말한 시간(구간 시간의 합, 멈춤 제외)</li>
 *   <li>빨라진 구간: 구간 속도가 본인 평균의 {@value #FAST_RATIO}배 이상</li>
 *   <li>긴 멈춤: 앞 구간 끝 ~ 다음 구간 시작이 {@value #LONG_PAUSE_MS}ms 이상</li>
 * </ul>
 */
public final class SpeechMetricsCalculator {

    static final double FAST_RATIO = 1.25;
    static final long LONG_PAUSE_MS = 1000;
    /** 너무 짧은 구간은 속도가 튀어서 빨라진 구간 판단에서 뺍니다. */
    private static final long MIN_FAST_SEGMENT_MS = 2000;

    private static final Pattern LATIN_OR_DIGIT = Pattern.compile("[A-Za-z0-9]+");

    private SpeechMetricsCalculator() {
    }

    public static Result calculate(List<TranscriptSegment> segments) {
        long speakingMs = 0;
        int totalSyllables = 0;
        for (TranscriptSegment s : segments) {
            speakingMs += Math.max(0, s.getEndMs() - s.getStartMs());
            totalSyllables += countSyllables(s.getText());
        }
        double average = speakingMs == 0 ? 0.0 : round(totalSyllables / (speakingMs / 1000.0));

        List<FastSegment> fast = new ArrayList<>();
        List<LongPause> pauses = new ArrayList<>();
        TranscriptSegment prev = null;
        for (TranscriptSegment s : segments) {
            long ms = s.getEndMs() - s.getStartMs();
            if (average > 0 && ms >= MIN_FAST_SEGMENT_MS) {
                double speed = round(countSyllables(s.getText()) / (ms / 1000.0));
                if (speed >= average * FAST_RATIO) {
                    fast.add(new FastSegment(s.getSegmentKey(), s.getStartMs(), speed));
                }
            }
            if (prev != null) {
                long gap = s.getStartMs() - prev.getEndMs();
                if (gap >= LONG_PAUSE_MS) {
                    pauses.add(new LongPause(s.getSegmentKey(), prev.getEndMs(), gap));
                }
            }
            prev = s;
        }
        return new Result(average, fast, pauses);
    }

    /**
     * 한글은 글자 1개 = 1음절. 영어·숫자는 정확히 셀 수 없어서 글자 2개를 1음절로 어림합니다.
     */
    static int countSyllables(String text) {
        if (text == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '가' && c <= '힣') {
                count++;
            }
        }
        Matcher m = LATIN_OR_DIGIT.matcher(text);
        while (m.find()) {
            count += Math.max(1, (m.group().length() + 1) / 2);
        }
        return count;
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    public record Result(double syllablesPerSec, List<FastSegment> fastSegments, List<LongPause> longPauses) {
    }
}

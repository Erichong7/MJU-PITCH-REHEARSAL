package org.mju.mjupitchrehearsal.infra.stt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;

/**
 * 한 문장 안에서 멈춘 곳을 Whisper 단어 시간으로 찾습니다.
 *
 * <p>Whisper는 문장 중간의 침묵을 두 가지로 남깁니다.
 * <ul>
 *   <li>단어 사이 간격: {@code 보여줬다가 → (2.4초) → 학생들이}</li>
 *   <li>간격은 0이고 앞뒤 단어가 길게 늘어남: {@code 나면(1.24초) 알림도(1.82초)}</li>
 * </ul>
 * 그래서 단어 사이 간격과, 단어가 평소 속도보다 늘어난 시간(초과분)을 이어서 더합니다.
 * 평소 속도로 말한 단어가 나오면 거기서 끊고, 합이 {@value #MIN_PAUSE_MS}ms 이상이면 멈춤으로 봅니다.
 *
 * <p>문장 첫 단어와 끝 단어의 초과분은 세지 않습니다. 문장 앞뒤의 침묵(문장 사이 멈춤)이나
 * 문장 끝을 길게 끄는 말투가 섞이기 때문입니다. 문장 사이 멈춤은 구간 간격으로 따로 계산합니다.
 */
final class InnerPauseDetector {

    static final long MIN_PAUSE_MS = 1000;
    /** 본인 평균 음절 시간의 이 배수까지는 정상 발음으로 봅니다. */
    static final double STRETCH_RATIO = 2.0;

    private static final Pattern LATIN_OR_DIGIT = Pattern.compile("[A-Za-z0-9]+");

    private InnerPauseDetector() {
    }

    /** 녹음 전체 단어로 계산한 본인 평균 음절 시간(ms). 단어가 없으면 0 */
    static double msPerSyllable(List<SentenceSegmenter.Word> words) {
        long totalMs = 0;
        int syllables = 0;
        for (SentenceSegmenter.Word w : words) {
            totalMs += Math.max(0, w.endMs() - w.startMs());
            syllables += syllables(w.word());
        }
        return syllables == 0 ? 0 : (double) totalMs / syllables;
    }

    /**
     * @param words         한 문장의 단어들 (시간 순서)
     * @param msPerSyllable {@link #msPerSyllable}로 구한 본인 평균
     */
    static List<SttResult.Pause> detect(List<SentenceSegmenter.Word> words, double msPerSyllable) {
        List<SttResult.Pause> pauses = new ArrayList<>();
        if (words.size() < 2 || msPerSyllable <= 0) {
            return pauses;
        }

        Run run = new Run();
        int last = words.size() - 1;
        for (int i = 0; i <= last; i++) {
            SentenceSegmenter.Word w = words.get(i);
            if (i > 0 && i < last) {
                long excess = excessMs(w, msPerSyllable);
                if (excess > 0) {
                    run.add(w.endMs() - excess, excess);
                } else {
                    run.closeInto(pauses);
                }
            }
            if (i < last) {
                long gap = words.get(i + 1).startMs() - w.endMs();
                if (gap > 0) {
                    run.add(w.endMs(), gap);
                }
            }
        }
        run.closeInto(pauses);
        return pauses;
    }

    /** 단어 길이 중 평소 속도로 설명되지 않는 부분 */
    private static long excessMs(SentenceSegmenter.Word w, double msPerSyllable) {
        long duration = w.endMs() - w.startMs();
        long expected = Math.round(Math.max(1, syllables(w.word())) * msPerSyllable * STRETCH_RATIO);
        return Math.max(0, duration - expected);
    }

    /** 한글은 글자 1개 = 1음절, 영어·숫자는 글자 2개를 1음절로 어림 (SpeechMetricsCalculator와 같은 기준) */
    static int syllables(String text) {
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

    /** 이어지는 침묵 조각을 모으는 중인 멈춤 */
    private static final class Run {
        private long startMs = -1;
        private long totalMs;

        void add(long atMs, long ms) {
            if (startMs < 0) {
                startMs = atMs;
            }
            totalMs += ms;
        }

        void closeInto(List<SttResult.Pause> pauses) {
            if (startMs >= 0 && totalMs >= MIN_PAUSE_MS) {
                pauses.add(new SttResult.Pause(startMs, totalMs));
            }
            startMs = -1;
            totalMs = 0;
        }
    }
}

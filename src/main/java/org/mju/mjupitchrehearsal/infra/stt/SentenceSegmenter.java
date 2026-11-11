package org.mju.mjupitchrehearsal.infra.stt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mju.mjupitchrehearsal.infra.stt.dto.SttResult;

/**
 * Whisper 전체 글을 문장 단위 구간으로 나누고, 단어별 시간으로 문장의 시작·끝을 정합니다.
 *
 * <p>Whisper의 segment는 문장 중간, 심지어 단어 중간("돌아다 | 닙니다")에서 잘립니다.
 * 그러면 AI가 인용한 문장이 두 구간에 걸쳐 인용 검증에서 버려지고, 재연습 비교의 1차 발언도 반쪽이 됩니다.
 * 그래서 문장 부호(. ? !)로 문장을 나누고, 단어 타임스탬프(words)를 글자 순서대로 맞춰 시간을 붙입니다.
 *
 * <p>words에는 문장 부호가 없으므로 공백·문장 부호를 뺀 글자 수로 맞춥니다.
 * 글자가 맞지 않거나 한 문장이 30초를 넘으면 null을 돌려주고, 호출하는 쪽은 Whisper segment를 그대로 씁니다.
 */
final class SentenceSegmenter {

    /** 문장 끝: . ? ! … 뒤에 공백이나 글 끝 ("3.5배"의 점에서는 자르지 않음) */
    private static final Pattern SENTENCE = Pattern.compile("(?s).+?(?:[.?!…]+(?=\\s|$)|$)");

    /** 문장 부호가 거의 없어 한 문장이 이보다 길면 문장 단위를 포기합니다. */
    private static final long MAX_SENTENCE_MS = 30_000;

    private SentenceSegmenter() {
    }

    record Word(String word, long startMs, long endMs) {
    }

    static List<SttResult.Segment> split(String text, List<Word> words) {
        if (text == null || text.isBlank() || words == null || words.isEmpty()) {
            return null;
        }

        // 단어 글자 하나하나가 어느 단어에 속하는지 펼쳐 둠
        List<Integer> charToWord = new ArrayList<>();
        for (int w = 0; w < words.size(); w++) {
            int len = normalize(words.get(w).word()).length();
            for (int k = 0; k < len; k++) {
                charToWord.add(w);
            }
        }

        List<SttResult.Segment> sentences = new ArrayList<>();
        int cursor = 0;
        Matcher m = SENTENCE.matcher(text);
        while (m.find()) {
            String sentence = m.group().strip();
            int len = normalize(sentence).length();
            if (len == 0) {
                continue;
            }
            if (cursor + len > charToWord.size()) {
                return null;
            }
            Word first = words.get(charToWord.get(cursor));
            Word last = words.get(charToWord.get(cursor + len - 1));
            long endMs = Math.max(first.startMs(), last.endMs());
            if (endMs - first.startMs() > MAX_SENTENCE_MS) {
                return null;
            }
            sentences.add(new SttResult.Segment(first.startMs(), endMs, sentence));
            cursor += len;
        }
        return cursor == charToWord.size() && !sentences.isEmpty() ? sentences : null;
    }

    /** 공백과 문장 부호를 빼고 글자만 남김 */
    static String normalize(String s) {
        return s == null ? "" : s.replaceAll("[\\s\\p{Punct}…“”‘’·]", "");
    }
}

package org.mju.mjupitchrehearsal.attempt.support;

import java.nio.ByteBuffer;
import java.util.OptionalLong;

/**
 * m4a(MP4) 파일의 재생 길이를 읽습니다. 외부 프로그램(ffmpeg) 없이 파일 헤더만 봅니다.
 *
 * <p>구조: [ftyp] ... [moov [mvhd(timescale, duration)] ...] — moov는 파일 앞이나 끝 어디에나 올 수 있습니다.
 * m4a가 아니거나 헤더가 깨졌으면 빈 값을 돌려줍니다.
 */
public final class M4aDurationReader {

    private M4aDurationReader() {
    }

    public static OptionalLong readDurationMs(byte[] data) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(data);
            if (data.length < 8 || !"ftyp".equals(type(buf, 4))) {
                return OptionalLong.empty();
            }
            long[] moov = findBox(buf, 0, data.length, "moov");
            if (moov == null) {
                return OptionalLong.empty();
            }
            long[] mvhd = findBox(buf, moov[0], moov[1], "mvhd");
            if (mvhd == null) {
                return OptionalLong.empty();
            }
            int p = (int) mvhd[0];
            int version = buf.get(p) & 0xFF;
            long timescale;
            long duration;
            if (version == 1) {
                timescale = Integer.toUnsignedLong(buf.getInt(p + 4 + 16));
                duration = buf.getLong(p + 4 + 16 + 4);
            } else {
                timescale = Integer.toUnsignedLong(buf.getInt(p + 4 + 8));
                duration = Integer.toUnsignedLong(buf.getInt(p + 4 + 8 + 4));
            }
            if (timescale <= 0 || duration < 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(duration * 1000 / timescale);
        } catch (IndexOutOfBoundsException e) {
            return OptionalLong.empty();
        }
    }

    /** [start, end) 안에서 해당 타입 박스를 찾아 {내용 시작, 박스 끝}을 돌려줍니다. */
    private static long[] findBox(ByteBuffer buf, long start, long end, String target) {
        long pos = start;
        while (pos + 8 <= end) {
            long size = Integer.toUnsignedLong(buf.getInt((int) pos));
            String type = type(buf, (int) pos + 4);
            long header = 8;
            if (size == 1) {
                size = buf.getLong((int) pos + 8);
                header = 16;
            } else if (size == 0) {
                size = end - pos;
            }
            if (size < header || pos + size > end) {
                return null;
            }
            if (target.equals(type)) {
                return new long[]{pos + header, pos + size};
            }
            pos += size;
        }
        return null;
    }

    private static String type(ByteBuffer buf, int offset) {
        byte[] b = new byte[4];
        buf.get(offset, b);
        return new String(b, java.nio.charset.StandardCharsets.US_ASCII);
    }
}

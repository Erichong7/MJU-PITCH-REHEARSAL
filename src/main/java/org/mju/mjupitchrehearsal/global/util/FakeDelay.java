package org.mju.mjupitchrehearsal.global.util;

/**
 * 가짜 STT·AI가 실제처럼 시간이 걸리도록 기다립니다.
 */
public final class FakeDelay {

    private FakeDelay() {
    }

    public static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

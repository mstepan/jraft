package com.github.mstepan.jraft;

import java.util.concurrent.TimeUnit;

public final class ConcurrencyUtils {

    private ConcurrencyUtils() {
        throw new AssertionError("Can't instantiate utility class");
    }

    /**
     * @param seconds - seconds to sleep
     * @return true if thread was interrupted during sleep, otherwise false.
     */
    public static boolean sleepSec(long seconds) {
        return sleep(seconds, TimeUnit.SECONDS);
    }

    /**
     * @param milliseconds - milliseconds to sleep
     * @return true if thread was interrupted during sleep, otherwise false.
     */
    public static boolean sleepMs(long milliseconds) {
        return sleep(milliseconds, TimeUnit.MILLISECONDS);
    }

    private static boolean sleep(long delay, TimeUnit unit) {
        try {
            unit.sleep(delay);
        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
            return true;
        }

        return false;
    }
}

package com.github.mstepan.jraft.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class ConcurrencyUtils {

    private ConcurrencyUtils() {
        throw new AssertionError("Can't instantiate utility class");
    }

    /**
     * Similar to randomSleepInRange but don't throw InterruptedException
     *
     * @return true if thread sleep completed successfully, otherwise if thread was interrupted
     *     return false
     */
    public static boolean randomSleepInRangeNoException(long from, int to) {
        try {
            randomSleepInRange(from, to);
        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
            return false;
        }

        return true;
    }

    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    public static void randomSleepInRange(long from, long to) throws InterruptedException {
        if (from > to) {
            throw new IllegalArgumentException("from > to, from: %d, to: %d".formatted(from, to));
        }
        if (from < 0) {
            throw new IllegalArgumentException("from < 0, should be positive or zero");
        }

        if (from == to) {
            TimeUnit.MILLISECONDS.sleep(from);
        } else {
            TimeUnit.MILLISECONDS.sleep(from + ThreadLocalRandom.current().nextLong(to - from + 1));
        }
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

package com.github.mstepan.jraft;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VoteTask implements Runnable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // TODO: use below values after testing
    //    private static final long VOTE_MIN_DELAY_IN_MS = 150L;
    //    private static final long VOTE_MAX_DELAY_IN_MS = 300L;

    private static final long VOTE_MIN_DELAY_IN_MS = 1500L;
    private static final long VOTE_MAX_DELAY_IN_MS = 3000L;

    @Override
    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    public void run() {
        LOGGER.info("Voting thread started");

        ThreadLocalRandom random = ThreadLocalRandom.current();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 150–300 ms
                TimeUnit.MILLISECONDS.sleep(
                        VOTE_MIN_DELAY_IN_MS
                                + random.nextLong(VOTE_MAX_DELAY_IN_MS - VOTE_MIN_DELAY_IN_MS + 1));

                LOGGER.debug("Checking if leader still alive");

            } catch (InterruptedException interEx) {
                Thread.currentThread().interrupt();
            }
        }

        LOGGER.info("Voting thread gracefully stopped");
    }
}

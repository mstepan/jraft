package com.github.mstepan.jraft.vote;

import com.github.mstepan.jraft.ConcurrencyUtils;
import com.github.mstepan.jraft.state.LeaderInfo;
import com.github.mstepan.jraft.state.NodeGlobalState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoteTask implements Runnable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // TODO: use below values after testing
    //    private static final long VOTE_MIN_DELAY_IN_MS = 150L;
    //    private static final long VOTE_MAX_DELAY_IN_MS = 300L;

    public static final long VOTE_MIN_DELAY_IN_MS = 1_500L;
    public static final long VOTE_MAX_DELAY_IN_MS = 3_000L;

    @Override
    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    public void run() {
        // Initial delay to give some time for a cluster to start
        if (ConcurrencyUtils.sleepSec(10L)) {
            return;
        }

        LOGGER.info("Voting thread started");

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (NodeGlobalState.INST.isLeader()) {
                    randomSleep(random);
                    return;
                }

                long voteStartTime = System.nanoTime();

                // 150–300 ms
                randomSleep(random);

                long leaderLastTimestamp = LeaderInfo.INST.lastLeaderTimestamp();

                if (leaderLastTimestamp < voteStartTime) {
                    // nothing heard from leader so far, starting election process
                    LOGGER.debug("Starting election process");

                    NodeGlobalState.INST.startElection();
                } else {
                    LOGGER.debug("Leader is still ALIVE");
                }
            } catch (InterruptedException interEx) {
                Thread.currentThread().interrupt();
            }
        }

        LOGGER.info("Voting thread gracefully stopped");
    }

    private void randomSleep(ThreadLocalRandom random) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(
                VOTE_MIN_DELAY_IN_MS
                        + random.nextLong(VOTE_MAX_DELAY_IN_MS - VOTE_MIN_DELAY_IN_MS + 1));
    }
}

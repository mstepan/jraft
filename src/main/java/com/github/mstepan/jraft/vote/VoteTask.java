package com.github.mstepan.jraft.vote;

import com.github.mstepan.jraft.state.LeaderInfo;
import com.github.mstepan.jraft.state.NodeGlobalState;
import com.github.mstepan.jraft.util.ConcurrencyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadLocalRandom;
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

        while (true) {
            try {
                // sleep this thread until it's NOT A LEADER anymore
                NodeGlobalState.INST.waitTillLeader();

                long voteStartTime = System.nanoTime();

                // 150–300 ms
                ConcurrencyUtils.randomSleepInRange(
                        random, VOTE_MIN_DELAY_IN_MS, VOTE_MAX_DELAY_IN_MS);

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
                break;
            }
        }

        LOGGER.info("Voting thread gracefully stopped");
    }
}

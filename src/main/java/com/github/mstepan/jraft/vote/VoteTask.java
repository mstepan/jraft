package com.github.mstepan.jraft.vote;

import static com.github.mstepan.jraft.ServerCliCommand.CLUSTER_TOPOLOGY_CONTEXT;

import com.github.mstepan.jraft.state.LeaderInfo;
import com.github.mstepan.jraft.state.NodeGlobalState;
import com.github.mstepan.jraft.topology.ClusterTopology;
import com.github.mstepan.jraft.util.ConcurrencyUtils;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class VoteTask implements Callable<Void> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // election timeout should be in range: 150–300 ms
    public static final long VOTE_MIN_DELAY_IN_MS = 150L;
    public static final long VOTE_MAX_DELAY_IN_MS = 300L;

    @Override
    public Void call() {
        Thread.currentThread().setName("Vote");

        ClusterTopology cluster = CLUSTER_TOPOLOGY_CONTEXT.get();
        MDC.put("nodeId", cluster.curNodeId());

        // Initial delay to give some time for a cluster to boot
        if (!ConcurrencyUtils.randomSleepInRangeNoException(1000L, 3_000)) {
            LOGGER.debug("Vote tass was interrupted during initial sleep");
            return null;
        }

        LOGGER.info("Voting thread started");

        while (true) {
            try {
                // sleep this thread until it's NOT A LEADER anymore
                NodeGlobalState.INST.waitTillLeader();

                final long voteStartTime = System.nanoTime();

                // election timeout
                ConcurrencyUtils.randomSleepInRange(VOTE_MIN_DELAY_IN_MS, VOTE_MAX_DELAY_IN_MS);

                long leaderLastTimestamp = LeaderInfo.INST.lastLeaderTimestamp();

                if (leaderLastTimestamp < voteStartTime) {
                    // If a follower receives no communication over a period of time called the
                    // election timeout, then it assumes there is no viable leader and begins an
                    // election to choose a new leader.
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

        return null;
    }
}

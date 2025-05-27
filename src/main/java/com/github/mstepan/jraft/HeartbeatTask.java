package com.github.mstepan.jraft;

import com.github.mstepan.jraft.state.NodeGlobalState;
import com.github.mstepan.jraft.vote.VoteTask;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatTask implements Runnable {

    // should be something like VoteTask.VOTE_MIN_DELAY_IN_MS / 3
    private static final long LEADER_PING_DELAY_IN_MS = VoteTask.VOTE_MIN_DELAY_IN_MS / 3;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void run() {
        LOGGER.info("Heartbeat started");

        while (true) {
            try {
                if (NodeGlobalState.INST.isLeader()) {
                    LOGGER.debug("I'm LEADER, sending heartbeat to other nodes...");
                    // TODO: send EMPTY AppendEntry request from leader for each node

                    ConcurrencyUtils.sleepMs(LEADER_PING_DELAY_IN_MS);
                } else {
                    LOGGER.debug("Not leader, do nothing...");
                    NodeGlobalState.INST.waitTillNotLeader();
                }
            } catch (InterruptedException interEx) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        LOGGER.info("Heartbeat stopped");
    }
}

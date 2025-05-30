package com.github.mstepan.jraft.heartbeat;

import static com.github.mstepan.jraft.ServerCliCommand.CLUSTER_TOPOLOGY_CONTEXT;

import com.github.mstepan.jraft.grpc.Raft;
import com.github.mstepan.jraft.state.NodeGlobalState;
import com.github.mstepan.jraft.topology.ClusterTopology;
import com.github.mstepan.jraft.topology.HostPort;
import com.github.mstepan.jraft.topology.ManagedChannelsPool;
import com.github.mstepan.jraft.util.ConcurrencyUtils;
import com.github.mstepan.jraft.vote.VoteTask;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class HeartbeatTask implements Callable<Void> {

    // should be 2 or 3 times less than 'VoteTask.VOTE_MIN_DELAY_IN_MS' value
    private static final long LEADER_PING_DELAY_IN_MS = VoteTask.VOTE_MIN_DELAY_IN_MS / 3;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Leaders send periodic heartbeats (AppendEntries RPCs that carry no log entries) to all
     * followers in order to maintain their authority
     */
    @Override
    public Void call() {
        Thread.currentThread().setName("Heartbeat");

        ClusterTopology cluster = CLUSTER_TOPOLOGY_CONTEXT.get();
        MDC.put("nodeId", cluster.curNodeId());

        LOGGER.info("Heartbeat started");

        while (true) {
            try {
                if (NodeGlobalState.INST.isLeader()) {
                    // LOGGER.debug("I'm LEADER, sending heartbeat to other nodes...");

                    //  send EMPTY AppendEntry request from leader to each node
                    try (var scope = new StructuredTaskScope<Void>()) {

                        final long curNodeTerm = NodeGlobalState.INST.currentTerm();

                        for (HostPort singleNode : cluster.seedNodes()) {
                            scope.fork(
                                    () -> {
                                        var stub =
                                                ManagedChannelsPool.INST.newStubInstance(
                                                        singleNode);
                                        Raft.AppendEntryRequest request =
                                                Raft.AppendEntryRequest.newBuilder()
                                                        .setNodeTerm(curNodeTerm)
                                                        .build();

                                        Raft.AppendEntryResponse response =
                                                stub.appendEntry(request);

                                        // If RPC request or response contains term T > currentTerm:
                                        // set currentTerm = T, convert to follower (§5.1)
                                        NodeGlobalState.INST.updateTermIfHigher(
                                                response.getNodeTerm());

                                        return null;
                                    });
                        }

                        scope.join();
                    }

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
        return null;
    }
}

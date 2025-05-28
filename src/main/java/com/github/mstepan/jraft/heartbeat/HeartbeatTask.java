package com.github.mstepan.jraft.heartbeat;

import static com.github.mstepan.jraft.ServerCliCommand.CLUSTER_TOPOLOGY_CONTEXT;

import com.github.mstepan.jraft.grpc.Raft;
import com.github.mstepan.jraft.grpc.RaftServiceGrpc;
import com.github.mstepan.jraft.state.NodeGlobalState;
import com.github.mstepan.jraft.topology.ClusterTopology;
import com.github.mstepan.jraft.topology.HostPort;
import com.github.mstepan.jraft.util.ConcurrencyUtils;
import com.github.mstepan.jraft.util.ManagedChannelWrapper;
import com.github.mstepan.jraft.util.NetworkUtils;
import com.github.mstepan.jraft.vote.VoteTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

public class HeartbeatTask implements Callable<Void> {

    // should be something like VoteTask.VOTE_MIN_DELAY_IN_MS / 3
    private static final long LEADER_PING_DELAY_IN_MS = VoteTask.VOTE_MIN_DELAY_IN_MS / 3;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
                        for (HostPort singleNode : cluster.seedNodes()) {
                            scope.fork(
                                    () -> {
                                        try (ManagedChannelWrapper channelWrapper =
                                                NetworkUtils.newGrpcChannel(singleNode)) {
                                            RaftServiceGrpc.RaftServiceBlockingStub stub =
                                                    RaftServiceGrpc.newBlockingStub(
                                                            channelWrapper.channel());

                                            Raft.AppendEntryRequest request =
                                                    Raft.AppendEntryRequest.newBuilder().build();

                                            Raft.AppendEntryResponse response =
                                                    stub.appendEntry(request);

                                            // TODO: If RPC request or response contains term T >
                                            // currentTerm:
                                            // set currentTerm = T, convert to follower

                                            return null;
                                        }
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

package com.github.mstepan.jraft.state;

import com.github.mstepan.jraft.grpc.Raft.*;
import com.github.mstepan.jraft.grpc.RaftServiceGrpc;
import com.github.mstepan.jraft.topology.ClusterTopology;
import com.github.mstepan.jraft.topology.HostPort;
import com.github.mstepan.jraft.util.ManagedChannelWrapper;
import com.github.mstepan.jraft.util.NetworkUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("ME_ENUM_FIELD_SETTER")
public enum NodeGlobalState {
    INST;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private NodeRole role;

    // candidateId that received vote in current term (or null if none)
    private volatile String votedFor;

    // latest term server has seen (initialized to 0 on first boot, increases monotonically)
    private final AtomicLong currentTerm = new AtomicLong(0L);

    private final AtomicLong logEntryIdx = new AtomicLong(0L);

    /*
    1. Converts to a Candidate
    2. Increments its current term
    3. Votes for itself
    4. Resets its election timeout
    5. Sends RequestVote RPCs to all other nodes
     */
    public synchronized void startElection() {

        // Node becomes a Candidate
        setRole(NodeRole.CANDIDATE);

        // Increments its current term.
        long newCurTerm = currentTerm.incrementAndGet();

        // Votes for itself.
        votedFor = ClusterTopology.INST.curNodeId();

        List<StructuredTaskScope.Subtask<VoteResponse>> allRequests = new ArrayList<>();

        try (var scope = new StructuredTaskScope<VoteResponse>()) {
            for (HostPort singleNode : ClusterTopology.INST.seedNodes()) {
                var subtask =
                        scope.fork(
                                () -> {
                                    try (ManagedChannelWrapper channelWrapper =
                                            NetworkUtils.newGrpcChannel(singleNode)) {

                                        RaftServiceGrpc.RaftServiceBlockingStub stub =
                                                RaftServiceGrpc.newBlockingStub(
                                                        channelWrapper.channel());

                                        VoteRequest request =
                                                VoteRequest.newBuilder()
                                                        .setCandidateId(
                                                                ClusterTopology.INST.curNodeId())
                                                        .setCandidateTerm(newCurTerm)
                                                        .setLogEntryIdx(logEntryIdx.get())
                                                        .build();

                                        VoteResponse response = stub.vote(request);

                                        LOGGER.info("Vote response: {}", response.getResult());

                                        return response;
                                    }
                                });

                allRequests.add(subtask);
            }

            scope.join();

            // initialised to 1, assuming we have voted for ourselves
            int grantedVotesCnt = 1;

            for (StructuredTaskScope.Subtask<VoteResponse> singleSubtask : allRequests) {

                if (singleSubtask.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                    VoteResponse singleVoteResponse = singleSubtask.get();
                    if (singleVoteResponse.getResult() == VoteResult.GRANTED) {
                        ++grantedVotesCnt;
                    }

                    if (grantedVotesCnt >= ClusterTopology.INST.clusterSize()) {
                        LOGGER.info("Vote majority reached");
                        // If the Candidate receives votes from a majority of nodes, it becomes the
                        // Leader.
                        markAsLeader();
                        break;
                    }
                }
            }

        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized boolean isLeader() {
        return role == NodeRole.LEADER;
    }

    public long currentTerm() {
        return currentTerm.get();
    }

    public String votedFor() {
        return votedFor;
    }

    public long logEntryIndex() {
        return logEntryIdx.get();
    }

    public synchronized void setRoleIfDifferent(NodeRole newRole) {
        if (role != newRole) {
            setRole(newRole);
        }
    }

    public synchronized void setRole(NodeRole newRole) {
        NodeRole prevRole = this.role;
        this.role = newRole;
        LOGGER.debug("Role changed: {} -> {}", prevRole, newRole);
        notifyAll();
    }

    public synchronized void markAsLeader() {
        setRole(NodeRole.LEADER);
    }

    public synchronized void waitTillNotLeader() throws InterruptedException {
        while (role != NodeRole.LEADER) {
            wait();
        }
    }

    public synchronized void waitTillLeader() throws InterruptedException {
        while (role == NodeRole.LEADER) {
            wait();
        }
    }
}

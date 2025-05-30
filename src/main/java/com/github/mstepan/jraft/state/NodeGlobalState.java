package com.github.mstepan.jraft.state;

import static com.github.mstepan.jraft.ServerCliCommand.CLUSTER_TOPOLOGY_CONTEXT;

import com.github.mstepan.jraft.grpc.Raft.*;
import com.github.mstepan.jraft.topology.ClusterTopology;
import com.github.mstepan.jraft.topology.HostPort;
import com.github.mstepan.jraft.topology.ManagedChannelsPool;
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

    /**
     * To begin an election, a follower increments its current term and transitions to candidate
     * state. It then votes for itself and issues RequestVote RPCs in parallel to each of the other
     * servers in the cluster. A candidate continues in this state until one of three things
     * happens:
     *
     * <p>(a) it wins the election,
     *
     * <p>(b) another server establishes itself as leader, or
     *
     * <p>(c) a period of time goes by with no winner.
     */
    public synchronized void startElection() {

        ClusterTopology cluster = CLUSTER_TOPOLOGY_CONTEXT.get();

        // 1. increments its current term
        long newCurTerm = currentTerm.incrementAndGet();

        // 2. transitions to candidate state
        setRole(NodeRole.CANDIDATE);

        // 3. votes for itself
        votedFor = cluster.curNodeId();

        // 4. issues RequestVote RPCs in parallel to each of the other servers in the cluster
        List<StructuredTaskScope.Subtask<VoteResponse>> allRequests = new ArrayList<>();

        try (var scope = new StructuredTaskScope<VoteResponse>()) {
            for (HostPort singleNode : cluster.seedNodes()) {
                var subtask =
                        scope.fork(
                                () -> {
                                    var stub = ManagedChannelsPool.INST.newStubInstance(singleNode);

                                    VoteRequest request =
                                            VoteRequest.newBuilder()
                                                    .setCandidateId(cluster.curNodeId())
                                                    .setCandidateTerm(newCurTerm)
                                                    .setLogEntryIdx(logEntryIdx.get())
                                                    .build();

                                    VoteResponse response = stub.vote(request);
                                    LOGGER.debug("Vote response: {}", response.getResult());

                                    return response;
                                });

                allRequests.add(subtask);
            }

            scope.join();

            // initialised to 1, assuming we have voted for ourselves
            int grantedVotesCnt = 1;

            for (StructuredTaskScope.Subtask<VoteResponse> singleSubtask : allRequests) {
                if (singleSubtask.state() == StructuredTaskScope.Subtask.State.SUCCESS) {

                    VoteResponse response = singleSubtask.get();

                    // If RPC request or response contains term T > currentTerm:
                    // set currentTerm = T, convert to follower (§5.1)

                    if (response.getNodeTerm() > NodeGlobalState.INST.currentTerm()) {
                        NodeGlobalState.INST.setCurrentTerm(response.getNodeTerm());
                        NodeGlobalState.INST.setRole(NodeRole.FOLLOWER);
                        break;
                    }

                    if (response.getResult() == VoteResult.GRANTED) {
                        ++grantedVotesCnt;
                    }

                    if (grantedVotesCnt >= cluster.clusterSize()) {
                        LOGGER.debug("Vote majority reached");
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

    public void setCurrentTerm(long newTerm) {
        currentTerm.set(newTerm);
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
        if (prevRole != newRole) {
            LOGGER.info("Role changed: {} -> {}", prevRole, newRole);
        }
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

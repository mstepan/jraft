package com.github.mstepan.jraft.state;

import static com.github.mstepan.jraft.ServerCliCommand.CLUSTER_TOPOLOGY_CONTEXT;

import com.github.mstepan.jraft.grpc.Raft.*;
import com.github.mstepan.jraft.topology.ClusterTopology;
import com.github.mstepan.jraft.topology.HostPort;
import com.github.mstepan.jraft.topology.ManagedChannelsPool;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NodeGlobalState {

    private NodeGlobalState() {}

    public static final NodeGlobalState INST = new NodeGlobalState();

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private NodeRole role;

    // candidateId that received vote in current term (or null if none)
    private volatile String votedFor;

    // latest term server has seen (initialized to 0 on first boot, increases monotonically)
    private long currentTerm;

    private long logEntryIdx;

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
    public void startElection() {

        ClusterTopology cluster = CLUSTER_TOPOLOGY_CONTEXT.get();

        long curTermSnapshot;
        long logEntryIdxSnapshot;

        synchronized (this) {
            // 1. increments its current term
            ++currentTerm;
            curTermSnapshot = currentTerm;
            logEntryIdxSnapshot = logEntryIdx;

            // 2. transitions to candidate state
            setRole(NodeRole.CANDIDATE);

            // 3. votes for itself
            votedFor = cluster.curNodeId();
        }

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
                                                    .setCandidateTerm(curTermSnapshot)
                                                    .setLogEntryIdx(logEntryIdxSnapshot)
                                                    .build();

                                    VoteResponse response = stub.vote(request);

                                    NodeGlobalState.INST.updateTermIfHigher(response.getNodeTerm());

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

                    if (response.getResult() == VoteResult.GRANTED) {
                        ++grantedVotesCnt;
                    }

                    if (grantedVotesCnt >= cluster.majorityCount()
                            && NodeGlobalState.INST.isCandidate()) {

                        LOGGER.debug("Vote majority reached, term {}", curTermSnapshot);

                        // If the Candidate receives votes from a majority of nodes,
                        // it becomes the Leader.
                        markAsLeader();
                        break;
                    }
                }
            }

        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized boolean isCandidate() {
        return role == NodeRole.CANDIDATE;
    }

    /*
    1. Reply false if term < currentTerm (§5.1)
    2. If votedFor is null or candidateId, and candidate’s log is at
    least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     */
    public synchronized boolean checkVoteGranted(
            String candidateId, long candidateTerm, long candidateLogEntryIdx) {

        if (candidateTerm < currentTerm) {
            return false;
        }

        if (candidateTerm > currentTerm) {
            currentTerm = candidateTerm;
            votedFor = null;
            setRole(NodeRole.FOLLOWER);
        }

        if ((votedFor == null || votedFor.equals(candidateId))
                && candidateLogEntryIdx >= NodeGlobalState.INST.logEntryIndex()) {

            votedFor = candidateId;
            return true;
        }

        return false;
    }

    public synchronized boolean isLeader() {
        return role == NodeRole.LEADER;
    }

    public synchronized long currentTerm() {
        return currentTerm;
    }

    /**
     * If RPC request or response contains term T > currentTerm: set currentTerm = T, convert to
     * follower (§5.1)
     *
     * @param otherNodeTerm - the term value received from another cluster node
     * @return true if the new term is greater than the current node's term; otherwise, false.
     */
    public synchronized boolean updateTermIfHigher(long otherNodeTerm) {
        if (otherNodeTerm > currentTerm) {
            currentTerm = otherNodeTerm;
            setRole(NodeRole.FOLLOWER);
            return true;
        }

        return false;
    }

    public synchronized long logEntryIndex() {
        return logEntryIdx;
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

    public synchronized void printState() {
        LOGGER.info("role: {}, currentTerm: {}, votedFor: {}", role, currentTerm, votedFor);
    }
}

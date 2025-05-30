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

    /*
    1. Converts to a Candidate
    2. Increments its current term
    3. Votes for itself
    4. Resets its election timeout
    5. Sends RequestVote RPCs to all other nodes
     */
    public synchronized void startElection() {

        ClusterTopology cluster = CLUSTER_TOPOLOGY_CONTEXT.get();

        // Node becomes a Candidate
        setRole(NodeRole.CANDIDATE);

        // Increments its current term.
        long newCurTerm = currentTerm.incrementAndGet();

        // Votes for itself.
        votedFor = cluster.curNodeId();

        List<StructuredTaskScope.Subtask<VoteResult>> allRequests = new ArrayList<>();

        try (var scope = new StructuredTaskScope<VoteResult>()) {
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

                                    return response.getResult();
                                });

                allRequests.add(subtask);
            }

            scope.join();

            // initialised to 1, assuming we have voted for ourselves
            int grantedVotesCnt = 1;

            for (StructuredTaskScope.Subtask<VoteResult> singleSubtask : allRequests) {

                if (singleSubtask.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                    if (singleSubtask.get() == VoteResult.GRANTED) {
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

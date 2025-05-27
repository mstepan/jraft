package com.github.mstepan.jraft.state;

import com.github.mstepan.jraft.grpc.Raft.*;
import com.github.mstepan.jraft.grpc.VoteServiceGrpc;
import com.github.mstepan.jraft.topology.ClusterTopology;
import com.github.mstepan.jraft.topology.HostPort;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("ME_ENUM_FIELD_SETTER")
public enum NodeGlobalState {
    INST;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private NodeRole role;

    private Optional<String> votedFor;

    private final AtomicLong currentTerm = new AtomicLong(0L);

    private final AtomicLong logEntryIdx = new AtomicLong(0L);

    public synchronized void setRole(NodeRole newRole) {
        NodeRole prevRole = this.role;
        this.role = newRole;
        LOGGER.debug("Role changed: {} -> {}", prevRole, newRole);
    }

    public synchronized void startElection() {
        // Once a node becomes a Candidate, it:
        setRole(NodeRole.CANDIDATE);

        // Increments its current term.
        long newCurTerm = currentTerm.incrementAndGet();

        // Votes for itself.
        votedFor = Optional.of(ClusterTopology.INST.curNodeId());

        for (HostPort singleNode : ClusterTopology.INST.seedNodes()) {

            // TODO: below code failed
            ManagedChannel channel =
                    ManagedChannelBuilder.forAddress(singleNode.host(), singleNode.port())
                            .usePlaintext() // Required for plaintext (non-SSL) connections
                            .build();

            VoteServiceGrpc.VoteServiceBlockingStub stub = VoteServiceGrpc.newBlockingStub(channel);

            RequestVote request =
                    RequestVote.newBuilder()
                            .setCandidateId(ClusterTopology.INST.curNodeId())
                            .setCandidateTerm(newCurTerm)
                            .setLogEntryIdx(logEntryIdx.get())
                            .build();

            VoteResponse response = stub.vote(request);

            LOGGER.info("Vote response: {}", response.getResult());

            channel.shutdown();
        }
    }
}

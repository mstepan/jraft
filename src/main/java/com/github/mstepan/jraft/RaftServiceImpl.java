package com.github.mstepan.jraft;

import com.github.mstepan.jraft.grpc.Raft;
import com.github.mstepan.jraft.grpc.RaftServiceGrpc;
import com.github.mstepan.jraft.state.LeaderInfo;
import com.github.mstepan.jraft.state.NodeGlobalState;
import com.github.mstepan.jraft.topology.ClusterTopology;
import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftServiceImpl extends RaftServiceGrpc.RaftServiceImplBase {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void appendEntry(
            Raft.AppendEntryRequest request,
            StreamObserver<Raft.AppendEntryResponse> responseObserver) {

        // append entry from leader received
        LeaderInfo.INST.recordMessageFromLeader();

        responseObserver.onNext(Raft.AppendEntryResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    /*
    1. Reply false if term < currentTerm (§5.1)
    2. If votedFor is null or candidateId, and candidate’s log is at
    least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     */
    @Override
    public void vote(Raft.VoteRequest request, StreamObserver<Raft.VoteResponse> responseObserver) {
        LOGGER.debug("Vote request: {}", request);

        long curTerm = NodeGlobalState.INST.currentTerm();

        if (request.getCandidateTerm() < curTerm) {
            reply(responseObserver, Raft.VoteResult.REJECTED);
            return;
        }

        String curNodeId = ClusterTopology.INST.curNodeId();
        String votedFor = NodeGlobalState.INST.votedFor();

        if ((votedFor == null || votedFor.equals(curNodeId))
                && request.getLogEntryIdx() >= NodeGlobalState.INST.logEntryIndex()) {
            reply(responseObserver, Raft.VoteResult.GRANTED);
            return;
        }

        throw new IllegalStateException("Undefined behaviour for 'vote'");
    }

    private static void reply(
            StreamObserver<Raft.VoteResponse> responseObserver, Raft.VoteResult status) {
        Raft.VoteResponse response = Raft.VoteResponse.newBuilder().setResult(status).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

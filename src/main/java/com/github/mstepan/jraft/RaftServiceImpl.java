package com.github.mstepan.jraft;

import com.github.mstepan.jraft.grpc.Raft;
import com.github.mstepan.jraft.grpc.RaftServiceGrpc;
import com.github.mstepan.jraft.state.LeaderInfo;
import com.github.mstepan.jraft.state.NodeGlobalState;
import io.grpc.stub.StreamObserver;

public final class RaftServiceImpl extends RaftServiceGrpc.RaftServiceImplBase {

    @Override
    public void appendEntry(
            Raft.AppendEntryRequest request,
            StreamObserver<Raft.AppendEntryResponse> responseObserver) {

        boolean hasHigherTerm = NodeGlobalState.INST.updateTermIfHigher(request.getNodeTerm());

        if (!hasHigherTerm) {
            // append entry from leader received
            LeaderInfo.INST.recordMessageFromLeader();
        }

        responseObserver.onNext(
                Raft.AppendEntryResponse.newBuilder()
                        .setNodeTerm(NodeGlobalState.INST.currentTerm())
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void vote(Raft.VoteRequest request, StreamObserver<Raft.VoteResponse> responseObserver) {
        if (NodeGlobalState.INST.checkVoteGranted(
                request.getCandidateId(), request.getCandidateTerm(), request.getLogEntryIdx())) {
            reply(responseObserver, Raft.VoteResult.GRANTED, NodeGlobalState.INST.currentTerm());
        } else {
            reply(responseObserver, Raft.VoteResult.REJECTED, NodeGlobalState.INST.currentTerm());
        }
    }

    private static void reply(
            StreamObserver<Raft.VoteResponse> responseObserver,
            Raft.VoteResult status,
            long curNodeTerm) {
        Raft.VoteResponse response =
                Raft.VoteResponse.newBuilder().setResult(status).setNodeTerm(curNodeTerm).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

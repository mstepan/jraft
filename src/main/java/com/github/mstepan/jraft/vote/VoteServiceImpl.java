package com.github.mstepan.jraft.vote;

import com.github.mstepan.jraft.grpc.Raft;
import com.github.mstepan.jraft.grpc.VoteServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoteServiceImpl extends VoteServiceGrpc.VoteServiceImplBase {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void vote(Raft.RequestVote request, StreamObserver<Raft.VoteResponse> responseObserver) {
        LOGGER.debug("Vote request: {}", request);

        Raft.VoteResponse response =
                Raft.VoteResponse.newBuilder().setResult(Raft.VoteResult.REJECTED).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

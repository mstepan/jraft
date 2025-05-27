package com.github.mstepan.jraft;

import com.github.mstepan.jraft.grpc.Raft;
import com.github.mstepan.jraft.grpc.RaftServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMain {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9091;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(SERVER_HOST, SERVER_PORT)
                        .usePlaintext() // Required for plaintext (non-SSL) connections
                        .build();

        RaftServiceGrpc.RaftServiceBlockingStub stub = RaftServiceGrpc.newBlockingStub(channel);

        Raft.VoteRequest request =
                Raft.VoteRequest.newBuilder()
                        .setCandidateId(UUID.randomUUID().toString())
                        .setCandidateTerm(123L)
                        .setLogEntryIdx(456L)
                        .build();

        Raft.VoteResponse response = stub.vote(request);

        LOGGER.info("Vote response: {}", response.getResult());

        channel.shutdown();
    }
}

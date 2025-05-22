package com.github.mstepan.jraft;

import com.github.mstepan.jraft.grpc.GreetingServiceGrpc;
import com.github.mstepan.jraft.grpc.Hello.HelloReply;
import com.github.mstepan.jraft.grpc.Hello.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMain {

    private static final int SERVER_DEFAULT_PORT = 9091;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress("localhost", SERVER_DEFAULT_PORT)
                        .usePlaintext() // Required for plaintext (non-SSL) connections
                        .build();

        GreetingServiceGrpc.GreetingServiceBlockingStub stub =
                GreetingServiceGrpc.newBlockingStub(channel);

        HelloRequest request = HelloRequest.newBuilder().setName("Maksym").build();
        HelloReply response = stub.sayHello(request);

        LOGGER.info("Received from server: {}", response.getMessage());

        channel.shutdown();
    }
}

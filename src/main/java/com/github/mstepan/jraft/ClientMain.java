package com.github.mstepan.jraft;

import com.github.mstepan.jraft.grpc.GreetingServiceGrpc;
import com.github.mstepan.jraft.grpc.Hello.HelloReply;
import com.github.mstepan.jraft.grpc.Hello.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientMain {

    public static void main(String[] args) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress("localhost", ServerMain.PORT)
                        .usePlaintext() // Required for plaintext (non-SSL) connections
                        .build();

        GreetingServiceGrpc.GreetingServiceBlockingStub stub =
                GreetingServiceGrpc.newBlockingStub(channel);

        HelloRequest request = HelloRequest.newBuilder().setName("Maksym").build();
        HelloReply response = stub.sayHello(request);

        System.out.println("Received from server: " + response.getMessage());

        channel.shutdown();
    }
}

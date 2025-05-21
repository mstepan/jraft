package com.github.mstepan.jraft;

import com.github.mstepan.jraft.grpc.GreetingServiceGrpc;
import com.github.mstepan.jraft.grpc.Hello.HelloReply;
import com.github.mstepan.jraft.grpc.Hello.HelloRequest;
import com.github.mstepan.jraft.state.NodeRole;
import com.github.mstepan.jraft.state.NodeState;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class ServerMain {

    static final String HOST = "localhost";
    static final int PORT = 9090;

    public static void main(String[] args) throws IOException, InterruptedException {
        NodeState nodeState = new NodeState();
        nodeState.setRole(NodeRole.FOLLOWER);

        Server server = ServerBuilder.forPort(PORT).addService(new GreeterServiceImpl()).build();
        server.start();
        System.out.printf("gRPC server started at: %s:%d%n", HOST, PORT);

        Thread votingThread = Thread.ofVirtual().start(new VoteTask());

        server.awaitTermination();
        System.out.println("gRPC server gracefully stopped");

        votingThread.interrupt();
    }

    static class GreeterServiceImpl extends GreetingServiceGrpc.GreetingServiceImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {

            String name = request.getName();

            HelloReply reply =
                    HelloReply.newBuilder().setMessage("Hello from server, " + name).build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}

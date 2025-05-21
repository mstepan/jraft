package com.github.mstepan.jraft;

import com.github.mstepan.jraft.grpc.GreeterGrpc;
import com.github.mstepan.jraft.grpc.Hello.HelloReply;
import com.github.mstepan.jraft.grpc.Hello.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class ServerMain {

    static final int PORT = 9090;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(PORT).addService(new GreeterImpl()).build();

        System.out.printf("Starting gRPC server on port %d...%n", PORT);

        server.start();
        server.awaitTermination();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
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

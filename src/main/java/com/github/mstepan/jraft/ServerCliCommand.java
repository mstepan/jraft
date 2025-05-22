package com.github.mstepan.jraft;

import com.github.mstepan.jraft.grpc.GreetingServiceGrpc;
import com.github.mstepan.jraft.grpc.Hello;
import com.github.mstepan.jraft.state.LeaderInfo;
import com.github.mstepan.jraft.state.NodeRole;
import com.github.mstepan.jraft.state.NodeState;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "jraft",
        mixinStandardHelpOptions = true,
        version = "jraft 0.0.1",
        description = "Raft implementation in java.")
class ServerCliCommand implements Callable<Integer> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @CommandLine.Option(
            names = {"-h", "--host"},
            defaultValue = "localhost",
            description = "Server hostname (default: localhost)")
    private String host;

    @CommandLine.Option(
            names = {"-p", "--port"},
            defaultValue = "9091",
            description = "Server port (default: 9091)")
    private int port;

    @CommandLine.Option(names = "--seed", description = "Seed nodes")
    private List<String> seeds;

    @Override
    public Integer call() {
        try {
            LOGGER.info("Seed nodes: {}", seeds);

            NodeState nodeState = new NodeState();
            nodeState.setRole(NodeRole.FOLLOWER);

            Server server =
                    ServerBuilder.forPort(port)
                            .addService(new ServerCliCommand.GreeterServiceImpl())
                            .build();
            server.start();
            LOGGER.info("gRPC server started at: {}:{}", host, port);

            Thread votingThread = Thread.ofVirtual().start(new VoteTask());

            server.awaitTermination();
            LOGGER.info("gRPC server gracefully stopped");

            votingThread.interrupt();

            return 0;
        } catch (Exception ex) {
            LOGGER.error("Server failed to start", ex);
            return -1;
        }
    }

    static class GreeterServiceImpl extends GreetingServiceGrpc.GreetingServiceImplBase {
        @Override
        public void sayHello(
                Hello.HelloRequest request, StreamObserver<Hello.HelloReply> responseObserver) {

            LeaderInfo.INST.recordMessageFromLeader();

            String name = request.getName();

            Hello.HelloReply reply =
                    Hello.HelloReply.newBuilder().setMessage("Hello from server, " + name).build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}

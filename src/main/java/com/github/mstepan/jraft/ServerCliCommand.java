package com.github.mstepan.jraft;

import com.github.mstepan.jraft.heartbeat.HeartbeatTask;
import com.github.mstepan.jraft.state.NodeGlobalState;
import com.github.mstepan.jraft.state.NodeRole;
import com.github.mstepan.jraft.topology.ClusterTopology;
import com.github.mstepan.jraft.topology.ManagedChannelsPool;
import com.github.mstepan.jraft.util.MDCInterceptor;
import com.github.mstepan.jraft.util.ScopedValueInterceptor;
import com.github.mstepan.jraft.vote.VoteTask;
import io.grpc.*;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "jraft",
        mixinStandardHelpOptions = true,
        version = "jraft 0.0.1",
        description = "Raft implementation in java.")
public final class ServerCliCommand implements Callable<Integer> {

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

    @CommandLine.Option(
            names = {"-n", "--name"},
            description = "Host node name")
    private String name;

    public static final ScopedValue<ClusterTopology> CLUSTER_TOPOLOGY_CONTEXT =
            ScopedValue.newInstance();

    @Override
    public Integer call() {
        try {
            ClusterTopology clusterTopology = new ClusterTopology(name, host, port, seeds);
            ScopedValue.where(CLUSTER_TOPOLOGY_CONTEXT, clusterTopology).run(new InitServer());
            return 0;
        } catch (Exception ex) {
            return -1;
        } finally {
            ManagedChannelsPool.INST.close();
        }
    }

    private static final class InitServer implements Runnable {

        @Override
        public void run() {
            try {
                ClusterTopology cluster = CLUSTER_TOPOLOGY_CONTEXT.get();

                MDC.put("nodeId", cluster.curNodeId());

                NodeGlobalState.INST.setRole(NodeRole.FOLLOWER);

                Server server =
                        ServerBuilder.forPort(cluster.port())
                                .addService(
                                        ServerInterceptors.intercept(
                                                new RaftServiceImpl(),
                                                new MDCInterceptor(cluster),
                                                new ScopedValueInterceptor(cluster)))
                                .build();

                server.start();
                LOGGER.info("gRPC server started at: {}:{}", cluster.host(), cluster.port());

                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    scope.fork(new VoteTask());
                    scope.fork(new HeartbeatTask());

                    server.awaitTermination();
                    LOGGER.info("gRPC server gracefully stopped");

                    scope.join();
                    scope.throwIfFailed();
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Initialisation phase failed", ex);
            } finally {
                MDC.clear();
            }
        }
    }
}

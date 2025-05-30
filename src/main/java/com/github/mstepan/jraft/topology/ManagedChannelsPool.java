package com.github.mstepan.jraft.topology;

import com.github.mstepan.jraft.grpc.RaftServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public enum ManagedChannelsPool implements AutoCloseable {
    INST;

    private final ConcurrentMap<HostPort, ManagedChannel> channelsCache = new ConcurrentHashMap<>();

    public RaftServiceGrpc.RaftServiceBlockingStub newStubInstance(HostPort hostPort) {

        var cachedChannel = channelsCache.get(hostPort);

        if (cachedChannel != null) {
            return RaftServiceGrpc.newBlockingStub(cachedChannel);
        }

        // Use a synchronized block to prevent multiple stubs from being created at the same time
        synchronized (ManagedChannelsPool.class) {
            var managedChannel =
                    ManagedChannelBuilder.forAddress(hostPort.host(), hostPort.port())
                            .usePlaintext() // Required for plaintext (non-SSL) connections
                            .build();

            channelsCache.put(hostPort, managedChannel);

            return RaftServiceGrpc.newBlockingStub(managedChannel);
        }
    }

    @Override
    public void close() {
        for (ManagedChannel channel : channelsCache.values()) {
            channel.shutdown();
        }
    }
}

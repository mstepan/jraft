package com.github.mstepan.jraft.topology;

import com.github.mstepan.jraft.grpc.RaftServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ManagedChannelsPool implements AutoCloseable {

    public static final ManagedChannelsPool INST = new ManagedChannelsPool();

    private final ConcurrentMap<HostPort, ManagedChannel> channelsCache = new ConcurrentHashMap<>();

    private ManagedChannelsPool() {}

    public RaftServiceGrpc.RaftServiceBlockingStub newStubInstance(HostPort hostPort) {

        var cachedChannel = channelsCache.get(hostPort);

        if (cachedChannel != null) {
            return RaftServiceGrpc.newBlockingStub(cachedChannel);
        }

        ManagedChannel channelToUse =
                channelsCache.compute(
                        hostPort,
                        (keyNotUsed, managedChannelValue) -> {
                            if (managedChannelValue == null) {
                                return ManagedChannelBuilder.forAddress(
                                                hostPort.host(), hostPort.port())
                                        .usePlaintext() // Required for plaintext (non-SSL)
                                        // connections
                                        .build();
                            }
                            return managedChannelValue;
                        });

        return RaftServiceGrpc.newBlockingStub(channelToUse);
    }

    @Override
    public void close() {
        for (ManagedChannel channel : channelsCache.values()) {
            channel.shutdown();
        }
    }
}

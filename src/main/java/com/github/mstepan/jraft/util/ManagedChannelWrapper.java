package com.github.mstepan.jraft.util;

import io.grpc.ManagedChannel;
import java.util.Objects;

public final class ManagedChannelWrapper implements AutoCloseable {

    private final ManagedChannel channel;

    public ManagedChannelWrapper(ManagedChannel channel) {
        this.channel = Objects.requireNonNull(channel);
    }

    @Override
    public void close() throws Exception {
        channel.shutdown();
    }

    public ManagedChannel channel() {
        return channel;
    }
}

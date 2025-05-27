package com.github.mstepan.jraft.util;

import com.github.mstepan.jraft.topology.HostPort;
import io.grpc.ManagedChannelBuilder;

public final class NetworkUtils {

    private NetworkUtils() {
        throw new AssertionError("Can't instantiate utility class");
    }

    public static ManagedChannelWrapper newGrpcChannel(HostPort hostPort) {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(hostPort.host(), hostPort.port())
                        .usePlaintext() // Required for plaintext (non-SSL) connections
                        .build());
    }
}

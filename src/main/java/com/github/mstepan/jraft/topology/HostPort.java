package com.github.mstepan.jraft.topology;

public record HostPort(String host, int port) {

    public String uniqueId() {
        return "%s:%d".formatted(host, port);
    }
}

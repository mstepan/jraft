package com.github.mstepan.jraft.topology;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClusterTopology {

    private static final int MIN_PORT_VALUE = 0;
    private static final int MAX_PORT_VALUE = 65535;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String curNodeId;
    private final String host;
    private final int port;
    private final List<HostPort> seedNodes;

    public ClusterTopology(String curNodeId, String host, int port, List<String> seeds) {
        if (host == null) {
            throw new IllegalArgumentException("'host' is null");
        }
        if (seeds == null) {
            throw new IllegalArgumentException("'seeds' is null");
        }
        if (port < MIN_PORT_VALUE || port > MAX_PORT_VALUE) {
            throw new IllegalArgumentException(
                    "'port' is out of range, should be between %d and %d"
                            .formatted(MIN_PORT_VALUE, MAX_PORT_VALUE));
        }

        if (curNodeId == null) {
            this.curNodeId = UUID.randomUUID().toString();
        } else {
            this.curNodeId = curNodeId;
        }
        this.host = host;
        this.port = port;
        this.seedNodes = new ArrayList<>(seeds.size());

        for (String singleSeed : seeds) {
            String[] parts = singleSeed.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid seed host and port provided: " + singleSeed);
            }
            seedNodes.add(new HostPort(parts[0], Integer.parseInt(parts[1])));
        }

        LOGGER.info("Seed nodes registered: {}", seedNodes);
        LOGGER.info("Current node ID: {}", curNodeId);
    }

    public String curNodeId() {
        return curNodeId;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public List<HostPort> seedNodes() {
        return Collections.unmodifiableList(seedNodes);
    }

    public int clusterSize() {
        return seedNodes.size() + 1;
    }
}

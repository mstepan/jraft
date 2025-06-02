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
    private static final int MAX_PORT_VALUE = 65_535;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String curNodeId;
    private final HostPort nodeAddress;
    private final List<HostPort> seedNodes;
    private final int clusterSize;

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
        this.nodeAddress = new HostPort(host, port);
        this.seedNodes = new ArrayList<>(seeds.size() + 1);

        for (String singleSeed : seeds) {
            String[] parts = singleSeed.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid seed host and port provided: " + singleSeed);
            }

            HostPort otherNode = new HostPort(parts[0], Integer.parseInt(parts[1]));

            if (otherNode.equals(nodeAddress)) {
                // Don't track current node address as a peer
                continue;
            }

            seedNodes.add(otherNode);
        }

        this.clusterSize = seedNodes.size() + 1;
        LOGGER.info("Current node ID: {}", curNodeId);
        LOGGER.info("Seed nodes registered: {}", seedNodes);
    }

    public String curNodeId() {
        return curNodeId;
    }

    public String host() {
        return nodeAddress.host();
    }

    public int port() {
        return nodeAddress.port();
    }

    public List<HostPort> seedNodes() {
        return Collections.unmodifiableList(seedNodes);
    }

    public int majorityCount() {
        return (clusterSize / 2) + 1;
    }
}

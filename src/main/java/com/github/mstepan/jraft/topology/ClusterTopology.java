package com.github.mstepan.jraft.topology;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ClusterTopology {
    INST;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<HostPort> seedNodes = new ArrayList<>();

    private final String curNodeId = UUID.randomUUID().toString();

    public void addSeedNodes(List<String> seeds) {
        seedNodes.clear();

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

    public List<HostPort> seedNodes() {
        return Collections.unmodifiableList(seedNodes);
    }

    public String curNodeId() {
        return curNodeId;
    }

    public int clusterSize() {
        return seedNodes.size() + 1;
    }
}

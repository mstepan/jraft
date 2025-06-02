package com.github.mstepan.jraft.topology;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ClusterTopologyTest {

    @Test
    void constructTopologyWithCurrentNodeAsSeeds() {
        ClusterTopology clusterOf3 =
                new ClusterTopology(
                        "server-1",
                        "localhost",
                        9091,
                        List.of("localhost:9091", "localhost:9092", "localhost:9093"));

        assertThat(clusterOf3.seedNodes())
                .hasSize(2)
                .containsOnly(new HostPort("localhost", 9092), new HostPort("localhost", 9093));
    }

    @Test
    void constructTopologyWithoutCurrentNodeAsSeeds() {
        ClusterTopology clusterOf3 =
                new ClusterTopology(
                        "server-1", "localhost", 9091, List.of("localhost:9092", "localhost:9093"));

        assertThat(clusterOf3.seedNodes())
                .hasSize(2)
                .containsOnly(new HostPort("localhost", 9092), new HostPort("localhost", 9093));
    }

    @Test
    void majorityCountOddClusterSize() {

        ClusterTopology clusterOf1 =
                new ClusterTopology("server-1", "localhost", 9091, List.of("localhost:9091"));

        assertEquals(1, clusterOf1.majorityCount());

        ClusterTopology clusterOf3 =
                new ClusterTopology(
                        "server-1",
                        "localhost",
                        9091,
                        List.of("localhost:9091", "localhost:9092", "localhost:9093"));

        assertEquals(2, clusterOf3.majorityCount());

        ClusterTopology clusterOf5 =
                new ClusterTopology(
                        "server-1",
                        "localhost",
                        9091,
                        List.of(
                                "localhost:9091",
                                "localhost:9092",
                                "localhost:9093",
                                "localhost:9094",
                                "localhost:9095"));

        assertEquals(3, clusterOf5.majorityCount());
    }

    @Test
    void majorityCountEventClusterSize() {

        ClusterTopology clusterOf2 =
                new ClusterTopology(
                        "server-1", "localhost", 9091, List.of("localhost:9091", "localhost:9092"));

        assertEquals(2, clusterOf2.majorityCount());

        ClusterTopology clusterOf4 =
                new ClusterTopology(
                        "server-1",
                        "localhost",
                        9091,
                        List.of(
                                "localhost:9091",
                                "localhost:9092",
                                "localhost:9093",
                                "localhost:9094"));

        assertEquals(3, clusterOf4.majorityCount());
    }
}

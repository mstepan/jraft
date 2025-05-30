package com.github.mstepan.jraft.state;

import java.util.concurrent.atomic.AtomicLong;

public final class LeaderInfo {

    public static final LeaderInfo INST = new LeaderInfo();

    private final AtomicLong lastMessageTimestampFromLeader = new AtomicLong(0L);

    private LeaderInfo() {}

    public void recordMessageFromLeader() {
        lastMessageTimestampFromLeader.set(System.nanoTime());
        NodeGlobalState.INST.setRoleIfDifferent(NodeRole.FOLLOWER);
    }

    public long lastLeaderTimestamp() {
        return lastMessageTimestampFromLeader.get();
    }
}

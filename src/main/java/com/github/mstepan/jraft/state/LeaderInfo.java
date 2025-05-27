package com.github.mstepan.jraft.state;

import java.util.concurrent.atomic.AtomicLong;

public enum LeaderInfo {
    INST;

    private final AtomicLong lastMessageTimestampFromLeader = new AtomicLong(0L);

    public void recordMessageFromLeader() {
        lastMessageTimestampFromLeader.set(System.nanoTime());
        NodeGlobalState.INST.setRoleIfDifferent(NodeRole.FOLLOWER);
    }

    public long lastLeaderTimestamp() {
        return lastMessageTimestampFromLeader.get();
    }
}

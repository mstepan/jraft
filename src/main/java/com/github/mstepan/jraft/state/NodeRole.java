package com.github.mstepan.jraft.state;

/*
Each node (or server) in RAFT can be in one of three states:
 - Follower (default state)
 - Candidate
 - Leader
 */
public enum NodeRole {
    FOLLOWER,

    CANDIDATE,

    LEADER;
}

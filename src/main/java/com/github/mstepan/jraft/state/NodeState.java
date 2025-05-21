package com.github.mstepan.jraft.state;

public class NodeState {

    private NodeRole role;

    public synchronized NodeRole getRole() {
        return role;
    }

    public synchronized void setRole(NodeRole role) {
        NodeRole prevRole = this.role;
        this.role = role;
        System.out.printf("Role changed: %s -> %s%n", prevRole, role);
    }
}

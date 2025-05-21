package com.github.mstepan.jraft.state;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeState {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private NodeRole role;

    public synchronized NodeRole getRole() {
        return role;
    }

    public synchronized void setRole(NodeRole role) {
        NodeRole prevRole = this.role;
        this.role = role;
        LOGGER.debug("Role changed: {} -> {}", prevRole, role);
    }
}

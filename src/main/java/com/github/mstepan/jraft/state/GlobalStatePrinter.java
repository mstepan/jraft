package com.github.mstepan.jraft.state;

import com.github.mstepan.jraft.util.ConcurrencyUtils;
import java.util.concurrent.Callable;

/** Print every node in-memory state to console. */
public final class GlobalStatePrinter implements Callable<Void> {

    @Override
    public Void call() throws Exception {

        while (!Thread.currentThread().isInterrupted()) {
            ConcurrencyUtils.sleepMs(10_000L);
            NodeGlobalState.INST.printState();
        }

        return null;
    }
}

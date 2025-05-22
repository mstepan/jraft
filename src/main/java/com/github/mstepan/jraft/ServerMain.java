package com.github.mstepan.jraft;

import picocli.CommandLine;

public final class ServerMain {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ServerCliCommand()).execute(args);
        System.exit(exitCode);
    }
}

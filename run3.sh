#!/usr/bin/env bash

JVM_THREADS="-Djdk.virtualThreadScheduler.parallelism=4 -Djdk.tracePinnedThreads=full"
JVM_HEAP="-Xms1G -Xmx1G"
JVM_JMX="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=true -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=127.0.0.1"

java $JVM_THREADS $JVM_HEAP $JVM_JMX --enable-preview -jar target/jraft-0.0.1-SNAPSHOT.jar \
--host=localhost --port=9093 \
--seed="localhost:9091" --seed="localhost:9092"

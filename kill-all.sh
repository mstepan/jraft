#!/usr/bin/env bash

# kill all running java processes
ps aux | grep java | grep -v grep | awk '{print $1}' | xargs -r kill
#!/usr/bin/env bash

ps aux | grep java | grep -v grep | awk '{print $1}' | xargs -r kill
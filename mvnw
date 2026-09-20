#!/bin/sh
# StegoVault Maven Wrapper Script for Unix/macOS
DIRNAME="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$DIRNAME/apache-maven-3.9.6/bin/mvn" ]; then
    exec "$DIRNAME/apache-maven-3.9.6/bin/mvn" "$@"
else
    exec mvn "$@"
fi

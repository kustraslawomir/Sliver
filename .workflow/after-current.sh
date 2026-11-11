#!/usr/bin/env bash
cd "$(dirname "$0")/.."
while pgrep -f "claude -p /build-app" >/dev/null; do sleep 60; done
exec ./scripts/run-until-done.sh "$HOME/Downloads/sliver-agent-plan.zip" 20 12 30

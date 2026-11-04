#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

echo "Stopping PrestIQ services..."

if [ -d ".pids" ]; then
    for pid_file in .pids/*.pid; do
        if [ -f "$pid_file" ]; then
            service_name=$(basename "$pid_file" .pid)
            pid=$(cat "$pid_file")
            if kill -0 "$pid" 2>/dev/null; then
                echo "-> Killing $service_name (PID $pid)..."
                kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null
            fi
            rm -f "$pid_file"
        fi
    done
fi

# Fallback cleanup by ports
for port in 8761 8080 8081 8082 8083 8086 3000; do
    pids=$(lsof -ti :$port 2>/dev/null || true)
    if [ -n "$pids" ]; then
        echo "Releasing port $port (PIDs: $pids)..."
        kill -9 $pids 2>/dev/null || true
    fi
done

echo "All PrestIQ services stopped."


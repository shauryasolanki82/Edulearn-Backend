#!/usr/bin/env bash
# Stops all locally running EduLearn services by PID file

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"

RED='\033[0;31m'
NC='\033[0m'

SERVICES=(
  auth-service course-service lesson-service enrollment-service
  payment-service progress-service assessment-service
  discussion-service notification-service api-gateway
)

for svc in "${SERVICES[@]}"; do
  PID_FILE="$LOG_DIR/$svc.pid"
  if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
      echo -e "${RED}[STOP]${NC} Stopping $svc (PID=$PID)..."
      kill "$PID"
    fi
    rm -f "$PID_FILE"
  fi
done

echo "All services stopped."

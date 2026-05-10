#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════
#  EduLearn LMS — Local Development Startup Script
#  Starts the API Gateway + all backend services (requires Maven)
#  Frontend is started separately with: cd edulearn-react && npm run dev
# ═══════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/LMS_Backend"
GATEWAY_DIR="$SCRIPT_DIR/api-gateway"
LOG_DIR="$SCRIPT_DIR/logs"

mkdir -p "$LOG_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[START]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# ── Helper: start a Spring Boot service ───────────────────────────
start_service() {
  local name=$1
  local dir=$2
  local port=$3
  log "Starting $name on port $port ..."
  cd "$dir"
  mvn spring-boot:run -q > "$LOG_DIR/$name.log" 2>&1 &
  echo $! > "$LOG_DIR/$name.pid"
  cd "$SCRIPT_DIR"
  log "$name started (PID=$(cat $LOG_DIR/$name.pid)) → log: logs/$name.log"
}

# ── Start all services ─────────────────────────────────────────────
start_service "auth-service"         "$BACKEND_DIR/auth-service"         8081
sleep 5   # auth-service must be up before others start validating tokens

start_service "course-service"       "$BACKEND_DIR/course-service"       8082
start_service "lesson-service"       "$BACKEND_DIR/lesson-service"       8083
start_service "enrollment-service"   "$BACKEND_DIR/enrollment-service"   8084
start_service "payment-service"      "$BACKEND_DIR/payment-service"      8085
start_service "progress-service"     "$BACKEND_DIR/progress-service"     8086
start_service "assessment-service"   "$BACKEND_DIR/assessment-service"   8087
start_service "discussion-service"   "$BACKEND_DIR/discussion-service"   8088
start_service "notification-service" "$BACKEND_DIR/notification-service" 8089

sleep 10  # Give services time to initialize before starting gateway

start_service "api-gateway"          "$GATEWAY_DIR"                      8080

echo ""
log "════════════════════════════════════════════"
log "All services started!"
log "  API Gateway  →  http://localhost:8080"
log "  Auth Service →  http://localhost:8081"
log ""
log "Now start the frontend:"
log "  cd edulearn-react && npm install && npm run dev"
log ""
log "To stop all services, run: ./stop-local.sh"
log "════════════════════════════════════════════"

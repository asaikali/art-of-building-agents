#!/usr/bin/env bash
#
# Run integration tests for workshop agent steps.
#
# Usage:
#   ./integration-tests/scripts/run-workshop-tests.sh           # Run all steps
#   ./integration-tests/scripts/run-workshop-tests.sh 02        # Run step 02 only
#   ./integration-tests/scripts/run-workshop-tests.sh 02 03 04  # Run specific steps
#
# Each step is tested by:
#   1. Building the module
#   2. Starting the Spring Boot app
#   3. Creating a session, sending a test message, validating the response
#   4. Tearing down the app
#
# Requires: OPENAI_API_KEY environment variable
# Requires: curl, jq

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_DIR="$REPO_ROOT/integration-tests/logs"
PORT=8080
STARTUP_TIMEOUT=60
RESPONSE_TIMEOUT=120

mkdir -p "$LOG_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check prerequisites
if [ -z "${OPENAI_API_KEY:-}" ]; then
  echo -e "${RED}ERROR: OPENAI_API_KEY is not set${NC}"
  exit 1
fi

if ! command -v jq &> /dev/null; then
  echo -e "${RED}ERROR: jq is required but not installed${NC}"
  exit 1
fi

# Discover available steps
discover_steps() {
  local steps=()
  for dir in "$REPO_ROOT"/agents/*/; do
    local name=$(basename "$dir")
    if [ -f "$dir/integration-tests/test-config.json" ]; then
      steps+=("$name")
    fi
  done
  echo "${steps[@]}"
}

# Kill any process on the test port
cleanup_port() {
  local pid=$(lsof -ti :"$PORT" 2>/dev/null || true)
  if [ -n "$pid" ]; then
    echo "  Cleaning up port $PORT (pid $pid)"
    kill "$pid" 2>/dev/null || true
    sleep 1
    kill -9 "$pid" 2>/dev/null || true
  fi
}

# Wait for app to start
wait_for_startup() {
  local elapsed=0
  while [ $elapsed -lt $STARTUP_TIMEOUT ]; do
    if curl -s "http://localhost:$PORT/api/sessions" > /dev/null 2>&1; then
      return 0
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done
  return 1
}

# Wait for a specific port to respond
wait_for_port() {
  local port="$1"
  local elapsed=0
  while [ $elapsed -lt $STARTUP_TIMEOUT ]; do
    if curl -s "http://localhost:$port/" > /dev/null 2>&1; then
      return 0
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done
  return 1
}

# Run a single step's test
run_step_test() {
  local step_name="$1"
  local step_dir="$REPO_ROOT/agents/$step_name"
  local config_file="$step_dir/integration-tests/test-config.json"
  local log_file="$LOG_DIR/${step_name}-$(date +%Y%m%d-%H%M%S).log"
  local dep_pid=""

  echo -e "\n${YELLOW}━━━ Testing: $step_name ━━━${NC}"

  # Read test config
  local test_message=$(jq -r '.testMessage' "$config_file")
  local timeout=$(jq -r '.timeoutSec // 120' "$config_file")
  local description=$(jq -r '.description // ""' "$config_file")

  if [ -n "$description" ]; then
    echo "  $description"
  fi

  # Check for dependency (e.g. MCP server that must be running first)
  local dep_module=$(jq -r '.dependsOn.module // ""' "$config_file")
  local dep_port=$(jq -r '.dependsOn.port // ""' "$config_file")

  # Clean up port
  cleanup_port
  if [ -n "$dep_port" ]; then
    local dep_old_pid=$(lsof -ti :"$dep_port" 2>/dev/null || true)
    if [ -n "$dep_old_pid" ]; then
      kill "$dep_old_pid" 2>/dev/null || true
      sleep 1
    fi
  fi

  # Build (include dependency module if needed)
  echo "  Building..."
  local build_modules="agents/$step_name"
  if [ -n "$dep_module" ]; then
    build_modules="agents/$dep_module,agents/$step_name"
  fi
  if ! (cd "$REPO_ROOT" && ./mvnw package -pl "$build_modules" -am -DskipTests -q) >> "$log_file" 2>&1; then
    echo -e "  ${RED}FAIL: Build failed${NC} (see $log_file)"
    return 1
  fi

  # Start dependency server if needed
  if [ -n "$dep_module" ]; then
    local dep_dir="$REPO_ROOT/agents/$dep_module"
    local dep_jar=$(find "$dep_dir/target" -name "*.jar" -not -name "*-sources*" -not -name "*-javadoc*" | head -1)
    if [ -z "$dep_jar" ]; then
      echo -e "  ${RED}FAIL: No JAR found for dependency $dep_module${NC}"
      return 1
    fi
    echo "  Starting dependency: $dep_module (port $dep_port)..."
    java -jar "$dep_jar" >> "$log_file" 2>&1 &
    dep_pid=$!
    if ! wait_for_port "$dep_port"; then
      echo -e "  ${RED}FAIL: Dependency $dep_module did not start within ${STARTUP_TIMEOUT}s${NC}"
      kill "$dep_pid" 2>/dev/null || true
      return 1
    fi
    echo "  Dependency started (pid $dep_pid)"
  fi

  # Start app
  echo "  Starting app..."
  local jar=$(find "$step_dir/target" -name "*.jar" -not -name "*-sources*" -not -name "*-javadoc*" | head -1)
  if [ -z "$jar" ]; then
    echo -e "  ${RED}FAIL: No JAR found in target/${NC}"
    [ -n "$dep_pid" ] && kill "$dep_pid" 2>/dev/null || true
    return 1
  fi

  java -jar "$jar" >> "$log_file" 2>&1 &
  local app_pid=$!

  # Wait for startup
  echo "  Waiting for startup..."
  if ! wait_for_startup; then
    echo -e "  ${RED}FAIL: App did not start within ${STARTUP_TIMEOUT}s${NC} (see $log_file)"
    kill "$app_pid" 2>/dev/null || true
    [ -n "$dep_pid" ] && kill "$dep_pid" 2>/dev/null || true
    return 1
  fi
  echo "  App started (pid $app_pid)"

  # Create session
  local session_response=$(curl -s -X POST "http://localhost:$PORT/api/sessions" \
    -H "Content-Type: application/json" \
    -d "{\"agentName\": \"test\", \"title\": \"integration-test\"}")
  local session_id=$(echo "$session_response" | jq -r '.sessionId')

  if [ "$session_id" = "null" ] || [ -z "$session_id" ]; then
    echo -e "  ${RED}FAIL: Could not create session${NC}"
    echo "  Response: $session_response"
    kill "$app_pid" 2>/dev/null || true
    [ -n "$dep_pid" ] && kill "$dep_pid" 2>/dev/null || true
    return 1
  fi
  echo "  Session: $session_id"

  # Send test message
  echo "  Sending: \"$test_message\""
  local msg_response=$(curl -s -X POST "http://localhost:$PORT/api/sessions/$session_id/messages" \
    -H "Content-Type: application/json" \
    -d "{\"role\": \"USER\", \"text\": \"$test_message\"}" \
    --max-time "$timeout")

  # Wait a moment for async processing, then get all messages
  sleep 2
  local messages=$(curl -s "http://localhost:$PORT/api/sessions/$session_id/messages" --max-time 10)
  local assistant_messages=$(echo "$messages" | jq -r '[.[] | select(.role == "ASSISTANT")] | last | .text // ""')

  # Tear down
  kill "$app_pid" 2>/dev/null || true
  [ -n "$dep_pid" ] && kill "$dep_pid" 2>/dev/null || true
  sleep 1

  # Validate response
  if [ -z "$assistant_messages" ] || [ "$assistant_messages" = "null" ]; then
    echo -e "  ${RED}FAIL: No assistant response${NC}"
    echo "  Messages: $messages" >> "$log_file"
    return 1
  fi

  # Check success patterns from config
  local patterns=$(jq -r '.successPatterns[]' "$config_file" 2>/dev/null)
  local all_matched=true

  if [ -n "$patterns" ]; then
    while IFS= read -r pattern; do
      if echo "$assistant_messages" | grep -qi "$pattern"; then
        echo -e "  ${GREEN}✓${NC} Found: \"$pattern\""
      else
        echo -e "  ${RED}✗${NC} Missing: \"$pattern\""
        all_matched=false
      fi
    done <<< "$patterns"
  else
    # No patterns specified — just check we got a non-empty response
    echo -e "  ${GREEN}✓${NC} Got response (${#assistant_messages} chars)"
  fi

  if [ "$all_matched" = true ]; then
    echo -e "  ${GREEN}PASS${NC}"
    return 0
  else
    echo -e "  ${RED}FAIL: Not all patterns matched${NC}"
    echo "  Full response: $assistant_messages" >> "$log_file"
    return 1
  fi
}

# Main
echo "═══════════════════════════════════════════════"
echo " Workshop Integration Tests"
echo "═══════════════════════════════════════════════"

# Determine which steps to run
if [ $# -gt 0 ]; then
  STEPS=("$@")
  # Expand short names: "02" -> "02-tool-calling"
  EXPANDED_STEPS=()
  for s in "${STEPS[@]}"; do
    matched=$(ls -d "$REPO_ROOT"/agents/${s}*/ 2>/dev/null | head -1)
    if [ -n "$matched" ]; then
      EXPANDED_STEPS+=("$(basename "$matched")")
    else
      echo -e "${RED}No step matching '$s'${NC}"
      exit 1
    fi
  done
  STEPS=("${EXPANDED_STEPS[@]}")
else
  STEPS=($(discover_steps))
fi

if [ ${#STEPS[@]} -eq 0 ]; then
  echo "No steps with test-config.json found."
  exit 0
fi

echo "Steps to test: ${STEPS[*]}"

# Run tests
PASSED=0
FAILED=0
FAILED_STEPS=()

for step in "${STEPS[@]}"; do
  if run_step_test "$step"; then
    PASSED=$((PASSED + 1))
  else
    FAILED=$((FAILED + 1))
    FAILED_STEPS+=("$step")
  fi
done

# Summary
echo ""
echo "═══════════════════════════════════════════════"
echo -e " Results: ${GREEN}${PASSED} passed${NC}, ${RED}${FAILED} failed${NC}"
if [ ${#FAILED_STEPS[@]} -gt 0 ]; then
  echo -e " Failed: ${RED}${FAILED_STEPS[*]}${NC}"
fi
echo " Logs: $LOG_DIR"
echo "═══════════════════════════════════════════════"

cleanup_port
exit $FAILED

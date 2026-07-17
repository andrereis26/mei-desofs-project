#!/usr/bin/env bash

set -euo pipefail

STACK_NAME="${STACK_NAME:-desofs}"
APP_SERVICE_NAME="${STACK_NAME}_app"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1:9090/actuator/health/readiness}"

require_command() {
  local command_name="$1"

  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Missing required command: ${command_name}" >&2
    exit 1
  fi
}

wait_for_service_convergence() {
  local timeout_seconds="${1:-300}"
  local start_time
  start_time="$(date +%s)"

  while true; do
    local service_state
    local desired_replicas
    service_state="$(docker service ls --format '{{.Name}} {{.Replicas}}' | awk -v name="${APP_SERVICE_NAME}" '$1 == name { print $2 }')"
    desired_replicas="$(docker service inspect "${APP_SERVICE_NAME}" --format '{{.Spec.Mode.Replicated.Replicas}}' 2>/dev/null || true)"

    if [[ -n "${desired_replicas}" && "${service_state}" == "${desired_replicas}/${desired_replicas}" ]]; then
      return 0
    fi

    if (( $(date +%s) - start_time >= timeout_seconds )); then
      echo "Timed out waiting for ${APP_SERVICE_NAME} to reach ${desired_replicas:-desired} replicas. Current state: ${service_state:-unknown}" >&2
      return 1
    fi

    sleep 5
  done
}

wait_for_healthcheck() {
  local timeout_seconds="${1:-180}"
  local start_time
  start_time="$(date +%s)"

  while true; do
    if curl --fail --silent --show-error --connect-timeout 5 --max-time 10 "${HEALTHCHECK_URL}" | grep -q '"status":"UP"'; then
      return 0
    fi

    if (( $(date +%s) - start_time >= timeout_seconds )); then
      echo "Timed out waiting for readiness endpoint ${HEALTHCHECK_URL} to report UP." >&2
      return 1
    fi

    sleep 5
  done
}

main() {
  require_command docker
  require_command curl

  if [[ "${DOCKER_SWARM_REQUIRED:-true}" == "true" ]]; then
    local swarm_state
    swarm_state="$(docker info --format '{{.Swarm.LocalNodeState}}')"
    if [[ "${swarm_state}" != "active" ]]; then
      echo "Docker Swarm is not active on the host." >&2
      exit 1
    fi
  fi

  if ! docker service inspect "${APP_SERVICE_NAME}" >/dev/null 2>&1; then
    echo "Service ${APP_SERVICE_NAME} not found; cannot rollback." >&2
    exit 1
  fi

  echo "Triggering rollback for ${APP_SERVICE_NAME}..."
  docker service rollback "${APP_SERVICE_NAME}"

  if ! wait_for_service_convergence || ! wait_for_healthcheck; then
    echo "Rollback failed to stabilize ${APP_SERVICE_NAME}." >&2
    docker service inspect "${APP_SERVICE_NAME}" --format '{{json .UpdateStatus}}' || true
    exit 1
  fi

  docker service inspect "${APP_SERVICE_NAME}" --format '{{json .UpdateStatus}}'
}

main "$@"

#!/usr/bin/env bash

set -euo pipefail

STACK_NAME="${STACK_NAME:-desofs}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/desofs-api}"
DEPLOYMENT_DIR="${DEPLOY_ROOT}/deployment"
STACK_FILE="${DEPLOYMENT_DIR}/docker-compose.swarm.yml"
APP_SERVICE_NAME="${STACK_NAME}_app"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1:9090/actuator/health/readiness}"
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
DOCKERHUB_TOKEN="${DOCKERHUB_TOKEN:?DOCKERHUB_TOKEN is required}"
APP_IMAGE="${APP_IMAGE:?APP_IMAGE is required}"

require_command() {
  local command_name="$1"

  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Missing required command: ${command_name}" >&2
    exit 1
  fi
}

print_docker_runtime_diagnostics() {
  local info_output
  info_output="$(docker info 2>/dev/null || true)"

  if [[ -z "${info_output}" ]]; then
    echo "Unable to collect docker info diagnostics." >&2
    return 0
  fi

  echo "Docker runtime diagnostics:" >&2
  printf '%s\n' "${info_output}" | grep -E 'Storage Driver|Backing Filesystem|Supports d_type|Native Overlay Diff|Docker Root Dir|Security Options|rootless|Rootless|Cgroup Version|Kernel Version' >&2 || true
}

validate_docker_runtime() {
  local info_output
  info_output="$(docker info 2>/dev/null || true)"

  if [[ -z "${info_output}" ]]; then
    echo "Unable to query docker info on the target host." >&2
    return 1
  fi

  if printf '%s\n' "${info_output}" | grep -qi 'rootless'; then
    cat >&2 <<'EOF'
Rootless Docker was detected on the target host.
This deployment uses Docker Swarm with an overlay network, and Docker documents overlay networks as unsupported in rootless mode.
Use a rootful Docker Engine for this Swarm deployment target.
EOF
    print_docker_runtime_diagnostics
    return 1
  fi
}

run_stack_with_doppler() {
  local doppler_cmd=(doppler run)

  if [[ -n "${DOPPLER_PROJECT:-}" ]]; then
    doppler_cmd+=(--project "${DOPPLER_PROJECT}")
  fi
  if [[ -n "${DOPPLER_CONFIG:-}" ]]; then
    doppler_cmd+=(--config "${DOPPLER_CONFIG}")
  fi

  doppler_cmd+=(-- docker stack "$@")
  "${doppler_cmd[@]}"
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

rollback_app_service() {
  if docker service inspect "${APP_SERVICE_NAME}" >/dev/null 2>&1; then
    docker service rollback "${APP_SERVICE_NAME}" || true
  fi
}

pull_release_image() {
  local pull_output

  if ! pull_output="$(docker pull "${APP_IMAGE}" 2>&1)"; then
    printf '%s\n' "${pull_output}" >&2

    if grep -Eqi 'failed to (extract layer|mount).*(overlay|overlayfs)|permission denied' <<< "${pull_output}"; then
      cat >&2 <<'EOF'
Docker failed while unpacking image layers into the local image store.
This is typically a host Docker storage-driver or host-kernel issue, not an application image issue.

Check on the target server:
  - docker info | grep -E 'Storage Driver|Backing Filesystem|Supports d_type|Native Overlay Diff|Docker Root Dir|Security Options|rootless|Kernel Version'
  - whether the host is running inside an unprivileged VM/container where overlay mounts are denied
  - whether the Docker data root backing filesystem satisfies overlay support requirements
  - whether the host needs a different supported storage backend

If the host is using rootless Docker, switch this Swarm target to a rootful Docker Engine.
EOF
      print_docker_runtime_diagnostics
    fi

    return 1
  fi

  printf '%s\n' "${pull_output}"
}

main() {
  require_command docker
  require_command doppler
  require_command curl

  if [[ "${DOCKER_SWARM_REQUIRED:-true}" == "true" ]]; then
    local swarm_state
    swarm_state="$(docker info --format '{{.Swarm.LocalNodeState}}')"
    if [[ "${swarm_state}" != "active" ]]; then
      echo "Docker Swarm is not active on the host." >&2
      exit 1
    fi
  fi

  validate_docker_runtime

  mkdir -p "${DEPLOY_ROOT}"

  echo "${DOCKERHUB_TOKEN}" | docker login --username "${DOCKERHUB_USERNAME}" --password-stdin

  export APP_IMAGE
  # Validate the stack against the Swarm parser before attempting the rollout.
  run_stack_with_doppler config --compose-file "${STACK_FILE}" >/dev/null

  pull_release_image
  run_stack_with_doppler deploy --compose-file "${STACK_FILE}" --prune --with-registry-auth "${STACK_NAME}"

  if ! wait_for_service_convergence || ! wait_for_healthcheck; then
    rollback_app_service
    exit 1
  fi

  docker service inspect "${APP_SERVICE_NAME}" --format '{{json .UpdateStatus}}'
}

main "$@"

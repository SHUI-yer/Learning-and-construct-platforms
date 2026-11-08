#!/usr/bin/env bash
set -Eeuo pipefail

release_id="${1:-}"
deploy_root="${2:-/opt/learning-platform}"
service_name="${3:-learning-platform}"
env_file="${4:-/home/ubuntu/.learning-platform.env}"
health_url="${5:-http://127.0.0.1:8081/actuator/health/readiness}"
automatic_app_rollback="${6:-true}"
health_attempts="${DEPLOY_HEALTH_ATTEMPTS:-30}"
health_delay_seconds="${DEPLOY_HEALTH_DELAY_SECONDS:-2}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

wait_for_health() {
  local release_label="$1"
  local attempts="${2:-30}"
  local delay_seconds="${3:-2}"
  local attempt

  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl --fail --silent --output /dev/null --max-time 5 "$health_url"; then
      echo "Health check passed for ${release_label} on attempt ${attempt}/${attempts}."
      return 0
    fi
    sleep "$delay_seconds"
  done

  echo "Health check failed for ${release_label} after ${attempts} attempts." >&2
  curl --fail --show-error --max-time 5 "$health_url" >/dev/null || true
  return 1
}

[[ "$release_id" =~ ^[0-9a-f]{7,40}$ ]] || fail "release id must be a Git commit SHA"
[[ "$deploy_root" =~ ^/[A-Za-z0-9._/-]+$ && "$deploy_root" != "/" ]] || fail "unsafe deploy root"
[[ "$service_name" =~ ^[A-Za-z0-9_.@-]+$ ]] || fail "unsafe systemd service name"
[[ "$env_file" =~ ^/[A-Za-z0-9._/-]+$ ]] || fail "unsafe environment file path"
[[ "$health_url" =~ ^http://127\.0\.0\.1:[0-9]+/actuator/health/readiness$ ]] || fail "health URL must use the local Actuator readiness endpoint"
[[ "$automatic_app_rollback" == "true" || "$automatic_app_rollback" == "false" ]] || fail "automatic rollback flag must be true or false"
[[ "$health_attempts" =~ ^[1-9][0-9]*$ ]] || fail "health attempts must be a positive integer"
[[ "$health_delay_seconds" =~ ^[0-9]+$ ]] || fail "health delay must be a non-negative integer"

for command_name in curl flock tar sudo systemctl; do
  command -v "$command_name" >/dev/null 2>&1 || fail "missing command: $command_name"
done

archive_file="${deploy_root}/incoming/${release_id}.tar.gz"
release_dir="${deploy_root}/releases/${release_id}"
current_link="${deploy_root}/current"
temporary_link="${deploy_root}/.current-${release_id}"
previous_link="${deploy_root}/previous"
temporary_previous_link="${deploy_root}/.previous-${release_id}"

[[ -f "$archive_file" ]] || fail "release archive is missing: $archive_file"
[[ -r "$env_file" ]] || fail "environment file is not readable: $env_file"
[[ ! -e "$current_link" || -L "$current_link" ]] || fail "current path must be absent or a symbolic link"

mkdir -p -- "${deploy_root}/releases"
exec 9>"${deploy_root}/.deploy.lock"
flock -n 9 || fail "another deployment is already running"

if [[ -L "$current_link" && "$(readlink -f "$current_link")" == "$(readlink -f "$release_dir")" ]]; then
  fail "this release is already current; inspect the failed deployment before retrying"
fi

if [[ -e "$release_dir" ]]; then
  [[ -d "$release_dir" && ! -L "$release_dir" ]] || fail "existing release path is unsafe"
  echo "Reusing a previously staged release: $release_id"
else
  mkdir -- "$release_dir"
  tar -xzf "$archive_file" -C "$release_dir"
fi
rm -f -- "$archive_file"

[[ -f "${release_dir}/backend/lp-starter.jar" ]] || fail "backend artifact is missing"
[[ -f "${release_dir}/web/index.html" ]] || fail "frontend artifact is missing"
[[ -f "${release_dir}/scripts/backup-db.sh" ]] || fail "backup script is missing"

previous_release=""
if [[ -L "$current_link" ]]; then
  previous_release="$(readlink -f "$current_link")"
fi

sudo systemctl stop "$service_name"

if ! ENV_FILE="$env_file" APP_VERSION="pre-${release_id}" \
  bash "${release_dir}/scripts/backup-db.sh"; then
  echo "Pre-deployment backup failed; restoring the previously running service." >&2
  if [[ -n "$previous_release" && -d "$previous_release" ]]; then
    if sudo systemctl start "$service_name" \
      && wait_for_health "unchanged release ${previous_release##*/}" "$health_attempts" "$health_delay_seconds"; then
      echo "The previous release is healthy; deployment was not activated." >&2
    else
      echo "The previous release could not be restored after the backup failure." >&2
    fi
  fi
  exit 1
fi

if [[ -n "$previous_release" && -d "$previous_release" ]]; then
  ln -s "$previous_release" "$temporary_previous_link"
  mv -Tf "$temporary_previous_link" "$previous_link"
fi

ln -s "$release_dir" "$temporary_link"
mv -Tf "$temporary_link" "$current_link"

activation_failure=""
if ! sudo systemctl start "$service_name"; then
  activation_failure="systemd could not start the new release"
elif ! wait_for_health "new release ${release_id}" "$health_attempts" "$health_delay_seconds"; then
  activation_failure="the new release failed readiness checks"
fi

if [[ -z "$activation_failure" ]]; then
  echo "Release activated successfully: $release_id"
  exit 0
fi

echo "Release activation failed: ${activation_failure}." >&2
sudo systemctl stop "$service_name" || true

if [[ "$automatic_app_rollback" == "true" && -n "$previous_release" && -d "$previous_release" ]]; then
  rollback_link="${deploy_root}/.rollback-${release_id}"
  rm -f -- "$rollback_link"
  ln -s "$previous_release" "$rollback_link"
  mv -Tf "$rollback_link" "$current_link"
  if sudo systemctl start "$service_name" \
    && wait_for_health "rolled-back release ${previous_release##*/}" "$health_attempts" "$health_delay_seconds"; then
    echo "Rollback succeeded; application files now point to: $previous_release" >&2
    echo "The failed release remains available for diagnosis: $release_dir" >&2
    echo "Database was not restored; migrations must follow the expand/contract policy." >&2
  else
    sudo systemctl stop "$service_name" || true
    echo "CRITICAL: rollback completed at the file level, but the previous release is not healthy." >&2
    echo "The service remains stopped. Follow docs/DEPLOYMENT_RUNBOOK.md immediately." >&2
  fi
else
  if [[ "$automatic_app_rollback" != "true" ]]; then
    echo "Automatic application rollback is disabled by configuration." >&2
  else
    echo "No previous release is available (this may be the first deployment)." >&2
  fi
  echo "The service remains stopped. Follow docs/DEPLOYMENT_RUNBOOK.md." >&2
fi

exit 1

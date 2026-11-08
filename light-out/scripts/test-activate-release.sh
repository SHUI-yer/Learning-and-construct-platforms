#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
activate_script="${script_dir}/activate-release.sh"
test_root="$(mktemp -d)"

cleanup() {
  case "$test_root" in
    /tmp/*|/private/tmp/*) rm -rf -- "$test_root" ;;
    *) echo "Refusing to remove unexpected test path: $test_root" >&2 ;;
  esac
}
trap cleanup EXIT

fail() {
  echo "TEST FAILURE: $*" >&2
  exit 1
}

write_mock_commands() {
  local mock_bin="$1"
  mkdir -p -- "$mock_bin"

  printf '%s\n' \
    '#!/usr/bin/env bash' \
    'set -Eeuo pipefail' \
    '[[ "${1:-}" == "systemctl" ]] || exit 64' \
    'action="${2:-}"' \
    'current="$(readlink -f "${DEPLOY_TEST_ROOT}/current" 2>/dev/null || true)"' \
    'release="${current##*/}"' \
    'printf "%s:%s\n" "$action" "$release" >> "${DEPLOY_TEST_LOG}"' \
    'if [[ "$action" == "start" && "${DEPLOY_TEST_START_MODE:-normal}" == "fail-new" && "$release" == "${DEPLOY_TEST_NEW_RELEASE}" ]]; then' \
    '  exit 1' \
    'fi' \
    'exit 0' > "${mock_bin}/sudo"

  printf '%s\n' \
    '#!/usr/bin/env bash' \
    'set -Eeuo pipefail' \
    'current="$(readlink -f "${DEPLOY_TEST_ROOT}/current" 2>/dev/null || true)"' \
    'release="${current##*/}"' \
    'case "${DEPLOY_TEST_HEALTH_MODE}" in' \
    '  new) [[ "$release" == "${DEPLOY_TEST_NEW_RELEASE}" ]] ;;' \
    '  rollback) [[ "$release" == "old-release" ]] ;;' \
    '  *) exit 64 ;;' \
    'esac' > "${mock_bin}/curl"

  printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "${mock_bin}/systemctl"
  printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "${mock_bin}/flock"
  chmod 0755 \
    "${mock_bin}/sudo" "${mock_bin}/curl" \
    "${mock_bin}/systemctl" "${mock_bin}/flock"
}

prepare_case() {
  local case_root="$1"
  local release_id="$2"
  local deploy_root="${case_root}/deploy"
  local archive_root="${case_root}/archive"

  mkdir -p \
    "${deploy_root}/incoming" \
    "${deploy_root}/releases/old-release/backend" \
    "${deploy_root}/releases/old-release/web" \
    "${archive_root}/backend" \
    "${archive_root}/web" \
    "${archive_root}/scripts"

  printf 'old jar\n' > "${deploy_root}/releases/old-release/backend/lp-starter.jar"
  printf '<html>old</html>\n' > "${deploy_root}/releases/old-release/web/index.html"
  ln -s "${deploy_root}/releases/old-release" "${deploy_root}/current"

  printf 'new jar\n' > "${archive_root}/backend/lp-starter.jar"
  printf '<html>new</html>\n' > "${archive_root}/web/index.html"
  printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "${archive_root}/scripts/backup-db.sh"
  printf '%s\n' '# test environment' > "${case_root}/application.env"
  tar -czf "${deploy_root}/incoming/${release_id}.tar.gz" -C "$archive_root" .
}

run_case() {
  local case_name="$1"
  local release_id="$2"
  local health_mode="$3"
  local expected_exit="$4"
  local expected_current="$5"
  local start_mode="${6:-normal}"
  local case_root="${test_root}/${case_name}"
  local deploy_root="${case_root}/deploy"
  local mock_bin="${case_root}/mock-bin"
  local log_file="${case_root}/systemctl.log"

  prepare_case "$case_root" "$release_id"
  write_mock_commands "$mock_bin"
  : > "$log_file"

  set +e
  PATH="${mock_bin}:$PATH" \
    DEPLOY_TEST_ROOT="$deploy_root" \
    DEPLOY_TEST_LOG="$log_file" \
    DEPLOY_TEST_HEALTH_MODE="$health_mode" \
    DEPLOY_TEST_NEW_RELEASE="$release_id" \
    DEPLOY_TEST_START_MODE="$start_mode" \
    DEPLOY_HEALTH_ATTEMPTS=1 \
    DEPLOY_HEALTH_DELAY_SECONDS=0 \
    bash "$activate_script" \
      "$release_id" "$deploy_root" learning-platform \
      "${case_root}/application.env" \
      http://127.0.0.1:8081/actuator/health/readiness true
  actual_exit=$?
  set -e

  [[ "$actual_exit" -eq "$expected_exit" ]] \
    || fail "${case_name}: expected exit ${expected_exit}, got ${actual_exit}"
  actual_current="$(readlink -f "${deploy_root}/current")"
  [[ "${actual_current##*/}" == "$expected_current" ]] \
    || fail "${case_name}: expected current ${expected_current}, got ${actual_current##*/}"
}

run_case successful-release aaaaaaa new 0 aaaaaaa
run_case failed-release-rolls-back bbbbbbb rollback 1 old-release
run_case start-failure-rolls-back ccccccc rollback 1 old-release fail-new

grep -q '^start:bbbbbbb$' "${test_root}/failed-release-rolls-back/systemctl.log" \
  || fail "failed release was not started"
grep -q '^start:old-release$' "${test_root}/failed-release-rolls-back/systemctl.log" \
  || fail "previous release was not restarted"
grep -q '^start:old-release$' "${test_root}/start-failure-rolls-back/systemctl.log" \
  || fail "previous release was not restarted after the new service failed to start"

echo "activate-release integration tests passed."

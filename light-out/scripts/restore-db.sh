#!/usr/bin/env bash
set -Eeuo pipefail

umask 077

ENV_FILE="${ENV_FILE:-/home/ubuntu/.learning-platform.env}"
backup_file="${1:-}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ -n "$backup_file" ]] || fail "usage: RESTORE_CONFIRM_DATABASE=<database> DB_ADMIN_USERNAME=<user> DB_ADMIN_PASSWORD=<password> bash restore-db.sh <backup.sql.gz>"
for command_name in mysql gzip sha256sum grep; do
  command -v "$command_name" >/dev/null 2>&1 || fail "missing command: $command_name"
done

[[ -r "$ENV_FILE" ]] || fail "environment file is not readable: $ENV_FILE"
[[ -f "$backup_file" ]] || fail "backup file does not exist: $backup_file"

load_env_value() {
  local key="$1"
  local line
  local value
  [[ -n "${!key:-}" ]] && return
  line="$(grep -E "^[[:space:]]*${key}=" "$ENV_FILE" | tail -n 1 || true)"
  [[ -n "$line" ]] || return
  value="${line#*=}"
  value="${value%$'\r'}"
  if [[ "$value" == \"*\" && "$value" == *\" ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "$value" == \'*\' && "$value" == *\' ]]; then
    value="${value:1:${#value}-2}"
  fi
  printf -v "$key" '%s' "$value"
  export "$key"
}

load_env_value SPRING_DATASOURCE_URL

: "${SPRING_DATASOURCE_URL:?SPRING_DATASOURCE_URL is required}"
: "${DB_ADMIN_USERNAME:?DB_ADMIN_USERNAME is required for destructive restore}"
: "${DB_ADMIN_PASSWORD:?DB_ADMIN_PASSWORD is required for destructive restore}"

jdbc_target="${SPRING_DATASOURCE_URL#jdbc:mysql://}"
[[ "$jdbc_target" != "$SPRING_DATASOURCE_URL" ]] || fail "only jdbc:mysql URLs are supported"
db_authority="${jdbc_target%%/*}"
db_path="${jdbc_target#*/}"
db_name="${db_path%%\?*}"
db_host="${DB_ADMIN_HOST:-${db_authority%%:*}}"
db_port="${DB_ADMIN_PORT:-3306}"
if [[ -z "${DB_ADMIN_PORT:-}" && "$db_authority" == *:* ]]; then
  db_port="${db_authority##*:}"
fi

[[ "$db_name" =~ ^[A-Za-z0-9_]+$ ]] || fail "unsafe database name parsed from JDBC URL"
[[ "${RESTORE_CONFIRM_DATABASE:-}" == "$db_name" ]] || fail "set RESTORE_CONFIRM_DATABASE=$db_name to confirm destructive restore"
[[ "$db_port" =~ ^[0-9]+$ ]] || fail "invalid database port"

backup_dir="$(cd "$(dirname "$backup_file")" && pwd -P)"
backup_name="$(basename "$backup_file")"
backup_file="${backup_dir}/${backup_name}"
checksum_file="${backup_file}.sha256"

[[ -f "$checksum_file" ]] || fail "checksum file is missing: $checksum_file"
(
  cd "$backup_dir"
  sha256sum -c "${backup_name}.sha256"
)
gzip -t -- "$backup_file"
dump_database_headers="$(gzip -dc -- "$backup_file" | grep -E '^-- Current Database: `[^`]+`$' || true)"
[[ "$dump_database_headers" == "-- Current Database: \`$db_name\`" ]] \
  || fail "backup must contain exactly one database matching $db_name"

echo "Restoring $db_name from $backup_file"
echo "The dump drops and recreates the database. The application service must already be stopped."

cleanup() {
  unset MYSQL_PWD || true
}
trap cleanup EXIT
export MYSQL_PWD="$DB_ADMIN_PASSWORD"
gzip -dc -- "$backup_file" | mysql \
  --host="$db_host" \
  --port="$db_port" \
  --user="$DB_ADMIN_USERNAME" \
  --default-character-set=utf8mb4
unset MYSQL_PWD

echo "Database restore completed: $db_name"

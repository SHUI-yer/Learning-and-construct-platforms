#!/usr/bin/env bash
set -Eeuo pipefail

umask 077

ENV_FILE="${ENV_FILE:-/home/ubuntu/.learning-platform.env}"
BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/backups/learning-platform}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

for command_name in mysqldump gzip sha256sum grep; do
  command -v "$command_name" >/dev/null 2>&1 || fail "missing command: $command_name"
done

[[ -r "$ENV_FILE" ]] || fail "environment file is not readable: $ENV_FILE"
[[ "$BACKUP_RETENTION_DAYS" =~ ^[0-9]+$ ]] || fail "BACKUP_RETENTION_DAYS must be a non-negative integer"

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
load_env_value SPRING_DATASOURCE_USERNAME
load_env_value SPRING_DATASOURCE_PASSWORD
load_env_value APP_VERSION

: "${SPRING_DATASOURCE_URL:?SPRING_DATASOURCE_URL is required}"
: "${SPRING_DATASOURCE_USERNAME:?SPRING_DATASOURCE_USERNAME is required}"
: "${SPRING_DATASOURCE_PASSWORD:?SPRING_DATASOURCE_PASSWORD is required}"

jdbc_target="${SPRING_DATASOURCE_URL#jdbc:mysql://}"
[[ "$jdbc_target" != "$SPRING_DATASOURCE_URL" ]] || fail "only jdbc:mysql URLs are supported"
db_authority="${jdbc_target%%/*}"
db_path="${jdbc_target#*/}"
db_name="${db_path%%\?*}"
db_host="${db_authority%%:*}"
db_port="3306"
if [[ "$db_authority" == *:* ]]; then
  db_port="${db_authority##*:}"
fi

[[ "$db_name" =~ ^[A-Za-z0-9_]+$ ]] || fail "unsafe database name parsed from JDBC URL"
[[ "$db_port" =~ ^[0-9]+$ ]] || fail "invalid database port parsed from JDBC URL"

mkdir -p -- "$BACKUP_DIR"
backup_root="$(cd "$BACKUP_DIR" && pwd -P)"
timestamp="$(date -u +'%Y%m%dT%H%M%SZ')"
version="${APP_VERSION:-unknown}"
version="${version//[^A-Za-z0-9._-]/_}"
backup_name="${db_name}_${timestamp}_${version}.sql.gz"
backup_file="${backup_root}/${backup_name}"
temporary_file="${backup_file}.tmp"

cleanup() {
  rm -f -- "$temporary_file"
  unset MYSQL_PWD || true
}
trap cleanup EXIT

export MYSQL_PWD="$SPRING_DATASOURCE_PASSWORD"
mysqldump \
  --host="$db_host" \
  --port="$db_port" \
  --user="$SPRING_DATASOURCE_USERNAME" \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --events \
  --hex-blob \
  --set-gtid-purged=OFF \
  --no-tablespaces \
  --default-character-set=utf8mb4 \
  --add-drop-database \
  --databases "$db_name" | gzip -9 > "$temporary_file"
unset MYSQL_PWD

gzip -t -- "$temporary_file"
mv -- "$temporary_file" "$backup_file"
(
  cd "$backup_root"
  sha256sum "$backup_name" > "${backup_name}.sha256"
)

if (( BACKUP_RETENTION_DAYS > 0 )); then
  find "$backup_root" -maxdepth 1 -type f \
    \( -name "${db_name}_*.sql.gz" -o -name "${db_name}_*.sql.gz.sha256" \) \
    -mtime "+${BACKUP_RETENTION_DAYS}" -delete
fi

echo "Backup created: $backup_file"
echo "Checksum: ${backup_file}.sha256"

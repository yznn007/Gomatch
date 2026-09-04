#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-${GOMATCH_ENV_FILE:-$PROJECT_ROOT/.env}}"
SQL_FILE="$PROJECT_ROOT/sql/init.sql"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "[ERROR] 找不到 .env：$ENV_FILE" >&2
    echo "        请先复制 .env.example 为 .env 并填写运行时配置。" >&2
    exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
    echo "[ERROR] 未找到 mysql 命令，请先安装 MySQL 客户端。" >&2
    exit 1
fi

dotenv_value() {
    local key="$1"
    awk -v key="$key" '
        /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
        {
            separator = index($0, "=")
            if (separator <= 0) next
            name = substr($0, 1, separator - 1)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
            if (name != key) next

            value = substr($0, separator + 1)
            sub(/^[[:space:]]+/, "", value)
            sub(/[[:space:]]+$/, "", value)
            if (length(value) >= 2 && substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"") {
                value = substr(value, 2, length(value) - 2)
            } else if (length(value) >= 2 && substr(value, 1, 1) == sprintf("%c", 39) && substr(value, length(value), 1) == sprintf("%c", 39)) {
                value = substr(value, 2, length(value) - 2)
            }
            print value
            exit
        }
    ' "$ENV_FILE"
}

DB_URL="$(dotenv_value GOMATCH_DB_URL)"
DB_USERNAME="$(dotenv_value GOMATCH_DB_USERNAME)"
DB_PASSWORD="$(dotenv_value GOMATCH_DB_PASSWORD)"
SEED_PASSWORD="$(dotenv_value GOMATCH_SEED_PASSWORD)"

if [[ "$DB_URL" != jdbc:mysql://* ]]; then
    echo "[ERROR] GOMATCH_DB_URL 必须使用 jdbc:mysql:// 格式。" >&2
    exit 1
fi

MYSQL_URL="${DB_URL#jdbc:mysql://}"
AUTHORITY="${MYSQL_URL%%/*}"
DB_PATH="${MYSQL_URL#*/}"
DB_NAME="${DB_PATH%%\?*}"

if [[ "$AUTHORITY" == *:* ]]; then
    DB_HOST="${AUTHORITY%%:*}"
    DB_PORT="${AUTHORITY##*:}"
else
    DB_HOST="$AUTHORITY"
    DB_PORT="3306"
fi

if [[ -z "$DB_HOST" || ! "$DB_HOST" =~ ^[A-Za-z0-9._-]+$ ]]; then
    echo "[ERROR] GOMATCH_DB_URL 中的主机名无效。" >&2
    exit 1
fi
if [[ -z "$DB_PORT" || ! "$DB_PORT" =~ ^[0-9]+$ ]]; then
    echo "[ERROR] GOMATCH_DB_URL 中的端口无效。" >&2
    exit 1
fi
if [[ -z "$DB_NAME" || ! "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "[ERROR] GOMATCH_DB_URL 中的数据库名无效。" >&2
    exit 1
fi
if [[ -z "$DB_USERNAME" || ! "$DB_USERNAME" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "[ERROR] GOMATCH_DB_USERNAME 只能包含字母、数字和下划线。" >&2
    exit 1
fi
if [[ -z "$DB_PASSWORD" ]]; then
    echo "[ERROR] GOMATCH_DB_PASSWORD 不能为空。" >&2
    exit 1
fi
if [[ -z "$SEED_PASSWORD" ]]; then
    echo "[ERROR] GOMATCH_SEED_PASSWORD 不能为空。" >&2
    exit 1
fi

sql_escape() {
    local value="$1"
    value="$(printf '%s' "$value" | sed "s/\\\\/\\\\\\\\/g; s/'/''/g")"
    printf '%s' "$value"
}

ESCAPED_PASSWORD="$(sql_escape "$DB_PASSWORD")"
ESCAPED_SEED_PASSWORD="$(sql_escape "$SEED_PASSWORD")"
SED_SEED_PASSWORD="$(printf '%s' "$ESCAPED_SEED_PASSWORD" | sed 's/[\/&]/\\&/g')"

echo "[INFO] 将初始化数据库 $DB_NAME，并配置本地应用账号 $DB_USERNAME。"
echo "[INFO] MySQL 客户端将提示输入 root 密码；应用密码只从 .env 读取。"

{
    sed "s/\${GOMATCH_SEED_PASSWORD}/$SED_SEED_PASSWORD/g" "$SQL_FILE"
    cat <<SQL

CREATE USER IF NOT EXISTS '$DB_USERNAME'@'localhost' IDENTIFIED BY '$ESCAPED_PASSWORD';
ALTER USER '$DB_USERNAME'@'localhost' IDENTIFIED BY '$ESCAPED_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'127.0.0.1' IDENTIFIED BY '$ESCAPED_PASSWORD';
ALTER USER '$DB_USERNAME'@'127.0.0.1' IDENTIFIED BY '$ESCAPED_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`$DB_NAME\`.* TO '$DB_USERNAME'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`$DB_NAME\`.* TO '$DB_USERNAME'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
} | mysql --protocol=TCP --host="$DB_HOST" --port="$DB_PORT" --user=root -p --default-character-set=utf8mb4

echo "[OK] 数据库初始化完成。"

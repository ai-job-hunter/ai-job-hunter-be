#!/usr/bin/env bash
# =============================================================================
# AI Job Hunter - 创建业务数据库
# 用途：在外部 PG 上（/opt/databases/）创建 job_hunter 库
# 触发：首次部署 / db.env 里 POSTGRES_DB 改名后
#
# 重要：CREATE DATABASE 不能在 PL/pgSQL 函数（DO 块）里跑，
#       也不能在事务块里跑，必须命令行顶层 autocommit。
#
# 用法：
#   bash 00-create-db.sh
#   PGSQL_DB=other_db bash 00-create-db.sh
# =============================================================================
set -euo pipefail

PG_HOST="${PGSQL_HOST:-localhost}"
PG_PORT="${PGSQL_PORT:-5432}"
PG_SUPER_USER="${PGSQL_SUPER_USER:-postgres}"
PG_DB="${PGSQL_DB:-job_hunter}"
# 默认连 postgres 库执行 CREATE DATABASE（不能连目标库再去 CREATE 同库）
PG_BOOT_DB="${PGSQL_BOOT_DB:-postgres}"
PG_PASSWORD_ENV="${PGPASSWORD:-}"

# 找一个 psql
PSQL_BIN="$(command -v psql || true)"
if [ -z "${PSQL_BIN}" ]; then
    echo "ERROR: psql not found. 安装 PostgreSQL client 后再跑。" >&2
    exit 1
fi

export PGHOST="${PG_HOST}"
export PGPORT="${PG_PORT}"
export PGUSER="${PG_SUPER_USER}"
export PGPASSWORD="${PG_PASSWORD_ENV}"

echo ">>> 目标: ${PG_SUPER_USER}@${PG_HOST}:${PG_PORT}/${PG_DB}"
echo ">>> 执行 CREATE DATABASE（绕过 PL/pgSQL 函数限制）"

# 1. 查库是否已建
EXISTS=$("${PSQL_BIN}" -d "${PG_BOOT_DB}" -tAc \
    "SELECT 1 FROM pg_database WHERE datname='${PG_DB}'" || true)

if [ "${EXISTS}" = "1" ]; then
    echo ">>> 数据库 ${PG_DB} 已存在，跳过 CREATE DATABASE"
else
    # 2. CREATE DATABASE（必须在 autocommit 顶层执行，不能塞 DO $$ 里）
    "${PSQL_BIN}" -d "${PG_BOOT_DB}" -c "
        CREATE DATABASE \"${PG_DB}\"
            WITH OWNER = \"${PG_SUPER_USER}\"
            ENCODING = 'UTF8'
            LC_COLLATE = 'en_US.utf8'
            LC_CTYPE   = 'en_US.utf8'
            TEMPLATE   = template0;
    "
    echo ">>> 数据库 ${PG_DB} 已创建"
fi

# 3. 验证
"${PSQL_BIN}" -d "${PG_BOOT_DB}" -tAc \
    "SELECT datname, pg_size_pretty(pg_database_size(datname)) AS size
     FROM pg_database WHERE datname='${PG_DB}'"

echo ""
echo "✅ 下一步：跑 01-init-schema.sql 在 ${PG_DB} 库内装扩展和建 schema"
echo "   psql -h ${PG_HOST} -p ${PG_PORT} -U ${PG_SUPER_USER} -d ${PG_DB} -f 01-init-schema.sql"

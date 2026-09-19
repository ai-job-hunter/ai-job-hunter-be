#!/usr/bin/env bash
# =============================================================================
# AI Job Hunter - 开发期重置脚本
# 用途：清空 job_hunter 库，重建 app schema，让 Hibernate 重新建表
# 警告：会 DROP 所有表（含 public 里残留的），仅用于开发期
# 用法：bash scripts/dev-reset-db.sh
# =============================================================================
set -euo pipefail

DB_CONTAINER="${DB_CONTAINER:-db-postgres}"
PG_USER="${PGSQL_USERNAME:-postgres}"
PG_DB="${PGSQL_DB:-job_hunter}"

echo ">>> 将清空数据库 ${PG_DB}（容器 ${DB_CONTAINER}），所有数据会丢失"
read -r -p "确认继续? [y/N] " ans
[[ "${ans}" == "y" ]] || { echo "已取消"; exit 1; }

echo ">>> 1. 杀掉当前连接到 ${PG_DB} 的会话"
docker exec "${DB_CONTAINER}" psql -U "${PG_USER}" -d postgres -c "
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '${PG_DB}' AND pid <> pg_backend_pid();
" >/dev/null

echo ">>> 2. DROP 并重建数据库"
docker exec "${DB_CONTAINER}" psql -U "${PG_USER}" -d postgres -c "
DROP DATABASE IF EXISTS ${PG_DB};
CREATE DATABASE ${PG_DB}
    WITH OWNER = ${PG_USER}
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TEMPLATE = template0;
" >/dev/null

echo ">>> 3. 装扩展 + 建 app schema"
docker exec "${DB_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DB}" <<'SQL' >/dev/null
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION postgres;
SQL

echo ">>> 4. 验证"
docker exec "${DB_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DB}" -c "
SELECT current_database() AS db,
       current_schema()   AS schema,
       (SELECT string_agg(extname, ', ' ORDER BY extname)
        FROM pg_extension
        WHERE extname IN ('vector','uuid-ossp','pg_trgm')) AS extensions;
"

echo ""
echo "✅ 完成。下次 BE 容器启动时，Hibernate 会在 app schema 自动建表。"

#!/usr/bin/env bash
# =============================================================================
# AI Job Hunter - 开发期重置脚本
# 用途：清空 job_hunter 库，重建 app schema，让 Hibernate 重新建表
# 警告：会 DROP 所有表（含 public 里残留的），仅用于开发期
#
# 用法：
#   bash scripts/dev-reset-db.sh
#   DB_CONTAINER=aijobhunter-db bash scripts/dev-reset-db.sh
#
# 前置：
#   - 已起本地 PG 容器（ai-job-hunter-be/docker-compose.dev.yml 里的 postgres 服务）
#   - 若使用默认 postgres:18-alpine，「vector」扩展示例数据按镜像自带版本创建；
#     如使用 pgvector/pgvector 镜像可创建真 vector 扩展
# =============================================================================
set -euo pipefail

DB_CONTAINER="${DB_CONTAINER:-aijobhunter-db}"
PG_USER="${PGSQL_USERNAME:-postgres}"
PG_DB="${PGSQL_DB:-job_hunter}"

# 容器内设 PGPASSWORD（避免交互式密码；本地默认 postgres/postgres）
export PGPASSWORD="${PGSQL_PASSWORD:-postgres}"

echo ">>> 容器: ${DB_CONTAINER}  用户: ${PG_USER}  库: ${PG_DB}"
echo ">>> 将清空数据库 ${PG_DB}，所有数据会丢失"
read -r -p "确认继续? [y/N] " ans
[[ "${ans}" == "y" ]] || { echo "已取消"; exit 1; }

# 校验容器在跑
if ! docker ps --format '{{.Names}}' | grep -qx "${DB_CONTAINER}"; then
    echo "ERROR: 容器 ${DB_CONTAINER} 未运行。请先启动 docker compose dev stack。" >&2
    exit 1
fi

echo ">>> 1. 杀掉当前连接到 ${PG_DB} 的会话"
docker exec -e PGPASSWORD "${DB_CONTAINER}" \
    psql -U "${PG_USER}" -d postgres -c "
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '${PG_DB}' AND pid <> pg_backend_pid();
" >/dev/null

echo ">>> 2. DROP 并重建数据库"
docker exec -e PGPASSWORD "${DB_CONTAINER}" \
    psql -U "${PG_USER}" -d postgres -c "
DROP DATABASE IF EXISTS ${PG_DB};
CREATE DATABASE ${PG_DB}
    WITH OWNER = ${PG_USER}
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE   = 'en_US.utf8'
    TEMPLATE   = template0;
" >/dev/null

echo ">>> 3. 装扩展 + 建 app schema"
docker exec -e PGPASSWORD "${DB_CONTAINER}" \
    psql -U "${PG_USER}" -d "${PG_DB}" <<'SQL' >/dev/null
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- vector/pg_trgm 在 pgvector 镜像里才有；标准 postgres 镜像会失败，单条容错
DO $$
BEGIN
    BEGIN
        EXECUTE 'CREATE EXTENSION IF NOT EXISTS "vector"';
    EXCEPTION WHEN feature_not_supported OR undefined_file THEN
        RAISE NOTICE 'vector 扩展不可用（需使用 pgvector 镜像），跳过';
    END;
    BEGIN
        EXECUTE 'CREATE EXTENSION IF NOT EXISTS "pg_trgm"';
    EXCEPTION WHEN feature_not_supported OR undefined_file THEN
        RAISE NOTICE 'pg_trgm 扩展不可用，跳过';
    END;
END
$$;
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION postgres;
SQL

echo ">>> 4. 验证"
docker exec -e PGPASSWORD "${DB_CONTAINER}" \
    psql -U "${PG_USER}" -d "${PG_DB}" -c "
SELECT current_database() AS db,
       current_schema()   AS schema,
       (SELECT string_agg(extname, ', ' ORDER BY extname)
        FROM pg_extension) AS installed_extensions;
"

echo ""
echo "✅ 完成。下次 BE 容器启动时，Hibernate 会在 app schema 自动建表。"

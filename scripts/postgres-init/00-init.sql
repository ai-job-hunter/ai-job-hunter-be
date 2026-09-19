-- =============================================================================
-- AI Job Hunter - PostgreSQL 一键初始化脚本（纯 SQL，复制粘贴执行）
-- =============================================================================
-- 执行步骤：
--
--   【第 1 步】在 postgres 库执行（创建业务库）
--   用法：PGPASSWORD='<your_pg_password>' \
--         psql -h <pg_host> -p 5432 -U postgres -d postgres
--   整段粘贴 ↓
--
--   CREATE DATABASE job_hunter
--       WITH OWNER = postgres
--       ENCODING = 'UTF8'
--       LC_COLLATE = 'en_US.utf8'
--       LC_CTYPE   = 'en_US.utf8'
--       TEMPLATE   = template0;
--
-- 跑成功后再做第 2 步（必须先建库才能连进去）
--
--   【第 2 步】切到 job_hunter 库执行（建扩展 + schema）
--   用法：PGPASSWORD='<your_pg_password>' \
--         psql -h <pg_host> -p 5432 -U postgres -d job_hunter
--   整段粘贴 ↓
--
-- =============================================================================


-- ========================== 第 1 段：在 postgres 库执行 =====================

CREATE DATABASE job_hunter
    WITH OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE   = 'en_US.utf8'
    TEMPLATE   = template0;


-- ========================== 第 2 段：切到 job_hunter 库执行 ==================
-- 切换方式：\c job_hunter  或  psql -d job_hunter 重连
-- =============================================================================

-- 必装扩展（与 docs/04-ER-DESIGN.md §7.1 对齐）
-- pgvector 由专用镜像 pgvector/pgvector:0.8.6-pg18-bookworm 自带，
-- CREATE EXTENSION IF NOT EXISTS 是幂等的；
-- 标准 postgres:18-alpine 镜像无 vector，二次执行会报 feature_not_supported，
-- 加 DO 块容错让脚本在两种镜像都能跑通
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'uuid-ossp 扩展创建失败: %', SQLERRM;
END
$$;

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS "vector";
EXCEPTION WHEN feature_not_supported OR undefined_file THEN
    RAISE NOTICE 'vector 扩展不可用（需 pgvector/pgvector 镜像），跳过';
WHEN OTHERS THEN
    RAISE NOTICE 'vector 扩展创建失败: %', SQLERRM;
END
$$;

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'pg_trgm 扩展创建失败: %', SQLERRM;
END
$$;

-- 业务用 schema（与 cogniforge 一致：业务表放 app schema，避免与 Flyway 历史表混杂）
-- 配套 application.yml: spring.jpa.properties.hibernate.default_schema: app
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION postgres;

-- 默认权限（postgres 用户已经是 owner，省去 GRANT；
-- 后续若拆业务用户，把下面取消注释）
-- GRANT ALL ON SCHEMA app TO business_user;
-- GRANT ALL ON ALL TABLES IN SCHEMA app TO business_user;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA app GRANT ALL ON TABLES TO business_user;

-- 自检：扩展 + schema 是否就绪
SELECT current_database() AS db,
       current_user      AS usr,
       current_schema()  AS default_schema,
       (SELECT string_agg(extname, ', ' ORDER BY extname)
        FROM pg_extension
        WHERE extname IN ('uuid-ossp','vector','pg_trgm')) AS installed_extensions,
       (SELECT nspname FROM pg_namespace WHERE nspname='app') AS app_schema;

-- 期望输出（dev 镜像无 vector 时，vector 那列会少）：
--      db         |   usr     | default_schema |     installed_extensions     | app_schema
-- ---------------+-----------+----------------+------------------------------+------------
--  job_hunter    | postgres  | app            | pg_trgm, uuid-ossp, vector   | app

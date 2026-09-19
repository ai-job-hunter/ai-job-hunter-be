-- =============================================================================
-- AI Job Hunter - PostgreSQL 初始化脚本
-- 部署位置（容器首次启动）：/docker-entrypoint-initdb.d/00-init.sql
-- 已有库场景：在服务器上用 psql 手动执行
-- 执行角色：postgres（超级用户），执行完成后切到业务库
--
-- 命名约定：
--   - 库名：{{PGSQL_DB}}           默认 job_hunter
--   - 用户：{{PGSQL_USERNAME}}     默认 postgres（与公用 PG 一致，不另建用户）
--   - 扩展：uuid-ossp + vector（pgvector 由镜像自带）
--
-- 与 cogniforge 工程统一一套环境变量命名（见 .env.example）
-- =============================================================================

-- 0. 防御性检查：避免重跑脚本时重复创建
--    （IF NOT EXISTS 让重复执行幂等）
DO $$
BEGIN
    -- 数据库不存在则创建（postgres 镜像默认已建 POSTGRES_DB，
    -- 本脚本兜底处理「旧库被删、新库没建」的场景）
    IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'job_hunter') THEN
        CREATE DATABASE job_hunter
            WITH OWNER = postgres
            ENCODING = 'UTF8'
            LC_COLLATE = 'en_US.utf8'
            LC_CTYPE = 'en_US.utf8'
            TEMPLATE = template0;
        RAISE NOTICE 'Database job_hunter created.';
    ELSE
        RAISE NOTICE 'Database job_hunter already exists, skip CREATE DATABASE.';
    END IF;
END
$$;

-- 1. 切到 job_hunter 库
\connect job_hunter

-- 2. 必装扩展（与 docs/04-ER-DESIGN.md §7.1 对齐，
--    pgvector 由镜像 pgvector/pgvector:0.8.6-pg18-bookworm 自带，
--    CREATE EXTENSION 只会激活、不重复安装）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";   -- 模糊匹配（职位标题/公司名搜索）

-- 3. 业务用 schema（与 cogniforge 一致：业务表放 app schema，避免与 Flyway 历史表混杂）
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION postgres;

-- 4. 默认权限（postgres 镜像下业务用户就是 postgres，省去 GRANT；
--    若以后拆业务用户，这里补 GRANT 段）
-- GRANT ALL ON SCHEMA app TO postgres;
-- GRANT ALL ON ALL TABLES IN SCHEMA app TO postgres;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA app GRANT ALL ON TABLES TO postgres;

-- 5. 完成
SELECT current_database() AS db, current_user AS usr, version() AS pg_version;

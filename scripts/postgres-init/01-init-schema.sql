-- =============================================================================
-- AI Job Hunter - 业务库初始化（在 job_hunter 库里跑）
--
-- 执行环境：外部 PostgreSQL（/opt/databases/）
-- 执行角色：postgres（超级用户）
-- 启动方式：
--   ① 容器初始化：挂到 /docker-entrypoint-initdb.d/，但当前部署用外部 PG，
--      实际不会触发；本路径在 dev 模式有效
--   ② 手动执行：psql -h <host> -U postgres -d job_hunter -f 01-init-schema.sql
--
-- 依赖：需先建 job_hunter 库（用 00-create-db.sh）
--
-- 与 cogniforge 工程统一一套环境变量命名（见 .env.example）
-- =============================================================================

-- 1. 必装扩展
--    pgvector 由专用镜像 pgvector/pgvector:0.8.6-pg18-bookworm 自带，
--    CREATE EXTENSION IF NOT EXISTS 只是激活、二次执行幂等
--    参考 docs/04-ER-DESIGN.md §7.1
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";    -- UUID 生成（主键策略）
CREATE EXTENSION IF NOT EXISTS "vector";       -- 向量字段（JD/简历 embedding）
CREATE EXTENSION IF NOT EXISTS "pg_trgm";      -- 三元组模糊匹配（职位标题/公司名搜索）

-- 2. 业务用 schema（与 cogniforge 一致）
--    业务表统一放 app schema，与 public 隔离
--    配套 application.yml 里 spring.jpa.properties.hibernate.default_schema: app
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION postgres;

-- 3. 默认权限（postgres 镜像下业务用户就是 postgres，省去 GRANT；
--    后续若拆业务用户，下面取消注释）
-- GRANT ALL ON SCHEMA app TO business_user;
-- GRANT ALL ON ALL TABLES IN SCHEMA app TO business_user;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA app GRANT ALL ON TABLES TO business_user;

-- 4. 完成自检
SELECT current_database() AS db,
       current_user      AS usr,
       current_schemas(true) AS search_path_schemas;

-- 5. 验证扩展 + schema 是否就绪
SELECT 'extension:vector'    AS item, extversion AS ok FROM pg_extension WHERE extname='vector'
UNION ALL
SELECT 'extension:uuid-ossp', extversion FROM pg_extension WHERE extname='uuid-ossp'
UNION ALL
SELECT 'extension:pg_trgm',   extversion FROM pg_extension WHERE extname='pg_trgm'
UNION ALL
SELECT 'schema:app',          nspname FROM pg_namespace WHERE nspname='app'
ORDER BY 1;

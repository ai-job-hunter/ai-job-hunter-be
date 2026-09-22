-- 测试用 schema（H2 内存数据库）
-- 应用配置里 spring.jpa.properties.hibernate.default_schema=app
-- H2 需要显式建出 app schema，否则会报 Schema "APP" not found
CREATE SCHEMA IF NOT EXISTS app;

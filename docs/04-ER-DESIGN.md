# AI Job Hunter - 数据库设计

> 版本：V1.0
> 日期：2026-09-02
> 状态：草稿
> 负责人：待定

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|-----|
| 2026-09-02 | v1.0 | 初始版本 | - |

---

# 一、设计原则

## 1.1 核心原则

```
业务优先 → 适度冗余 → 便于查询
```

## 1.2 数据类型选择

| 场景 | 选择 |
|-----|------|
| 固定结构数据 | 关系字段（VARCHAR、INTEGER） |
| 灵活结构数据 | JSONB |
| 长文本数据 | TEXT |
| 需要全文搜索 | Full Text Index |
| 语义相似度搜索 | pgvector |
| 敏感数据 | 加密存储（AES） |
| 大文件 | 不存数据库，只存路径 |

## 1.3 索引策略

| 场景 | 索引类型 |
|-----|---------|
| 主键 | btree (默认) |
| 唯一约束 | unique btree |
| 状态查询 | btree + partial index |
| 全文搜索 | gin / gist |
| 地理查询 | gist (城市) |

---

# 二、数据库结构

## 2.1 Schema 划分

```
ai_job_hunter
├── public           # 公共表
├── resume           # 简历相关
├── job              # 岗位相关
├── chat             # 沟通相关
├── application      # 投递相关
├── strategy         # 策略相关
└── agent            # AI Agent 相关
```

> V1 暂不启用 Schema 隔离，所有表放 public schema。

---

# 三、表结构设计

## 3.1 用户表 (users)

```sql
CREATE TABLE users (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 基础信息
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    
    -- BOSS 账号（加密存储）
    boss_cookie TEXT,                          -- 加密存储
    boss_token VARCHAR(255),                   -- 加密存储
    boss_connected BOOLEAN DEFAULT FALSE,
    boss_connected_at TIMESTAMP,
    
    -- 求职偏好（JSONB）
    preferences JSONB DEFAULT '{}',
    -- 结构示例：
    -- {
    --   "keywords": ["Java", "后端"],
    --   "cities": ["北京", "上海"],
    --   "salary_min": 20000,
    --   "salary_max": 40000
    -- }
    
    -- 自动化配置（JSONB）
    automation_config JSONB DEFAULT '{
        "mode": "semi",
        "daily_greet_limit": 50,
        "daily_apply_limit": 20,
        "reply_delay_seconds": 2,
        "auto_apply": true
    }',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_active_at TIMESTAMP,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- 约束
ALTER TABLE users ADD CONSTRAINT chk_users_status 
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'));
```

## 3.2 简历表 (resumes)

```sql
CREATE TABLE resumes (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联用户
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- 简历基本信息
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) DEFAULT 'GENERAL',
    is_default BOOLEAN DEFAULT FALSE,
    
    -- 解析后的简历数据（JSONB）
    parsed_data JSONB DEFAULT '{}',
    -- 结构示例：
    -- {
    --   "personal_info": { "name": "...", "phone": "...", "email": "..." },
    --   "summary": "...",
    --   "skills": ["Java", "Spring", "MySQL"],
    --   "experience": [...],
    --   "education": [...],
    --   "projects": [...]
    -- }
    
    -- 文件信息（JSONB）
    file_info JSONB DEFAULT '{}',
    -- {
    --   "original_name": "我的简历.pdf",
    --   "storage_path": "/data/resumes/xxx.pdf",
    --   "mime_type": "application/pdf",
    --   "size": 1024000
    -- }
    
    -- AI 分析（JSONB）
    analysis JSONB DEFAULT '{}',
    -- {
    --   "strengths": [...],
    --   "weaknesses": [...],
    --   "suggestions": [...],
    --   "skills": [...],
    --   "model": "gpt-4o",
    --   "tokens_used": 1500
    -- }
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_resumes_user_default ON resumes(user_id, is_default) WHERE is_default = TRUE;
CREATE INDEX idx_resumes_type ON resumes(type);
CREATE INDEX idx_resumes_parsed_data ON resumes USING GIN (parsed_data);
```

## 3.3 简历版本表 (resume_versions)

```sql
CREATE TABLE resume_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    
    version_number INTEGER NOT NULL,
    
    -- 版本内容快照（JSONB）
    content JSONB NOT NULL DEFAULT '{}',
    
    -- 版本信息
    source VARCHAR(20) DEFAULT 'ORIGINAL',  -- ORIGINAL, AUTO_REWRITE, MANUAL_EDIT, TEMPLATE
    change_note TEXT,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 唯一约束：同一简历的版本号唯一
    UNIQUE(resume_id, version_number)
);

-- 索引
CREATE INDEX idx_resume_versions_resume_id ON resume_versions(resume_id);
CREATE INDEX idx_resume_versions_created ON resume_versions(created_at);
```

## 3.4 岗位表 (jobs)

```sql
CREATE TABLE jobs (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联用户
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- BOSS 平台信息
    boss_job_id VARCHAR(100),
    boss_company_id VARCHAR(100),
    source VARCHAR(20) DEFAULT 'BOSS',
    
    -- 岗位基本信息
    title VARCHAR(200) NOT NULL,
    company_name VARCHAR(200),
    city VARCHAR(50),
    district VARCHAR(50),
    salary_min VARCHAR(20),
    salary_max VARCHAR(20),
    salary_text VARCHAR(50),              -- 如：20k-35k·13薪
    
    -- JD 原始文本
    raw_description TEXT,
    
    -- AI 分析结果（JSONB）
    analysis JSONB DEFAULT '{}',
    -- {
    --   "skills": ["Java", "Spring Boot", "MySQL"],
    --   "responsibilities": [...],
    --   "requirements": [...],
    --   "highlights": [...],
    --   "min_experience_years": 3,
    --   "education": "本科"
    -- }
    
    keywords TEXT[],                      -- PostgreSQL 数组类型
    match_scores JSONB DEFAULT '{}',      -- 各简历的匹配度
    
    -- 关联公司
    company_id UUID REFERENCES companies(id),
    
    -- 状态
    status VARCHAR(30) DEFAULT 'NEW',
    
    -- 来源信息
    source_url TEXT,
    discovered_at TIMESTAMP,
    last_synced_at TIMESTAMP,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_jobs_user_id ON jobs(user_id);
CREATE INDEX idx_jobs_user_status ON jobs(user_id, status);
CREATE INDEX idx_jobs_boss_job_id ON jobs(boss_job_id);
CREATE INDEX idx_jobs_title ON jobs(title);
CREATE INDEX idx_jobs_city ON jobs(city);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_discovered ON jobs(discovered_at DESC);
CREATE INDEX idx_jobs_raw_description ON jobs USING GIN (to_tsvector('chinese', raw_description));
CREATE INDEX idx_jobs_keywords ON jobs USING GIN (keywords);

-- 约束
ALTER TABLE jobs ADD CONSTRAINT chk_jobs_status 
    CHECK (status IN (
        'NEW', 'ANALYZED', 'GREETED', 'REPLIED', 
        'APPLIED', 'INTERVIEWING', 'OFFER', 
        'REJECTED', 'EXPIRED', 'BLACKLISTED'
    ));
```

## 3.5 公司表 (companies)

```sql
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- BOSS 平台信息
    boss_company_id VARCHAR(100) UNIQUE,
    
    -- 公司基本信息
    name VARCHAR(200) NOT NULL,
    logo VARCHAR(500),
    industry VARCHAR(100),
    scale VARCHAR(50),                   -- 人数规模
    stage VARCHAR(50),                    -- 融资阶段
    description TEXT,
    
    -- 认证信息
    verified BOOLEAN DEFAULT FALSE,
    boss_direct BOOLEAN DEFAULT FALSE,
    
    -- 黑名单
    is_blacklisted BOOLEAN DEFAULT FALSE,
    
    -- 统计
    total_jobs INTEGER DEFAULT 0,
    active_jobs INTEGER DEFAULT 0,
    
    -- 时间
    first_seen_at TIMESTAMP,
    last_seen_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_companies_name ON companies(name);
CREATE INDEX idx_companies_industry ON companies(industry);
CREATE INDEX idx_companies_blacklisted ON companies(is_blacklisted) WHERE is_blacklisted = TRUE;
```

## 3.6 对话表 (conversations)

```sql
CREATE TABLE conversations (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联用户
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- 关联岗位
    job_id UUID REFERENCES jobs(id) ON DELETE SET NULL,
    
    -- BOSS 平台信息
    boss_conversation_id VARCHAR(100),
    boss_hr_id VARCHAR(100),
    hr_name VARCHAR(100),
    hr_avatar VARCHAR(500),
    
    -- 状态
    status VARCHAR(30) DEFAULT 'GREETING',
    
    -- 消息统计
    total_messages INTEGER DEFAULT 0,
    unread_count INTEGER DEFAULT 0,
    
    -- 最后一条消息（JSONB）
    last_message JSONB DEFAULT '{}',
    -- {
    --   "content": "最后一条消息内容",
    --   "direction": "INBOUND",
    --   "sent_at": "2024-01-01T10:00:00Z"
    -- }
    
    -- 打招呼信息
    greeting_sent BOOLEAN DEFAULT FALSE,
    greeting_text TEXT,
    greeted_at TIMESTAMP,
    
    -- 最新消息时间
    last_message_at TIMESTAMP,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_user_status ON conversations(user_id, status);
CREATE INDEX idx_conversations_job_id ON conversations(job_id);
CREATE INDEX idx_conversations_boss_id ON conversations(boss_conversation_id);
CREATE INDEX idx_conversations_last_message ON conversations(last_message_at DESC NULLS LAST);
CREATE INDEX idx_conversations_unread ON conversations(user_id, unread_count) WHERE unread_count > 0;
```

## 3.7 消息表 (messages)

```sql
CREATE TABLE messages (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联对话
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    
    -- 消息方向
    direction VARCHAR(20) NOT NULL,      -- INBOUND / OUTBOUND
    
    -- 消息内容
    content TEXT NOT NULL,
    
    -- 附件（JSONB）
    attachments JSONB DEFAULT '[]',
    
    -- AI 元数据（JSONB）
    ai_metadata JSONB DEFAULT '{}',
    -- {
    --   "agent_type": "GREET",
    --   "model": "gpt-4o",
    --   "confidence": 0.92,
    --   "execution_id": "xxx"
    -- }
    
    -- HR 意图分析（JSONB）
    hr_intent JSONB DEFAULT '{}',
    -- {
    --   "type": "SALARY_INQUIRY",
    --   "confidence": 0.88,
    --   "entities": {...},
    --   "suggested_reply": "..."
    -- }
    
    -- 发送状态
    status VARCHAR(20) DEFAULT 'SENT',
    
    -- 时间
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_conversation_time ON messages(conversation_id, sent_at DESC);
CREATE INDEX idx_messages_direction ON messages(direction);
CREATE INDEX idx_messages_status ON messages(status);
```

## 3.8 打招呼模板表 (greeting_templates)

```sql
CREATE TABLE greeting_templates (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联用户
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- 模板信息
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(30) DEFAULT 'GREETING',
    
    -- 模板内容
    content TEXT NOT NULL,
    -- 支持变量：{{job_title}}, {{company_name}}, {{skills}}, {{salary}}
    
    -- 适用条件（JSONB）
    conditions JSONB DEFAULT '{}',
    -- {
    --   "job_titles": ["Java工程师", "后端开发"],
    --   "exclude_keywords": ["销售", "保险"]
    -- }
    
    -- 使用统计
    use_count INTEGER DEFAULT 0,
    avg_response_rate DECIMAL(5,2) DEFAULT 0,
    
    -- 状态
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_greeting_templates_user_id ON greeting_templates(user_id);
CREATE INDEX idx_greeting_templates_user_active ON greeting_templates(user_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_greeting_templates_type ON greeting_templates(type);
```

## 3.9 投递记录表 (applications)

```sql
CREATE TABLE applications (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联用户
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- 关联岗位
    job_id UUID REFERENCES jobs(id) ON DELETE SET NULL,
    
    -- 关联对话
    conversation_id UUID REFERENCES conversations(id) ON DELETE SET NULL,
    
    -- 投递的简历
    resume_id UUID REFERENCES resumes(id),
    
    -- BOSS 平台信息
    boss_application_id VARCHAR(100),
    
    -- 投递信息
    applied_at TIMESTAMP,
    apply_note TEXT,
    
    -- 匹配信息
    match_score INTEGER,
    matched_skills TEXT[],
    missing_skills TEXT[],
    
    -- 投递状态
    status VARCHAR(30) DEFAULT 'PENDING',
    
    -- HR 反馈（JSONB）
    hr_feedback JSONB DEFAULT '{}',
    
    -- 面试安排（JSONB）
    interview_schedule JSONB DEFAULT '{}',
    -- {
    --   "interview_type": "视频面试",
    --   "scheduled_at": "2024-01-15T14:00:00Z",
    --   "meeting_link": "..."
    -- }
    
    -- 时间线
    replied_at TIMESTAMP,
    interviewed_at TIMESTAMP,
    offered_at TIMESTAMP,
    closed_at TIMESTAMP,
    closed_reason TEXT,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_user_status ON applications(user_id, status);
CREATE INDEX idx_applications_job_id ON applications(job_id);
CREATE INDEX idx_applications_resume_id ON applications(resume_id);
CREATE INDEX idx_applications_applied_at ON applications(applied_at DESC);
CREATE INDEX idx_applications_status ON applications(status);
```

## 3.10 投递时间线表 (application_timelines)

```sql
CREATE TABLE application_timelines (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联投递
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    
    -- 事件类型
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(200),
    description TEXT,
    
    -- 额外数据（JSONB）
    metadata JSONB DEFAULT '{}',
    
    -- 时间
    occurred_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_timelines_application_id ON application_timelines(application_id);
CREATE INDEX idx_timelines_event_type ON application_timelines(event_type);
CREATE INDEX idx_timelines_occurred ON application_timelines(occurred_at DESC);
```

## 3.11 黑名单表 (blacklists)

```sql
CREATE TABLE blacklists (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联用户
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- 黑名单类型
    type VARCHAR(30) NOT NULL,         -- COMPANY, JOB_TITLE, KEYWORD, HR
    
    -- 黑名单值
    value VARCHAR(200) NOT NULL,
    
    -- 原因和备注
    reason TEXT,
    note TEXT,
    
    -- 有效期
    expires_at TIMESTAMP,
    
    -- 审计字段
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_blacklists_user_id ON blacklists(user_id);
CREATE INDEX idx_blacklists_user_type ON blacklists(user_id, type);
CREATE INDEX idx_blacklists_value ON blacklists(value);
CREATE INDEX idx_blacklists_expires ON blacklists(expires_at) WHERE expires_at IS NOT NULL;
```

## 3.12 AI 执行记录表 (agent_executions)

```sql
CREATE TABLE agent_executions (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联用户
    user_id UUID REFERENCES users(id),
    
    -- Agent 类型
    agent_type VARCHAR(50) NOT NULL,
    
    -- Prompt 信息
    prompt_template_id UUID,
    prompt_version INTEGER,
    
    -- 模型信息
    model VARCHAR(50),
    temperature DECIMAL(3,2),
    top_p DECIMAL(3,2),
    
    -- 输入输出（JSONB）
    input JSONB NOT NULL DEFAULT '{}',
    output JSONB NOT NULL DEFAULT '{}',
    
    -- Token 消耗
    prompt_tokens INTEGER DEFAULT 0,
    completion_tokens INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    cost DECIMAL(10,4) DEFAULT 0,
    
    -- 性能
    latency_ms BIGINT DEFAULT 0,
    
    -- 状态
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error TEXT,
    stack_trace TEXT,
    
    -- 关联业务（可选）
    job_id UUID REFERENCES jobs(id),
    resume_id UUID REFERENCES resumes(id),
    conversation_id UUID REFERENCES conversations(id),
    
    -- 时间
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_agent_executions_user_id ON agent_executions(user_id);
CREATE INDEX idx_agent_executions_agent_type ON agent_executions(agent_type);
CREATE INDEX idx_agent_executions_user_time ON agent_executions(user_id, created_at DESC);
CREATE INDEX idx_agent_executions_status ON agent_executions(status);
CREATE INDEX idx_agent_executions_created ON agent_executions(created_at DESC);
CREATE INDEX idx_agent_executions_cost ON agent_executions(user_id, total_tokens) WHERE created_at > CURRENT_DATE - INTERVAL '30 days';
```

## 3.13 Prompt 模板表 (prompt_templates)

```sql
CREATE TABLE prompt_templates (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Agent 类型
    agent_type VARCHAR(50) NOT NULL,
    
    -- 模板信息
    name VARCHAR(100) NOT NULL,
    description TEXT,
    version INTEGER DEFAULT 1,
    
    -- 模板内容
    system_prompt TEXT,
    user_prompt_template TEXT NOT NULL,
    
    -- 变量定义（JSONB）
    variables JSONB DEFAULT '[]',
    -- [{
    --   "name": "job_title",
    --   "type": "string",
    --   "required": true
    -- }]
    
    -- 模型配置
    model VARCHAR(50) DEFAULT 'gpt-4o',
    temperature DECIMAL(3,2) DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 2000,
    
    -- 效果统计
    use_count INTEGER DEFAULT 0,
    avg_rating DECIMAL(3,2),
    success_rate DECIMAL(5,2),
    
    -- 状态
    is_active BOOLEAN DEFAULT TRUE,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_prompt_templates_agent_type ON prompt_templates(agent_type);
CREATE INDEX idx_prompt_templates_active ON prompt_templates(agent_type, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_prompt_templates_use_count ON prompt_templates(use_count DESC);
```

## 3.14 每日统计表 (daily_stats)

```sql
CREATE TABLE daily_stats (
    -- 主键
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 关联用户
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- 统计日期
    date DATE NOT NULL,
    
    -- 数量统计
    jobs_discovered INTEGER DEFAULT 0,
    greetings_sent INTEGER DEFAULT 0,
    greetings_replied INTEGER DEFAULT 0,
    applications_sent INTEGER DEFAULT 0,
    interviews_scheduled INTEGER DEFAULT 0,
    
    -- 比率
    reply_rate DECIMAL(5,2) DEFAULT 0,
    interview_rate DECIMAL(5,2) DEFAULT 0,
    
    -- Token 消耗
    tokens_used INTEGER DEFAULT 0,
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 唯一约束
    UNIQUE(user_id, date)
);

-- 索引
CREATE INDEX idx_daily_stats_user_date ON daily_stats(user_id, date DESC);
CREATE INDEX idx_daily_stats_tokens ON daily_stats(user_id, tokens_used) WHERE date > CURRENT_DATE - INTERVAL '30 days';
```

---

# 四、辅助表

## 4.1 枚举值参考表 (enum_values) - 可选

V1 暂不创建枚举表，使用代码枚举。

---

# 五、触发器和函数

## 5.1 更新时间戳触发器

```sql
-- 自动更新 updated_at 字段
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为需要自动更新的表创建触发器
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_resumes_updated_at
    BEFORE UPDATE ON resumes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_jobs_updated_at
    BEFORE UPDATE ON jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_applications_updated_at
    BEFORE UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

## 5.2 投递统计更新函数

```sql
-- 更新每日统计
CREATE OR REPLACE FUNCTION update_daily_stats(
    p_user_id UUID,
    p_date DATE,
    p_stat_type VARCHAR(50),
    p_increment INTEGER DEFAULT 1
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO daily_stats (user_id, date, jobs_discovered, greetings_sent, applications_sent)
    VALUES (p_user_id, p_date, 0, 0, 0)
    ON CONFLICT (user_id, date) DO UPDATE
    SET 
        jobs_discovered = daily_stats.jobs_discovered + 
            CASE WHEN p_stat_type = 'JOBS_DISCOVERED' THEN p_increment ELSE 0 END,
        greetings_sent = daily_stats.greetings_sent + 
            CASE WHEN p_stat_type = 'GREETINGS_SENT' THEN p_increment ELSE 0 END,
        applications_sent = daily_stats.applications_sent + 
            CASE WHEN p_stat_type = 'APPLICATIONS_SENT' THEN p_increment ELSE 0 END,
        updated_at = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;
```

---

# 六、视图

## 6.1 投递状态统计视图

```sql
CREATE OR REPLACE VIEW v_application_stats AS
SELECT 
    user_id,
    status,
    COUNT(*) as count,
    COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (PARTITION BY user_id) as percentage
FROM applications
GROUP BY user_id, status;
```

## 6.2 每日求职漏斗视图

```sql
CREATE OR REPLACE VIEW v_daily_funnel AS
SELECT 
    user_id,
    date,
    jobs_discovered,
    greetings_sent,
    greetings_replied,
    applications_sent,
    interviews_scheduled,
    CASE 
        WHEN greetings_sent > 0 
        THEN ROUND(greetings_replied * 100.0 / greetings_sent, 2)
        ELSE 0 
    END as reply_rate,
    CASE 
        WHEN greetings_replied > 0 
        THEN ROUND(applications_sent * 100.0 / greetings_replied, 2)
        ELSE 0 
    END as conversion_rate
FROM daily_stats
ORDER BY user_id, date DESC;
```

---

# 七、数据库配置

## 7.1 PostgreSQL 配置建议

```sql
-- 开启 JSONB 支持
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 性能优化配置（postgresql.conf）
-- shared_buffers = 256MB
-- effective_cache_size = 1GB
-- work_mem = 64MB
-- maintenance_work_mem = 128MB
-- max_connections = 100
```

## 7.2 连接池配置（HikariCP）

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
```

---

# 八、迁移策略

## 8.1 V1 迁移脚本命名

```
V1__init_schema.sql
```

## 8.2 后续迁移

```
V1.1__add_xxx.sql
V1.2__add_xxx.sql
```

---

# 九、数据保留策略

| 数据类型 | 保留时间 | 说明 |
|---------|---------|------|
| 投递记录 | 永久 | 重要求职数据 |
| 对话记录 | 永久 | 重要沟通数据 |
| 消息记录 | 永久 | 重要沟通数据 |
| 岗位记录 | 6个月 | 过期岗位清理 |
| AI 执行记录 | 3个月 | 统计分析用 |
| 每日统计 | 1年 | 长期趋势分析 |

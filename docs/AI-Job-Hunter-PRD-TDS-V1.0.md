# AI Job Hunter 文档包

> 版本：V1.0  
> 日期：2026-09-02  
> 定位：个人 AI 求职与投递管理系统

## 文档说明

本项目建议至少维护两类核心文档：

1. **PRD / 需求规格书**：定义“为什么做、给谁用、解决什么问题、有哪些业务场景、V1 做什么、不做什么”。
2. **TDS / 技术设计规格书**：定义“系统怎么实现、怎么拆模块、数据怎么流、AI Agent 如何工作、技术栈和演进路线”。

需求文档和技术设计文档应该同时存在。  
**PRD 是业务基线，TDS 是技术基线。**

---

# 一、PRD：AI Job Hunter 产品需求规格书

## 1. 项目概述

AI Job Hunter 是一个面向个人用户的 AI 智能求职系统。

系统通过 AI 对岗位进行采集、解析、匹配、筛选，并结合用户个人经历、技能、项目和简历，辅助生成针对性的求职材料，同时记录投递、沟通、面试和 Offer 结果，通过反馈持续优化求职策略。

核心目标：

> 让 AI 帮用户找到“最值得投”的岗位，而不是盲目批量投递。

核心闭环：

```text
岗位发现
  ↓
岗位理解
  ↓
候选人匹配
  ↓
岗位筛选
  ↓
简历适配
  ↓
人工确认
  ↓
投递
  ↓
面试
  ↓
结果反馈
  ↓
策略优化
```

## 2. 产品目标

### 2.1 岗位发现

帮助用户集中管理和分析不同来源的招聘岗位。

### 2.2 智能筛选

通过 AI 判断岗位与候选人的匹配程度，降低阅读 JD 的成本。

### 2.3 简历适配

根据目标岗位重新组织真实经历、技能和项目，生成岗位适配建议或简历版本。

### 2.4 投递管理

记录每次投递、使用的简历、岗位状态、HR 沟通、面试和最终结果。

### 2.5 数据闭环

根据投递、回复、面试和 Offer 数据分析求职策略。

## 3. 核心用户场景

### 场景 A：每日查看推荐岗位

系统展示：

- 今日发现岗位
- AI 筛选岗位
- 高匹配岗位
- 推荐投递岗位
- 今日剩余投递额度

用户优先处理 Top N 推荐岗位。

### 场景 B：查看 JD AI 分析

用户打开岗位后看到：

- 综合匹配度
- 匹配技能
- 不匹配技能
- 工作经验匹配
- 岗位方向匹配
- 薪资与地点匹配
- 推荐理由
- 风险和不足

### 场景 C：生成岗位适配简历

流程：

```text
JD
 ↓
岗位关键词
 ↓
Candidate Profile
 ↓
匹配项目
 ↓
生成简历适配建议
 ↓
用户确认
```

AI 不允许虚构候选人不存在的经历。

### 场景 D：投递

V1：

```text
AI 推荐
 ↓
用户查看
 ↓
用户确认
 ↓
选择简历
 ↓
生成投递话术
 ↓
用户完成投递
 ↓
系统记录
```

默认不做全自动投递。

### 场景 E：面试

记录：

- 面试轮次
- 面试类型
- 面试时间
- 面试结果
- 面试反馈

AI 可以根据 JD + 简历生成：

- 自我介绍
- 技术问题
- 项目问题
- 可能追问
- 反问问题

### 场景 F：每周复盘

系统统计：

- 发现岗位数量
- 推荐数量
- 投递数量
- 回复率
- 面试率
- Offer 率

AI 输出下周求职策略。

## 4. 功能范围

### V1 必须实现

- Candidate Profile
- Skills
- Experience
- Projects
- Resume
- Resume Version
- Company
- Job
- JD 导入
- JD AI Analysis
- Job Matching
- Job Recommendation
- Application
- Interview
- Daily Application Quota
- Dashboard
- Weekly Report

### V1 暂不实现

- 自动登录招聘网站
- 自动验证码处理
- 反爬绕过
- 全自动批量投递
- 多用户 SaaS
- 移动 App
- Milvus
- MCP Server
- 分布式 Multi-Agent

## 5. 匹配评分

V1 采用规则 + LLM 的混合评分：

```text
Final Score =
    40% Rule Score
  + 60% LLM Score
```

默认等级：

| 分数 | 推荐 |
|---|---|
| 90-100 | STRONG_APPLY |
| 80-89 | APPLY |
| 70-79 | CONSIDER |
| 60-69 | LOW_PRIORITY |
| 0-59 | REJECT |

后续根据真实投递结果优化权重。

## 6. 每日投递规则

默认：

```yaml
daily:
  max_apply: 10
  min_match_score: 80
  duplicate_job: true
  require_confirmation: true
```

同一公司同一岗位默认只允许投递一次。

## 7. 产品页面

V1 页面：

```text
Dashboard
Jobs
Job Detail
Candidate
Projects
Skills
Resumes
Applications
Application Detail
Interviews
Analytics
Settings
```

Application 建议采用 Kanban：

```text
推荐 → 待投 → 已投 → 沟通 → 面试 → Offer
```

## 8. 产品原则

1. AI 辅助，而不是 AI 代替用户做最终决策。
2. 投递数量受控，优先质量。
3. AI 结论必须尽可能可追溯到真实经历。
4. AI 不得虚构工作经历、项目、技能和成果。
5. 用户拥有自己的核心数据。
6. Notion / Obsidian 是辅助知识库或同步出口，不作为核心业务数据库。

## 9. 产品演进

### V1

```text
岗位
+
候选人
+
AI 匹配
+
简历
+
投递
+
面试
+
统计
```

### V2

```text
Local Agent
+
Playwright
+
Browser Automation
+
Notion Sync
+
Obsidian Import
+
pgvector
```

### V3

```text
Feedback Learning
+
Strategy Optimization
+
智能推荐
```

---

# 二、TDS：AI Job Hunter 技术设计规格书

## 1. 技术目标

系统采用：

```text
Web
+
Backend
+
AI Agent
+
PostgreSQL
+
Redis
```

第一阶段强调简单、可维护、可演进，不做过度微服务化。

## 2. 总体架构

```text
Browser
   ↓
Vue 3
   ↓
Spring Boot
   ↓
┌──────────────┬──────────────┬──────────────┐
│ Candidate    │ Job          │ Application  │
│ Domain       │ Domain       │ Domain       │
└──────────────┴──────────────┴──────────────┘
                     ↓
                Agent Runtime
                     ↓
       ┌─────────────┼─────────────┐
       ↓             ↓             ↓
 Job Analyzer     Match Agent   Resume Agent
                     ↓
                  LLM
                     ↓
              PostgreSQL 18
                  + pgvector
                     +
                   Redis
```

## 3. 技术栈

| 层 | 技术 |
|---|---|
| Frontend | Vue 3 + TypeScript |
| Backend | Spring Boot 3 + JDK 21 |
| Database | PostgreSQL 18 |
| Vector | pgvector |
| Cache | Redis |
| AI | OpenAI / DeepSeek / Gemini / Local Model |
| Browser Automation | Playwright |
| Local Agent | Python |
| Auth | JWT / OAuth2 |
| Deployment | Docker Compose |
| Proxy | Nginx / Traefik |
| Monitoring | Prometheus + Grafana |

## 4. Domain 划分

```text
Candidate Domain
Job Domain
Matching Domain
Application Domain
AI Domain
Analytics Domain
```

## 5. Candidate Domain

### Candidate

保存候选人的求职身份：

- 基本信息
- 工作年限
- 当前职位
- 目标职位
- 目标城市
- 薪资期望
- 求职偏好

### Skill

```text
Skill
├── name
├── category
├── level
├── years
├── last_used_at
└── evidence
```

### Project

项目是 AI 生成简历时的事实来源：

```text
Project
├── name
├── description
├── role
├── responsibilities
├── achievements
├── tech_stack
├── keywords
└── evidence
```

原则：

> Project 是事实来源，Resume 是表达形式。

## 6. Resume Domain

简历支持多个方向和多个版本：

```text
Base Resume
   ├── AI Agent Resume
   ├── Java Resume
   └── LLM Resume
```

岗位适配产生：

```text
ResumeVersion
```

需要记录：

- base_resume_id
- job_id
- prompt_version
- model
- generated_at

## 7. Job Domain

### Job

```text
Job
├── company_id
├── title
├── description
├── salary
├── location
├── employment_type
├── source
├── source_url
├── published_at
├── collected_at
└── status
```

### Raw JD 与 Normalized JD

必须分离：

```text
Job
├── Raw JD
└── Normalized JD
```

原始 JD 用于重新解析和审计。

## 8. Job Analysis

Job Analysis 独立保存，不覆盖 Job：

```text
Job
  └── JobAnalysis
```

原因：

```text
模型 A → 89
模型 B → 94
Prompt v2 → 92
```

需要保留分析历史。

## 9. Match Domain

```text
Job
+
Candidate
 ↓
Match
```

核心字段：

```text
match_score
recommendation
skill_match
experience_match
direction_match
salary_match
location_match
advantages
gaps
reason
```

## 10. Evidence 机制

AI 结论必须尽量提供 Evidence：

```text
AI Conclusion
    ↓
Evidence
    ↓
Project / Skill / Experience
```

例如：

```text
结论：具备 MCP 经验
Evidence：Agent Platform 项目 → MCP Protocol
```

用于降低幻觉并支持用户核验。

## 11. Application Domain

状态：

```text
DISCOVERED
↓
RECOMMENDED
↓
TO_APPLY
↓
APPLIED
↓
VIEWED
↓
CONTACTED
↓
INTERVIEWING
↓
OFFER
```

异常终态：

```text
REJECTED
WITHDRAWN
EXPIRED
```

## 12. 每日投递额度

核心对象：

```text
DailyApplicationQuota
```

示例：

```text
date = 2026-09-02
limit = 10
used = 6
remaining = 4
```

额度校验必须是服务端业务规则，前端展示不能作为唯一限制。

## 13. AI Agent 架构

V1 不做复杂 Multi-Agent 对话。

采用职责明确的 AI Workflow：

```text
Agent Runtime
├── Job Analyzer
├── Match Agent
├── Resume Agent
├── Interview Agent
└── Feedback Agent
```

## 14. Job Analyzer

输入：

```text
Raw JD
```

处理：

```text
Extract
↓
Normalize
↓
Classify
↓
Analyze
```

输出必须使用 JSON Schema。

## 15. Match Agent

输入：

```text
Candidate Profile
+
Projects
+
Skills
+
Job Analysis
```

输出：

```json
{
  "score": 92,
  "recommendation": "STRONG_APPLY",
  "matched_skills": [],
  "missing_skills": [],
  "evidence": [],
  "reason": ""
}
```

## 16. Resume Agent

```text
JD
↓
岗位关键词
↓
Candidate Skills
↓
Candidate Projects
↓
选择相关项目
↓
生成 Resume Variant
```

禁止虚构经历。

## 17. Interview Agent

输入：

```text
Job
JD
Resume
Candidate Projects
```

输出：

```text
Self Introduction
Technical Questions
Project Questions
Potential Follow-ups
Reverse Questions
```

## 18. Feedback Agent

输入：

```text
Application
Interview
Result
```

输出：

```text
feedback
strategy
recommendations
```

## 19. Prompt 管理

Prompt 不直接硬编码到 Service。

建议：

```text
PromptTemplate
├── name
├── version
├── type
├── content
├── model
├── temperature
└── status
```

例如：

```text
job-analysis-v1
match-agent-v1
resume-agent-v1
interview-agent-v1
feedback-agent-v1
```

## 20. AgentExecution

所有重要 AI 调用记录：

```text
AgentExecution
├── agent_type
├── prompt_version
├── model
├── input
├── output
├── tokens
├── latency
├── status
├── error
└── created_at
```

用途：

- Debug
- Token 成本统计
- Prompt 优化
- 模型对比
- AI 行为审计

## 21. API 规范

统一前缀：

```text
/api/v1
```

### Candidate

```text
GET    /api/v1/candidate/profile
PUT    /api/v1/candidate/profile

GET    /api/v1/candidate/skills
POST   /api/v1/candidate/skills

GET    /api/v1/candidate/projects
POST   /api/v1/candidate/projects
PUT    /api/v1/candidate/projects/{id}
```

### Job

```text
GET    /api/v1/jobs
POST   /api/v1/jobs
GET    /api/v1/jobs/{id}

POST   /api/v1/jobs/{id}/analyze
POST   /api/v1/jobs/{id}/match

POST   /api/v1/jobs/import
POST   /api/v1/jobs/import/url
POST   /api/v1/jobs/import/text
```

### Resume

```text
GET    /api/v1/resumes
POST   /api/v1/resumes
GET    /api/v1/resumes/{id}

POST   /api/v1/resumes/{id}/generate
POST   /api/v1/jobs/{jobId}/resume/generate
```

### Application

```text
GET    /api/v1/applications
POST   /api/v1/applications
PUT    /api/v1/applications/{id}

POST   /api/v1/applications/{id}/apply
POST   /api/v1/applications/{id}/withdraw
```

### Interview

```text
GET    /api/v1/applications/{id}/interviews
POST   /api/v1/applications/{id}/interviews
PUT    /api/v1/interviews/{id}
```

### Dashboard

```text
GET /api/v1/dashboard/today
GET /api/v1/dashboard/funnel
GET /api/v1/dashboard/statistics
GET /api/v1/dashboard/weekly-report
```

## 22. 异步 AI 任务

AI 分析不阻塞普通 HTTP 请求。

```text
POST /jobs/{id}/analyze
        ↓
     taskId
        ↓
     PENDING
        ↓
     RUNNING
        ↓
     SUCCESS / FAILED
```

后台任务保存结果到 PostgreSQL。

## 23. 幂等与去重

岗位优先使用：

```text
source_url
```

没有 URL 时使用：

```text
company
+
title
+
JD content hash
```

生成：

```text
job_hash
```

避免重复导入。

## 24. 数据库原则

PostgreSQL 18 作为核心数据库：

```text
Relational
+
JSONB
+
Full Text Search
+
pgvector
```

V1 不需要 Milvus。

未来岗位和个人经历需要语义匹配时增加：

```text
JD Embedding
Project Embedding
Skill Embedding
```

只有数据规模和查询需求真正增长后，再考虑 Milvus / OpenSearch。

## 25. Redis

Redis 用于：

- Cache
- AI Task
- Rate Limit
- Daily Quota
- Session

核心业务数据不依赖 Redis 持久化。

## 26. 文件存储

简历文件不直接存 PostgreSQL。

数据库保存：

```text
file_id
file_name
storage_path
mime_type
size
```

V1 可使用本地文件系统。

未来可切换：

```text
S3 / MinIO
```

## 27. 安全

必须：

- HTTPS
- JWT
- 密码安全存储
- API 权限控制
- 敏感数据保护
- 审计日志

招聘网站 Cookie / Session：

> V2 Local Agent 优先本地保存，不上传服务器。

## 28. Local Agent

V2：

```text
Cloud
 ↓
HTTPS
 ↓
Local Agent
 ↓
Playwright
 ↓
Browser
 ↓
招聘平台
```

Local Agent 负责：

- 浏览器自动化
- 本地文件访问
- 简历上传
- 浏览器 Session
- 用户确认

Server 负责：

- AI
- 业务逻辑
- 数据
- 推荐
- 分析

## 29. Notion / Obsidian

V1 不作为核心依赖。

系统核心数据：

```text
PostgreSQL
```

Obsidian：

```text
Markdown Import / Export
```

Notion：

```text
PostgreSQL
 ↓
Notion Sync
```

同步：

- 岗位
- 投递
- 面试
- 周报

## 30. 监控

至少监控：

```text
API latency
API error
AI latency
AI error
Token usage
Task failure
Database
Redis
```

后续：

```text
Prometheus
+
Grafana
```

## 31. AI Evaluation

必须建立 Golden Dataset：

```text
JD
+
人工判断
```

用于比较：

```text
Model A
Model B
Prompt v1
Prompt v2
```

指标可包括：

- Precision
- Recall
- Ranking Quality

## 32. 开发 Epic

### Epic 01 基础工程

- Spring Boot
- Vue
- PostgreSQL
- Redis
- Docker
- CI

### Epic 02 Candidate

- Profile
- Skill
- Experience
- Project

### Epic 03 Resume

- Resume
- Resume Version
- File Storage

### Epic 04 Job

- Job
- Company
- JD Import
- JD Normalization

### Epic 05 AI

- Job Analyzer
- Match Agent
- Resume Agent

### Epic 06 Application

- Application
- Status
- Daily Quota
- Duplicate Check

### Epic 07 Interview

- Interview
- Interview Preparation

### Epic 08 Dashboard

- Daily
- Funnel
- Analytics

### Epic 09 Feedback

- Feedback
- Weekly Report

### Epic 10 Quality

- Unit Test
- Integration Test
- AI Evaluation
- Security
- Observability

## 33. 开发顺序

不要直接让 Cursor / Codex 编码。

推荐：

```text
PRD
 ↓
Domain Model
 ↓
ER Design
 ↓
API Specification
 ↓
Agent Specification
 ↓
Frontend Specification
 ↓
Infrastructure Design
 ↓
Test Strategy
 ↓
Epic
 ↓
Story
 ↓
Task
 ↓
Implementation
```

每一步先设计、确认，再进入下一步。

## 34. V1 最终架构

```text
                         Browser
                            │
                            ↓
                       Vue 3 Web
                            │
                            ↓
                    Spring Boot API
                            │
          ┌─────────────────┼─────────────────┐
          ↓                 ↓                 ↓
     Candidate           Job              Application
          │                 │                 │
          └─────────────────┼─────────────────┘
                            ↓
                       Agent Runtime
                            │
             ┌──────────────┼──────────────┐
             ↓              ↓              ↓
        Job Analyzer    Match Agent    Resume Agent
             │              │              │
             └──────────────┼──────────────┘
                            ↓
                     LLM Provider
                            │
                            ↓
                  PostgreSQL 18 + pgvector
                            +
                          Redis
```

## 35. 核心架构原则

1. **业务系统优先，AI 能力其次。**
2. **PostgreSQL 是核心 Source of Truth。**
3. **AI 输出结构化、可验证、可追踪。**
4. **Project / Skill / Experience 是事实来源。**
5. **Resume 是表达形式，不是事实来源。**
6. **Agent 可替换，模型可替换。**
7. **核心 AI 操作必须有执行记录。**
8. **投递数量必须由服务端控制。**
9. **最终投递权交给用户。**
10. **V1 避免过度架构。**
11. **Local Agent 作为 V2 独立演进。**
12. **Notion / Obsidian 是辅助，不是核心数据库。**

---

# 三、后续设计文档拆分

建议正式开发时继续拆成：

```text
docs/
├── 01-PRD.md
├── 02-TDS.md
├── 03-DOMAIN-MODEL.md
├── 04-ER-DESIGN.md
├── 05-API-SPECIFICATION.md
├── 06-AGENT-SPECIFICATION.md
├── 07-FRONTEND-SPECIFICATION.md
├── 08-LOCAL-AGENT-SPECIFICATION.md
├── 09-AI-EVALUATION.md
├── 10-TEST-STRATEGY.md
└── 11-DEVELOPMENT-PLAN.md
```

其中：

- `01-PRD.md`：产品和业务基线
- `02-TDS.md`：总体技术架构
- `03~10`：详细设计
- `11-DEVELOPMENT-PLAN.md`：最终交给 Cursor / Codex 执行

> **建议现在先不要写 03~11。先确认 PRD + TDS 的方向。方向确定后，再把数据库、API、Agent 逐层锁死。**

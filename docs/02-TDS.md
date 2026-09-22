# AI Job Hunter - 技术设计规格书

> 版本：V1.0
> 日期：2026-09-02
> 状态：草稿
> 负责人：待定

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|-----|
| 2026-09-22 | v1.2 | 升级 Spring Boot 3 → 4.1.1（Jackson 2 → 3），修复 Agent 构造器注入歧义，新增 Testcontainers 冒烟测试（真实 PostgreSQL + Redis） | - |
| 2026-09-06 | v1.1 | 架构调整：移除 Python BOSS Agent，Java 后端集成 Spring AI，V1 聚焦纯 AI 辅助能力 | - |

---

# 一、技术目标

## 1.1 核心原则

```
简单优先 → 可维护 → 可演进
```

第一阶段不追求过度架构，用最简单的方式实现核心功能。

## 1.2 技术愿景

```
用户设置求职策略
       ↓
AI Agent 自动完成 BOSS 求职全流程
       ↓
用户只需关注重要决策，其他交给 AI
```

---

# 二、技术架构

## 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户浏览器                                       │
│                         (Vue 3 + Nuxt 3 + shadcn-vue)                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Java Spring Boot API Server                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Resume API  │  │  Job API    │  │  Chat API   │  │ Strategy   │        │
│  │             │  │             │  │             │  │    API     │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
        ┌────────────────────────────┼────────────────────────────┐
        │                            │                            │
        ▼                            ▼                            ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│  Agent        │          │  Python       │          │  Database     │
│  Runtime      │          │  BOSS Agent  │          │  PostgreSQL   │
│  (Java)       │          │  (Playwright)│          │  + Redis      │
│               │          └───────────────┘          └───────────────┘
│  • JD Analyzer│                                              │
│  • Match Agent│                                              │
│  • Greet Agent│                                              │
│  • Chat Agent │                                              │
│  • Resume Agent│                                             │
└───────────────┘                                              │
        │                                                      │
        ▼                                                      │
┌───────────────┐                                              │
│  LLM Provider │                                              │
│  (OpenAI/DeepSeek)                                          │
└───────────────┘
```

## 2.2 架构分层

```
┌────────────────────────────────────────┐
│         Presentation Layer              │  Vue 3 + Nuxt 3
├────────────────────────────────────────┤
│           API Layer                     │  REST API
├────────────────────────────────────────┤
│          Service Layer                  │  Business Logic (Java)
├────────────────────────────────────────┤
│          Agent Runtime                  │  AI Agent (Java)
├────────────────────────────────────────┤
│    Python BOSS Agent   │   LLM Provider │  External
├────────────────────────────────────────┤
│   PostgreSQL   │   Redis   │  File     │  Storage
└────────────────────────────────────────┘
```

## 2.3 技术选型

| 层级 | 技术 | 说明 |
|-----|------|------|
| **前端** | Vue 3 + Nuxt 3 + TypeScript | 现代化前端框架 |
| **前端 UI** | shadcn-vue | 组件库（个人品牌风格） |
| **前端状态** | Pinia | 状态管理 |
| **后端** | ~~Spring Boot 3 + JDK 21~~ Spring Boot 4.1.1 + JDK 21 | 企业级后端框架（变更日期：2026-09-22，Jackson 2 → 3） |
| **后端** | ~~Spring Boot 3 + JDK 21 + Spring AI~~ Spring Boot 4.1.1 + JDK 21 + Spring AI 2.0.1 | 企业级后端 + AI 能力 |
| **数据库** | PostgreSQL 18 | 核心业务数据 |
| **向量搜索** | pgvector | 简历-JD 语义匹配 |
| **缓存** | Redis | 任务队列、缓存 |
| **AI** | OpenAI / DeepSeek API | LLM 能力 |
| **文件存储** | 本地文件系统 / MinIO | 简历文件存储 |
| **部署** | Docker Compose | 容器化部署 |

---

# 三、三大模块职责

## 3.1 前端模块 (ai-job-hunter-fe)

```
Vue 3 + Nuxt 3 + shadcn-vue + TypeScript
```

**职责：**
- 用户界面展示
- 用户交互处理
- 调用后端 API
- 实时状态展示

**不负责：**
- 业务逻辑
- 数据处理
- 浏览器自动化

## 3.2 后端模块 (ai-job-hunter-be)

```
~~Spring Boot 3 + JDK 21 + Java~~ **Spring Boot 4.1.1 + JDK 21 + Java**（变更日期：2026-09-22）
```

**职责：**
- 用户认证、权限管理
- 简历管理、存储
- 岗位数据管理
- 对话、投递记录
- AI Agent（JD分析、匹配度计算、话术生成）
- LLM 调用
- API 接口暴露

**不负责：**
- 浏览器操作
- 招聘平台交互（V1 阶段）

## 3.3 V1 阶段：纯 AI 能力

**核心目标：** 提供 AI 求职辅助功能，不做自动投递

**提供功能：**
- JD 分析：解析岗位描述，提取关键技能和要求
- 匹配分析：评估简历与岗位的匹配度
- 打招呼话术：生成个性化打招呼内容
- 聊天回复：分析 HR 消息，生成回复建议
- 简历分析：识别简历优势和不足
- 简历优化：根据岗位要求优化简历
- 周报生成：生成求职复盘周报

**未来扩展（V2+）：**
- 猎聘平台合规接入（打招呼无上限）
- 更多招聘平台支持
- 自动化投递（需平台授权）

---

# 四、模块间通信

## 4.1 通信架构（V1 简化版）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户浏览器                                       │
│                         (Vue 3 + Nuxt 3)                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Java Spring Boot 后端                                │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                │
│  │ 业务逻辑     │    │  AI Agent    │    │  用户数据     │                │
│  │ (Service)   │    │  (LLM 调用)  │    │  (User/Job)  │                │
│  └──────────────┘    └──────────────┘    └──────────────┘                │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LLM Provider (OpenAI/DeepSeek)                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

**V1 说明：**
- 用户手动在 BOSS/猎聘等平台操作
- AI 提供话术生成、简历分析等辅助能力
- 未来 V2+ 接入合规招聘平台 API

# 五、Domain 划分（Java 后端）

## 5.1 核心 Domain

```
src/main/java/com/aijobhunter/
├── controller/          # API 控制器
│   ├── AuthController   # 认证
│   └── AgentController  # AI Agent
├── agent/               # AI Agent 运行时
│   ├── BaseAgent        # Agent 基类
│   ├── JdAnalyzerAgent  # JD 分析
│   ├── MatchAgent       # 匹配分析
│   ├── GreetAgent       # 打招呼话术
│   ├── ChatAgent        # 聊天回复
│   ├── ResumeAgent      # 简历分析
│   ├── ResumeRewriteAgent # 简历优化
│   ├── WeeklyReportAgent  # 周报生成
│   └── model/           # Agent 结果模型
│   ├── application/     # 投递域
│   │   ├── Application
│   │   └── ApplicationTimeline
│   └── strategy/        # 策略域
│       ├── Strategy
│       └── Blacklist
├── agent/              # AI Agent（Java 实现）
│   ├── jd/             # JD 分析 Agent
│   ├── match/          # 匹配 Agent
│   ├── greet/          # 打招呼 Agent
│   ├── chat/           # 聊天回复 Agent
│   └── resume/         # 简历分析 Agent
├── common/             # 公共模块
│   ├── config/         # 配置
│   ├── security/        # 安全
│   └── exception/       # 异常
└── AiJobHunterApplication.java
```

---

# 六、Agent 架构（Java 实现）

## 6.1 V1 阶段说明

V1 阶段聚焦 AI 辅助能力，所有 Agent 运行在 Java 后端，通过 Spring AI 调用 LLM。

## 6.2 Agent 职责

```
┌──────────────────────────────────────────────────────────────┐
│                      Agent Runtime (Java)                     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐      │
│  │ JD Analyzer │ -> │ Match Agent │ -> │ Greet Agent │      │
│  │   Agent    │    │             │    │             │      │
│  └─────────────┘    └─────────────┘    └─────────────┘      │
│         │                                    │               │
│         ▼                                    ▼               │
│  ┌─────────────┐                      ┌─────────────┐         │
│  │ Resume      │                      │ Chat Agent  │       │
│  │ Agent       │                      │             │       │
│  └─────────────┘                      └─────────────┘         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## 7.2 Agent 输入输出

### 7.2.1 JD Analyzer Agent

```json
输入: { "raw_jd": "岗位JD原文" }
输出: {
  "keywords": ["Java", "Spring", "微服务"],
  "skills": ["Java", "Spring Boot", "MySQL"],
  "experience_years": 3,
  "salary_range": "20k-35k",
  "highlights": ["技术成长空间大", "团队技术氛围好"]
}
```

### 7.2.2 Match Agent

```json
输入: { "resume_id": "xxx", "job_id": "yyy" }
输出: {
  "match_score": 85,
  "matched_skills": ["Java", "Spring"],
  "missing_skills": ["Kafka", "Kubernetes"],
  "recommendation": "APPLY",
  "reason": "匹配度高，建议投递"
}
```

### 7.2.3 Greet Agent

```json
输入: { "job_id": "yyy", "resume_id": "xxx" }
输出: {
  "greeting_text": "您好！看到您在招聘Java工程师...",
  "confidence": 0.92
}
```

### 7.2.4 Chat Agent

```json
输入: { "hr_message": "您方便说下期望薪资吗？", "context": {...} }
输出: {
  "reply_text": "我的期望薪资是...",
  "intent": "SALARY_NEGOTIATION",
  "confidence": 0.88
}
```

### 7.2.5 Resume Agent

```json
输入: { "resume_id": "xxx", "job_id": "yyy", "action": "analyze" }
输出: {
  "strengths": ["项目经验丰富", "技术栈匹配"],
  "weaknesses": ["缺少XX经验", "简历排版不够专业"],
  "suggestions": ["建议突出XX项目", "补充XX技能关键词"],
  "rewritten_content": "..."  // 仅当 action=rewrite 时
}
```

---

# 八、Prompt 管理

## 8.1 Prompt 模板设计

```
┌─────────────────────────────────────┐
│        PromptTemplate                │
├─────────────────────────────────────┤
│  id: uuid                           │
│  name: "greet-agent-v1"            │
│  version: 1                         │
│  type: "GREET"                      │
│  content: "你是一个求职助手..."      │
│  variables: ["job_title", "skills"] │
│  model: "gpt-4o"                   │
│  temperature: 0.7                   │
│  status: "ACTIVE"                   │
└─────────────────────────────────────┘
```

## 8.2 模板类型

| 类型 | 用途 |
|-----|------|
| JD_ANALYSIS | JD 解析 |
| MATCH | 简历-JD 匹配 |
| GREET | 打招呼话术 |
| CHAT_REPLY | 聊天回复 |
| RESUME_ANALYSIS | 简历分析 |
| RESUME_REWRITE | 简历重写 |
| WEEKLY_REPORT | 周报生成 |

---

# 九、AI 执行记录

## 9.1 AgentExecution

所有重要 AI 调用必须记录：

```
┌─────────────────────────────────────┐
│        AgentExecution                │
├─────────────────────────────────────┤
│  id: uuid                           │
│  agent_type: "GREET_AGENT"          │
│  prompt_version: "greet-v1"        │
│  model: "gpt-4o"                   │
│  input: { ... }                     │
│  output: { ... }                    │
│  tokens_used: 1500                  │
│  latency_ms: 1200                   │
│  status: "SUCCESS"                  │
│  error: null                        │
│  created_at: timestamp              │
└─────────────────────────────────────┘
```

## 9.2 用途

- **Debug**：复现 AI 行为
- **成本统计**：Token 消耗
- **Prompt 优化**：对比不同版本效果
- **模型对比**：A/B 测试不同模型

---

# 十、异步任务

## 10.1 任务类型

| 任务类型 | 执行方 | 说明 |
|---------|-------|------|
| JD_ANALYSIS | Java (Spring AI) | JD 解析任务 |
| RESUME_ANALYSIS | Java (Spring AI) | 简历分析任务 |
| RESUME_REWRITE | Java (Spring AI) | 简历重写任务 |
| MATCH_CALCULATION | Java (Spring AI) | 匹配度计算 |
| GREET_GENERATION | Java (Spring AI) | 打招呼话术生成 |
| CHAT_REPLY | Java (Spring AI) | HR 消息回复生成 |
| GREETING_SEND | Python | 打招呼发送 |
| CHAT_REPLY | Python | 聊天消息发送 |

## 10.2 任务状态

```
PENDING -> RUNNING -> SUCCESS / FAILED
```

## 10.3 任务队列

使用 Redis 实现轻量级任务队列：

```redis
# 任务队列（Java 后端）
LPUSH aijobhunter:task:queue '{"task_id":"xxx","type":"JD_ANALYSIS","payload":{...}}'

# 任务状态
SET aijobhunter:task:status:xxx '{"status":"RUNNING","progress":50}'
```

---

# 十一、Redis 使用规范

> 遵循 Cogniforge Redis 键命名规范

## 11.1 键命名规则

```
aijobhunter:{模块}:{名称}

示例：
aijobhunter:agent:execution:{id}
aijobhunter:task:queue
aijobhunter:task:status:{id}
aijobhunter:cache:resume:{id}
aijobhunter:quota:daily:{date}
```

## 11.2 用途

| 用途 | 键前缀 | 说明 |
|-----|--------|------|
| AI 执行记录 | aijobhunter:agent:* | Agent 执行日志 |
| 任务队列 | aijobhunter:task:* | 异步任务 |
| 缓存 | aijobhunter:cache:* | 热点数据缓存 |
| 额度控制 | aijobhunter:quota:* | 每日次数限制 |

---

# 十二、数据库设计原则

## 12.1 PostgreSQL 使用

```
关系数据 + JSONB + Full Text Search + pgvector
```

## 12.2 核心表

- **users**：用户表
- **resume**：简历主表
- **job**：岗位主表
- **conversation**：对话记录
- **application**：投递记录
- **strategy**：求职策略
- **agent_execution**：AI 执行记录
- **agent_task**：任务表（Java 下发，Python 执行）

## 12.3 JSONB 使用场景

- JD 原始内容（保留可重新解析）
- AI 分析结果（结构可能变化）
- 聊天消息扩展字段

---

# 十三、文件存储

## 13.1 简历文件

```
不上传到数据库，只存：
• file_id
• file_name
• storage_path
• mime_type
• size
```

## 13.2 存储方案

| 阶段 | 方案 |
|-----|------|
| V1 | 本地文件系统 |
| V2+ | MinIO / S3 |

---

# 十四、安全设计

## 14.1 数据安全

| 数据类型 | 处理方式 |
|---------|---------|
| 简历文件 | 本地加密存储 |
| API 密钥 | 环境变量 / GitHub Secrets |
| 用户密码 | BCrypt 哈希 |
| OpenAI API Key | 环境变量 / GitHub Secrets |

---

# 十五、监控设计

## 15.1 监控指标

| 类别 | 指标 |
|-----|------|
| API | 请求量、延迟、错误率 |
| AI | Token 消耗、响应延迟、错误率 |
| 系统 | CPU、内存、磁盘 |

## 15.2 日志

- 结构化日志（JSON 格式）
- 请求链路追踪（Trace ID）
- AI 调用详情记录

---

# 十六、部署架构

## 16.1 V1 部署

```
┌─────────────────────────────────────┐
│           Docker Compose             │
├─────────────────────────────────────┤
│  • api-server (Spring Boot)         │
│  • postgres (PostgreSQL 18)         │
│  • redis                            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│         用户浏览器                    │
│  • 手动操作 BOSS/猎聘等平台          │
│  • 使用 AI 辅助功能                  │
└─────────────────────────────────────┘
```

## 16.2 目录结构

```
ai-job-hunter/
├── ai-job-hunter-be/         # Java 后端（包含 Agent）
├── ai-job-hunter-fe/         # Nuxt 前端
├── docker-compose.yml        # 部署配置
└── docs/                     # 文档
```

---

# 十七、演进路线

## V1 (当前)
```
• Java 后端 + Spring AI
• 纯 AI 辅助能力（话术生成、简历分析等）
• 用户手动操作招聘平台
```

## V2
```
• 猎聘平台合规接入（打招呼无上限）
• 多平台支持
• 简历 A/B 测试
• 薪资谈判助手
```

## V3
```
• BOSS 平台自动化（需官方授权）
• pgvector 语义匹配
• 反馈学习优化
• 智能策略调优
```

---

# 附录：模块仓库

| 模块 | 仓库路径 |
|-----|---------|
| 后端（含 Agent） | `ai-job-hunter-be/` |
| 前端 | `ai-job-hunter-fe/` |

---

# [变更] Spring Boot 3 → 4.1.1 + Jackson 3 迁移（2026-09-22）

## 变更原因
2026-09-22 部署失败：`Parameter 1 of constructor in com.aijobhunter.agent.ChatAgent required a bean of type 'com.fasterxml.jackson.databind.ObjectMapper' that could not be found.`

根因：
1. 项目使用的 Spring Boot 版本是 **4.1.1**（不是 3.x），Spring Boot 4 默认迁移到 `tools.jackson.core:jackson-databind:3.x`（Jackson 3），自动注册的 `ObjectMapper` 是 `tools.jackson.databind.ObjectMapper`，而非 `com.fasterxml.jackson.databind.ObjectMapper`（Jackson 2）。
2. 项目代码全部使用 Jackson 2 的 import。
3. CI 跑 `./gradlew test` 但 `src/test/` 是空目录，测试瞬间通过，错误逃逸到生产环境。

## 包含代码
- `build.gradle`：删除 `com.fasterxml.jackson.core:jackson-databind` 显式声明
- `BaseAgent.java` 及 7 个 Agent 子类（`ChatAgent`、`GreetAgent`、`WeeklyReportAgent`、`ResumeRewriteAgent`、`JdAnalyzerAgent`、`MatchAgent`、`ResumeAgent`）：将 `import com.fasterxml.jackson.databind.ObjectMapper` 改为 `import tools.jackson.databind.ObjectMapper`
- `BaseAgent.java`：删除无参构造器（死代码，曾导致 Spring 构造器注入歧义）
- `ChatClientConfig.java`：删除手动注册的 Jackson 2 ObjectMapper Bean（改由 Spring Boot 4 自动配置提供 Jackson 3 ObjectMapper）
- `src/test/java/com/aijobhunter/ContextLoadsTest.java`（新增）：Spring 上下文冒烟测试，使用 Testcontainers 启动真实 PostgreSQL 18 + Redis 7 容器，与生产环境完全等价
- `src/test/resources/schema-test.sql`（新增）：PostgreSQL 测试数据库初始化脚本（建 `app` schema）
- `build.gradle`：新增 Testcontainers 依赖（`testcontainers-bom`、`junit-jupiter`、`postgresql`）；移除 H2 和 embedded-redis
- `.github/workflows/ci.yml`：重写 CI 任务（拆分 compile / test / smoke 三个任务；smoke 用 Testcontainers 跑真实 DB/Redis）

## 影响范围
- 所有 8 个 Agent 文件
- CI 工作流
- 测试基础设施

## 变更前 vs 变更后

### 变更前
- Spring Boot 3.x，Jackson 2.x
- CI 跑空 `./gradlew test` 直接通过，无 Bean 注入校验
- 部署后启动失败，错误信息：`No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper'`

### 变更后
- Spring Boot 4.1.1，Jackson 3.x
- CI 拆分为 compile（编译校验）+ test（单元测试）+ smoke（Testcontainers 真实 PostgreSQL + Redis）
- `ContextLoadsTest` 强制 Spring 启动，所有 Bean 装配错误都会在 CI 阶段被抓到
- smoke 环境与生产 docker-compose.yml 完全等价（PostgreSQL 18 + Redis 7）

## 关键差异

| 维度 | 新增 | 移除 | 修改 |
|------|------|------|------|
| 依赖 | Testcontainers（`testcontainers-bom`、`junit-jupiter`、`postgresql`） | H2、`embedded-redis`、`com.fasterxml.jackson.core:jackson-databind` | 无 |
| 代码 | `ContextLoadsTest`（Testcontainers）、`schema-test.sql` | `ChatClientConfig` 中的 ObjectMapper Bean、`BaseAgent` 无参构造器 | 8 个 Agent 文件的 ObjectMapper import |
| CI | smoke 任务（Testcontainers 真实 DB/Redis 冒烟） | 单个 `test` 任务 | 拆分为 compile / test / smoke 三个任务 |


# AI Job Hunter - API 规范

> 版本：V1.0
> 日期：2026-09-02
> 状态：草稿
> 负责人：待定

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|-----|
| 2026-11-26 | v1.15 | DRAFT_GREET/FILL_REPLY 须 `draftText`；确认返回 `clipboardText` 供前端复制 | orjrs |
| 2026-11-26 | v1.14 | 平台 SYNC_JOBS 可带 JD，确认后 `source=IMPORT`；不建 agent 仓 | orjrs |
| 2026-11-26 | v1.13 | V2 阶段 A：`/platforms/connections|tasks` MOCK 半自动 | orjrs |
| 2026-11-26 | v1.12 | `PUT /users/me/password` 改密码 | orjrs |
| 2026-11-26 | v1.11 | 投递时间线备注：`GET /applications/{id}`、`POST …/notes`；状态变更可带 note | orjrs |
| 2026-11-23 | v1.10 | `/api/v1/agent/**` 与 Swagger 须登录；匿名仅 health/auth | orjrs |
| 2026-11-20 | v1.9 | Word(doc/docx) 抽文本；上传后可 PARSED 供 AI 分析 | orjrs |
| 2026-11-19 | v1.8 | PDF 抽文本；创建岗位校验黑名单；打招呼优先默认模板；Boot→4.1.2 | orjrs |
| 2026-11-06 | v1.7 | Sprint5：统计 today/trend/funnel/weekly-report；模板与黑名单 CRUD | orjrs |
| 2026-10-26 | v1.6 | Sprint4：沟通对话 CRUD + AI 推荐回复；投递创建/看板/改状态 | orjrs |
| 2026-10-26 | v1.5 | 简历 AI 分析/重写同步 API（`/resumes/{id}/analyze|rewrite`） | orjrs |
| 2026-10-17 | v1.4 | 岗位：粘贴 JD 创建 + 列表/详情/分析/匹配/打招呼/删除（Spring AI，无爬虫） | orjrs |
| 2026-10-17 | v1.3 | 未携带有效 JWT 访问受保护接口统一 **401**（不再默认 403） | orjrs |
| 2026-10-17 | v1.2 | 落地 GET/PUT `/users/me`；简历 upload/list/detail/update/delete（txt 解析） | orjrs |
| 2026-10-12 | v1.1 | 登录支持用户名或邮箱（`account` / 兼容 `email`·`username`） | orjrs |
| 2026-09-02 | v1.0 | 初始版本 | - |

---

## [变更] 平台草稿话术返回剪贴板字段（2026-11-26）

- **变更原因**：阶段 C 小步——打招呼/回复不代发，只帮用户复制到剪贴板。
- **包含代码**：`CreateTaskRequest.draftText`；确认结果 `clipboardText`；`PlatformTaskLiveIT` PLAT-005/006。
- **约定**：`DRAFT_GREET`/`FILL_REPLY` 缺 `draftText` → 400；确认后前端 `navigator.clipboard.writeText`。

## [变更] 平台 MOCK 任务骨架（2026-11-26）

- **变更原因**：落地 V2 阶段 A，先打通半自动确认流。
- **包含代码**：`PlatformController` / `PlatformService`；`PlatformTaskLiveIT`。
- **约定**：`POST /platforms/connections` 仅 MOCK；`POST /tasks/{id}/confirm` 立即 DONE（mock），不爬站。

## [变更] 登录用户可改密码（2026-11-26）

- **变更原因**：「我的」页此前只能改资料，无法改登录密码。
- **包含代码**：`PUT /api/v1/users/me/password`；`ChangePasswordRequest`；`UsersMeLiveIT` USER-004。
- **约定**：须登录；校验当前密码；新密码 ≥8 且不可与旧密相同；成功后旧密不可再登录。

## [变更] 投递时间线备注（2026-11-26）

- **变更原因**：看板只有状态、不好记面试安排等；ER 已有 `application_timelines` 草案但未落地。
- **包含代码**：`ApplicationTimeline*`；`ApplicationService` 自动记 SUBMITTED/STATUS_CHANGED；`POST /applications/{id}/notes`；`GET /applications/{id}`；DTO 含 `timeline`。
- **约定**：备注最长 2000 字；列表/看板项同样带 `timeline`；~~V1 暂不落 timeline note~~（2026-11-26）。

## [变更] Agent 直连接口须登录（2026-11-23）

- **变更原因**：安全抽查发现匿名可调 LLM Agent，浪费额度。
- **包含代码**：`SecurityConfig` 去掉 `/api/v1/agent/**`、Swagger 的 `permitAll`。
- **约定**：前端/业务应继续用 `/api/v1/jobs|resumes|conversations|stats`；调试 Agent 时带 JWT。匿名仅 `/api/v1/auth/**` 与 `/actuator/health|info`。

## [变更] Word 简历抽文本（2026-11-20）

- **变更原因**：V1 半成品：doc/docx 此前只落盘不可分析。
- **包含代码**：`poi-ooxml`/`poi-scratchpad` 5.4.1；`ResumeService#extractWordText`；`ResumeWordExtractTest`。
- **约定**：抽出有效文本 → `status=PARSED` + `parsedData.rawText`；抽不出字仍 `UPLOADED` 并写 `note`；分析/重写错误提示改为支持 Word。

## [变更] PDF 解析 + 黑名单/模板落地（2026-11-19）

- **变更原因**：补齐半成品：PDF 简历可抽文本做 AI；黑名单真正拦截建岗；默认模板优先于 AI 打招呼。
- **包含代码**：`pdfbox:3.0.8`、`ResumeService` PDF 抽取；`JobService#matchBlacklist` / `fillTemplate`；Boot `4.1.1→4.1.2`。
- **约定**：扫描件 PDF 可能抽不出字仍 `UPLOADED`；建岗命中黑名单 → 400；`POST /jobs/{id}/greet` 响应增 `source=TEMPLATE|AI`。
- **技术选型**：不升 Nuxt 4 / Boot 4.2 RC（破坏面大）；PDF 选 PDFBox 3.0.8 而非 2.x。

## [变更] 统计 / 模板 / 黑名单（2026-11-06）

- **变更原因**：Sprint 5：仪表盘与求职漏斗；打招呼模板与黑名单本地管理。
- **包含代码**：`StatsService`/`StatsController`；`GreetingTemplate*`；`Blacklist*`；`StatsLiveIT`。
- **约定**：统计从岗位/对话/投递**实时聚合**（不落 `daily_stats`）；周报走 `WeeklyReportAgent`，失败降级仍 200；自动化模式 / BOSS Cookie 仍不做。

## [变更] 沟通与投递最小闭环（2026-10-26）

- **变更原因**：Sprint 4：手动录入 HR 对话 + ChatAgent 推荐回复 + 投递看板（不接 BOSS 实时同步）。
- **包含代码**：`Conversation`/`Message`/`Application` 实体；`ConversationService`/`ApplicationService` + Controller；`ConversationLiveIT`。
- **新增**：`POST /api/v1/conversations`（可带 `firstHrMessage`）；消息 `direction=INBOUND|OUTBOUND`；`GET …/suggested-reply` / `POST …/send-suggested-reply`；`POST /api/v1/applications`；`GET …/kanban`；`PUT …/{id}/status`。
- **约定**：推荐回复 LLM 失败降级仍 200；同一用户同一岗位不可重复投递；投递成功可将岗位状态置为 `APPLIED`；V1 无 timeline / BOSS 抓取。

## [变更] 简历 AI 分析 / 重写（2026-10-26）

- **变更原因**：补齐 Sprint2 Resume Agent；详情页可分析优劣并生成优化建议。
- **包含代码**：`ResumeService#analyze/rewrite`、`ResumeController`；复用 `ResumeAgent` / `ResumeRewriteAgent`。
- **约定**：同步返回完整简历；`status=ANALYZED`；重写结果写入 `analysis.rewrite`（不覆盖原文）；LLM 失败降级仍 200；无 `rawText` 时不可分析（请用可抽字的 txt/PDF/Word）。

## [变更] 岗位粘贴 JD + AI 分析/匹配/打招呼（2026-10-17）

- **变更原因**：Sprint 3：不建 Python Agent、不爬 BOSS；用户粘贴 JD，后端 Spring AI 分析。
- **包含代码**：`Job` / `JobService` / `JobController`；复用 `JdAnalyzerAgent` / `MatchAgent` / `GreetAgent`（失败降级）。
- **新增**：`POST /api/v1/jobs`（粘贴创建）；`POST /api/v1/jobs/{id}/greet`；`analyze` 同步返回完整岗位（含 analysis）。
- **约定**：`source=MANUAL`；可选 `sourceUrl` 仅存档；LLM 失败时关键词降级，接口仍 200。

## [变更] 未登录统一 401（2026-10-17）

- **变更原因**：Spring Security 默认对匿名访问回 403，前端误判为权限错误且不清会话。
- **包含代码**：`SecurityConfig` AuthenticationEntryPoint
- **约定**：受保护接口无/无效 Bearer → HTTP **401**，body `code=2001`「未登录或登录已失效」

## [变更] 用户信息与简历 CRUD（2026-10-17）

- **变更原因**：Sprint 2 推进：认证之后补用户资料与简历管理。
- **包含代码**：`UserController`、`ResumeController`/`ResumeService`、实体 `Resume`；前端 `/profile`、`/resumes`。
- **说明**：txt/PDF/doc/docx 能抽出文本则 `status=PARSED` 并写入 `parsedData.rawText`；抽不出字（如扫描件）仍 `UPLOADED`。

## [变更] 登录支持用户名或邮箱（2026-10-12）

**变更原因**：用户希望注册后既可用用户名也可用邮箱登录。

**包含代码**：`LoginRequest.java`、`AuthController.java`

**变更前 → 变更后**：只认 `email` → 先按邮箱查，没有再按用户名查；推荐字段 `account`，仍兼容旧字段 `email` / `username`。

---

# 一、API 设计原则

## 1.1 RESTful 规范

```
资源导向 → HTTP 方法 → 状态码规范
```

| 方法 | 用途 | 示例 |
|-----|------|------|
| GET | 查询资源 | GET /api/v1/jobs |
| POST | 创建资源 | POST /api/v1/resumes |
| PUT | 更新资源 | PUT /api/v1/resumes/{id} |
| DELETE | 删除资源 | DELETE /api/v1/resumes/{id} |

## 1.2 响应格式

```json
{
  "code": 0,           // 0=成功，非0=失败
  "message": "success",
  "data": { ... },
  "timestamp": 1704067200000,
  "traceId": "abc123"
}
```

## 1.3 分页格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [...],
    "page": 1,
    "pageSize": 20,
    "total": 100,
    "totalPages": 5
  }
}
```

---

# 二、基础信息

## 2.1 Base URL

```
开发环境: http://localhost:8080/api/v1
生产环境: https://api.aijobhunter.com/api/v1
```

## 2.2 认证方式

```
Bearer Token (JWT)
Authorization: Bearer <token>
```

## 2.3 通用错误码

| 错误码 | 说明 |
|-------|------|
| 0 | 成功 |
| 1001 | 参数错误 |
| 1002 | 缺少必填参数 |
| 2001 | 认证失败 |
| 2002 | Token 过期 |
| 3001 | 资源不存在 |
| 3002 | 资源已存在 |
| 4001 | 权限不足 |
| 5001 | 系统错误 |
| 5002 | AI 服务异常 |
| 5003 | BOSS 服务异常 |

---

# 三、用户接口

## 3.1 用户注册

```
POST /api/v1/auth/register
```

**请求体：**

```json
{
  "username": "zhangsan",
  "email": "zhangsan@example.com",
  "password": "securePassword123"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "uuid",
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "createdAt": "2026-09-02T10:00:00Z"
  }
}
```

## 3.2 用户登录

```
POST /api/v1/auth/login
```

**请求体：**（`account` 填用户名或邮箱均可；也兼容旧字段 `email` / `username`）

```json
{
  "account": "zhangsan@example.com",
  "password": "securePassword123"
}
```

```json
{
  "account": "zhangsan",
  "password": "securePassword123"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400,
    "user": {
      "id": "uuid",
      "username": "zhangsan",
      "email": "zhangsan@example.com"
    }
  }
}
```

## 3.3 获取当前用户信息

```
GET /api/v1/users/me
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "phone": "138****8888",
    "bossConnected": true,
    "automationConfig": {
      "mode": "SEMI",
      "dailyGreetLimit": 50,
      "dailyApplyLimit": 20
    },
    "preferences": {
      "keywords": ["Java", "后端"],
      "cities": ["北京", "上海"]
    },
    "lastActiveAt": "2026-09-02T10:00:00Z"
  }
}
```

## 3.4 更新用户信息

```
PUT /api/v1/users/me
```

**请求体：**

```json
{
  "phone": "13812345678",
  "preferences": {
    "keywords": ["Python", "AI"],
    "cities": ["深圳"],
    "salaryMin": 25000,
    "salaryMax": 50000
  },
  "automationConfig": {
    "mode": "FULL",
    "dailyGreetLimit": 100
  }
}
```

## 3.4.1 修改密码

```
PUT /api/v1/users/me/password
```

**请求体：**

```json
{
  "currentPassword": "oldPass123",
  "newPassword": "newPass4567"
}
```

当前密码错误 → 400「当前密码不正确」。新密码少于 8 位或与旧密相同 → 400。

## 3.5 更新 BOSS 连接

```
POST /api/v1/users/me/boss-connect
```

**请求体：**

```json
{
  "bossCookie": "xxx",
  "bossToken": "yyy"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "connected": true,
    "connectedAt": "2026-09-02T10:00:00Z"
  }
}
```

## 3.6 断开 BOSS 连接

```
POST /api/v1/users/me/boss-disconnect
```

---

# 四、简历接口

## 4.1 上传简历

```
POST /api/v1/resumes/upload
Content-Type: multipart/form-data
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| file | File | 是 | 简历文件 |
| name | String | 是 | 简历名称 |
| type | String | 否 | 简历类型：GENERAL/TECHNICAL/MANAGEMENT/CUSTOM |
| isDefault | Boolean | 否 | 是否设为默认简历 |

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "name": "技术简历V1",
    "type": "TECHNICAL",
    "isDefault": true,
    "fileInfo": {
      "originalName": "我的简历.pdf",
      "mimeType": "application/pdf",
      "size": 1024000
    },
    "parsedData": {
      "personalInfo": { "name": "张三", "phone": "138****", "email": "zhangsan@example.com" },
      "skills": ["Java", "Spring", "MySQL"],
      "experience": [...]
    },
    "status": "PARSED",
    "createdAt": "2026-09-02T10:00:00Z"
  }
}
```

## 4.2 获取简历列表

```
GET /api/v1/resumes
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认20 |
| type | String | 否 | 简历类型过滤 |

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "uuid",
        "name": "技术简历V1",
        "type": "TECHNICAL",
        "isDefault": true,
        "analysis": {
          "strengths": ["项目经验丰富"],
          "weaknesses": ["缺少XX经验"]
        },
        "createdAt": "2026-09-02T10:00:00Z"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

## 4.3 获取简历详情

```
GET /api/v1/resumes/{id}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "name": "技术简历V1",
    "type": "TECHNICAL",
    "isDefault": true,
    "parsedData": {
      "personalInfo": { "name": "张三", "phone": "...", "email": "..." },
      "summary": "5年Java开发经验...",
      "skills": ["Java", "Spring Boot", "MySQL", "Redis", "Kafka"],
      "experience": [
        {
          "company": "XX科技",
          "position": "高级Java工程师",
          "duration": "2021.03 - 至今",
          "description": "负责核心业务开发..."
        }
      ],
      "education": [...],
      "projects": [...]
    },
    "fileInfo": { ... },
    "analysis": {
      "strengths": ["技术栈匹配度高", "项目经验丰富"],
      "weaknesses": ["缺少微服务架构经验", "缺少大数据经验"],
      "suggestions": ["建议在简历中突出XX项目经验", "补充XX技术关键词"],
      "extractedSkills": ["Java", "Spring", "MySQL"],
      "model": "gpt-4o",
      "tokensUsed": 1500,
      "analyzedAt": "2026-09-02T10:00:00Z"
    },
    "versions": [
      {
        "id": "uuid",
        "versionNumber": 1,
        "source": "ORIGINAL",
        "createdAt": "2026-09-02T10:00:00Z"
      }
    ],
    "createdAt": "2026-09-02T10:00:00Z",
    "updatedAt": "2026-09-02T10:00:00Z"
  }
}
```

## 4.4 更新简历基本信息

```
PUT /api/v1/resumes/{id}
```

**请求体：**

```json
{
  "name": "技术简历V2",
  "isDefault": true
}
```

## 4.5 删除简历

```
DELETE /api/v1/resumes/{id}
```

## 4.6 分析简历

```
POST /api/v1/resumes/{id}/analyze
```

**请求体（可选）：** `{ "targetJobId": "uuid", "targetJob": "Java 后端" }`

**响应：** 同步返回完整简历（`status=ANALYZED`，`analysis` 已写入）。LLM 不可用时降级评估，仍 200。  
~~异步 ANALYZING + 轮询~~（2026-10-26：V1 改为同步）。

## 4.7 重写简历

```
POST /api/v1/resumes/{id}/rewrite
```

**请求体：**

```json
{
  "targetJobId": "uuid",
  "specialRequirements": "突出微服务经验",
  "tone": "professional"
}
```

**响应：** 同步返回完整简历；优化稿在 `data.analysis.rewrite`（不覆盖原文 `parsedData`）。

## 4.8 获取简历版本列表

```
GET /api/v1/resumes/{id}/versions
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "uuid",
        "versionNumber": 2,
        "source": "AUTO_REWRITE",
        "changeNote": "针对Java岗位优化",
        "createdAt": "2026-09-02T10:00:00Z"
      },
      {
        "id": "uuid",
        "versionNumber": 1,
        "source": "ORIGINAL",
        "changeNote": "原始上传",
        "createdAt": "2026-09-01T10:00:00Z"
      }
    ]
  }
}
```

## 4.9 切换简历版本

```
POST /api/v1/resumes/{id}/versions/{versionId}/activate
```

---

# 五、岗位接口

> V1 数据来源：**用户粘贴 JD**（`source=MANUAL`），不做招聘网站爬取。

## 5.0 粘贴 JD 创建岗位

```
POST /api/v1/jobs
```

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| rawDescription | String | 是 | JD 原文 |
| title | String | 否 | 默认取原文首行 |
| companyName / city / salaryText | String | 否 | 元信息 |
| sourceUrl | String | 否 | 招聘页链接，仅存档 |
| analyze | Boolean | 否 | 默认 true，创建后立即 AI 分析 |

**响应：** `data` 为岗位对象（含 `analysis` / `status`）。

## 5.1 获取岗位列表

```
GET /api/v1/jobs
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认20 |
| status | String | 否 | 状态过滤 |
| city | String | 否 | 城市过滤 |
| keyword | String | 否 | 关键词搜索 |
| minMatchScore | Integer | 否 | 最低匹配度 |

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "uuid",
        "title": "Java高级工程师",
        "companyName": "XX科技",
        "city": "北京",
        "salaryText": "25k-40k·14薪",
        "status": "GREETED",
        "matchScore": 85,
        "analysis": {
          "skills": ["Java", "Spring Boot", "MySQL"],
          "highlights": ["技术成长空间大"]
        },
        "greetingSent": true,
        "applied": false,
        "discoveredAt": "2026-09-02T10:00:00Z"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 100,
    "totalPages": 5
  }
}
```

## 5.2 获取岗位详情

```
GET /api/v1/jobs/{id}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "title": "Java高级工程师",
    "companyName": "XX科技",
    "company": {
      "name": "XX科技有限公司",
      "logo": "https://...",
      "industry": "互联网",
      "scale": "500-999人",
      "stage": "C轮"
    },
    "city": "北京",
    "district": "朝阳区",
    "salaryText": "25k-40k·14薪",
    "salaryMin": 25000,
    "salaryMax": 40000,
    "rawDescription": "岗位描述原文...",
    "analysis": {
      "skills": ["Java", "Spring Boot", "MySQL", "Redis", "Kafka"],
      "responsibilities": ["负责核心业务开发", "技术方案设计"],
      "requirements": ["5年以上Java开发经验", "本科及以上学历"],
      "highlights": ["技术成长空间大", "团队技术氛围好"],
      "minExperienceYears": 5,
      "education": "本科",
      "mustHaveSkills": ["Java", "Spring"],
      "niceToHaveSkills": ["Kubernetes", "大数据"]
    },
    "keywords": ["Java", "Spring", "微服务"],
    "matchScores": {
      "resume-uuid-1": { "overallScore": 85, "matchedSkills": [...], "missingSkills": [...] },
      "resume-uuid-2": { "overallScore": 72, "matchedSkills": [...], "missingSkills": [...] }
    },
    "status": "GREETED",
    "greetingSent": true,
    "applied": false,
    "discoveredAt": "2026-09-02T10:00:00Z"
  }
}
```

## 5.3 分析岗位 JD

```
POST /api/v1/jobs/{id}/analyze
```

**响应：** 同步返回完整岗位（`status=ANALYZED`，`analysis` 已写入）。LLM 不可用时降级为关键词抽取，仍返回 200。

## 5.3.1 生成打招呼话术

```
POST /api/v1/jobs/{id}/greet
```

**请求体（可选）：** `{ "resumeId": "uuid", "style": "friendly" }`

**响应：** `{ "jobId", "greetingText", "length", "style", "highlightedPoints" }`；同时写入岗位 `greetingText`，状态可为 `GREETED`。

## 5.4 获取岗位匹配度

```
GET /api/v1/jobs/{id}/match
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| resumeId | String | 是 | 简历ID |

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "jobId": "uuid",
    "resumeId": "uuid",
    "overallScore": 85,
    "skillMatch": {
      "score": 80,
      "matched": ["Java", "Spring Boot", "MySQL"],
      "missing": ["Kafka", "Kubernetes"]
    },
    "experienceMatch": {
      "score": 90,
      "assessment": "符合要求"
    },
    "educationMatch": {
      "score": 100,
      "assessment": "符合"
    },
    "recommendation": "APPLY",
    "reason": "技能匹配度高，建议投递"
  }
}
```

## 5.5 添加岗位到黑名单

```
POST /api/v1/jobs/{id}/blacklist
```

**请求体：**

```json
{
  "reason": "公司加班严重"
}
```

## 5.6 删除岗位

```
DELETE /api/v1/jobs/{id}
```

---

# 六、沟通接口

## 6.0 创建对话（手动）

```
POST /api/v1/conversations
```

**请求体：**

```json
{
  "jobId": "uuid（可选）",
  "hrName": "李HR",
  "firstHrMessage": "您好，方便聊聊吗？（可选，作为首条 INBOUND）"
}
```

**响应：** 同列表项结构（含 `id` / `hrName` / `job` / `lastMessage` 等）。

## 6.1 获取对话列表

```
GET /api/v1/conversations
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认20 |
| status | String | 否 | 状态过滤 |

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "uuid",
        "job": {
          "id": "uuid",
          "title": "Java高级工程师",
          "companyName": "XX科技"
        },
        "hrName": "李HR",
        "hrAvatar": "https://...",
        "status": "CHATTING",
        "lastMessage": {
          "content": "您方便说下期望薪资吗？",
          "direction": "INBOUND",
          "sentAt": "2026-09-02T10:00:00Z"
        },
        "totalMessages": 5,
        "unreadCount": 1,
        "greetingSent": true,
        "lastMessageAt": "2026-09-02T10:00:00Z"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 50,
    "totalPages": 3
  }
}
```

## 6.2 获取对话详情

```
GET /api/v1/conversations/{id}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "job": {
      "id": "uuid",
      "title": "Java高级工程师",
      "companyName": "XX科技",
      "city": "北京"
    },
    "hrName": "李HR",
    "hrAvatar": "https://...",
    "status": "CHATTING",
    "greetingSent": true,
    "greetingText": "您好！看到您在招聘Java工程师...",
    "messages": [
      {
        "id": "uuid",
        "direction": "OUTBOUND",
        "content": "您好！看到您在招聘Java工程师...",
        "status": "SENT",
        "sentAt": "2026-09-02T09:00:00Z",
        "aiMetadata": {
          "agentType": "GREET",
          "confidence": 0.92
        }
      },
      {
        "id": "uuid",
        "direction": "INBOUND",
        "content": "您好！看到您投递了我们公司的Java工程师岗位，方便聊聊吗？",
        "status": "READ",
        "sentAt": "2026-09-02T09:30:00Z",
        "hrIntent": {
          "type": "GENERAL",
          "confidence": 0.95
        }
      },
      {
        "id": "uuid",
        "direction": "INBOUND",
        "content": "您方便说下期望薪资吗？",
        "status": "READ",
        "sentAt": "2026-09-02T10:00:00Z",
        "hrIntent": {
          "type": "SALARY_INQUIRY",
          "confidence": 0.88
        }
      }
    ],
    "totalMessages": 3,
    "lastMessageAt": "2026-09-02T10:00:00Z"
  }
}
```

## 6.3 发送消息（手动）

```
POST /api/v1/conversations/{id}/messages
```

**请求体：**

```json
{
  "content": "我的期望薪资是30k-35k",
  "direction": "OUTBOUND"
}
```

`direction`：`INBOUND`=录入 HR 来信；`OUTBOUND`=我发出（默认）。

## 6.4 获取 AI 推荐回复

```
GET /api/v1/conversations/{id}/suggested-reply
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "suggestions": [
      {
        "content": "我的期望薪资是30k-35k，看贵司的薪资结构和福利如何？",
        "confidence": 0.92,
        "intent": "SALARY_NEGOTIATION"
      },
      {
        "content": "我目前在职，计划月底离职，入职时间可以灵活安排。",
        "confidence": 0.85,
        "intent": "AVAILABILITY"
      }
    ],
    "context": {
      "hrLastMessage": "您方便说下期望薪资吗？",
      "hrIntent": "SALARY_INQUIRY"
    }
  }
}
```

## 6.5 发送 AI 推荐回复

```
POST /api/v1/conversations/{id}/send-suggested-reply
```

**请求体：**

```json
{
  "suggestionIndex": 0,
  "content": "我的期望薪资是30k-35k，看贵司的薪资结构和福利如何？"
}
```

---

# 七、投递接口

## 7.0 创建投递

```
POST /api/v1/applications
```

**请求体：**

```json
{
  "jobId": "uuid",
  "resumeId": "uuid",
  "applyNote": "可选备注"
}
```

同一用户同一岗位已投递过 → `IllegalStateException`（冲突）。成功后岗位状态可变为 `APPLIED`。

## 7.1 获取投递列表

```
GET /api/v1/applications
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认20 |
| status | String | 否 | 状态过滤 |

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "uuid",
        "job": {
          "id": "uuid",
          "title": "Java高级工程师",
          "companyName": "XX科技",
          "city": "北京"
        },
        "resume": {
          "id": "uuid",
          "name": "技术简历V2"
        },
        "status": "INTERVIEWING",
        "matchScore": 85,
        "appliedAt": "2026-09-02T10:00:00Z",
        "timeline": [
          {
            "eventType": "APPLICATION_SUBMITTED",
            "title": "投递了简历",
            "occurredAt": "2026-09-02T10:00:00Z"
          },
          {
            "eventType": "RESUME_VIEWED",
            "title": "简历被查看",
            "occurredAt": "2026-09-02T12:00:00Z"
          },
          {
            "eventType": "INTERVIEW_SCHEDULED",
            "title": "面试安排",
            "description": "一面：技术面试",
            "occurredAt": "2026-09-02T14:00:00Z"
          }
        ]
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 30,
    "totalPages": 2
  }
}
```

## 7.2 获取投递看板

```
GET /api/v1/applications/kanban
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "columns": [
      {
        "status": "SUBMITTED",
        "title": "已投递",
        "count": 10,
        "items": [...]
      },
      {
        "status": "VIEWED",
        "title": "已查看",
        "count": 8,
        "items": [...]
      },
      {
        "status": "REPLIED",
        "title": "HR有回复",
        "count": 5,
        "items": [...]
      },
      {
        "status": "INTERVIEWING",
        "title": "面试中",
        "count": 3,
        "items": [...]
      },
      {
        "status": "OFFER",
        "title": "Offer",
        "count": 1,
        "items": [...]
      }
    ],
    "closedColumns": [
      {
        "status": "REJECTED",
        "title": "不合适",
        "count": 15,
        "items": [...]
      },
      {
        "status": "WITHDRAWN",
        "title": "已撤回",
        "count": 2,
        "items": [...]
      }
    ]
  }
}
```

## 7.3 获取投递详情

```
GET /api/v1/applications/{id}
```

## 7.4 更新投递状态

```
PUT /api/v1/applications/{id}/status
```

**请求体：**

```json
{
  "status": "INTERVIEWING"
}
```

`status` 枚举：`SUBMITTED` / `VIEWED` / `REPLIED` / `INTERVIEWING` / `OFFERED` / `REJECTED` / `WITHDRAWN`。  
可选字段 `note`：写入时间线 `STATUS_CHANGED`。

## 7.5 添加投递备注

```
POST /api/v1/applications/{id}/notes
```

**请求体：**

```json
{
  "note": "一面约周五下午"
}
```

写入时间线 `NOTE`；若投递尚无 `applyNote`，顺带写入首条备注。列表 / 看板 / 详情的 `timeline` 字段返回事件数组（`eventType` / `title` / `description` / `occurredAt`）。

---

# 七-B、平台连接（V2 阶段 A · MOCK）

```
GET  /api/v1/platforms/connections
POST /api/v1/platforms/connections   { "platform": "MOCK" }
GET  /api/v1/platforms/tasks
POST /api/v1/platforms/tasks         { "connectionId", "action", "mode": "SEMI_AUTO", "rawDescription"?, "draftText"? }
POST /api/v1/platforms/tasks/{id}/confirm
```

`action`：`SYNC_JOBS` / `DRAFT_GREET` / `FILL_REPLY` / `MARK_APPLIED`。  
`SYNC_JOBS` 须带 `rawDescription`；确认后创建岗位 `source=IMPORT`（不爬站、**无独立 agent 仓**）。  
`DRAFT_GREET` / `FILL_REPLY` 须带 `draftText`；确认后 `result.clipboardText` 返回原文，前端可写入系统剪贴板（用户再粘贴到招聘 App）。  
`BOSS`/`LIEPIN` 创建连接 → 400。

---

# 八、策略接口

## 8.1 获取打招呼模板列表

```
GET /api/v1/templates
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "uuid",
        "name": "技术岗通用模板",
        "type": "GREETING",
        "content": "您好！看到您在招聘{{job_title}}，{{my_skills}}，{{enthusiasm}}",
        "isDefault": true,
        "useCount": 50,
        "avgResponseRate": 35.5,
        "isActive": true
      }
    ]
  }
}
```

## 8.2 创建打招呼模板

```
POST /api/v1/templates
```

**请求体：**

```json
{
  "name": "自定义模板",
  "type": "GREETING",
  "content": "您好！看到您在招聘{{job_title}}，{{my_skills}}，{{enthusiasm}}",
  "conditions": {
    "jobTitles": ["Java工程师", "后端开发"]
  }
}
```

## 8.3 更新打招呼模板

```
PUT /api/v1/templates/{id}
```

## 8.4 删除打招呼模板

```
DELETE /api/v1/templates/{id}
```

## 8.5 获取黑名单列表

```
GET /api/v1/blacklist
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "companies": [
      { "id": "uuid", "value": "XX公司", "reason": "加班严重", "addedAt": "2026-09-01" }
    ],
    "keywords": [
      { "id": "uuid", "value": "销售", "reason": "不感兴趣", "addedAt": "2026-09-01" }
    ]
  }
}
```

## 8.6 添加黑名单

```
POST /api/v1/blacklist
```

**请求体：**

```json
{
  "type": "COMPANY",
  "value": "XX公司",
  "reason": "加班严重",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

## 8.7 删除黑名单项

```
DELETE /api/v1/blacklist/{id}
```

---

# 九、统计接口

## 9.1 获取今日概览

```
GET /api/v1/stats/today
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "date": "2026-09-02",
    "jobsDiscovered": 15,
    "greetingsSent": 20,
    "greetingsReplied": 8,
    "applicationsSent": 5,
    "interviewsScheduled": 2,
    "replyRate": 40.0,
    "conversionRate": 62.5,
    "tokensUsed": 5000,
    "comparedToYesterday": {
      "greetingsSent": +5,
      "applicationsSent": +2
    }
  }
}
```

## 9.2 获取统计趋势

```
GET /api/v1/stats/trend
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| period | String | 是 | 统计周期：week/month/quarter |
| metrics | String | 否 | 指标，多个用逗号分隔 |

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "period": "week",
    "startDate": "2026-08-27",
    "endDate": "2026-09-02",
    "dataPoints": [
      {
        "date": "2026-08-27",
        "greetingsSent": 15,
        "applicationsSent": 3,
        "replyRate": 33.3
      },
      ...
    ],
    "summary": {
      "totalGreetings": 100,
      "totalApplications": 25,
      "avgReplyRate": 38.5,
      "avgInterviewRate": 12.0
    }
  }
}
```

## 9.3 获取求职漏斗

```
GET /api/v1/stats/funnel
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalJobsDiscovered": 200,
    "totalGreetingsSent": 150,
    "totalReplies": 60,
    "totalApplications": 40,
    "totalInterviews": 10,
    "totalOffers": 2,
    "funnel": [
      { "stage": "发现岗位", "count": 200, "percentage": 100 },
      { "stage": "打招呼", "count": 150, "percentage": 75 },
      { "stage": "HR回复", "count": 60, "percentage": 30 },
      { "stage": "投递简历", "count": 40, "percentage": 20 },
      { "stage": "进入面试", "count": 10, "percentage": 5 },
      { "stage": "拿到Offer", "count": 2, "percentage": 1 }
    ],
    "conversionRates": {
      "greetToReply": 40.0,
      "replyToApply": 66.7,
      "applyToInterview": 25.0,
      "interviewToOffer": 20.0
    }
  }
}
```

## 9.4 生成周报

```
POST /api/v1/stats/weekly-report
```

**请求体：**

```json
{
  "weekStartDate": "2026-08-26"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "reportId": "uuid",
    "weekStartDate": "2026-08-26",
    "weekEndDate": "2026-09-01",
    "summary": {
      "greetingsSent": 80,
      "repliesReceived": 32,
      "applicationsSent": 20,
      "interviewsScheduled": 5
    },
    "highlights": [
      "本周打招呼数较上周增长 20%",
      "某公司的 Java 岗位回复率达到 60%"
    ],
    "concerns": [
      "简历投递后回复率偏低，建议优化简历内容"
    ],
    "suggestions": [
      "建议增加对 XX 技术的简历描述",
      "可以尝试对 XX 类岗位打招呼"
    ],
    "content": "## 本周求职复盘\n\n### 概览\n...（AI 生成的完整周报）"
  }
}
```

---

# 十、AI Agent 接口

## 10.1 生成打招呼话术

```
POST /api/v1/ai/greet
```

**请求体：**

```json
{
  "jobId": "uuid",
  "resumeId": "uuid"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "greetingText": "您好！看到您在招聘Java高级工程师，熟悉Spring Cloud微服务架构，有5年Java开发经验。贵司的项目方向我很感兴趣，希望能进一步沟通～",
    "confidence": 0.92,
    "executionId": "uuid",
    "model": "gpt-4o"
  }
}
```

## 10.2 生成聊天回复

```
POST /api/v1/ai/chat-reply
```

**请求体：**

```json
{
  "conversationId": "uuid",
  "hrMessage": "您方便说下期望薪资吗？"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "replyText": "我的期望薪资是30k-35k，看贵司的薪资结构和福利如何？",
    "confidence": 0.88,
    "intent": "SALARY_INQUIRY",
    "executionId": "uuid",
    "model": "gpt-4o"
  }
}
```

## 10.3 分析 HR 意图

```
POST /api/v1/ai/analyze-intent
```

**请求体：**

```json
{
  "conversationId": "uuid",
  "message": "方便约个面试吗？下周三上午可以吗？"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "intent": "INTERVIEW_INVITE",
    "confidence": 0.95,
    "entities": {
      "interviewTime": "下周三上午"
    },
    "suggestedActions": [
      "确认面试时间",
      "询问面试形式"
    ]
  }
}
```

## 10.4 计算匹配度

```
POST /api/v1/ai/match
```

**请求体：**

```json
{
  "jobId": "uuid",
  "resumeId": "uuid"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "overallScore": 85,
    "skillMatch": {
      "score": 80,
      "matched": ["Java", "Spring Boot", "MySQL"],
      "missing": ["Kafka", "Kubernetes"]
    },
    "experienceMatch": {
      "score": 90,
      "yearsMatch": true,
      "levelMatch": true
    },
    "recommendation": "APPLY",
    "reason": "技能匹配度较高，建议投递"
  }
}
```

## 10.5 批量匹配（可选）

```
POST /api/v1/ai/match/batch
```

**请求体：**

```json
{
  "jobId": "uuid",
  "resumeIds": ["uuid1", "uuid2", "uuid3"]
}
```

---

# 十一、Webhook 接口（可选）

## 11.1 注册 Webhook

```
POST /api/v1/webhooks
```

**请求体：**

```json
{
  "url": "https://your-server.com/webhook",
  "events": ["application.status_changed", "conversation.new_message"],
  "secret": "your-secret"
}
```

## 11.2 Webhook 事件格式

```json
{
  "event": "application.status_changed",
  "timestamp": "2026-09-02T10:00:00Z",
  "data": {
    "applicationId": "uuid",
    "oldStatus": "SUBMITTED",
    "newStatus": "REPLIED",
    "job": {
      "title": "Java高级工程师",
      "companyName": "XX科技"
    }
  }
}
```

---

# 十二、轮询接口

## 12.1 获取任务状态

```
GET /api/v1/tasks/{taskId}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "type": "RESUME_ANALYSIS",
    "status": "RUNNING",
    "progress": 50,
    "result": null,
    "error": null,
    "createdAt": "2026-09-02T10:00:00Z",
    "completedAt": null
  }
}
```

---

# 附录：错误码完整列表

| 错误码 | 说明 |
|-------|------|
| 0 | 成功 |
| 1001 | 参数错误 |
| 1002 | 缺少必填参数 |
| 1003 | 参数格式错误 |
| 1004 | 参数值超出范围 |
| 2001 | 认证失败 |
| 2002 | Token 过期 |
| 2003 | Token 无效 |
| 2004 | 权限不足 |
| 3001 | 资源不存在 |
| 3002 | 资源已存在 |
| 3003 | 资源状态不允许操作 |
| 4001 | 操作被拒绝 |
| 4002 | BOSS 未连接 |
| 4003 | 今日额度已用完 |
| 4004 | 打招呼过于频繁 |
| 5001 | 系统错误 |
| 5002 | AI 服务异常 |
| 5003 | BOSS 服务异常 |
| 5004 | 数据库错误 |
| 5005 | 文件存储异常 |
| 5006 | 异步任务执行失败 |

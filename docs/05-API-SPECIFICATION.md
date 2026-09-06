# AI Job Hunter - API 规范

> 版本：V1.0
> 日期：2026-09-02
> 状态：草稿
> 负责人：待定

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|-----|
| 2026-09-02 | v1.0 | 初始版本 | - |

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

**请求体：**

```json
{
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

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "uuid",
    "status": "ANALYZING",
    "estimatedTime": 30
  }
}
```

**轮询状态：**

```
GET /api/v1/resumes/{id}/analysis/status
```

## 4.7 重写简历

```
POST /api/v1/resumes/{id}/rewrite
```

**请求体：**

```json
{
  "targetJobId": "uuid",      // 可选，针对特定岗位优化
  "focusOn": ["skills", "experience"],  // 可选，优化重点
  "tone": "professional"      // professional / casual / technical
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "versionId": "uuid",
    "status": "REWRITING",
    "estimatedTime": 60
  }
}
```

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

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "ANALYZING",
    "estimatedTime": 10
  }
}
```

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
  "content": "我的期望薪资是30k-35k"
}
```

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
  "status": "INTERVIEWING",
  "note": "约了明天上午10点面试"
}
```

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

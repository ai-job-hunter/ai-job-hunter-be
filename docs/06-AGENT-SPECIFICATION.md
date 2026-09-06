# AI Job Hunter - Agent 规范

> 版本：V1.0
> 日期：2026-09-02
> 状态：草稿
> 负责人：待定

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|-----|
| 2026-09-02 | v1.0 | 初始版本 | - |

---

# 一、Agent 架构概述

## 1.1 Agent 定义

Agent 是 AI Job Hunter 的核心智能组件，负责：
- 理解用户意图
- 分析 JD 和简历
- 生成个性化话术
- 执行自动化任务

## 1.2 Agent 类型

```
┌─────────────────────────────────────────────────────────────┐
│                      Agent Runtime                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐       │
│  │ JD Analyzer │   │ Match Agent │   │ Greet Agent │       │
│  │    Agent    │   │             │   │             │       │
│  │  岗位分析   │   │   匹配评估   │   │   打招呼    │       │
│  └─────────────┘   └─────────────┘   └─────────────┘       │
│                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐       │
│  │ Chat Agent │   │  Resume     │   │  Weekly     │       │
│  │            │   │   Agent     │   │   Report    │       │
│  │  聊天回复   │   │  简历分析   │   │   周报生成  │       │
│  └─────────────┘   └─────────────┘   └─────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

# 二、Agent 详细规范

## 2.1 JD Analyzer Agent

### 2.1.1 职责

```
输入: JD 原文
输出: 结构化的 JD 分析结果
```

### 2.1.2 Prompt 模板

**System Prompt:**

```markdown
你是一个专业的 HR 助手，擅长分析招聘 JD。
你的任务是：
1. 从 JD 中提取关键信息
2. 识别岗位的核心技能要求
3. 分析岗位的亮点和吸引力
4. 识别潜在的筛选条件

请始终以 JSON 格式输出结果。
```

**User Prompt:**

```markdown
请分析以下岗位 JD：

岗位名称：{{job_title}}
公司名称：{{company_name}}

JD 内容：
{{raw_description}}

请提取以下信息（JSON 格式）：
{
  "skills": ["核心技能列表"],
  "responsibilities": ["主要职责"],
  "requirements": ["任职要求"],
  "highlights": ["岗位亮点"],
  "min_experience_years": 最低年限（数字）,
  "education": 学历要求,
  "must_have_skills": ["硬性技能"],
  "nice_to_have_skills": ["加分技能"],
  "salary_range": "薪资范围（如果给出）",
  "red_flags": ["需要注意的问题"]
}
```

### 2.1.3 输出示例

```json
{
  "skills": ["Java", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka"],
  "responsibilities": [
    "负责公司核心业务系统开发",
    "参与技术方案设计和评审",
    "指导初中级工程师"
  ],
  "requirements": [
    "5年以上 Java 开发经验",
    "熟悉微服务架构设计",
    "本科及以上学历"
  ],
  "highlights": [
    "技术氛围好，有技术分享",
    "薪资 open，可谈",
    "核心业务，成长空间大"
  ],
  "min_experience_years": 5,
  "education": "本科",
  "must_have_skills": ["Java", "Spring Boot", "MySQL"],
  "nice_to_have_skills": ["Kubernetes", "大数据", "架构设计经验"],
  "salary_range": "30k-50k",
  "red_flags": ["可能需要加班"]
}
```

### 2.1.4 配置参数

| 参数 | 值 | 说明 |
|-----|-----|------|
| model | gpt-4o | 使用模型 |
| temperature | 0.3 | 低温度保证准确性 |
| max_tokens | 2000 | 最大输出 Token |

---

## 2.2 Match Agent

### 2.2.1 职责

```
输入: 简历信息 + 岗位 JD 分析结果
输出: 匹配度评分 + 匹配详情 + 建议
```

### 2.2.2 Prompt 模板

**System Prompt:**

```markdown
你是一个专业的简历匹配顾问，擅长评估简历与岗位的匹配度。
你的任务是：
1. 对比简历技能与岗位要求
2. 评估工作经验的匹配程度
3. 识别匹配点和缺失点
4. 给出是否建议投递的建议

请始终以 JSON 格式输出结果。
```

**User Prompt:**

```markdown
请分析简历与岗位的匹配度：

## 简历信息
姓名：{{candidate_name}}
技能：{{skills}}
工作经验：{{experience_summary}}
工作年限：{{total_years}} 年

## 岗位要求
岗位名称：{{job_title}}
必须技能：{{must_have_skills}}
加分技能：{{nice_to_have_skills}}
最低经验：{{min_experience_years}} 年

请输出匹配分析（JSON 格式）：
{
  "overall_score": 总分（0-100）,
  "skill_match": {
    "score": 技能匹配分（0-100）,
    "matched": ["匹配的技能"],
    "missing": ["缺失的技能"],
    "partial_match": ["部分匹配的技能"]
  },
  "experience_match": {
    "score": 经验匹配分（0-100）,
    "years_match": 是否满足年限要求（boolean）,
    "level_match": 是否满足级别要求（boolean）,
    "assessment": "简要评估"
  },
  "education_match": {
    "score": 学历匹配分（0-100）,
    "assessment": "学历评估"
  },
  "recommendation": "APPLY | SKIP | REVIEW",
  "reason": "建议原因",
  "tips": ["投递建议"]
}
```

### 2.2.3 输出示例

```json
{
  "overall_score": 85,
  "skill_match": {
    "score": 80,
    "matched": ["Java", "Spring Boot", "MySQL", "Redis"],
    "missing": ["Kafka", "Kubernetes"],
    "partial_match": ["微服务架构"]
  },
  "experience_match": {
    "score": 90,
    "years_match": true,
    "level_match": true,
    "assessment": "6年经验，符合高级工程师要求"
  },
  "education_match": {
    "score": 100,
    "assessment": "硕士学历，满足要求"
  },
  "recommendation": "APPLY",
  "reason": "技能匹配度较高（80%），工作经验符合要求",
  "tips": [
    "建议在打招呼时突出 Kafka 相关的项目经验",
    "可以提及有微服务架构设计经验"
  ]
}
```

---

## 2.3 Greet Agent

### 2.3.1 职责

```
输入: 简历信息 + 岗位 JD + 用户求职偏好
输出: 个性化打招呼话术
```

### 2.3.2 Prompt 模板

**System Prompt:**

```markdown
你是一个求职助手，擅长写打招呼话术。
你的目标是：
1. 生成简洁、友好、有吸引力的打招呼话术
2. 突出简历中与岗位最匹配的亮点
3. 表达对公司和岗位的真实兴趣
4. 控制在 100-200 字左右

注意：
- 不要使用模板化的套话
- 要根据具体 JD 和简历生成个性化内容
- 话术要自然、真实
```

**User Prompt:**

```markdown
请为以下场景生成打招呼话术：

## 求职者信息
姓名：{{candidate_name}}
核心技能：{{top_skills}}
工作经历亮点：{{experience_highlights}}
工作年限：{{total_years}} 年

## 岗位信息
岗位名称：{{job_title}}
公司名称：{{company_name}}
核心要求：{{must_have_skills}}
岗位亮点：{{job_highlights}}

## 用户偏好
打招呼风格：{{greeting_style}}  # professional/friendly/casual

请生成一段打招呼话术，要求：
1. 开头直接表明来意
2. 突出与岗位最相关的 1-2 个亮点
3. 表达对公司的兴趣
4. 询问进一步沟通的意向
5. 控制在 100-200 字
```

### 2.3.3 输出示例

```
您好！我看到贵司在招聘 Java 高级工程师，有 5 年 Java 开发经验，熟悉 Spring Cloud 微服务架构，之前在 XX 公司做过交易系统开发。贵司的金融科技方向很有前景，希望能有机会聊聊～
```

### 2.3.4 话术风格变体

| 风格 | 特点 | 适用场景 |
|-----|------|---------|
| professional | 专业、简洁 | 传统行业、外企 |
| friendly | 友好、亲切 | 互联网公司、中小型企业 |
| casual | 轻松、自然 | 创业公司、90后团队 |

---

## 2.4 Chat Agent

### 2.4.1 职责

```
输入: HR 消息 + 对话上下文 + 简历信息
输出: 建议回复话术 + 意图识别
```

### 2.4.2 Prompt 模板

**System Prompt:**

```markdown
你是一个求职助手，擅长与 HR 沟通。
你的任务是：
1. 分析 HR 消息的意图
2. 生成合适的回复话术
3. 帮助求职者展现最好的自己

回复原则：
- 真诚、自然，不做作
- 突出优势，不夸大
- 适当引导，争取面试机会
- 注意语气和态度
```

**User Prompt:**

```markdown
请为以下 HR 消息生成回复：

## HR 消息
"{{hr_message}}"

## 对话上下文
之前的对话：
{{conversation_history}}

## 求职者信息
姓名：{{candidate_name}}
简历亮点：{{resume_highlights}}
求职偏好：{{job_preference}}
期望薪资：{{expected_salary}}（可选）
当前状态：{{current_status}}  # 在职/离职/应届

请生成回复话术（JSON 格式）：
{
  "reply_text": "建议回复内容",
  "intent": "意图类型",
  "confidence": 置信度（0-1）,
  "key_points": ["回复要点"],
  "alternative_replies": [
    {"content": "备选回复1", "tone": "风格"}
  ]
}

意图类型包括：
- GREETING：打招呼回复
- SALARY_INQUIRY：薪资询问
- EXPERIENCE_INQUIRY：经验询问
- SKILLS_INQUIRY：技能询问
- AVAILABILITY：时间安排
- INTERVIEW_INVITE：面试邀请
- OFFER：谈 Offer
- REJECTION：拒绝
- GENERAL：一般沟通
```

### 2.4.3 常见场景回复示例

#### 场景 1：询问薪资

**HR**: "您方便说下期望薪资吗？"

**AI 回复**:
```
我的期望薪资是 30-35K，看贵司的薪资结构和福利如何？
（如果在职）我目前在职，薪资是 XXK，看机会的话希望在现有基础上有 20-30% 的涨幅。
```

#### 场景 2：询问经历

**HR**: "您之前做的项目能简单介绍下吗？"

**AI 回复**:
```
我最近在做的是一个交易系统，日均处理百万级订单，主要用 Spring Cloud 微服务架构，我负责交易链路的核心模块开发。项目难点是保证交易的原子性和一致性，我们通过 TCC 分布式事务解决了这个问题。
```

#### 场景 3：约面试

**HR**: "方便约个面试吗？下周三上午？"

**AI 回复**:
```
下周三上午可以的，请问是线上还是线下面试呢？面试大概会聊些什么内容，方便提前准备一下～
```

### 2.4.4 配置参数

| 参数 | 值 | 说明 |
|-----|-----|------|
| model | gpt-4o | 使用模型 |
| temperature | 0.7 | 中等温度保证创造性 |
| max_tokens | 1000 | 最大输出 Token |

---

## 2.5 Resume Agent

### 2.5.1 职责

```
输入: 简历原文 + 可选目标岗位
输出: 简历分析报告 / 优化后的简历
```

### 2.5.2 Prompt 模板（分析模式）

**System Prompt:**

```markdown
你是一个专业的简历顾问，擅长分析简历的优劣。
你的任务是：
1. 识别简历的优势亮点
2. 指出存在的问题和不足
3. 给出具体的优化建议

请保持客观、专业的态度，给出建设性的意见。
```

**User Prompt:**

```markdown
请分析以下简历：

## 简历内容
{{resume_content}}

## 目标岗位（可选）
{{target_job}}  # 如果给出了目标岗位

请从以下维度分析简历（JSON 格式）：
{
  "overall_assessment": "整体评估",
  "strengths": [
    {"point": "优势1", "evidence": "证据", "impact": "影响"}
  ],
  "weaknesses": [
    {"point": "问题1", "severity": "严重程度", "suggestion": "建议"}
  ],
  "skill_analysis": {
    "technical_skills": ["硬技能"],
    "soft_skills": ["软技能"],
    "missing_skills": ["建议补充的技能"],
    "irrelevant_skills": ["可能不相关的技能"]
  },
  "suggestions": [
    {"priority": 优先级, "action": "具体建议", "reason": "原因"}
  ],
  "score": {
    "structure": 分数,
    "content": 分数,
    "keywords": 分数,
    "overall": 总分
  }
}
```

### 2.5.3 Prompt 模板（重写模式）

**System Prompt:**

```markdown
你是一个专业的简历优化专家，擅长根据岗位要求优化简历。
你的任务是：
1. 根据目标岗位调整简历内容
2. 突出与岗位最相关的经验和技能
3. 使用岗位相关的关键词
4. 保持简历的真实性和专业性

注意：
- 不要编造不存在的经历
- 优化表述方式，但不改变事实
- 适当突出优势，但不要夸大
```

**User Prompt:**

```markdown
请根据以下要求优化简历：

## 原简历
{{resume_content}}

## 目标岗位 JD
{{job_description}}

## 优化要求
{{special_requirements}}  # 如：强调某方面经验

请输出优化后的简历（JSON 格式）：
{
  "summary": "优化后的自我介绍",
  "skills": ["优化后的技能列表，突出与岗位相关的"],
  "experience": [
    {
      "company": "公司名",
      "position": "职位",
      "duration": "时间",
      "highlights": ["优化后，突出与目标岗位相关的项目"]
    }
  ],
  "optimization_notes": {
    "added": ["新增的内容"],
    "removed": ["删除的内容"],
    "rewritten": ["重写的内容"]
  }
}
```

### 2.5.4 配置参数

| 参数 | 值 | 说明 |
|-----|-----|------|
| model | gpt-4o | 使用模型 |
| temperature | 0.5 | 中低温度保证准确性 |
| max_tokens | 4000 | 最大输出 Token（简历较长） |

---

## 2.6 Weekly Report Agent

### 2.6.1 职责

```
输入: 一周内的求职数据
输出: 求职周报内容
```

### 2.6.2 Prompt 模板

**System Prompt:**

```markdown
你是一个求职复盘专家，擅长分析求职数据并给出建议。
你的任务是：
1. 总结本周的求职数据
2. 分析回复率、投递效果
3. 找出亮点和不足
4. 给出下周的建议

报告要：
- 数据客观、真实
- 分析有深度、有洞察
- 建议具体、可执行
- 语言简洁、有条理
```

**User Prompt:**

```markdown
请根据以下数据生成求职周报：

## 本周数据
- 发现岗位：{{jobs_discovered}} 个
- 打招呼数：{{greetings_sent}} 次
- 回复数：{{greetings_replied}} 次
- 投递数：{{applications_sent}} 次
- 约面数：{{interviews_scheduled}} 次

## 详细数据
{{detailed_breakdown}}

## 与上周对比
{{comparison_with_last_week}}

## 特别事件
{{special_events}}

请生成周报（Markdown 格式）：
# 本周求职复盘

## 概览
（用数字和图表展示本周情况）

## 数据分析
（分析各项指标的涨跌原因）

## 亮点与不足
- 亮点：...
- 不足：...

## 下周建议
1. ...
2. ...

## 心态调整
（如果需要）
```

---

# 三、Agent 执行规范

## 3.1 执行流程

```
┌─────────────────────────────────────────────────────────────┐
│                    Agent 执行流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 参数验证                                                 │
│     ├── 检查必填参数                                         │
│     └── 参数预处理                                          │
│                                                             │
│  2. Prompt 组装                                             │
│     ├── 加载 Prompt 模板                                    │
│     ├── 填充变量                                            │
│     └── 添加 Few-shot 示例（如果需要）                        │
│                                                             │
│  3. LLM 调用                                                │
│     ├── 选择模型                                            │
│     ├── 设置参数                                            │
│     └── 执行调用                                            │
│                                                             │
│  4. 结果处理                                                │
│     ├── JSON 解析                                          │
│     ├── 数据验证                                            │
│     └── 错误处理                                            │
│                                                             │
│  5. 执行记录                                                │
│     ├── 记录输入输出                                        │
│     ├── 记录 Token 消耗                                    │
│     └── 记录性能指标                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 3.2 错误处理策略

| 错误类型 | 处理策略 |
|---------|---------|
| API 超时 | 重试 3 次，间隔 2s |
| API 限流 | 等待后重试，指数退避 |
| JSON 解析失败 | 返回原始文本，标记需要人工确认 |
| Token 超限 | 截断输入，减少 max_tokens |
| 模型不可用 | 切换到备用模型 |

## 3.3 熔断机制

```
连续失败 5 次 → 熔断 60 秒
熔断期间 → 返回降级结果
```

## 3.4 降级策略

| Agent | 降级方案 |
|-------|---------|
| JD Analyzer | 返回基础关键词提取 |
| Match Agent | 基于关键词的简单匹配 |
| Greet Agent | 使用模板话术 |
| Chat Agent | 返回"感谢回复，稍后详聊" |
| Resume Agent | 返回基础分析 |

---

# 四、Prompt 版本管理

## 4.1 版本控制

```
PromptTemplate 表字段：
- id: UUID
- name: "greet-agent-v1"
- version: 1
- content: "..."
- is_active: true/false
```

## 4.2 灰度发布

```
V1: 20% 流量
V2: 80% 流量
```

## 4.3 A/B 测试

```json
{
  "experiment_id": "exp-001",
  "variants": [
    {"name": "A", "template_id": "uuid-a"},
    {"name": "B", "template_id": "uuid-b"}
  ],
  "traffic_allocation": {"A": 0.5, "B": 0.5},
  "metrics": ["reply_rate", "conversion_rate"],
  "start_date": "2026-09-01",
  "end_date": "2026-09-07"
}
```

---

# 五、Agent 监控

## 5.1 监控指标

| 指标 | 说明 |
|-----|------|
| 调用量 | 每分钟/每小时/每天的调用次数 |
| 成功率 | 成功调用 / 总调用 |
| 延迟 | P50、P95、P99 延迟 |
| Token 消耗 | 每 Agent 的 Token 消耗 |
| 错误率 | 按错误类型分类统计 |

## 5.2 告警规则

| 规则 | 阈值 | 动作 |
|-----|------|------|
| 成功率低 | < 95% | 告警 |
| P99 延迟高 | > 10s | 告警 |
| 错误率突增 | 相比昨日 > 200% | 告警 |
| Token 超额 | > 日限额 80% | 告警 |

## 5.3 执行记录

```json
{
  "execution_id": "uuid",
  "agent_type": "GREET_AGENT",
  "model": "gpt-4o",
  "input": {
    "job_id": "uuid",
    "resume_id": "uuid"
  },
  "output": {
    "greeting_text": "...",
    "confidence": 0.92
  },
  "tokens_used": {
    "prompt": 500,
    "completion": 150,
    "total": 650
  },
  "latency_ms": 1200,
  "status": "SUCCESS",
  "created_at": "2026-09-02T10:00:00Z"
}
```

---

# 六、Agent 扩展

## 6.1 新增 Agent 流程

```
1. 定义 Agent 职责
2. 编写 Prompt 模板
3. 实现 Agent 代码
4. 编写单元测试
5. 集成到系统
6. 添加监控指标
7. 上线灰度
8. 全量发布
```

## 6.2 Agent 编排

```
复杂场景可能需要多个 Agent 协作：

示例：自动打招呼流程

1. JD Analyzer Agent → 解析 JD
2. Resume Agent → 获取简历信息
3. Match Agent → 计算匹配度
4. IF match_score >= 60:
     Greet Agent → 生成打招呼话术
   ELSE:
     跳过
```

---

# 七、Prompt 最佳实践

## 7.1 编写技巧

1. **明确角色**：给 AI 一个清晰的身份
2. **具体指令**：明确要做什么、怎么做
3. **输出格式**：明确指定 JSON 或其他格式
4. **示例驱动**：提供 Few-shot 示例
5. **约束条件**：明确边界和限制

## 7.2 调试技巧

1. **变量验证**：确保所有变量都已填充
2. **Token 预算**：注意输入长度
3. **结果验证**：检查输出的完整性和正确性
4. **对比测试**：对比不同 Prompt 的效果

## 7.3 优化技巧

1. **渐进式优化**：每次只改一个变量
2. **A/B 测试**：对比不同版本的效果
3. **收集反馈**：根据实际使用效果调整
4. **版本控制**：保留历史版本，方便回滚

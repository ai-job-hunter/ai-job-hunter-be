# AI Job Hunter - 领域模型

> 版本：V1.0
> 日期：2026-09-02
> 状态：草稿
> 负责人：待定

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|-----|
| 2026-09-02 | v1.0 | 初始版本 | - |

---

# 一、领域概览

## 1.1 领域划分

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AI Job Hunter 领域模型                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Resume    │  │    Job      │  │   Chat      │  │ Application │    │
│  │   简历域     │  │   岗位域     │  │   沟通域     │  │   投递域     │    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                     │
│  │  Strategy   │  │   Agent     │  │    User     │                     │
│  │   策略域     │  │   Agent域   │  │   用户域     │                     │
│  └─────────────┘  └─────────────┘  └─────────────┘                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 1.2 领域依赖关系

```
                    ┌─────────┐
                    │  User   │
                    └────┬────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │ Resume   │   │ Strategy │   │  Agent   │
   └────┬─────┘   └────┬─────┘   └────┬─────┘
        │               │               │
        │               ▼               │
        │         ┌──────────┐          │
        │         │   Job    │          │
        │         └────┬─────┘          │
        │              │                │
        │              ▼                │
        │         ┌──────────┐          │
        │         │  Chat    │          │
        │         └────┬─────┘          │
        │              │                │
        └──────────────┼────────────────┘
                       ▼
                 ┌──────────┐
                 │Application│
                 └──────────┘
```

---

# 二、用户域 (User)

## 2.1 User

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // 基础信息
    private String username;           // 用户名
    private String email;              // 邮箱
    private String phone;              // 手机号（用于 BOSS 登录）
    private String passwordHash;        // 密码（BCrypt）

    // BOSS 账号信息（加密存储）
    @Column(columnDefinition = "text")
    private String bossCookie;         // BOSS Cookie
    private String bossToken;          // BOSS Token

    // 求职偏好
    @Embedded
    private JobPreference preference;  // 求职偏好

    // 自动化设置
    @Embedded
    private AutomationConfig automationConfig;  // 自动化配置

    // 状态
    private Boolean bossConnected;     // BOSS 是否已连接
    private LocalDateTime bossConnectedAt;  // BOSS 连接时间
    private LocalDateTime lastActiveAt;    // 最后活跃时间

    // 状态
    private UserStatus status;         // 状态

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum UserStatus {
    ACTIVE,          // 正常
    SUSPENDED,       // 封禁
    DELETED          // 已删除
}
```

## 2.2 JobPreference（求职偏好）

```java
@Embeddable
public class JobPreference {

    private String[] keywords;         // 求职关键词：["Java", "后端", "Spring"]
    private String[] cities;            // 求职城市：["北京", "上海"]
    private Integer salaryMin;          // 最低薪资
    private Integer salaryMax;         // 最高薪资
    private String[] jobTypes;         // 工作类型：["全职", "实习"]
    private String[] experienceLevels;  // 经验要求：["1-3年", "3-5年"]
    private String[] educationLevels;  // 学历要求：["本科", "硕士"]
    private Boolean remoteAllowed;      // 是否接受远程
}
```

## 2.3 AutomationConfig（自动化配置）

```java
@Embeddable
public class AutomationConfig {

    @Enumerated(EnumType.STRING)
    private AutomationMode mode;       // 自动化模式

    private Integer dailyGreetLimit;   // 每日打招呼上限
    private Integer dailyApplyLimit;   // 每日投递上限
    private Integer replyDelaySeconds; // 回复延迟（秒）
    private Boolean autoApply;          // HR 回复后自动投递
    private Boolean soundNotification;  // 声音提醒
}
```

```java
public enum AutomationMode {
    FULL,     // 全自动
    SEMI,     // 半自动（需要确认）
    MANUAL    // 手动模式
}
```

---

# 三、简历域 (Resume)

## 3.1 Resume（简历主表）

```java
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;             // 所属用户

    // 简历基本信息
    private String name;               // 简历名称（如：技术简历/产品简历）
    private ResumeType type;           // 简历类型
    private Boolean isDefault;         // 是否默认简历

    // 解析后的简历数据
    @Column(columnDefinition = "jsonb")
    private JsonNode parsedData;       // 解析后的简历内容

    // 文件信息
    @Column(columnDefinition = "jsonb")
    private JsonNode fileInfo;         // 文件信息（name, path, size, mime）

    // AI 分析
    @OneToOne(mappedBy = "resume")
    private ResumeAnalysis analysis;   // 简历分析结果

    // 版本管理
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL)
    private List<ResumeVersion> versions;  // 历史版本

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum ResumeType {
    GENERAL,     // 通用简历
    TECHNICAL,    // 技术简历
    MANAGEMENT,  // 管理简历
    CUSTOM       // 自定义
}
```

## 3.2 ResumeVersion（简历版本）

```java
@Entity
@Table(name = "resume_versions")
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String resumeId;          // 关联简历
    private Integer versionNumber;     // 版本号

    // 版本内容
    @Column(columnDefinition = "jsonb")
    private JsonNode content;         // 简历内容快照

    // 版本说明
    private String changeNote;         // 变更说明
    private VersionSource source;      // 来源：AUTO_REWRITE / MANUAL_EDIT

    private LocalDateTime createdAt;
}

public enum VersionSource {
    ORIGINAL,        // 原始上传
    AUTO_REWRITE,    // AI 自动重写
    MANUAL_EDIT,     // 手动编辑
    TEMPLATE         // 模板生成
}
```

## 3.3 ResumeAnalysis（简历分析）

```java
@Entity
@Table(name = "resume_analyses")
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String resumeId;          // 关联简历

    // 分析结果
    @Column(columnDefinition = "jsonb")
    private JsonNode strengths;       // 优势亮点

    @Column(columnDefinition = "jsonb")
    private JsonNode weaknesses;       // 问题不足

    @Column(columnDefinition = "jsonb")
    private JsonNode suggestions;     // 优化建议

    // 技能提取
    @Column(columnDefinition = "jsonb")
    private JsonNode extractedSkills; // 提取的技能列表

    @Column(columnDefinition = "jsonb")
    private JsonNode extractedExperience;  // 提取的工作经历

    // AI 模型信息
    private String model;             // 使用的 AI 模型
    private Integer tokensUsed;       // 消耗的 Token

    private LocalDateTime analyzedAt;
}
```

---

# 四、岗位域 (Job)

## 4.1 Job（岗位主表）

```java
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;           // 所属用户

    // BOSS 平台信息
    private String bossJobId;         // BOSS 平台岗位 ID
    private String bossCompanyId;     // BOSS 公司 ID
    private String source;            // 来源平台

    // 岗位基本信息
    private String title;             // 岗位名称
    private String companyName;       // 公司名称
    private String city;             // 城市
    private String district;          // 区域
    private String salaryMin;         // 薪资下限
    private String salaryMax;         // 薪资上限
    private String salaryText;        // 薪资文本（如：20k-35k·13薪）

    // JD 原始内容
    @Column(columnDefinition = "text")
    private String rawDescription;    // JD 原始文本

    // AI 分析结果
    @Column(columnDefinition = "jsonb")
    private JsonNode analysis;        // JD 分析结果

    @Column(columnDefinition = "jsonb")
    private JsonNode keywords;        // 关键词列表

    // 公司信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;          // 公司信息

    // 匹配度（与用户简历）
    @Column(columnDefinition = "jsonb")
    private JsonNode matchScores;     // 各简历版本的匹配度

    // 状态
    @Enumerated(EnumType.STRING)
    private JobStatus status;         // 状态

    // 来源信息
    private String sourceUrl;         // 来源 URL
    private LocalDateTime discoveredAt;  // 发现时间
    private LocalDateTime lastSyncedAt;  // 最后同步时间

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum JobStatus {
    NEW,           // 新发现
    ANALYZED,      // 已分析
    GREETED,       // 已打招呼
    REPLIED,       // HR 已回复
    APPLIED,       // 已投递
    INTERVIEWING,  // 面试中
    OFFER,         // 已 Offer
    REJECTED,      // 不合适
    EXPIRED,       // 已过期
    BLACKLISTED    // 黑名单
}
```

## 4.2 Company（公司表）

```java
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String bossCompanyId;     // BOSS 公司 ID
    private String name;              // 公司名称

    // 公司信息
    private String logo;              // 公司 Logo URL
    private String industry;           // 行业
    private String scale;              // 规模
    private String stage;              // 融资阶段
    private String description;        // 公司简介

    // BOSS 认证信息
    private Boolean verified;          // 是否已认证
    private Boolean bossDirect;        // 是否BOSS直聘认证

    // 黑名单
    private Boolean isBlacklisted;     // 是否在黑名单

    // 统计
    private Integer totalJobs;         // 在招岗位数
    private Integer activeJobs;        // 活跃岗位数

    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
}
```

## 4.3 JobAnalysis（岗位分析）

```java
@Entity
@Table(name = "job_analyses")
public class JobAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String jobId;             // 关联岗位

    // JD 解析结果
    @Column(columnDefinition = "jsonb")
    private JsonNode skills;          // 核心技能：["Java", "Spring Boot", "MySQL"]

    @Column(columnDefinition = "jsonb")
    private JsonNode responsibilities;  // 岗位职责

    @Column(columnDefinition = "jsonb")
    private JsonNode requirements;    // 任职要求

    @Column(columnDefinition = "jsonb")
    private JsonNode highlights;      // 岗位亮点

    // 硬性要求
    private Integer minExperienceYears;  // 最低经验要求
    private String educationRequirement;  // 学历要求
    @Column(columnDefinition = "jsonb")
    private JsonNode mustHaveSkills;  // 必须掌握的技能
    @Column(columnDefinition = "jsonb")
    private JsonNode niceToHaveSkills;  // 加分技能

    // 分析元数据
    private String model;             // 使用的 AI 模型
    private Integer tokensUsed;

    private LocalDateTime analyzedAt;
}
```

---

# 五、沟通域 (Chat)

## 5.1 Conversation（对话）

```java
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;           // 所属用户
    private String jobId;            // 关联岗位

    // BOSS 平台信息
    private String bossConversationId;  // BOSS 对话 ID
    private String bossHrId;         // HR ID
    private String hrName;            // HR 姓名
    private String hrAvatar;          // HR 头像

    // 状态
    @Enumerated(EnumType.STRING)
    private ConversationStatus status;

    // 消息统计
    private Integer totalMessages;    // 总消息数
    private Integer unreadCount;      // 未读消息数

    // 最后一条消息
    @Column(columnDefinition = "jsonb")
    private JsonNode lastMessage;    // 最后一条消息快照

    // 打招呼状态
    private Boolean greetingSent;    // 是否已打招呼
    private String greetingText;      // 打招呼内容
    private LocalDateTime greetedAt;  // 打招呼时间

    // 最新消息时间
    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum ConversationStatus {
    GREETING,      // 打招呼中（等待回复）
    CHATTING,      // 沟通中
    REPLIED,       // HR 有兴趣
    REJECTED,      // HR 拒绝
    APPLIED,       // 已投递简历
    INTERVIEWING,  // 面试安排中
    OFFERED,       // 已发 Offer
    CLOSED         // 对话关闭
}
```

## 5.2 Message（消息）

```java
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String conversationId;    // 关联对话

    // 消息方向
    @Enumerated(EnumType.STRING)
    private MessageDirection direction;

    // 消息内容
    @Column(columnDefinition = "text")
    private String content;           // 消息文本

    // 附件
    @Column(columnDefinition = "jsonb")
    private JsonNode attachments;     // 附件列表

    // AI 生成（如果是 AI 生成的消息）
    @Column(columnDefinition = "jsonb")
    private JsonNode aiMetadata;     // AI 元数据

    // AI 分析（如果是 HR 发的消息）
    @Column(columnDefinition = "jsonb")
    private JsonNode hrIntent;        // HR 意图分析

    // 发送状态
    @Enumerated(EnumType.STRING)
    private MessageStatus status;     // PENDING / SENT / READ / FAILED

    // 时间
    private LocalDateTime sentAt;     // 发送时间
    private LocalDateTime readAt;     // 已读时间

    private LocalDateTime createdAt;
}

public enum MessageDirection {
    INBOUND,   // HR -> 用户
    OUTBOUND   // 用户 -> HR
}

public enum MessageStatus {
    PENDING,   // 待发送
    SENT,      // 已发送
    READ,      // 已读
    FAILED     // 发送失败
}
```

## 5.3 GreetingTemplate（打招呼模板）

```java
@Entity
@Table(name = "greeting_templates")
public class GreetingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;           // 所属用户

    // 模板信息
    private String name;             // 模板名称
    private String description;       // 模板描述
    private TemplateType type;       // 模板类型

    // 模板内容
    @Column(columnDefinition = "text")
    private String content;          // 模板内容（支持变量替换）

    // 适用条件
    @Column(columnDefinition = "jsonb")
    private JsonNode conditions;     // 适用条件

    // 使用统计
    private Integer useCount;        // 使用次数
    private Double avgResponseRate;  // 平均回复率

    private Boolean isDefault;       // 是否默认模板
    private Boolean isActive;        // 是否启用

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum TemplateType {
    GREETING,      // 打招呼模板
    CHAT_REPLY,    // 聊天回复模板
    FOLLOW_UP,     // 跟进模板
    INTERVIEW,     // 面试确认模板
    REJECTION      // 拒绝模板
}
```

---

# 六、投递域 (Application)

## 6.1 Application（投递记录）

```java
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;           // 所属用户
    private String jobId;            // 关联岗位
    private String conversationId;   // 关联对话
    private String resumeId;         // 投递的简历版本

    // BOSS 平台信息
    private String bossApplicationId;  // BOSS 投递 ID

    // 投递信息
    private LocalDateTime appliedAt;   // 投递时间
    private String applyNote;          // 投递备注

    // 简历匹配信息
    private Integer matchScore;        // 匹配度分数
    @Column(columnDefinition = "jsonb")
    private JsonNode matchedSkills;    // 匹配的技能
    @Column(columnDefinition = "jsonb")
    private JsonNode missingSkills;    // 缺失的技能

    // 投递状态
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    // HR 回复信息
    @Column(columnDefinition = "jsonb")
    private JsonNode hrFeedback;     // HR 反馈

    // 面试安排
    @Column(columnDefinition = "jsonb")
    private JsonNode interviewSchedule;  // 面试安排

    // 时间线
    private LocalDateTime repliedAt;   // HR 回复时间
    private LocalDateTime interviewedAt;  // 面试时间
    private LocalDateTime offeredAt;   // Offer 时间
    private LocalDateTime closedAt;    // 结束时间

    private String closedReason;       // 结束原因

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum ApplicationStatus {
    PENDING,       // 待处理
    SUBMITTED,     // 已投递
    VIEWED,        // HR 已查看
    REPLIED,       // HR 有回复
    INTERVIEWING,  // 面试中
    OFFER,         // 已 Offer
    REJECTED,      // 已拒绝
    WITHDRAWN,     // 已撤回
    EXPIRED        // 已过期
}
```

## 6.2 ApplicationTimeline（投递时间线）

```java
@Entity
@Table(name = "application_timelines")
public class ApplicationTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String applicationId;    // 关联投递

    @Enumerated(EnumType.STRING)
    private TimelineEventType eventType;

    private String title;            // 事件标题
    private String description;      // 事件描述
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;       // 额外数据

    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}

public enum TimelineEventType {
    GREETING_SENT,      // 打招呼
    GREETING_REPLIED,   // 打招呼被回复
    APPLICATION_SUBMITTED,  // 简历投递
    RESUME_VIEWED,      // 简历被查看
    HR_REPLY,           // HR 回复
    INTERVIEW_SCHEDULED,  // 面试安排
    INTERVIEW_COMPLETED,  // 面试完成
    OFFER_RECEIVED,     // 收到 Offer
    APPLICATION_REJECTED, // 被拒绝
    APPLICATION_WITHDRAWN,  // 撤回投递
    STATUS_CHANGED      // 状态变更
}
```

---

# 七、策略域 (Strategy)

## 7.1 Blacklist（黑名单）

```java
@Entity
@Table(name = "blacklists")
public class Blacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;           // 所属用户

    @Enumerated(EnumType.STRING)
    private BlacklistType type;     // 黑名单类型

    private String value;            // 黑名单值
    private String reason;          // 加入原因
    private String note;            // 备注

    private LocalDateTime addedAt;
    private LocalDateTime expiresAt; // 过期时间（可选）
}

public enum BlacklistType {
    COMPANY,         // 公司黑名单
    JOB_TITLE,       // 岗位黑名单
    KEYWORD,         // 关键词黑名单
    HR               // HR 黑名单
}
```

## 7.2 DailyStats（每日统计）

```java
@Entity
@Table(name = "daily_stats")
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;           // 所属用户
    private LocalDate date;          // 统计日期

    // 数量统计
    private Integer jobsDiscovered;  // 发现岗位数
    private Integer greetingsSent;   // 打招呼数
    private Integer greetingsReplied; // 打招呼回复数
    private Integer applicationsSent; // 投递数
    private Integer interviewsScheduled;  // 约面数

    // 比率计算（存储，方便查询）
    private Double replyRate;        // 回复率
    private Double interviewRate;    // 约面率

    // Token 消耗
    private Integer tokensUsed;      // 消耗 Token 数

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

# 八、Agent 域

## 8.1 AgentExecution（AI 执行记录）

```java
@Entity
@Table(name = "agent_executions")
public class AgentExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;           // 关联用户

    @Enumerated(EnumType.STRING)
    private AgentType agentType;     // Agent 类型

    // Prompt 版本
    private String promptTemplateId; // Prompt 模板 ID
    private Integer promptVersion;   // Prompt 版本

    // 模型信息
    private String model;            // 使用的模型
    private Double temperature;      // 温度参数
    private Double topP;

    // 输入输出
    @Column(columnDefinition = "jsonb")
    private JsonNode input;          // 输入参数

    @Column(columnDefinition = "jsonb")
    private JsonNode output;         // 输出结果

    // 成本统计
    private Integer promptTokens;    // Prompt Token
    private Integer completionTokens;  // Completion Token
    private Integer totalTokens;     // 总 Token
    private Double cost;             // 成本（美元）

    // 性能指标
    private Long latencyMs;          // 延迟（毫秒）

    // 状态
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    private String error;            // 错误信息
    private String stackTrace;       // 错误堆栈

    // 关联业务
    private String jobId;            // 关联岗位（可选）
    private String resumeId;         // 关联简历（可选）
    private String conversationId;   // 关联对话（可选）

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}

public enum AgentType {
    JD_ANALYZER,     // JD 分析 Agent
    MATCH,           // 匹配 Agent
    GREET,           // 打招呼 Agent
    CHAT,            // 聊天回复 Agent
    RESUME_ANALYSIS, // 简历分析 Agent
    RESUME_REWRITE,  // 简历重写 Agent
    WEEKLY_REPORT    // 周报生成 Agent
}

public enum ExecutionStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}
```

## 8.2 PromptTemplate（Prompt 模板）

```java
@Entity
@Table(name = "prompt_templates")
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;     // Agent 类型

    private String name;             // 模板名称
    private String description;       // 模板描述
    private Integer version;         // 版本号

    // 模板内容
    @Column(columnDefinition = "text")
    private String systemPrompt;      // System Prompt

    @Column(columnDefinition = "text")
    private String userPromptTemplate;  // User Prompt 模板

    // 变量定义
    @Column(columnDefinition = "jsonb")
    private JsonNode variables;      // 变量列表

    // 模型配置
    private String model;            // 默认模型
    private Double temperature;      // 默认温度
    private Integer maxTokens;       // 最大 Token

    // 效果统计
    private Integer useCount;        // 使用次数
    private Double avgRating;        // 平均评分
    private Double successRate;      // 成功率

    // 状态
    private Boolean isActive;        // 是否启用

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

# 九、关系图

## 9.1 核心实体关系

```
┌──────────┐     1:N      ┌──────────┐
│   User   │─────────────│  Resume  │
└──────────┘             └────┬─────┘
      │                       │
      │ 1:N                   │ 1:1
      ▼                       ▼
┌──────────┐             ┌──────────┐
│ Strategy │             │Analysis  │
└──────────┘             └──────────┘

┌──────────┐     1:N      ┌──────────┐
│   User   │─────────────│   Job    │
└──────────┘             └────┬─────┘
      │                       │
      │                       │ 1:1
      │                       ▼
      │                 ┌──────────┐
      │                 │Analysis  │
      │                 └──────────┘
      │
      │ 1:N
      ▼
┌──────────┐     1:N      ┌──────────┐     1:N      ┌──────────┐
│Conversation│────────────│  Message │             │   Job    │
└─────┬─────┘             └──────────┘             └──────────┘
      │
      │ 1:1
      ▼
┌──────────┐     1:1      ┌──────────┐
│Application│─────────────│Conversation│
└──────────┘             └──────────┘
```

---

# 十、值对象

## 10.1 MatchScore（匹配度）

```java
@Embeddable
public class MatchScore {

    private String resumeId;          // 简历 ID
    private Integer overallScore;     // 综合匹配度 0-100

    @Column(columnDefinition = "jsonb")
    private JsonNode skillMatch;     // 技能匹配详情

    @Column(columnDefinition = "jsonb")
    private JsonNode experienceMatch;  // 经验匹配详情

    @Column(columnDefinition = "jsonb")
    private JsonNode educationMatch;  // 学历匹配详情

    private String recommendation;   // 建议：APPLY / SKIP / REVIEW
}
```

## 10.2 HRIntent（HR 意图）

```java
@Embeddable
public class HRIntent {

    @Enumerated(EnumType.STRING)
    private IntentType type;         // 意图类型

    private Double confidence;      // 置信度

    @Column(columnDefinition = "jsonb")
    private JsonNode entities;       // 识别的实体

    private String suggestedReply;   // 建议回复
}
```

```java
public enum IntentType {
    GREETING,         // 打招呼回复
    SALARY_INQUIRY,   // 询问薪资
    EXPERIENCE_INQUIRY,  // 询问经验
    SKILLS_INQUIRY,   // 询问技能
    AVAILABILITY,     // 询问时间
    INTERVIEW_INVITE, // 邀请面试
    OFFER,           // 发 Offer
    REJECT,          // 拒绝
    GENERAL          // 一般聊天
}
```

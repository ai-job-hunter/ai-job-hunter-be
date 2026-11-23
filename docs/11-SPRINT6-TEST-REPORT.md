# Sprint 6 测试与发布报告（V1.0）

> 版本：v1.0  
> 日期：2026-11-23  
> 范围：V1 手工粘贴 + AI 辅助（不含 BOSS 自动化）

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|-----|
| 2026-11-23 | v1.0 | Sprint6 收口：门禁结果、安全抽查、已知延后项 | orjrs |

---

## 1. 结论

**V1.0 可发布（功能门禁本地绿 + CI/CD 已通）。**  
BOSS 爬取 / 全自动模式 / 完整策略引擎 / 专项压测 / 正式渗透 → **明确延后 V2+**，不阻塞本版。

---

## 2. 功能验收（对照 `08-DEVELOPMENT-PLAN`）

| 模块 | 结果 | 说明 |
|------|------|------|
| 注册 / 登录 / 资料 | ✅ | JWT；未登录 401 |
| 简历上传解析 | ✅ | txt / PDF / doc / docx 抽文本；扫描件可能无字 |
| 简历 AI 分析 / 重写 | ✅ | 失败降级仍 200 |
| 粘贴 JD / 分析 / 匹配 / 打招呼 | ✅ | 黑名单拦截；默认模板优先 |
| 沟通 / AI 推荐回复 | ✅ | 手动录入；intent 随建议返回 |
| 投递看板 | ✅ | 手动改状态 |
| 统计 / 漏斗 / 周报 | ✅ | 实时聚合；周报可降级 |
| 设置（模板 / 黑名单） | ✅ | |
| BOSS 自动化 | ⏸️ | V2 |
| 自动化模式切换 | ⏸️ | 依赖 BOSS，V2 |

---

## 3. 测试执行记录

| 门禁 | 命令 / 位置 | 结果 |
|------|-------------|------|
| 单元 | `JwtTokenProviderTest` + `JobServiceTest` + `ResumeWordExtractTest` | ✅（本地 + CI） |
| CI Smoke | `ContextLoadsTest` + `ApiFlowIT`（Testcontainers） | ✅ |
| Live API | `*LiveIT`（Job 001～009 等，IDEA 启 BE） | ✅（2026-11-19） |
| FE E2E | Playwright：auth / jobs / resume-ai / conversations / stats | ✅（2026-11-19） |
| 前端 CI | `npm run generate` | ✅ |
| 前端 CD | 构建推镜像 + 部署 | ✅（2026-11-20） |
| 后端 CD | 构建推镜像 + 部署健康检查 | ✅（2026-11-20，curl→wget 修复后） |

细则见 `09-TEST-STRATEGY.md` §1.3。

---

## 4. 安全抽查（非渗透）

| 项 | 结果 | 处理 |
|----|------|------|
| 未登录业务 API | ✅ 401 | `SecurityConfig` |
| `/actuator/health` 匿名 | ✅ | Docker HEALTHCHECK 需要 |
| `/api/v1/agent/**` 匿名烧 LLM | ❌→已修 | 改为必须登录（2026-11-23） |
| Swagger / OpenAPI 匿名 | ❌→已修 | 随 `anyRequest().authenticated()` |
| 生产弱 JWT 默认值 | ❌→已修 | `ProdSecretValidator` 在 `prod` profile 启动失败 |
| 密钥进库 / 进文档 | ✅ 抽查 | 仅 `.env` / Secrets；示例用占位符 |
| 正式渗透 / 依赖 CVE 全扫 | ⬜ | V1 不做；后续按需 |

---

## 5. 部署注意

- 服务器 compose **CD 不覆盖**；BE healthcheck 建议 `wget`（新镜像也带 `curl` 兼容旧配置）。
- 生产须设 `SPRING_PROFILES_ACTIVE=prod` 与强 `JWT_SECRET`（见 `.env.example`）。
- 细节：`10-DEPLOYMENT.md`。

---

## 6. 已知限制（用户可见）

1. 岗位与聊天靠**自己粘贴**，不会自动上 BOSS。  
2. 扫描件 PDF / 空 Word 可能抽不出字，无法做 AI 分析。  
3. 无独立「HR 意图」接口；意图在推荐回复里带出。  
4. 无完整「求职策略」大配置；用模板 + 黑名单代替。

---

## 7. 签字栏（自检）

| 角色 | 确认 |
|------|------|
| 开发 | 门禁用例已跑通，文档已同步 |
| 发布 | CI/CD 成功后方可视为线上 V1 |

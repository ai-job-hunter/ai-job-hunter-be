# AI Job Hunter Backend

Spring Boot 后端：求职材料管理 + DeepSeek AI（分析 JD / 简历 / 打招呼 / 回复 / 周报）。

## V1 范围

- **做**：注册登录、简历（txt/PDF/Word）、粘贴 JD、匹配与打招呼、手动沟通、投递看板、统计与设置  
- **不做**：BOSS 爬取与自动发消息（见 V2）

V2 平台接入方案（合规边界 + 分阶段）：`docs/12-V2-PLATFORM-AGENT-PLAN.md`。  
用户说明书与页面路径见前端仓库 `ai-job-hunter-fe/README.md`。

## 本地启动

```bash
# 需本机 PostgreSQL + Redis；环境变量见 .env.example（只填键，勿把密钥贴进对话）
./gradlew bootRun
# 健康检查
curl -s http://127.0.0.1:8080/actuator/health
```

端口默认 `8080`；若被占用可在 IDEA / `SERVER_PORT` 改成 `8088`。

## 测试门禁

```bash
# CI 同款单元测试
./gradlew test \
  --tests com.aijobhunter.common.security.JwtTokenProviderTest \
  --tests com.aijobhunter.service.JobServiceTest \
  --tests com.aijobhunter.service.ResumeWordExtractTest

# Live API：先 IDEA 启本后端
LIVE_API_BASE=http://127.0.0.1:8088 ./gradlew test --tests com.aijobhunter.controller.JobLiveIT
```

完整策略：`docs/09-TEST-STRATEGY.md`。Sprint6 发布结论：`docs/11-SPRINT6-TEST-REPORT.md`。

## 文档

| 文档 | 内容 |
|------|------|
| `docs/01-PRD.md` | 产品需求 |
| `docs/02-TDS.md` | 技术设计 |
| `docs/05-API-SPECIFICATION.md` | API |
| `docs/08-DEVELOPMENT-PLAN.md` | 开发计划 |
| `docs/09-TEST-STRATEGY.md` | 测试策略 |
| `docs/10-DEPLOYMENT.md` | 部署运维 |
| `docs/11-SPRINT6-TEST-REPORT.md` | V1 测试与发布报告 |
| `docs/12-V2-PLATFORM-AGENT-PLAN.md` | V2 平台 / 半自动 Agent 方案 |

## 部署

生产 compose 与 Traefik 见 `docker-compose.yml` + `docs/10-DEPLOYMENT.md`。  
CD：`.github/workflows/cd.yml`（服务器 compose / `.env` 由人工维护，CD 不覆盖）。

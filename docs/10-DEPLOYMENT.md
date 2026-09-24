# 部署与运维手册

> 本文件说明 AI Job Hunter Backend 的部署流程、回滚机制、通知配置。
> 工作流文件：`.github/workflows/cd.yml`

---

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|---|
| 2026-09-24 | v1.0 | 初始版本（自动回滚 + 邮件通知 + 手动触发） | orjrs |

---

## 1. 部署流程

```
git push → CI 通过 → CD 自动构建镜像 → 推送 GHCR → SSH 到服务器 → pull & up → 健康检查
```

触发条件：
- **自动部署**：push 到 `main` 且 CI 通过
- **手动部署**：GitHub → Actions → CD → Run workflow（可选填入 `image_tag` 回滚到任意版本）

---

## 2. 自动回滚机制

| 步骤 | 说明 |
|---|---|
| 读取当前 tag | 从服务器的 `.env` 中 `IMAGE_TAG=xxx` 读取 |
| 部署新 tag | `docker compose pull + up -d` |
| 健康检查 | 最多 90 秒（18 × 5s），用 Docker 原生 `HEALTHCHECK` 状态 |
| 失败回滚 | 健康检查不通过 → 用上一个 tag 重启容器 → `.env` 回写 → 升级日志记录 |
| 邮件通知 | 失败时发邮件，包含回滚前后 tag |

**回滚触发条件**：
1. 新容器 90 秒内未进入 `healthy` 状态
2. 容器在等待过程中停止运行

---

## 3. GitHub Secrets 配置清单

### 必需（部署相关）
| Secret | 说明 |
|---|---|
| `DEPLOY_SSH_KEY` | SSH 私钥（推荐 ed25519） |
| `DEPLOY_HOST` | 服务器地址（如 `cogniforge.example.com`） |
| `DEPLOY_USER` | SSH 用户名（如 `deploy`） |
| `DEPLOY_SSH_PORT` | SSH 端口（默认 22） |
| `DEPLOY_PATH` | 服务器部署目录（如 `/opt/project/ai-job-hunter`） |

### 可选（邮件通知）
| Secret | 说明 |
|---|---|
| `NOTIFY_EMAIL` | 收件邮箱 |
| `MAIL_USERNAME` | 发件邮箱（QQ 邮箱推荐 `xxx@qq.com`） |
| `MAIL_PASSWORD` | SMTP 授权码（**不是登录密码**，需在邮箱后台生成） |

**QQ 邮箱获取授权码**：设置 → 账户 → POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务 → 开启 SMTP → 生成授权码

---

## 4. 手动回滚（运维手动操作）

### 方式 A：GitHub 网页（推荐）
1. 打开 `https://github.com/<owner>/ai-job-hunter-be/actions/workflows/cd.yml`
2. 点 `Run workflow`
3. `image_tag` 填写要回滚到的 tag（**纯时间戳格式**，如 `20260924153000`），留空则部署最新构建
4. `reason` 填写回滚原因

### 方式 B：SSH 到服务器手动操作
```bash
ssh deploy@cogniforge.example.com
cd /opt/project/ai-job-hunter
# 查看历史 tag（升级日志里）
cat UPGRADE_LOG.md
# 编辑 .env 改回旧 tag（纯时间戳）
sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=20260924153000/' .env
docker compose pull ai-job-hunter-be
docker compose up -d ai-job-hunter-be
```

---

## 6. 镜像 tag 命名规范

| 项 | 值 |
|---|---|
| 仓库 | `ghcr.io/<owner>/ai-job-hunter-be` |
| tag 格式 | `yyyyMMddHHmmss`（纯时间戳，UTC） |
| 示例 | `ghcr.io/liujun/ai-job-hunter-be:20260924153000` |
| 不带前缀 | tag 不带 `ai-job-hunter-be-` 前缀，因为仓库名已经区分项目 |

历史 tag 通过 `UPGRADE_LOG.md` 追溯，不依赖任何外部系统。

---

## 5. 常见问题

### Q1：部署卡在「Health check」
- 查看 GHCR 是否有新镜像：`docker images | grep ai-job-hunter-be`
- SSH 进去看日志：`docker logs ai-job-hunter-be --tail 100`
- 看容器状态：`docker inspect ai-job-hunter-be | grep -A 5 State`

### Q2：邮件没收到
- 检查 Secrets 里 `NOTIFY_EMAIL`、`MAIL_USERNAME`、`MAIL_PASSWORD` 是否都配了
- QQ 邮箱必须用授权码（不是登录密码）
- 查垃圾邮件箱

### Q3：回滚失败
- 如果当前没有可回滚的 tag（首次部署失败），不会触发自动回滚
- 手动 SSH 上去处理即可

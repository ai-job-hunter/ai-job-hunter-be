# 部署与运维手册

> 本文件说明 AI Job Hunter Backend 的部署流程、回滚机制、通知配置。
> 工作流文件：`.github/workflows/cd.yml`

---

## [变更记录]
| 日期 | 版本 | 变更摘要 | 负责人 |
|---|---|---|---|
| 2026-09-28 | v1.2 | fix：三处 `sed -i` 改 `tee` 原子写入；加 §0 边界声明；详见 §5「工作流踩坑」 | orjrs |
| 2026-09-24 | v1.1 | 通知改用 GitHub Issue（去 SMTP 配置），自动去重告警 | orjrs |
| 2026-09-24 | v1.0 | 初始版本（自动回滚 + 邮件通知 + 手动触发） | orjrs |

---

## [边界声明] AI 与远程 .env 的权限分离

**硬规则（强制）**：

> **AI 助手（含 Cursor / Codex / Agent 自动化）不允许直接读写远程服务器上的 `.env` 文件。**

任何对**远程** `/opt/project/ai-job-hunter/.env` 的改动，**只能**通过下面两个合法路径之一：

1. **CI 自动化**（由本仓库的 `cd.yml` 触发）：仅允许改 `IMAGE_TAG` 一项；其它行不触碰。
2. **运维手工**（人工 `ssh deploy@...`，`vim` 或 `vi` 编辑）：用于首装、改密钥、改数据库连接等任何其它字段。

**禁止路径**：
- ❌ AI 直连 SSH 写 `.env`（即使是 `tee` 也不行）
- ❌ AI 读 `.env` 后回显到 chat（包含密钥、API Key、JWT Secret）
- ❌ AI 推断 `IMAGE_TAG` 的「当前值」并据此重写历史回滚逻辑

**理由**：
- `.env` 含真实密钥（参考 `.env.example` 的数据库密码 / JWT / 加密密钥 / DeepSeek API Key 共 5+ 项）
- 部署目录 `/opt/project/ai-job-hunter/` 由 root 拥有，SSH 用户无写权限是有意为之
- cd.yml 用 `tee` 原子覆写是「CI 安全写法」，与「AI 直连」是不同的事，不能混用

**自动化层面的边界**：

| 改的内容 | 谁改 | 路径 |
|---|---|---|
| git tracked 代码 / 文档 | AI / 开发者 | 本地改 → push → CI |
| `.env.example` 占位符 | AI / 开发者 | git 提交，不含真实值 |
| 服务器 `.env` 的 IMAGE_TAG | **CI 自动化** | `cd.yml` 的 `tee .env` 步骤 |
| 服务器 `.env` 的其它密钥 | **人工** | ssh + vim |
| 服务器 `docker-compose.yml` | **人工** | ssh + vim；CD 不覆盖 |

**对应工作流注释**：`.github/workflows/cd.yml` 顶部 `## 边界声明` 已固化本约定，避免后续人维护 workflow 时遗漏。

---

## [变更] sed -i 改 tee 原子写入（2026-09-28）

**原因**：`cd.yml` 旧版本用 `sed -i "s|^IMAGE_TAG=.*|..." .env` 原地改 `.env`，以及 `sed -i` 改 `UPGRADE_LOG.md`。

**症状**：CD 跑到 "Update .env" 步骤时失败，整 job exit 4：

```
sed: couldn't open temporary file ./seda96rCm: Permission denied
2026/09/26 08:19:37 Process exited with status 4
Error: Process completed with exit code 1.
```

**根因**：`sed -i` 在**当前工作目录**（即 `$DEPLOY_PATH`）建临时文件再 `rename`。CD 通过 SSH 以 `deploy` 用户登录，`/opt/project/ai-job-hunter/` 由 root 拥有时 `deploy` 无 `.` 写权限，临时文件建不起来。

**影响范围**：`.github/workflows/cd.yml` 三处 `sed -i`（L210 / L221 / L262），覆盖以下场景：

1. 把当前（即将被替换的）旧 tag 在 `UPGRADE_LOG.md` 中标为「历史」—— 这是日志，原本看 `|| true` 兜住，但语义错误，且对 Owner 严格的目录仍可能炸。
2. 部署新 tag 时把 `.env` 的 `IMAGE_TAG` 改成新值 —— **这处没 `|| true`，脚本 `set -e` 直接非 0**，整个 deploy job 失败，GHCR 镜像已 push 但没进健康检查。
3. 失败回滚时把 `.env` 改回旧 tag —— 回滚路径同样问题。

**变更前 → 变更后**：

```diff
- # ── 1. .env 更新 IMAGE_TAG（旧写法，set -e 直接 exit）──
- if grep -qE '^IMAGE_TAG=' .env 2>/dev/null; then
-   sed -i "s|^IMAGE_TAG=.*|IMAGE_TAG=$DEPLOY_TAG|" .env
- else
-   echo "IMAGE_TAG=$DEPLOY_TAG" >> .env
- fi
+ # ── 1. .env 更新 IMAGE_TAG（tee 原子写入）──
+ {
+   echo "IMAGE_TAG=$DEPLOY_TAG"
+   grep -v '^IMAGE_TAG=' .env 2>/dev/null || true
+ } | tee .env > /dev/null
```

**关键差异**：

| 维度 | sed -i（旧） | tee（新） |
|---|---|---|
| 临时文件 | 在当前目录（`./sedaXXXXX`） | 无 |
| 是否创建新文件 | 是（mv 替换） | 否（流式覆写） |
| 对 `.` 写权限的依赖 | 强依赖 | 不依赖 |
| 跨用户 mv 报错 | 偶尔发生（root tmp → 用户 mv） | 不存在 |
| 兼容性 | BSD / GNU / BusyBox 差异 | POSIX 标准用法 |

`UPGRADE_LOG.md` 的「历史标记」改法改用 `awk` + `tee`，无需临时文件即可完成字段替换。

**为什么不直接 `chmod` 整个目录**：

- `/opt/project/ai-job-hunter/` 是 root 拥有是有意为之（防运维误改）
- 让 SSH 用户拿到大目录写权会扩大攻击面
- `tee` 流式写入只触碰被改的两个文件，不动目录的 owner/mode

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
| 读取当前 tag | 从服务器的 `UPGRADE_LOG.md` 第一条 `| latest |` 行提取镜像 tag（**不读 .env**，废弃 IMAGE_TAG 约定） |
| 部署新 tag | `docker compose pull + up -d` |
| 健康检查 | 最多 90 秒（18 × 5s），用 Docker 原生 `HEALTHCHECK` 状态 |
| 失败回滚 | 健康检查不通过 → 用上一个 tag 重启容器 → 升级日志记录（**.env 的 IMAGE_TAG 不再维护**） |
| 失败告警 | 自动创建 `cd-alert` Issue（含回滚信息），已存在则追加评论 |

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

### 通知机制（无需配置）

CD 失败时 GitHub Actions 会自动在仓库创建告警 Issue（标签 `cd-alert`），通过 GitHub 自身的通知体系送达（站内 + 邮件订阅，无需任何 SMTP 配置）。

如需订阅通知：`https://github.com/<owner>/ai-job-hunter-be/issues` → Watch → Custom → Issues

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
# 手动 pull + up 指定旧 tag（不依赖 .env IMAGE_TAG）
docker pull ghcr.io/liujun/ai-job-hunter-be:<旧tag>
IMAGE_TAG=<旧tag> docker compose up -d ai-job-hunter-be
# 升级日志里把旧 tag 标为 latest（手工编辑 UPGRADE_LOG.md）
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

### Q2：部署失败告警
- 失败时 GitHub 会自动创建 `cd-alert` 标签的 Issue
- 如果已有同名 Issue 没关闭，会自动追加评论（去重）
- 在 `https://github.com/<owner>/ai-job-hunter-be/issues` 可查看
- 修复后请手动关闭 Issue

### Q3：回滚失败
- 如果当前没有可回滚的 tag（首次部署失败），不会触发自动回滚
- 手动 SSH 上去处理即可

### Q4：CD 失败，SSH 步骤报错 `sed: couldn't open temporary file ./sedaXXXXX: Permission denied`

**症状**：

```
=== Login to GHCR ===
  Login Succeeded
  Deploying tag: 20260926081732 (mode: auto)
  sed: couldn't open temporary file ./seda96rCm: Permission denied
  Process exited with status 4
  Error: Process completed with exit code 1.
```

**根因**：旧版 cd.yml（< v1.2）用 `sed -i` 改 `.env` 文件，`sed -i` 在当前目录（`$DEPLOY_PATH`）建临时文件，SSH 用户无写权限时失败。

**修复**：v1.2（2026-09-28）已彻底废弃 `.env` 的 `IMAGE_TAG` 约定，改为**只通过 `UPGRADE_LOG.md` 记录镜像 tag 历史**。CD workflow 不会再触碰 `.env`。当前版本已自带修复；合并 PR 后 pull 最新即可。

**临时绕过（v1.2 之前止血）**：

```bash
ssh deploy@cogniforge.example.com
# 编辑 UPGRADE_LOG.md，手动把当前失败的 tag 改回上一行
# 不需要动 .env
```

⚠️ 不推荐长期 `chown` 部署目录，会让 SSH 用户能改 compose / `.env`，扩大运维事故面；正确做法是合并 PR 升级 workflow。

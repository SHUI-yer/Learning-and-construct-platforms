# 综合学习平台部署、备份与回滚手册

本手册适用于 Linux 云服务器、systemd 管理的后端服务和 MySQL 8。示例服务名为 `learning-platform`，请按服务器实际名称替换。

## 1. CI/CD 的边界

仓库中的 `.github/workflows/ci-deploy.yml` 只处理以下两条路径：

- 向 `main` 提交 Pull Request：运行后端 Maven `verify`、前端 lint/build/生产依赖审计、敏感信息检查和依赖变更审查，**不接触服务器**。
- Pull Request 合并后产生 `main` push：重新运行后端、前端和敏感信息检查；全部通过、GitHub API 确认该提交来自已合并到 `main` 的 PR，且该提交仍是 `main` 最新提交时，才进入 `production` Environment 并部署。

工作流本身不能禁止有权限的成员直接 push。必须在 GitHub 的 `Settings → Rules → Rulesets`（或 Branch protection rules）中保护 `main`：

1. 要求所有修改通过 Pull Request 合并，并禁止 force push 和删除分支。
2. 要求分支合并前是最新状态，且以下检查全部通过：
   - `Backend tests`
   - `Frontend checks`
   - `Sensitive information`
   - `Dependency review`
   - `Deployment script tests`
3. 不允许管理员或其他角色绕过上述规则；确需紧急绕过时，应限定到单独的维护角色并保留审计记录。

在 `Settings → Environments` 创建 `production`，将 Deployment branches 设为 **Protected branches only**。如需“合并后仍由运维确认”，再给该环境添加 Required reviewers；不添加 reviewer 时会在主分支检查通过后自动部署。

### GitHub production 配置

在 `production` Environment 中保存以下 Secrets：

| Secret | 内容 |
|---|---|
| `DEPLOY_HOST` | 云服务器 DNS 名或 IPv4 地址 |
| `DEPLOY_USER` | 专用 SSH 部署用户，例如 `ubuntu` |
| `DEPLOY_SSH_KEY` | 该部署用户的专用 SSH 私钥，不要复用个人私钥 |
| `DEPLOY_KNOWN_HOSTS` | 已人工核验指纹的 known_hosts 完整记录 |

`DEPLOY_KNOWN_HOSTS` 不应在 CI 中临时信任。应从可信管理终端取得服务器公钥，并与云厂商控制台或服务器本机显示的指纹核对后再保存。

可选 Environment Variables：

| Variable | 默认值 | 说明 |
|---|---|---|
| `DEPLOY_PORT` | `22` | SSH 端口 |
| `DEPLOY_ROOT` | `/opt/learning-platform` | 版本化发布根目录 |
| `DEPLOY_SERVICE` | `learning-platform` | systemd 服务名 |
| `DEPLOY_ENV_FILE` | `/home/ubuntu/.learning-platform.env` | 服务器私密环境文件 |
| `DEPLOY_HEALTH_URL` | `http://127.0.0.1:8081/actuator/health/readiness` | 本机就绪探针 |
| `AUTO_ROLLBACK_APPLICATION` | `true` | readiness 失败或新服务无法启动时自动切回并验证上一版本；存在破坏性迁移的特殊发布必须临时设为 `false` 并人工值守 |

## 2. 服务器首次准备

服务器需安装 Java 17、`curl`、`tar`、`flock`（通常来自 `util-linux`）、MySQL 客户端、`gzip` 和 `sha256sum`。以下初始化命令只执行一次，不应放进每次发布：

```bash
sudo install -d -o ubuntu -g ubuntu -m 0750 \
  /opt/learning-platform/incoming /opt/learning-platform/releases
sudo install -d -o ubuntu -g ubuntu -m 0700 \
  /home/ubuntu/backups/learning-platform

sudo install -m 0644 light-out/deploy/systemd/learning-platform.service \
  /etc/systemd/system/learning-platform.service
sudo systemctl daemon-reload
sudo systemctl enable learning-platform
```

`/home/ubuntu/.learning-platform.env` 必须由部署用户拥有且权限为 `0600`。生产环境至少配置：

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/light_out?useSSL=false&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_USERNAME=应用专用数据库账号
SPRING_DATASOURCE_PASSWORD=真实密码
SPRING_DATA_REDIS_PASSWORD=真实密码
JWT_SECRET=至少32字节的随机密钥
FLYWAY_BASELINE_ON_MIGRATE=false
MANAGEMENT_SERVER_ADDRESS=127.0.0.1
MANAGEMENT_SERVER_PORT=8081
```

部署用户只需拥有发布目录写权限，以及启动/停止本服务的受限 sudo 权限。用 `sudo visudo -f /etc/sudoers.d/learning-platform-deploy` 写入（先用 `command -v systemctl` 确认路径）：

```sudoers
ubuntu ALL=(root) NOPASSWD: /usr/bin/systemctl start learning-platform, /usr/bin/systemctl stop learning-platform
```

不要给 CI 用户开放任意 `sudo`。Nginx 的站点根目录应指向 `/opt/learning-platform/current/web`，API 再反向代理到 `127.0.0.1:8080`；不要把 `/actuator` 暴露到公网。题库导入文件由 systemd 保存到 `/var/lib/learning-platform/uploads`，不会随版本切换丢失。

## 3. 固定目录与环境变量

建议使用版本化发布目录，不覆盖上一版本：

```text
/opt/learning-platform/
├── incoming/
├── releases/
│   ├── <Git commit SHA>/
│   │   ├── backend/lp-starter.jar
│   │   ├── web/
│   │   └── scripts/{backup-db.sh,restore-db.sh}
│   └── ...
├── previous -> releases/<上一版本 SHA>
└── current -> releases/<当前版本 SHA>
```

Actuator 只能由服务器本机或受控监控代理访问。不要在 Nginx 中把 `/actuator` 代理到公网。

执行迁移的数据库账号需要对目标库具备相应 DDL 权限。恢复脚本需要权限更高、可删除和创建数据库的临时管理员账号，两者不要混用。

## 4. Flyway 首次接管现有数据库

`V1__baseline_schema.sql` 用于全新空数据库。生产数据库已有表和数据时，第一次部署必须执行一次基线登记，不能执行 V1 建表。

1. 进入维护窗口并停止写入：

   ```bash
   sudo systemctl stop learning-platform
   ```

2. 创建并校验上线前备份：

   ```bash
   cd /opt/learning-platform/current
   ENV_FILE=/home/ubuntu/.learning-platform.env \
     bash scripts/backup-db.sh
   ```

3. 确认备份文件和同名 `.sha256` 文件已复制到另一台机器或对象存储。
4. 在环境文件中临时设置：

   ```env
   FLYWAY_BASELINE_ON_MIGRATE=true
   ```

5. 启动新版本并检查日志。Flyway 应创建 `flyway_schema_history`，登记版本 `1`、类型 `BASELINE`，不能执行 89 张表的创建语句。
6. 检查就绪状态：

   ```bash
   curl --fail --silent --show-error \
     http://127.0.0.1:8081/actuator/health/readiness
   ```

7. 立即把环境文件恢复为：

   ```env
   FLYWAY_BASELINE_ON_MIGRATE=false
   ```

后续版本若发现非空数据库缺少 Flyway 历史表，会直接启动失败，防止把错误数据库静默当成正确版本。

全新数据库无需开启基线开关；应用第一次启动会直接执行 V1。

## 5. 自动发布流程

1. 功能分支发起 Pull Request，五项必需检查通过后才能合并。
2. 数据库变更新增 `V2__description.sql`、`V3__description.sql` 等文件。已在共享环境执行过的迁移禁止修改。
3. 优先使用 expand/contract 迁移：先新增兼容结构，再部署代码和回填数据，最后在后续版本删除旧结构。
4. 合并后，CI 为当前 commit 分别下载已验证的后端 JAR 和前端 `dist`，组合成不可变发布包。
5. 服务器停止应用、创建并校验上线前数据库备份，然后原子切换 `current` 软链接并启动服务。
6. CI 最多等待约 60 秒检查 Actuator readiness。新服务无法启动或探针持续失败时，脚本自动停止失败版本、原子切回 `previous`、启动旧服务并再次检查 readiness。
7. 发布后检查服务、就绪状态和 Flyway 日志：

   ```bash
   sudo systemctl status learning-platform --no-pager
   curl --fail --silent --show-error \
     http://127.0.0.1:8081/actuator/health/liveness
   curl --fail --silent --show-error \
     http://127.0.0.1:8081/actuator/health/readiness
   ```

8. 完成最小业务冒烟测试后再结束维护窗口。

回滚恢复健康后，本次 GitHub Actions 仍以失败结束，提醒维护人员排查，而不是把“旧版本恢复运行”误报成“新版本发布成功”。同一提交重新运行工作流前，应先查看 `journalctl -u learning-platform` 和 Flyway 记录。

自动流程只回滚应用文件，不自动恢复数据库。所有常规迁移必须遵循 expand/contract，确保上一应用版本仍可运行；删除或重命名旧字段等破坏性迁移必须设置 `AUTO_ROLLBACK_APPLICATION=false`，进入维护窗口并按下一节人工处理。

## 6. 回滚判断

### 只回滚应用

满足以下任一条件时可以只切回旧 JAR：

- 新版本没有执行数据库迁移；
- 迁移仅新增表、可空字段或索引，并确认旧版本仍兼容。

```bash
sudo systemctl stop learning-platform
sudo ln -sfn "$(readlink -f /opt/learning-platform/previous)" /opt/learning-platform/current
sudo systemctl start learning-platform
curl --fail http://127.0.0.1:8081/actuator/health/readiness
```

### 回滚应用并恢复数据库

如果迁移删除或改名了旧版本依赖的结构，必须恢复数据库。该操作会丢弃备份之后产生的数据，只能由明确授权的维护人员执行。

1. 停止应用。
2. 先对失败现场再做一次备份，保留排查和数据补偿依据。
3. 找到上线前的 `.sql.gz` 和 `.sha256`，重新执行校验。
4. 临时导出具有数据库创建/删除权限的管理员凭据；不要把它们写入仓库或长期环境文件。
5. 明确指定数据库名执行恢复：

   ```bash
   export DB_ADMIN_USERNAME=root
   export DB_ADMIN_PASSWORD='临时输入的管理员密码'
   export RESTORE_CONFIRM_DATABASE=light_out

   ENV_FILE=/home/ubuntu/.learning-platform.env \
     bash scripts/restore-db.sh \
     /home/ubuntu/backups/learning-platform/light_out_时间_版本.sql.gz

   unset DB_ADMIN_PASSWORD DB_ADMIN_USERNAME RESTORE_CONFIRM_DATABASE
   ```

6. 切回旧 JAR，启动并检查 Actuator 和核心业务。

数据库备份包含 `flyway_schema_history`，因此恢复后 Flyway 版本也会回到备份时的状态。禁止使用 `flyway clean` 处理生产事故。

## 7. 备份策略

- 发布前必须备份；日常至少每天一次，并定期做异机或对象存储复制。
- `BACKUP_RETENTION_DAYS` 默认 14 天，只清理指定备份目录中本项目命名的备份。
- 至少每季度在隔离环境执行一次真实恢复演练；只有成功恢复并通过业务校验的备份才算有效备份。
- 数据库备份不包含上传文件、Nginx、systemd 和服务器环境文件，这些内容需要单独备份。

仓库提供了每天 `03:15` 执行的 systemd timer 模板。确认服务器用户和路径与模板一致后安装：

```bash
sudo install -d -o ubuntu -g ubuntu -m 0700 /home/ubuntu/backups/learning-platform
sudo install -m 0644 light-out/deploy/systemd/learning-platform-backup.service \
  /etc/systemd/system/learning-platform-backup.service
sudo install -m 0644 light-out/deploy/systemd/learning-platform-backup.timer \
  /etc/systemd/system/learning-platform-backup.timer
sudo systemctl daemon-reload
sudo systemctl enable --now learning-platform-backup.timer
sudo systemctl start learning-platform-backup.service
sudo systemctl status learning-platform-backup.service --no-pager
sudo systemctl list-timers learning-platform-backup.timer
```

首次启用时手动执行一次 service，确认备份和校验文件真实生成，再依赖定时任务。

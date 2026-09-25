# QuestionHub Desktop

JavaFX + SQLite 的本地桌面版，迁移自仓库现有 Vue/Spring Boot 版。

## 已迁移功能

- 访问口令登录（沿用原 Web 版 SHA-256 口令）
- 文件夹创建 / 重命名 / 删除
- 问题创建 / 编辑 / 删除
- 问题与答案搜索
- 答案查看与独立编辑保存
- JSON 导入并全量覆盖
- JSON 导出（兼容原 `version: 1 / folders / qaItems` 格式）
- SQLite 本地持久化
- 每日启动自动备份，最多保留最近 10 份
- 打开当前数据目录

## 数据目录

默认：

`%LOCALAPPDATA%\QuestionHub\data`

数据库：

`questionhub.db`

自动备份：

`backup\questionhub-*.db`

安装器会单独询问“数据存储目录”，该目录与程序安装目录分离。升级、覆盖安装和卸载不会主动删除数据。

安装器选择的数据路径保存在：

`%LOCALAPPDATA%\QuestionHub\config\data-dir.txt`

如果开发环境直接启动、且这个配置不存在，应用首次运行也会弹出目录选择器。

## IDEA 本地开发

要求 Java 21 + Maven。

在 IDEA 中打开 `desktop-app/pom.xml`，等待 Maven 导入后运行：

`com.questionhub.desktop.QuestionHubApp`

也可以：

```powershell
mvn javafx:run
```

## GitHub 自动发布

仓库根目录包含：

`.github/workflows/desktop-release.yml`

提交后打标签：

```powershell
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions 会生成：

- `QuestionHub-Setup-1.0.0.exe`：推荐，新电脑直接安装
- `QuestionHub-Windows-x64.zip`：免安装便携版

目标电脑无需安装 JDK、Maven、Node.js、IDEA 或 SQLite。

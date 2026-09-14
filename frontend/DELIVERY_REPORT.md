# 项目交付报告

## 1. 实现功能

- 本地口令登录（默认口令 `140810921`，SHA-256 摘要比较）
- 文件夹列表、新增、重命名、删除
- 删除文件夹时级联删除对应问答并二次确认
- 问题气泡列表、新增、修改、删除
- 点击问题查看答案、独立编辑答案
- 问题/答案关键字过滤
- IndexedDB 本地持久化
- JSON 完整归档导出
- JSON Schema 级基础校验、版本校验、覆盖式导入
- PC / 平板 / 手机响应式布局
- Pinia 状态管理和模块化目录
- Vitest 单元测试用例
- Playwright 登录冒烟测试
- README 与开发执行计划

## 2. 项目结构

核心代码位于 `src/`，按 components / modules / services / stores / views 分层。详见 README。

## 3. 核心技术

- Vue 3.5.42
- Vite 7.3.6
- Pinia 3.0.4
- IndexedDB
- JSON 导入/导出
- Vitest 3.2.7
- Playwright 1.62.1

## 4. 数据库

无服务端数据库。浏览器 IndexedDB 数据库名：`question_archive_db`。

对象仓库：

- `folders`
- `qa_items`（包含 `folderId` 索引）
- `app_config`

## 5. API

无 HTTP API。业务通过 Pinia Store + 本地 Service 调用 IndexedDB。

## 6. 启动方式

```bash
npm install
npm run dev
```

生产构建：

```bash
npm run build
```

## 7. 测试结果

已在交付环境实际完成：

- JavaScript `node --check`：通过
- 所有 Vue `<script setup>` 提取语法检查：通过
- 核心校验逻辑原生 Node 断言：8/8 通过

当前交付沙箱无法完成的验证：

- `npm install`：运行环境对 `registry.npmjs.org` DNS 解析失败并超时
- 因依赖无法下载，无法在该沙箱继续执行 `npm run build`、Vitest 和 Playwright 浏览器测试

项目未包含 `node_modules`，请在有正常 npm 网络的环境执行上述命令完成最终依赖安装与构建验证。

## 8. 验收条件对应表

| 验收项 | 实现位置 | 验证方式 | 状态 |
|---|---|---|---|
| 默认口令登录 | authService / LoginView | Playwright 用例 | 已实现 |
| 文件夹 CRUD | folderStore / FolderSidebar | 页面操作 | 已实现 |
| 问题 CRUD | qaStore / QuestionList | 页面操作 | 已实现 |
| 答案查看与编辑 | AnswerPanel | 页面操作 | 已实现 |
| 搜索 | qaStore.filteredItems | 页面输入 | 已实现 |
| 本地持久化 | storageService | 刷新浏览器 | 已实现 |
| JSON 导出/导入 | archiveService | 导出再导入 | 已实现 |
| 删除二次确认 | ConfirmDialog | 页面操作 | 已实现 |
| 移动端适配 | main.css | 响应式视口 | 已实现 |
| 生产构建 | Vite | `npm run build` | 待联网环境最终执行 |

## 9. 已知限制

- 纯前端口令属于本地访问保护，不是强认证。
- 浏览器站点数据被清除后 IndexedDB 可能丢失，需定期导出 JSON。
- 不支持多级文件夹、Markdown、图片、附件、云同步。
- 不处理多标签页同时编辑的数据冲突合并。

## 10. 最终交付文件

- `question-archive.zip`

> 当前状态：源代码实现与静态/核心逻辑验证完成；生产构建验证因交付沙箱无法访问 npm registry，需在正常联网的 Node.js 环境执行 `npm install && npm run build && npm test` 后完成最终确认。

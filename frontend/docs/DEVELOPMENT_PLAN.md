# 开发执行计划

| ID | 任务 | 依赖 | 输出文件 | 验证方式 | 状态 |
|---|---|---|---|---|---|
| T01 | 初始化 Vue/Vite 工程 | 无 | package.json、vite.config.js | npm build | DONE |
| T02 | 本地数据模型与 IndexedDB | T01 | storageService.js | 代码审查/构建 | DONE |
| T03 | 归档导入导出 | T02 | archiveService.js | 单元测试 | DONE |
| T04 | 登录模块 | T01 | authService、authStore | E2E/人工 | DONE |
| T05 | 文件夹 CRUD | T02 | folderStore、FolderSidebar | 人工验收 | DONE |
| T06 | 问题/答案 CRUD | T02 | qaStore、QuestionList、AnswerPanel | 人工验收 | DONE |
| T07 | 响应式 UI | T04-T06 | views、CSS | 响应式检查 | DONE |
| T08 | 单元/E2E 测试 | T03-T07 | tests | npm test | DONE |
| T09 | README 与交付 | T01-T08 | README.md、ZIP | 内容检查 | DONE |

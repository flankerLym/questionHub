# 问题归档系统

一个纯前端的个人问答归档工具。使用 Vue 3 + Vite + Pinia 构建，数据自动保存在浏览器 IndexedDB 中，并支持 JSON 导入/导出备份。

## 功能

- 默认访问口令登录
- 文件夹新增、重命名、删除
- 问题新增、查看、修改、删除
- 答案查看和独立编辑
- 问题/答案关键字搜索
- IndexedDB 本地持久化
- JSON 导入、导出
- 删除二次确认
- PC / 手机响应式布局

## 默认访问口令

```text
140810921
```

> 这是纯前端访问保护，不属于服务端强认证。不要在系统中保存私钥、银行卡密码、API Key 等高敏感信息。

## 环境要求

- Node.js >= 20.19.0
- npm >= 10（推荐）
- 支持 IndexedDB 的现代浏览器

## 安装

```bash
npm install
```

## 开发启动

```bash
npm run dev
```

浏览器打开终端提示的本地地址即可。

## 生产构建

```bash
npm run build
```

构建产物位于：

```text
dist/
```

## 本地预览

```bash
npm run preview
```

## 测试

单元测试：

```bash
npm test
```

E2E 测试首次需要安装浏览器：

```bash
npx playwright install chromium
npm run test:e2e
```

## 数据存储

日常数据保存在当前浏览器的 IndexedDB：

```text
question_archive_db
├── folders
├── qa_items
└── app_config
```

清除浏览器站点数据可能导致归档丢失，因此建议定期点击右上角“导出”保存 JSON 备份。

## JSON 导入导出

- **导出**：右上角点击“导出”，下载完整归档 JSON。
- **导入**：右上角点击“导入”，选择之前导出的 JSON。导入会覆盖当前数据，并在执行前二次确认。

## 项目结构

```text
question-archive
├── src
│   ├── assets              # 全局样式
│   ├── components          # UI 组件
│   ├── modules             # 登录/文件夹/问题业务校验
│   ├── services            # IndexedDB 与归档服务
│   ├── stores              # Pinia 状态管理
│   ├── utils               # 通用工具
│   ├── views               # 登录页与归档主页面
│   ├── App.vue
│   └── main.js
├── tests
│   ├── e2e
│   └── validators.test.js
├── docs/DEVELOPMENT_PLAN.md
├── index.html
├── package.json
├── playwright.config.js
└── vite.config.js
```

## 已知限制

1. 没有后端，多设备之间不会同步数据。
2. 口令验证运行于浏览器，仅用于普通访问保护。
3. IndexedDB 数据属于浏览器站点数据，清除浏览器数据后可能丢失。
4. 当前为一级文件夹，不支持多级目录。
5. 答案为纯文本，不包含 Markdown、图片和附件上传。
6. 多标签页同时编辑时不做复杂冲突合并。

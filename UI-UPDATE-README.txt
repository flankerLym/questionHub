QuestionHub UI 优化覆盖包

使用方式：
1. 关闭 IDEA 中正在运行的 QuestionHub。
2. 将本压缩包直接解压到：E:\workSpace\MemoryTool
3. 选择“覆盖/替换目标中的文件”。
4. 重新打开 E:\workSpace\MemoryTool\questionHub。

本次只修改桌面端 UI 层，不改数据库结构和业务数据：
- desktop-app/src/main/java/com/questionhub/desktop/ui/MainView.java
- desktop-app/src/main/java/com/questionhub/desktop/ui/LoginView.java
- desktop-app/src/main/java/com/questionhub/desktop/ui/Dialogs.java
- desktop-app/src/main/java/com/questionhub/desktop/QuestionHubApp.java
- desktop-app/src/main/resources/app.css

主要变化：
- 登录页改为更适合长期学习使用的文案和布局
- 主界面重新整理为“学习分类 / 问题列表 / 学习笔记”三栏
- 新增问题数量、答案摘要、更新时间、空状态提示、状态栏
- 新建分类、记录问题、删除确认、导入导出提示窗口全部重新设计
- 统一暖白 + 灰绿护眼风格，降低长时间阅读刺激
- 不改变 SQLite、导入导出格式、登录逻辑和现有数据

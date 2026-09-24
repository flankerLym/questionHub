# Question Archive Mobile

Flutter 手机题库阅读器。数据只保存在手机本地 SQLite，支持导入原 Question Archive 导出的 JSON 并**全量覆盖**本地题库。

## 功能

- 文件夹浏览
- 文件夹内问题列表
- 问题/答案搜索
- 全局搜索
- Markdown / 代码块阅读
- 复制答案
- JSON 全量覆盖导入
- SQLite 离线持久化
- 清新木色护眼主题，正文黑色

## 兼容的 JSON

与原项目 `archiveService.js` 导出格式一致：

```json
{
  "version": 1,
  "exportedAt": 0,
  "folders": [],
  "qaItems": []
}
```

导入会先校验，再在一个 SQLite 事务中清空旧数据并写入新数据。导入失败不会留下半套数据。

## 第一次准备

本项目把业务源码完整放在 `lib/`，Android 平台壳由你本机已安装的 Flutter 版本生成，避免 Gradle/Flutter 版本不匹配。

PowerShell：

```powershell
.\Setup.ps1
```

等价于：

```powershell
flutter create . --platforms=android --org com.flankerlym --project-name question_archive_mobile
flutter pub get
```

`flutter create .` 不会删除 `lib/` 中本项目代码。

## IDEA 运行

1. IDEA -> Settings -> Plugins，安装 `Flutter`（会提示一并安装 Dart）。
2. 配置 Flutter SDK 路径。
3. File -> Open，打开本 `mobile-app` 目录。
4. 先在 PowerShell 执行 `flutter doctor`。
5. 准备 Android 设备：Android 模拟器，或开启 USB 调试的安卓手机。
6. IDEA 顶部设备下拉框选择设备。
7. 运行 `lib/main.dart`。

命令行完全等价：

```powershell
flutter devices
flutter run
```

## 在手机上导入题库

1. Java/PC 题库导出 `question-archive-xxxx-xx-xx.json`。
2. 把 JSON 发到手机（微信文件传输、USB、网盘均可）。
3. App 首页右上角点“导入”。
4. 选择 JSON。
5. 确认提示的文件夹和题目数量。旧手机题库已被全量覆盖。

## 打 APK

```powershell
.\Build-Apk.ps1
```

或：

```powershell
flutter build apk --release
```

生成：

```text
build/app/outputs/flutter-apk/app-release.apk
```

把 **app-release.apk** 传到安卓手机，允许“安装未知应用”后点击安装即可。不要把源码 ZIP 传给手机。

## SQLite 文件

数据库名：`question_archive_mobile.db`。它位于 Android 应用自己的数据目录，由系统沙箱管理；卸载 App 会删除它，所以重要数据仍建议保留 PC 端 JSON 备份。

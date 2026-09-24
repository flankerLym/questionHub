$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) {
    throw "未找到 flutter。请先安装 Flutter SDK，并把 flutter\bin 加入 PATH。"
}

if (-not (Test-Path "$Root\android")) {
    flutter create . --platforms=android --org com.flankerlym --project-name question_archive_mobile
}

flutter pub get
Write-Host ""
Write-Host "准备完成。用 IDEA 打开：$Root" -ForegroundColor Green
Write-Host "运行设备查看：flutter devices"
Write-Host "启动：flutter run"

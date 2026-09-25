$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root
flutter pub get
flutter build apk --release
Write-Host ""
Write-Host "APK: $Root\build\app\outputs\flutter-apk\app-release.apk" -ForegroundColor Green

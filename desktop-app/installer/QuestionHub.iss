#ifndef MyAppVersion
  #define MyAppVersion "1.0.0"
#endif

#define MyAppName "QuestionHub"
#define MyAppPublisher "flankerLym"
#define MyAppExeName "QuestionHub.exe"

[Setup]
AppId={{A58F55AE-5B7A-4EA6-9F89-1C9A6F5460C4}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={localappdata}\Programs\QuestionHub
DefaultGroupName=QuestionHub
OutputDir=..\target\installer
OutputBaseFilename=QuestionHub-Setup-{#MyAppVersion}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
UsePreviousAppDir=yes
UninstallDisplayName=QuestionHub

[Files]
Source: "..\target\jpackage\QuestionHub\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\QuestionHub"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\QuestionHub"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; GroupDescription: "附加选项："; Flags: unchecked

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "启动 QuestionHub"; Flags: nowait postinstall skipifsilent

[Code]
var
  DataDirPage: TInputDirWizardPage;

function ConfigDir(): String;
begin
  Result := ExpandConstant('{localappdata}\QuestionHub\config');
end;

function ConfigFile(): String;
begin
  Result := AddBackslash(ConfigDir()) + 'data-dir.txt';
end;

procedure InitializeWizard();
var
  Existing: AnsiString;
  InitialDataDir: String;
begin
  InitialDataDir := ExpandConstant('{localappdata}\QuestionHub\data');
  if LoadStringFromFile(ConfigFile(), Existing) then
    if Trim(String(Existing)) <> '' then
      InitialDataDir := Trim(String(Existing));

  DataDirPage := CreateInputDirPage(
    wpSelectDir,
    '选择数据存储目录',
    'QuestionHub 数据保存在哪里？',
    '数据库、自动备份等数据会保存在这个目录。升级或卸载应用不会自动删除这里的数据。',
    False,
    ''
  );
  DataDirPage.Add('数据目录：');
  DataDirPage.Values[0] := InitialDataDir;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  DataDir: String;
begin
  if CurStep = ssPostInstall then
  begin
    DataDir := DataDirPage.Values[0];
    ForceDirectories(DataDir);
    ForceDirectories(ConfigDir());
    SaveStringToFile(ConfigFile(), DataDir, False);
  end;
end;

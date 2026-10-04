; Run with /DSourceDir=... /DOutputDir=... /DAppVersion=... /DFileVersion=...
#ifndef SourceDir
  #error SourceDir is required
#endif
#ifndef OutputDir
  #error OutputDir is required
#endif
#ifndef AppVersion
  #error AppVersion is required
#endif
#ifndef FileVersion
  #error FileVersion is required
#endif
[Setup]
AppId=SweetCherry.Windows.x64.Portable
AppName=SweetCherry (Windows x64)
AppVersion={#AppVersion}
AppPublisher=Turker Ozturk
AppPublisherURL=https://github.com/turkerozturk/SweetCherry
DefaultDirName={src}\SweetCherry
DisableProgramGroupPage=yes
UsePreviousAppDir=no
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0
Uninstallable=no
CreateUninstallRegKey=no
CloseApplications=no
RestartApplications=no
OutputDir={#OutputDir}
OutputBaseFilename=SweetCherry-{#AppVersion}-windows-x64-setup
VersionInfoVersion={#FileVersion}
SetupIconFile={#SourceDir}\SweetCherry.ico
LicenseFile={#SourceDir}\LICENSE
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
DisableDirPage=no
[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "turkish"; MessagesFile: "compiler:Languages\Turkish.isl"
[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; Flags: unchecked
Name: "startmenuicon"; Description: "Start menu shortcut / Baslat menusu kisayolu"; Flags: unchecked
[Dirs]
Name: "{app}\CTBDATA"
Name: "{app}\allTenants"
Name: "{app}\exportedFiles"
[Files]
; Package only a clean CI distribution, never a user's data directory.
Source: "{#SourceDir}\*"; DestDir: "{app}"; Excludes: "CTBDATA\*,allTenants\*,exportedFiles\*,application.yml,login-credentials.properties,*.log,*.log.*"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#SourceDir}\application.yml"; DestDir: "{app}"; Flags: onlyifdoesntexist
Source: "{#SourceDir}\CTBDATA\demo.ctb"; DestDir: "{app}\CTBDATA"; Flags: onlyifdoesntexist
Source: "{#SourceDir}\allTenants\demo.txt"; DestDir: "{app}\allTenants"; Flags: onlyifdoesntexist
[Icons]
Name: "{userdesktop}\SweetCherry"; Filename: "{app}\SweetCherry.exe"; WorkingDir: "{app}"; Tasks: desktopicon
Name: "{userprograms}\SweetCherry"; Filename: "{app}\SweetCherry.exe"; WorkingDir: "{app}"; Tasks: startmenuicon
[Run]
Filename: "{app}\SweetCherry.exe"; Description: "{cm:LaunchProgram,SweetCherry}"; Flags: nowait postinstall skipifsilent
[Code]
function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  Result := '';
  if CheckForMutexes('SweetCherry.Windows.x64') then
    Result := 'Close SweetCherry before installing or updating. / Kurulumdan once SweetCherry uygulamasini kapatin.';
end;

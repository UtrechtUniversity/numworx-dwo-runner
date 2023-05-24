; -- Sample1.iss --
; Demonstrates copying 3 files and creating an icon.

; SEE THE DOCUMENTATION FOR DETAILS ON CREATING .ISS SCRIPT FILES!

[Setup]
AppName=DWO-Teuniz
AppVerName=DWO_Teuniz version 2.0
AppCopyright=Copyright (C) UU.
DefaultDirName={pf}\DWO\Teuniz
DefaultGroupName=DWO
UninstallDisplayIcon={app}\uninstall.exe
OutputDir=..\output\setup\DWO-Teuniz
OutputBaseFilename=DWO-teuniz-setup
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: en; MessagesFile: "compiler:Default.isl"
Name: nl; MessagesFile: "compiler:Languages\Dutch.isl"

[Files]
Source: "..\output\exe\start-teuniz.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\output\jar\teuniz\DWO.properties"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\tools\jre-64\*"; DestDir: "{app}\jre"; Check: Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\tools\jre-32\*"; DestDir: "{app}\jre"; Check: not Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\tools\lib\*"; DestDir: "{app}\lib"; Check: Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\DWO Teuniz"; Filename: "{app}\start-teuniz.exe"

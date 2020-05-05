; -- Sample1.iss --
; Demonstrates copying 3 files and creating an icon.

; SEE THE DOCUMENTATION FOR DETAILS ON CREATING .ISS SCRIPT FILES!

[Setup]
AppName=Numworx-ACCUU
AppVerName=Numworx_ACCUU version 2.0
AppCopyright=Copyright (C) UU.
DefaultDirName={pf}\Numworx\ACCUU
DefaultGroupName=Numworx
UninstallDisplayIcon={app}\uninstall.exe
OutputDir=..\output\setup\Numworx-ACCUU
OutputBaseFilename=Numworx-accuu-setup
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: en; MessagesFile: "compiler:Default.isl"
Name: nl; MessagesFile: "compiler:Languages\Dutch.isl"

[Files]
Source: "..\output\exe\start-accuu.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\tools\jre-64\*"; DestDir: "{app}\jre"; Check: Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\tools\jre-32\*"; DestDir: "{app}\jre"; Check: not Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\tools\lib\*"; DestDir: "{app}\lib"; Check: Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Author-acceptatie-uu"; Filename: "{app}\start-accuu.exe"

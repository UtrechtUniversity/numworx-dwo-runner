; -- Sample1.iss --
; Demonstrates copying 3 files and creating an icon.

; SEE THE DOCUMENTATION FOR DETAILS ON CREATING .ISS SCRIPT FILES!

[Setup]
AppName=Numworx Author
AppVerName=Numworx Author version 2.4
AppCopyright=Copyright (C) Numworx Solutions BV.
DefaultDirName={pf}\Numworx
DefaultGroupName=Numworx
UninstallDisplayIcon={app}\uninstall.exe
OutputDir=..\output\setup\Numworx
OutputBaseFilename=NumworxAuthor-setup
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: en; MessagesFile: "compiler:Default.isl"
Name: nl; MessagesFile: "compiler:Languages\Dutch.isl"

[Files]
Source: "..\output\exe\start-numworx.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\output\jar\numworx\DWO.properties"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\tools\jre-64\*"; DestDir: "{app}\jre"; Check: Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\tools\jre-32\*"; DestDir: "{app}\jre"; Check: not Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\tools\lib\*"; DestDir: "{app}\lib"; Check: Is64BitInstallMode; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Numworx Author"; Filename: "{app}\start-numworx.exe"

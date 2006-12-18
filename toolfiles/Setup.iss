; -- Sample1.iss --
; Demonstrates copying 3 files and creating an icon.

; SEE THE DOCUMENTATION FOR DETAILS ON CREATING .ISS SCRIPT FILES!

[Setup]
AppName=DWO_runner
AppVerName=DWO_runner version 1.0
AppCopyright=Copyright (C) Freudenthal Instituut.
DefaultDirName={pf}\Wisweb\DWO_runner
DefaultGroupName=Wisweb
UninstallDisplayIcon={app}\DWO_runner.exe
MessagesFile=compiler:Dutch-1-2_0_18.isl
OutputDir=..\output\setup

[Files]
Source: "..\output\exe\DWO_runner.exe"; DestDir: "{app}"


[Icons]
Name: "{group}\DWO_runner"; Filename: "{app}\DWO_runner.exe"

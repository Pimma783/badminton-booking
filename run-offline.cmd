@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\project.ps1" -Action offline
if errorlevel 1 pause

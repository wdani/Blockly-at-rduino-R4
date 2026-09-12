@echo off
setlocal
cd /d "%~dp0"
start "Blockly@rduino R4 Server" powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\start-r4-server.ps1"
exit /b 0

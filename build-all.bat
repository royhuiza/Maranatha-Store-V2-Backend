@echo off
cd /d %~dp0
call mvnw.cmd clean package -DskipTests
if errorlevel 1 exit /b 1
echo Backend compilado correctamente.

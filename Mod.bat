@echo off
title NoKABOOM Client
cd /d "%~dp0"
call gradlew.bat runClient
pause

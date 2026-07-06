@echo off
title mPOS Pro — Gradle Build
cd /d "%~dp0"
echo ============================================================
echo  mPOS Pro — assembleDebug
echo ============================================================
echo.

set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

echo JAVA_HOME = %JAVA_HOME%
echo.

call gradlew.bat assembleDebug --stacktrace > build_log.txt 2>&1
type build_log.txt

echo.
echo ============================================================
if %ERRORLEVEL%==0 (
    echo  BUILD THANH CONG! APK tai: app\build\outputs\apk\debug\
) else (
    echo  BUILD THAT BAI! Xem chi tiet trong build_log.txt
)
echo ============================================================
pause

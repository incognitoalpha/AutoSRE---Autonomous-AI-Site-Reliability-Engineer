@echo off
setlocal enabledelayedexpansion
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d C:\Users\sarth\Desktop\AI Agent

echo Running Gradle build...
call gradlew.bat build > build_output.txt 2>&1

echo Done. Checking build_output.txt for results.
@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
echo Java version:
java -version
echo.
echo Running Gradle build:
cd /d C:\Users\sarth\Desktop\AI Agent
call gradlew.bat build
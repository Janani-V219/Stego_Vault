@REM ----------------------------------------------------------------------------
@REM StegoVault Maven Wrapper Script for Windows
@REM ----------------------------------------------------------------------------
@echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set MVN_CMD="%DIRNAME%apache-maven-3.9.6\bin\mvn.cmd"
if exist %MVN_CMD% (
    %MVN_CMD% %*
) else (
    mvn %*
)

@echo off
REM This script launches Unicode Rewriter

SETLOCAL

REM Java 1.4.2 or above is needed
REM ##################################################################
REM ## Modify this so that the correct Java virtual machine is used ##
SET JAVA=javaw
REM ##################################################################

%JAVA% -jar UnicodeRewriter.jar 

ENDLOCAL

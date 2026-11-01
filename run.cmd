@echo off
pushd "%~dp0"
java -jar dist\smart-home.jar %*
set RESULT=%ERRORLEVEL%
popd
exit /b %RESULT%

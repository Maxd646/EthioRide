@echo off
setlocal

set LIB=lib\javafx.base.jar;lib\javafx.controls.jar;lib\javafx.fxml.jar;lib\javafx.graphics.jar;lib\javafx.web.jar;lib\mysql-connector-j.jar
set OUT=out

echo [1/5] Cleaning output...
if exist %OUT% rmdir /s /q %OUT%
mkdir %OUT%

echo [2/5] Compiling shared...
for /r shared\src\main\java %%f in (*.java) do echo %%f >> sources_tmp.txt
javac -cp "%LIB%" -d %OUT% @sources_tmp.txt
if errorlevel 1 ( echo FAILED: shared & del sources_tmp.txt & exit /b 1 )
del sources_tmp.txt

echo [3/5] Compiling server...
for /r server\src\main\java %%f in (*.java) do echo %%f >> sources_tmp.txt
javac -cp "%LIB%;%OUT%" -d %OUT% @sources_tmp.txt
if errorlevel 1 ( echo FAILED: server & del sources_tmp.txt & exit /b 1 )
del sources_tmp.txt

echo [4/5] Compiling client-passenger...
for /r client-passenger\src\main\java %%f in (*.java) do echo %%f >> sources_tmp.txt
javac -cp "%LIB%;%OUT%" -d %OUT% @sources_tmp.txt
if errorlevel 1 ( echo FAILED: client-passenger & del sources_tmp.txt & exit /b 1 )
del sources_tmp.txt

echo [5/5] Compiling client-driver and admin-dashboard...
for /r client-driver\src\main\java %%f in (*.java) do echo %%f >> sources_tmp.txt
for /r admin-dashboard\src\main\java %%f in (*.java) do echo %%f >> sources_tmp.txt
javac -cp "%LIB%;%OUT%" -d %OUT% @sources_tmp.txt
if errorlevel 1 ( echo FAILED: client-driver/admin & del sources_tmp.txt & exit /b 1 )
del sources_tmp.txt

echo.
echo Copying resources...
xcopy /s /y server\src\main\resources\* %OUT%\ >nul
xcopy /s /y client-passenger\src\main\resources\* %OUT%\ >nul
xcopy /s /y client-driver\src\main\resources\* %OUT%\ >nul
xcopy /s /y admin-dashboard\src\main\resources\* %OUT%\ >nul

echo.
echo BUILD SUCCESSFUL — classes in: %OUT%
endlocal

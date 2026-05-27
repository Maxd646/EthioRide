@echo off
set JFXSDK=C:\Users\hp\Downloads\openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1
set LIB=%JFXSDK%\lib\javafx.base.jar;%JFXSDK%\lib\javafx.controls.jar;%JFXSDK%\lib\javafx.fxml.jar;%JFXSDK%\lib\javafx.graphics.jar;lib\mysql-connector-j.jar
java --module-path "%JFXSDK%\lib" --add-modules javafx.controls,javafx.graphics,javafx.base -cp "out;%LIB%" com.ethioride.admin.Main

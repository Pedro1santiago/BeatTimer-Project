@echo off
cd /d "%~dp0"
jre-custom\bin\java.exe --module-path "lib\javafx-sdk-17.0.16\lib" --add-modules javafx.controls,javafx.fxml -jar Relogio.jar
pause

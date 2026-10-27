@echo off
title Bang! - Server Java
echo ===================================
echo   Avvio Server Java Bang!
echo   Porta: http://localhost:8080
echo ===================================
echo.

cd /d "%~dp0"

:: Trova gson jar
for %%f in ("lib\gson*.jar") do set GSON=%%f
if not defined GSON (
    echo ERRORE: gson.jar non trovato in lib\
    pause
    exit /b 1
)

:: Compila
echo [1/2] Compilazione in corso...
if exist bin rmdir /s /q bin
mkdir bin

for /r src %%f in (*.java) do echo %%f >> _sources.txt
javac -encoding UTF-8 -cp "bin;%GSON%" -d bin @_sources.txt 2>&1
del _sources.txt

if errorlevel 1 (
    echo.
    echo ERRORE: Compilazione fallita!
    pause
    exit /b 1
)

echo [2/2] Compilazione OK! Avvio server...
echo.
echo Apri http://localhost:3000 nel browser dopo aver avviato il frontend.
echo Premi CTRL+C per fermare il server.
echo.

java -cp "bin;%GSON%" bang.Main
pause

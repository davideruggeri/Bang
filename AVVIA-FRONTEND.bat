@echo off
title Bang! - Frontend Vite
echo ===================================
echo   Avvio Frontend Vite Bang!
echo   Porta: http://localhost:3000
echo ===================================
echo.

cd /d "%~dp0frontend"

:: Installa dipendenze se node_modules non esiste
if not exist node_modules (
    echo Installazione dipendenze npm...
    npm install
)

echo Avvio server Vite su http://localhost:3000 ...
echo Il browser si aprira automaticamente.
echo Premi CTRL+C per fermare il frontend.
echo.

start "" "http://localhost:3000"
npm run dev
pause

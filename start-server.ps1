# Script PowerShell per avviare il server Java Bang! (headless, senza GUI Swing)
# Esegui da: c:\App\Bang\

$ErrorActionPreference = "Stop"

$ROOT = $PSScriptRoot
$SRC  = "$ROOT\src"
$BIN  = "$ROOT\bin"

# Trova il JAR di Gson in lib\
$GSON = Get-ChildItem "$ROOT\lib\gson*.jar" | Select-Object -First 1
if (-not $GSON) {
    Write-Error "ERRORE: gson.jar non trovato in $ROOT\lib\"
    exit 1
}

Write-Host "==================================================" -ForegroundColor Yellow
Write-Host "  Bang! Java Server — Compilazione + Avvio"        -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Yellow

# 1. Pulisci e ricrea bin\
if (Test-Path $BIN) { Remove-Item $BIN -Recurse -Force }
New-Item -ItemType Directory -Path $BIN | Out-Null

# 2. Raccogli tutti i sorgenti .java
$SOURCES = Get-ChildItem -Path $SRC -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

Write-Host "`nCompilazione in corso..." -ForegroundColor Cyan
$CLASSPATH = "$BIN;$($GSON.FullName)"

& javac -cp $CLASSPATH -d $BIN $SOURCES 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "`nERRORE: Compilazione fallita."
    exit 1
}

Write-Host "Compilazione OK!" -ForegroundColor Green
Write-Host "`nAvvio server su http://localhost:8080 ..." -ForegroundColor Cyan
Write-Host "Apri http://localhost:3000 nel browser (avvia prima npm run dev)`n" -ForegroundColor Magenta

# 3. Avvia il server (rimane in foreground finché non si chiude con Ctrl+C)
& java -cp $CLASSPATH bang.Main

@echo off
setlocal
rem ============================================================
rem  Avvia Atlante.
rem
rem  Cerca un Java utilizzabile in questo ordine:
rem    1. il JDK portatile dentro il progetto (strumenti\jdk8)
rem    2. java nel PATH
rem    3. le installazioni in Program Files (JRE o JDK)
rem
rem  Il tema e il catalogo si caricano dalle cartelle del
rem  progetto: va avviato da qui, non da un'altra cartella.
rem ============================================================

set "QUI=%~dp0"
cd /d "%QUI%"

if not exist classi (
  echo Le classi non ci sono: eseguo prima la compilazione.
  call compila.bat
  if errorlevel 1 (
    pause
    exit /b 1
  )
)

set "JAVA="

if exist "strumenti\jdk8\bin\javaw.exe" set "JAVA=%QUI%strumenti\jdk8\bin\javaw.exe"

if not defined JAVA (
  where javaw >nul 2>nul
  if not errorlevel 1 set "JAVA=javaw"
)

if not defined JAVA (
  for /d %%D in ("%ProgramFiles%\Java\jre*") do (
    if exist "%%~fD\bin\javaw.exe" set "JAVA=%%~fD\bin\javaw.exe"
  )
)

if not defined JAVA (
  for /d %%D in ("%ProgramFiles%\Java\jdk*") do (
    if exist "%%~fD\bin\javaw.exe" set "JAVA=%%~fD\bin\javaw.exe"
  )
)

if not defined JAVA (
  if exist "%ProgramFiles%\Java\latest\bin\javaw.exe" set "JAVA=%ProgramFiles%\Java\latest\bin\javaw.exe"
)

if not defined JAVA (
  for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\*") do (
    if exist "%%~fD\bin\javaw.exe" set "JAVA=%%~fD\bin\javaw.exe"
  )
)

if not defined JAVA (
  echo.
  echo Non trovo Java su questo computer.
  echo Serve Java 8 o superiore. Scaricalo da: https://adoptium.net/
  echo.
  pause
  exit /b 1
)

echo Avvio con: %JAVA%
start "Atlante" "%JAVA%" -Xmx2g -Dfile.encoding=UTF-8 -cp "classi;lib\flatlaf.jar" it.atlante.Atlante
exit /b 0

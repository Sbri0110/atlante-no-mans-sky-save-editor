@echo off
setlocal
rem ============================================================
rem  Avvia Atlante.
rem
rem  Usa il JDK incluso nella cartella del progetto se c'e'
rem  (strumenti\jdk8), altrimenti il java del sistema.
rem
rem  Il tema e il catalogo si caricano dalle cartelle del
rem  progetto: va avviato da qui, non da un'altra cartella.
rem ============================================================

set "QUI=%~dp0"
cd /d "%QUI%"

if not exist classi (
  echo Le classi non ci sono: eseguo prima la compilazione.
  call compila.bat
  if errorlevel 1 exit /b 1
)

set "JAVA=java"
if exist "strumenti\jdk8\bin\java.exe" set "JAVA=strumenti\jdk8\bin\java.exe"

"%JAVA%" -Xmx2g -Dfile.encoding=UTF-8 -cp "classi;lib\flatlaf.jar" it.atlante.Atlante
if errorlevel 1 (
  echo.
  echo Avvio non riuscito. Se manca il tema, controlla che lib\flatlaf.jar ci sia.
  pause
)

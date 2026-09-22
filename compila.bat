@echo off
setlocal
rem ============================================================
rem  Compila Atlante e prepara Atlante.jar.
rem
rem  Serve un JDK (non basta il JRE: serve javac). Viene cercato
rem  in questo ordine:
rem    1. il JDK portatile dentro il progetto (strumenti\jdk8)
rem    2. javac nel PATH
rem    3. i JDK installati in Program Files
rem    4. il JDK portatile del progetto precedente, se esiste
rem
rem  La libreria del tema (lib\flatlaf.jar) e' gia' nel progetto:
rem  non c'e' nulla da scaricare.
rem
rem  Il flag -encoding UTF-8 e' OBBLIGATORIO: i sorgenti
rem  contengono accenti, e senza di esso il compilatore li legge
rem  come Latin-1 producendo testo doppiamente codificato.
rem ============================================================

set "QUI=%~dp0"
cd /d "%QUI%"

set "JAVAC="
set "JAR="

if exist "strumenti\jdk8\bin\javac.exe" (
  set "JAVAC=%QUI%strumenti\jdk8\bin\javac.exe"
  set "JAR=%QUI%strumenti\jdk8\bin\jar.exe"
)

if not defined JAVAC (
  where javac >nul 2>nul
  if not errorlevel 1 (
    set "JAVAC=javac"
    set "JAR=jar"
  )
)

if not defined JAVAC (
  for /d %%D in ("%ProgramFiles%\Java\jdk*") do (
    if exist "%%~fD\bin\javac.exe" (
      set "JAVAC=%%~fD\bin\javac.exe"
      set "JAR=%%~fD\bin\jar.exe"
    )
  )
)

if not defined JAVAC (
  for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\*") do (
    if exist "%%~fD\bin\javac.exe" (
      set "JAVAC=%%~fD\bin\javac.exe"
      set "JAR=%%~fD\bin\jar.exe"
    )
  )
)

if not defined JAVAC (
  echo.
  echo Non trovo javac su questo computer.
  echo Per compilare serve un JDK, non basta il JRE.
  echo Scaricalo da: https://adoptium.net/  ^(versione 8 o superiore^)
  echo.
  echo Se hai solo il JRE, le classi compilate potrebbero essere
  echo gia' presenti: in quel caso avvia.bat funziona lo stesso.
  echo.
  pause
  exit /b 1
)

echo Compilatore: %JAVAC%
if not exist classi mkdir classi
pushd src
"%JAVAC%" -encoding UTF-8 -cp "..\lib\flatlaf.jar" -d ..\classi it\atlante\*.java it\atlante\io\*.java it\atlante\json\*.java it\atlante\nms\*.java it\atlante\finestra\*.java it\atlante\strumenti\*.java
if errorlevel 1 goto errore
popd

rem --- archivio avviabile, per il doppio clic ---
if defined JAR (
  if exist "%JAR%" (
    echo Creo Atlante.jar ...
    > manifest.txt echo Manifest-Version: 1.0
    >> manifest.txt echo Main-Class: it.atlante.Atlante
    >> manifest.txt echo Class-Path: lib/flatlaf.jar
    "%JAR%" cfm Atlante.jar manifest.txt -C classi .
    del manifest.txt
  )
)

echo.
echo Compilazione completata.
echo.
echo Per avviare:  avvia.bat     (oppure doppio clic su Atlante.jar)
exit /b 0

:errore
popd
echo.
echo Compilazione FALLITA.
pause
exit /b 1

@echo off
setlocal
rem ============================================================
rem  Compila Atlante.
rem
rem  Serve un JDK 8 o superiore nel PATH. La libreria del tema
rem  (lib\flatlaf.jar) e' gia' dentro il progetto: non c'e'
rem  nulla da scaricare.
rem
rem  Il flag -encoding UTF-8 e' OBBLIGATORIO: i sorgenti
rem  contengono accenti, e senza di esso il compilatore li legge
rem  come Latin-1 producendo testo doppiamente codificato
rem  ("Difficolta'" diventa "DifficoltA^").
rem ============================================================

if not exist classi mkdir classi
pushd src
javac -encoding UTF-8 -cp "..\lib\flatlaf.jar" -d ..\classi it\atlante\*.java it\atlante\io\*.java it\atlante\json\*.java it\atlante\nms\*.java it\atlante\finestra\*.java it\atlante\strumenti\*.java
if errorlevel 1 goto errore
popd
echo.
echo Compilazione completata.
echo Avvia con:  avvia.bat
echo Riga di comando:  java -cp "classi;lib\flatlaf.jar" it.atlante.Main --aiuto
exit /b 0

:errore
popd
echo.
echo Compilazione FALLITA.
exit /b 1

#!/bin/sh
# ============================================================
#  Compila Atlante.
#
#  Serve un JDK 8 o superiore nel PATH. La libreria del tema
#  (lib/flatlaf.jar) e' gia' dentro il progetto: non c'e'
#  nulla da scaricare.
#
#  Il flag -encoding UTF-8 e' obbligatorio, altrimenti il
#  compilatore legge i sorgenti come Latin-1 e gli accenti
#  finiscono doppiamente codificati.
# ============================================================
set -e

mkdir -p classi
cd src
javac -encoding UTF-8 -cp "../lib/flatlaf.jar" -d ../classi \
    it/atlante/*.java \
    it/atlante/io/*.java \
    it/atlante/json/*.java \
    it/atlante/nms/*.java \
    it/atlante/finestra/*.java \
    it/atlante/strumenti/*.java
cd ..

echo
echo "Compilazione completata."
echo "Avvia con:  java -cp \"classi:lib/flatlaf.jar\" it.atlante.Atlante"
echo "Da riga di comando:  java -cp \"classi:lib/flatlaf.jar\" it.atlante.Main --aiuto"

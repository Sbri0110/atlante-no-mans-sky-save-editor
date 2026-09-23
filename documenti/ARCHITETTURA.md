# Architettura

## Il codice

```
src/it/atlante/
  Atlante.java              avvio dell'interfaccia
  Main.java                 riga di comando
  io/
    Lz4.java                compressione LZ4, scritta a mano
    FlussoBlocchi.java      il flusso a blocchi con magic e intestazioni
  json/
    Json.java               lettore e scrittore JSON
  nms/
    Formato.java            riconoscimento dei due formati
    Catalogo.java           catalogo di gioco: nomi, icone, descrizioni
    MappaChiavi.java        mappa terna <-> nome proprieta'
    ContenitoreWgs.java     contenitori Xbox Game Pass
    Rilevatore.java         ricerca dei salvataggi sulla macchina
    Salvataggio.java        il salvataggio aperto, con verifica e scrittura
  finestra/
    Aspetto.java            tema FlatLaf con la tavolozza del marchio
    Marchio.java            icona dell'applicazione
    Icone.java              icone di gioco, con memoria
    AlberoDati.java         albero dei campi, con icone e modifica
    PannelloDettagli.java   scheda dell'oggetto e modifica dei valori
    Finestra.java           finestra principale
  strumenti/
    Schermate.java          genera le schermate della documentazione
    Collaudo.java           prova lettura e scrittura su una copia del file
lib/
  flatlaf.jar               tema dell'interfaccia (Apache 2.0)
risorse/
  db/                       catalogo, mappe delle chiavi, database di gioco
  icone/                    icone degli oggetti
```

---

## Quattro decisioni che vale la pena spiegare

**I numeri non passano per la virgola mobile.** Un campo come
`0.6000000238418579` letto come `double` e riscritto diventa `0.6`, e il gioco
riceve un valore diverso da quello che aveva scritto. Un identificatore intero
grande supera i 53 bit e viene arrotondato in silenzio. Ogni numero conserva il
testo originale.

**L'ordine dei campi viene conservato.** Un JSON con le chiavi in ordine diverso
resta valido, ma rende impossibile il confronto durante il collaudo.

**La verifica viene prima della scrittura, non dopo.** Non c'è un "sei sicuro?"
da cliccare: se il contenuto non sopravvive al giro completo, il programma si
ferma. Una copia di sicurezza con data e ora viene comunque creata, e la
sostituzione è atomica.

**Il catalogo è separato dal salvataggio.** Il salvataggio contiene sigle; il
catalogo sa che `^AF_METAL` è "Tainted Metal", che è un rottame recuperato, e
qual è la sua descrizione. Tenendoli separati, un aggiornamento del gioco si
gestisce aggiornando un file di dati, senza toccare una riga di codice.

---

## Perché è scritto da zero

Esiste già un editor di salvataggi per No Man's Sky, molto diffuso, e non
dichiara alcuna licenza. Modificarlo o ridistribuirlo significherebbe usare
codice altrui senza permesso.

Questo progetto non contiene codice di terzi: legge un formato di file, e i
formati non sono protetti dal diritto d'autore. L'unica libreria inclusa è
FlatLaf (Apache 2.0), il tema dell'interfaccia, con la sua licenza.

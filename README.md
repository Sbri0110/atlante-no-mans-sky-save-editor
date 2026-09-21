<p align="center">
  <img src="assets/marchio/logo-512.png" width="150" alt="Marchio di Atlante">
</p>

<h1 align="center">Atlante</h1>

<p align="center">
  <strong>Editor di salvataggi per No Man's Sky</strong><br>
  Scritto da zero in Java, senza dipendenze esterne
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/licenza-MIT-E5484D.svg" alt="Licenza MIT"></a>
  <a href="https://adoptium.net/"><img src="https://img.shields.io/badge/Java-8%2B-FF7A7A.svg" alt="Java 8 o superiore"></a>
  <img src="https://img.shields.io/badge/dipendenze-nessuna-4ADE80.svg" alt="Nessuna dipendenza">
  <img src="https://img.shields.io/badge/catalogo-5587%20voci-FEBC2E.svg" alt="Catalogo di 5587 voci">
</p>

---

Legge i salvataggi di No Man's Sky, ne decodifica le chiavi cifrate, li mostra in un
albero con **le icone e i nomi di gioco** e li riscrive **verificando che i dati non
cambino**.

Prima di ogni scrittura il contenuto viene ricompattato, riscompattato e confrontato
campo per campo con l'originale: se anche un solo valore differisce, il file non viene
toccato.

<p align="center">
  <img src="assets/schermate/atlante-tuta.png" width="100%" alt="Finestra di Atlante: elenco dei salvataggi a sinistra, albero dei campi con le icone degli oggetti al centro, scheda dell'oggetto a destra">
</p>

---

## Cosa fa

| Funzione | Stato |
|---|---|
| Lettura dei salvataggi Xbox Game Pass (WGS) | funziona |
| Lettura dei salvataggi Steam e GOG | funziona |
| Dati account (ricompense, QuickSilver) | funziona |
| Scompattazione e ricompressione LZ4 | funziona — scritta a mano |
| Decodifica delle chiavi cifrate | 640.510 su 640.567 |
| Catalogo di gioco con nomi, icone, categorie e descrizioni | 5.587 voci |
| Interfaccia grafica con tema scuro e icone di gioco | funziona |
| Ricerca fra i campi e fra gli oggetti | funziona |
| Modifica dei valori | funziona |
| Scrittura con verifica automatica e copia di sicurezza | funziona |
| Copertura dei campi del salvataggio | tutti |
| Riga di comando per script e automazioni | funziona |

**Misure sul salvataggio di prova** (Xbox Game Pass, settembre 2026): 1.743.369 byte
compressi → 9.842.314 byte decompressi, 19 blocchi, durata di gioco 185 h 58 min.

---

## Come si prova

**Su Windows non serve installare nulla.** Il progetto include un JDK portatile in
`strumenti/jdk8/`: basta avviarlo.

| | |
|---|---|
| **Doppio clic su `avvia.bat`** | cerca Java, compila se serve e apre la finestra |
| **Doppio clic su `Atlante.jar`** | apre direttamente l'interfaccia, se hai già compilato |

`avvia.bat` cerca Java in questo ordine: il JDK dentro il progetto, `java` nel PATH,
le installazioni in `Program Files`, e infine il JDK portatile del progetto
precedente. Se non lo trova te lo dice invece di fallire in silenzio.

### Compilare

Serve un **JDK** (non basta il JRE: serve `javac`). Il JDK portatile incluso va bene.

```bat
:: Windows
compila.bat
```

```bash
# Linux / macOS — serve un JDK nel PATH
./compila.sh
java -cp "classi:lib/flatlaf.jar" it.atlante.Atlante
```

Nessuna libreria da scaricare: il tema (`lib/flatlaf.jar`) è già nel progetto.

### Cosa fa all'avvio

Trova i salvataggi sulla macchina e apre il primo. Se non ne trova, usa
**File → Apri file...**.

Se le icone degli oggetti non compaiono, mancano i PNG in `risorse/icone/`:
il programma funziona lo stesso, mostrando gli identificatori al posto delle
immagini. Vedi [NOTICE.md](NOTICE.md) per il motivo per cui non sono incluse.

### Riga di comando

```bash
java -cp "classi;lib/flatlaf.jar" it.atlante.Main elenca "<cartella WGS>"
java -cp "classi;lib/flatlaf.jar" it.atlante.Main info   <file>
java -cp "classi;lib/flatlaf.jar" it.atlante.Main dump   <file> uscita.json
java -cp "classi;lib/flatlaf.jar" it.atlante.Main giro   <file>
```

| Comando | Cosa fa |
|---|---|
| `elenca <cartella>` | elenca gli slot trovati, con nome, durata di gioco e dimensioni |
| `info <file>` | riassunto: blocchi, rapporto, chiavi riconosciute, campi di primo livello |
| `dump <file> [uscita]` | scrive il salvataggio in JSON con i nomi dei campi leggibili |
| `giro <file>` | **il collaudo che conta**: legge, riscrive, rilegge e verifica che i dati coincidano |

### Dove sono i salvataggi

| Piattaforma | Percorso |
|---|---|
| Steam / GOG | `%USERPROFILE%\AppData\Roaming\HelloGames\NMS\` |
| Xbox Game Pass | `%LOCALAPPDATA%\Packages\HelloGames.NoMansSky_bs190hzg1sesy\SystemAppData\wgs\` |

---

## Il formato dei salvataggi

Documentazione verificata sui byte, non ricavata da supposizioni. Serve anche a chi
vuole scrivere un editor per conto proprio.

### 1. I due formati

No Man's Sky scrive i dati in due modi diversi, e vanno riconosciuti dal **contenuto**,
perché nulla nel nome del file li distingue:

| Formato | Dove | Com'è fatto |
|---|---|---|
| **Blocchi** | salvataggi principali | sequenza di blocchi, ognuno con intestazione di 16 byte e un blocco LZ4 |
| **LZ4 grezzo** | dati account | un solo blocco LZ4 senza intestazione |

### 2. Il flusso a blocchi

```
ripetuto fino a fine file:
  magic             4 byte    E5 A1 ED FE
  dimensioneComp    4 byte    uint32 little endian
  dimensioneDecomp  4 byte    uint32 little endian   (524288 = 512 KB)
  riservato         4 byte    sempre zero
  dati              dimensioneComp byte, blocco LZ4 grezzo
```

Non c'è intestazione di file: il primo blocco comincia subito. La dimensione decompressa
è sempre 524.288 byte, tranne nell'ultimo blocco.

### 3. Il contenitore Xbox Game Pass

```
<radice>/containers.index                   elenco dei contenitori
<radice>/<GUID 32 caratteri>/container.NN   descrittore, contiene il nome "data"
<radice>/<GUID>/<GUID>                      payload: flusso a blocchi
<radice>/<GUID>/<GUID>                      metadati: durata, nome del salvataggio
```

I due file con nome a GUID si distinguono dal contenuto, non dal nome: il payload
comincia con il magic del flusso a blocchi, i metadati con un'intestazione di 20 byte.

Il suffisso di `container.NN` **non è fisso**: il gioco lo incrementa (`container.23`,
`container.27`, `container.31`). Va sempre cercato il più recente presente su disco.

### 4. `containers.index`

Serializzazione .NET. Ogni stringa è preceduta da un `uint32` con il numero di
caratteri, seguita da UTF-16LE. Ogni voce ha questa forma:

```
<nome slot>   <identificativo fra virgolette>   <GUID binario di 16 byte>
```

Il nome dello slot **precede** il GUID, che compare in forma binaria con i primi tre
campi invertiti (ordine .NET). Verificato:

| GUID | Slot |
|---|---|
| `5FFAB16A…` | `AccountData` |
| `18C4F9F2…` | `Slot3Auto` |
| `6D75D8AC…` | `Slot3Manual` |

### 5. I metadati

```
0   uint32   costante 4225 nei salvataggi di prova
4   uint32   costante 1
8   uint32   durata di gioco in secondi
12  uint32   zero
16  uint32   dimensione del payload decompresso
20  ...      nome del salvataggio, testo a terminazione nulla
```

### 6. Le chiavi cifrate

Il gioco non scrive i nomi dei campi. Scrive terne di tre caratteri prodotte da un hash
del nome: `{"F2P":4737}` invece di `{"Version":4737}`.

La corrispondenza fra terna e nome è **un dato, non un algoritmo**: le terne sono
calcolabili, i nomi si scoprono leggendo i salvataggi. `risorse/db/jsonmap.txt` contiene
quell'elenco, una voce per riga nel formato `terna<TAB>NomeProprieta`.

Le 57 chiavi ancora ignote sono proprietà introdotte da aggiornamenti recenti del gioco.
Restano visibili con la loro terna invece di essere perse.

---

## Architettura

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
lib/
  flatlaf.jar               tema dell'interfaccia (Apache 2.0)
risorse/
  db/                       catalogo, mappe delle chiavi, database di gioco
  icone/                    icone degli oggetti
```

### Quattro decisioni che vale la pena spiegare

**I numeri non passano per la virgola mobile.** Un campo come `0.6000000238418579`
letto come `double` e riscritto diventa `0.6`, e il gioco riceve un valore diverso da
quello che aveva scritto. Un identificatore intero grande supera i 53 bit e viene
arrotondato in silenzio. Ogni numero conserva il testo originale.

**L'ordine dei campi viene conservato.** Un JSON con le chiavi in ordine diverso resta
valido, ma rende impossibile il confronto durante il collaudo.

**La verifica viene prima della scrittura, non dopo.** Non c'è un "sei sicuro?" da
cliccare: se il contenuto non sopravvive al giro completo, il programma si ferma. Una
copia di sicurezza con data e ora viene comunque creata, e la sostituzione è atomica.

**Il catalogo è separato dal salvataggio.** Il salvataggio contiene sigle; il catalogo
sa che `^AF_METAL` è "Tainted Metal", che è un rottame recuperato, e qual è la sua
descrizione. Tenendoli separati, un aggiornamento del gioco si gestisce aggiornando un
file di dati, senza toccare una riga di codice.

---

## Perché è scritto da zero

Esiste già un editor di salvataggi per No Man's Sky, molto diffuso, e non dichiara alcuna
licenza. Modificarlo o ridistribuirlo significherebbe usare codice altrui senza permesso.

Questo progetto non contiene codice di terzi: legge un formato di file, e i formati non
sono protetti dal diritto d'autore. L'unica libreria inclusa è FlatLaf (Apache 2.0), il
tema dell'interfaccia, con la sua licenza.

---

## Come contribuire

Il contributo più utile è **segnalare una discrepanza**: un salvataggio che non si legge,
un valore che cambia dopo il giro completo, una chiave ignota di cui conosci il nome.

Il comando `giro` è lo strumento con cui verificare qualsiasi modifica alla lettura o
alla scrittura: **se non passa, la modifica è sbagliata.**

---

## Licenza

Codice rilasciato con licenza **MIT** — vedi [LICENSE](LICENSE).
Le librerie incluse, i dati di gioco e i marchi sono documentati in [NOTICE.md](NOTICE.md).

No Man's Sky e tutti i nomi correlati sono marchi di Hello Games Limited.
Questo progetto non è affiliato, approvato o sponsorizzato da Hello Games.

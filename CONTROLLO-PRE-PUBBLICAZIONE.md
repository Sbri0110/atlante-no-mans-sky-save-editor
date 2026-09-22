# Controllo prima della pubblicazione su GitHub

**Progetto:** Atlante — editor di salvataggi per No Man's Sky
**Data:** 22 settembre 2026
**Esito:** cinque problemi trovati, tutti corretti; il codice regge

---

## In una riga

Il motore di lettura e scrittura è solido. I problemi trovati erano di due
tipi: **cosa sarebbe finito su GitHub** (la memoria di lavoro con i dati
privati, gli appunti sul vecchio editor, le schermate di una versione
superata) e **cosa non avrebbe funzionato sul salvataggio di un altro** — in
particolare la lettura della partita principale al posto della spedizione, che
sul mio salvataggio non si vedeva.

---

## 1. Cosa è stato verificato, e come

Non a occhio: con prove eseguibili su un salvataggio vero.

| Controllo | Comando | Esito |
|---|---|---|
| Compilazione completa | `javac -Xlint:all` su 41 file | **0 errori** |
| Avvisi del compilatore | — | 66, tutti innocui |
| Giro completo dei dati | `Main giro` | **byte identici** |
| Scrittura su disco | `strumenti.Collaudo` | **26 prove su 26** |
| Collisioni nella mappa chiavi | analisi di `jsonmap.txt` | **0 su 1310** |
| Avvio e tutte le sezioni | `strumenti.Schermate` | **15 sezioni su 15** |
| Percorsi legati al contesto attivo | dentro `Collaudo` | **42 su 42** |

**Il giro completo è la garanzia che conta:** il salvataggio viene letto,
riscritto e riletto, e i due contenuti devono coincidere campo per campo. Su
un file reale da 9,4 MB decompressi e 19 blocchi, il risultato è identico.

### Gli avvisi del compilatore, spiegati

Sono 66, nessuno dei quali è un difetto:

- **39 `serial`** — `JPanel` e simili non dichiarano un numero di versione.
  Irrilevante: questi oggetti non vengono mai serializzati.
- **25 `unchecked`** — conversioni di tipo sul contenuto JSON, che è per sua
  natura non tipizzato. Sono già marcate come volute nel codice.
- **2 `cast`** ridondanti, in `AlberoDati`. Innocui.

---

## 2. I problemi trovati, e cosa è stato fatto

Alcuni riguardano **cosa sarebbe finito su GitHub**, altri **cosa non avrebbe
funzionato sul salvataggio di un altro**. In ordine di gravità.

### 2.1 Dati privati pronti a partire per GitHub — il più grave

I file di memoria di lavoro erano **tracciati da git**. Contenevano:

- il percorso completo dei salvataggi veri;
- i nomi delle partite usate come prova;
- il nome della corvetta e altre misure fatte su quei file;
- il percorso del vecchio editor sulla tua macchina.

Pubblicando, sarebbero stati visibili a chiunque.

**Fatto:** `.workbuddy-ai/` è entrato nel `.gitignore` e i file sono stati
tolti dal tracciamento con `git rm --cached`. **I file restano sul disco**:
non sono stati cancellati, solo esclusi dalla pubblicazione.

### 2.2 Appunti di lavoro dentro il repository

`riferimenti/` conteneva 8 fotogrammi del vecchio editor, la griglia delle
icone e `funzioni-grezze.txt` (l'estratto delle funzioni del vecchio jar):
strumenti di ricostruzione, non documentazione del progetto.

**Fatto:** fuori dal tracciamento. Restano sul disco per il lavoro.

### 2.3 Schermate di una versione superata

`index.html` e il `README` citano le immagini in `assets/schermate/`. Ma la
cartella `schermate/` (uscita grezza dello strumento) era anch'essa tracciata,
con tre file identici e uno in più.

E le immagini pubblicate erano **vecchie**: fotografate prima che la sezione
Corvette entrasse nella navigazione. Confrontandole con quelle appena
generate, 14 su 15 differivano — per circa il 2% dei pixel, tutti nella fascia
dell'elenco a sinistra, dove mancavano la voce "Corvette" e il pulsante
"Importa parti...".

**Fatto:** cartella `schermate/` fuori dal tracciamento, immagini di
`assets/schermate/` **rigenerate** dal salvataggio vero. Ora combaciano byte
per byte con quello che l'applicazione mostra.

### 2.4 Gli identificativi del salvataggio, sparsi ovunque

Cercando le tracce del salvataggio di prova in **tutti** i file pubblicati ne
sono usciti altri, che una prima passata aveva mancato:

- `assets/schermate/elenca.svg` mostrava il **percorso completo** della tua
  cartella WGS: nome utente, identificativo dell'account, GUID dei contenitori
  e il nome della tua partita.
- `giro.svg` e `info.svg` citavano lo stesso identificativo del salvataggio.
- Due commenti nel codice (`ContenitoreWgs`, `Rilevatore`) spiegavano l'indice
  WGS usando i tuoi GUID veri.
- Il `README` aveva una tabella con quegli stessi GUID.
- `avvia.bat` e `compila.bat` avevano un ultimo tentativo che cercava il JDK
  in una cartella del tuo disco (il progetto precedente): inutile per chi
  clona il repository, e rivelatore di come sono organizzate le tue cartelle.

**Fatto:** sostituiti con esempi evidenti (`0123456789ABCDEF…`, `<nome>`).
Spiegano la stessa cosa senza dire niente di nessuno.

### 2.5 Un residuo minore

- `index.html` aveva i pulsanti che puntavano a `github.com/Sbri0110/atlante`.
  Segnalato come errore e poi **verificato meglio**: l'account giusto è proprio
  `Sbri0110` (nome visualizzato "Sbri", creato il 30 agosto 2026), mentre
  l'omonimo `Sbri` è un account di terzi del 2014 con due librerie PHP. Il
  dubbio è nato da un'indagine superficiale: l'errore vero era il nome del
  repository, non l'account. Ricontrollato sul profilo GitHub, non a memoria.

### 2.6 In spedizione si leggeva la partita principale

**Il problema più importante di tutti, e si vedeva solo sui salvataggi altrui.**

Un salvataggio tiene due copie dello stato del giocatore: la partita
(`BaseContext`) e la spedizione (`ExpeditionContext`). `ActiveContext` dice
quale delle due è in corso. Chi gioca solo la partita non se ne accorge mai:
il difetto salta fuori solo aprendo un salvataggio che ha una spedizione in
corso — cioè il salvataggio di un altro, non il mio.

Diverse schede leggevano l'elenco delle cose dal contesto giusto, ma
costruivano il percorso degli slot con `BaseContext` **scritto a mano**. Il
risultato: in spedizione si vedevano i **nomi** delle cose della spedizione con
dentro gli **oggetti** della partita principale.

Misurato sul salvataggio di prova, forzando il contesto a spedizione: **9 navi
su 12** hanno contenuti diversi fra le due copie. La nave 0 ha 7 oggetti nella
partita e 16 nella spedizione, con due nomi diversi: sono due partite diverse
tenute nello stesso file, ed è normale che lo siano.

Corrette: navi, corvette, veicoli, multi-tool, i depositi della corvetta e i
percorsi degli elenchi (squadrone, fregate, compagni, insediamenti). Corrette
anche le sezioni che leggevano lo stato del giocatore dal ramo sbagliato:
statistiche, traguardi, stazioni.

### 2.7 La copia di sicurezza poteva far fallire un salvataggio

La copia prendeva il nome dal secondo (`...20260922-084431.bak`). Due
salvataggi nello stesso secondo — o due esecuzioni del collaudo — producevano
lo stesso nome, e copiare su un file che esiste già solleva un'eccezione: la
scrittura si sarebbe **interrotta**. Ora il nome prende un suffisso finché non
trova un posto libero.

È il caso meno probabile e proprio per questo il più fastidioso: capita quando
si salva due volte di fretta, cioè esattamente quando non si sta attenti.

### 2.8 Il metodo, per la prossima volta

Una ricerca a mano non basta: le tracce erano in immagini SVG, in script
`.bat` e nei commenti. Il controllo che le trova tutte è questo, e va rifatto
prima di ogni pubblicazione:

```bash
for f in $(git ls-files); do
  grep -lE "<nome utente>|<nome partita>|<GUID>" "$f" 2>/dev/null
done
```

Passando in rassegna **ogni file tracciato**, non solo il codice.

### 2.9 Le schermate non erano davvero rigenerabili

Il README dice che le immagini sono "la fotografia dell'applicazione vera", e
lo sono. Ma rigenerandole sono uscite **tutte diverse** da quelle pubblicate,
pur senza che l'interfaccia fosse cambiata. Due cause, entrambe di sostanza:

- **La data.** Il pannello mostra "Ultima modifica" leggendo l'ora del file, e
  il file di prova viene copiato a ogni esecuzione: ogni generazione produce
  un'immagine con una data diversa. Il confronto fra due versioni non poteva
  dire niente di utile. Ora lo strumento delle schermate **fissa la data**
  (14 agosto 2026, 21:04): nel programma resta quella vera del file, solo le
  immagini usano una data scelta una volta.
- **La prima schermata si faceva a mano.** `atlante-partita.png` non era
  generata da nessuno: era rimasta con la barra del titolo vecchia mentre le
  altre quindici erano già aggiornate. Ora la produce lo stesso comando delle
  altre.

Verificato dopo la correzione: due generazioni indipendenti danno **16 immagini
su 16 identiche byte per byte**. E le quindici già pubblicate risultano
riprodotte esattamente, tranne la barra del titolo — che è proprio la modifica
voluta.

Questa è la differenza fra un'immagine vera e un'immagine vera *e
riproducibile*: la seconda si può verificare.

### 2.10 `.gitignore` bloccava le immagini nuove

La regola `schermate/` (senza barra iniziale) prende anche `assets/schermate/`.
Le diciassette immagini già pubblicate sono tracciate, quindi il problema non
si vedeva: sarebbe saltato fuori alla **prima immagine nuova**, che sarebbe
rimasta fuori dal repository senza che niente lo segnalasse. Corretta in
`/schermate/`, che ancora il percorso alla radice.

Trovato proprio aggiungendo la schermata della finestra Informazioni.

---

## 3. Cosa è cambiato, in numeri

| | Prima | Dopo |
|---|---|---|
| File nel repository | 113 | **3.619** |
| Dimensione | 13 MB | **90 MB** (di cui 82 di icone) |
| Icone di gioco incluse | 0 | **3.518 su 3.518** |
| Dati privati tracciati | sì | **no** |
| Riferimenti personali nei file pubblicati | 12 | **0** |
| Schermate fedeli all'interfaccia | 1 su 15 | **16 su 16** |
| Schermate rigenerabili identiche | 0 su 15 | **16 su 16** |
| Prove automatiche che passano | 0 | **26 su 26** |
| Schede che seguono il contesto attivo | 30 su 42 | **42 su 42** |

### I commit

1. `1620a53` — Pulizia per la pubblicazione: via i dati privati dal repository
2. `683a022` — Collaudo: le prove che toccano il disco, e il README che le spiega
3. `d282e4c` — Schermate della documentazione rigenerate: c'era la Corvette
4. `d7a8087` — Script di avvio: via i percorsi del mio computer
5. `9917540` — Documentazione e commenti: via gli identificativi del salvataggio vero
6. `fcf34f7` — Rispetto del contesto attivo: navi, veicoli e multi-tool
7. `5dac788` — Collaudo ripetibile e copia di sicurezza senza collisioni
8. `edc6a0e` — Commento: misure senza il nome della nave usata come prova
9. `489516a` — Nome del repository, dichiarazione IA e avvertenze d'uso
10. `16f8f5c` — Schermate riproducibili, e la finestra Informazioni in vetrina
11. `5e7d529` — Le icone di gioco entrano nel repository: servono tutte

### Un incidente, e come è finito

Mentre preparavo il commit delle icone ho lanciato un `git stash -u` di troppo.
Su questa macchina il comando è morto con un errore di segmentazione a metà
strada e ha lasciato il repository in uno stato incoerente: la punta di `main`
cancellata, l'indice corrotto, 5 commit recenti (dal 6 al 10 dell'elenco qui
sopra) spariti dal deposito degli oggetti.

Ricostruito così:

- rimossi il `.git/index.lock` rimasto appeso e l'indice corrotto;
- ritrovata la punta valida più recente leggendo il reflog, che era intatto;
- riportato `main` su quel commit con `git update-ref`;
- ripristinato il `.gitignore` con le regole per i dati privati, che la
  versione vecchia non aveva;
- tolto dal tracciamento i file che erano tornati dentro (`.workbuddy-ai/`,
  `schermate/`, `riferimenti/`).

**I file di lavoro non si sono mai persi**: nessuno. Il danno era solo nella
cronologia, che non era ancora stata pubblicata da nessuna parte. I commit dal
6 al 10 sono stati rifatti con lo stesso contenuto.

**Lezione:** `git stash -u` sposta anche i file non tracciati, e in questo
ambiente git non è affidabile su quella strada. Non l'ho più usato: per mettere
da parte qualcosa bastano `git add` e un commit.

---

## 4. Uno strumento in più, per il futuro

`it.atlante.strumenti.Collaudo` — **26 prove, 0 fallite**.

Il comando `giro` esisteva già ma lavora **tutto in memoria**: non tocca mai
il disco. Le parti che invece ci scrivono erano coperte solo a mano. Il nuovo
strumento le mette alla prova: apertura, riconoscimento del formato,
salvataggio vero, copia di sicurezza, rilettura, una modifica a un valore e la
sua sopravvivenza, i casi storti (file qualunque, vuoto, di due byte, cartella,
inesistente) che devono dare un errore pulito e **mai un arresto improvviso**,
e infine il rispetto del contesto attivo su 42 percorsi.

La prova sul contesto l'ho verificata **rimettendo il difetto per un momento**:
le schede finite nel ramo sbagliato vengono segnalate e il risultato diventa
"26 prove, 1 fallita". Una prova che non fallisce mai quando il codice è rotto
non serve a niente, e questa fallisce.

Lavora su una **copia** del salvataggio: l'originale non viene mai toccato.
Esce con codice 1 se una prova fallisce, quindi si può mettere in una catena
automatica, e si può rilanciare quante volte si vuole.

```bash
java -cp "classi;lib/flatlaf.jar" it.atlante.strumenti.Collaudo <file> [cartella]
```

---

## 5. Prima di caricare: la lista

- [x] Compilazione pulita, nessun errore
- [x] Nessun dato privato tracciato (`git ls-files` controllato)
- [x] Nessun percorso di salvataggio personale nel codice
- [x] Nessun riferimento personale nei commenti o nella documentazione
- [x] Link al repository corretto (`github.com/Sbri0110/atlante-no-mans-sky-save-editor`)
- [x] Schermate fedeli all'interfaccia vera, e **rigenerabili identiche**
- [x] Le schede seguono il contesto attivo (42 percorsi su 42)
- [x] 26 prove automatiche su 26, ripetibili
- [x] Licenza e nota di attribuzione presenti (`LICENSE`, `NOTICE.md`)
- [x] Il programma funziona anche **senza** le icone di gioco (mostra le sigle)
- [x] Le 3.518 icone di gioco sono incluse: servono tutte, verificato che
      nessuna è orfana
- [x] Nome del repository scelto: **Atlante No Man Sky | Save Editor** —
      l'indirizzo diventa `github.com/Sbri0110/atlante-no-mans-sky-save-editor`
- [x] Dichiarazione dello sviluppo con IA (README, `index.html`, `NOTICE.md`,
      finestra *Informazioni*)
- [x] Avvertenze d'uso: moderazione, multigiocatore, rischio del ban
- [ ] **Creare il repository** su GitHub con quel nome
- [ ] Un `git push` verso il repository vuoto
- [ ] Dopo il push: *Settings → About*, mettere la descrizione e le etichette
      (`no-mans-sky`, `save-editor`, `java`, `swing`)

**Le tre caselle vuote sono bloccate su una cosa sola: l'autenticazione.**
`gh` è installato (2.98.0) ma non ha un accesso attivo, e sulla macchina non
c'è nessun token, nessuna credenziale salvata e nessuna chiave SSH. Creare il
repository e fare il push richiede un accesso tuo. Il resto è pronto.


---

## 6. Cosa non ho toccato, e perché

- **Il salvataggio vero.** Nessuna prova l'ha mai modificato: tutte lavorano
  su una copia.
- **Il vecchio editor sulla tua macchina.** Non l'ho sfiorato.

---

## 6-bis. Le icone di gioco: la decisione, e l'errore che avevo fatto

Questa merita di essere scritta per esteso, perché è il punto in cui ho
sbagliato **due volte**, in due direzioni opposte, e la seconda volta me ne
sono accorto solo perché Sbri ha contestato la scelta.

**Il primo errore** è nell'HEAD precedente: avevo escluso tutte le icone dal
repository, con la motivazione che sono arte di Hello Games. Sul piano legale
il ragionamento si sostiene. Sul piano pratico ha prodotto un danno: il
programma, a chi lo clonava, si presentava con le sigle al posto delle icone.
Il 78% dello schermo riempito di `SUBSTANCE-FUEL2`, `PRODUCT-ABAND_BARREL` e roba
simile.

**Il secondo errore** è nella prima versione di questo documento: contando le
icone citate dal catalogo ne avevo trovate 3.428 su 3.518, e ne avevo concluse
90 «orfane». Sbagliato. Le altre 98 sono usate **direttamente dal codice**, e
non passano dal catalogo perché descrivono tipi e non oggetti:

| Dove | Cosa |
|---|---|
| `Piloti.java` | i tipi di nave del pilota (10 icone) |
| `Icone.java` | le classi delle fregate e i loro tratti |
| `PannelloCompagno.java` | i biomi dei compagni |
| `Navigazione.java` | l'icona della corvetta |
| il resto | glifi dei portali, potenziamenti dei compagni, razze |

Contando anche quelle: **3.518 su 3.518 sono usate. Zero orfane.** Non c'era
niente da buttare.

**La decisione (di Sbri, 22 settembre 2026):** le icone entrano tutte nel
repository. Il repository passa da 8,8 a circa 90 MB — il file più grande è
310 KB, la media 24 KB, sotto qualunque soglia di GitHub.

**Come si verifica:** il clone pulito, compilato senza JDK portatile e senza
niente di preinstallato, produce 223 classi, passa 26 prove su 26, conserva i
dati byte per byte e genera **16 schermate identiche byte per byte** a quelle
pubblicate in `assets/schermate/`. Se le icone fossero rimaste fuori, queste
ultime non coinciderebbero: sarebbero piene di sigle.

`NOTICE.md` è stato riscritto: ora spiega quali sono le icone, perché restano e
che la questione è discutibile — chi non è d'accordo cancella la cartella e il
programma ripiega sugli identificatori testuali.

---

## 7. Nome del repository e dichiarazioni

### Il nome

Il repository si chiama **Atlante No Man Sky | Save Editor**. GitHub trasforma
gli spazi in trattini e converte le maiuscole: l'indirizzo diventa

```
github.com/Sbri0110/atlante-no-mans-sky-save-editor
```

Gli spazi di un nome di repository non sono ufficialmente ammessi da GitHub
(il campo li rifiuta). Dato il nome scelto, la forma consigliata è quindi
**`atlante-no-mans-sky-save-editor`**: la barra verticale, pur permessa, è
scomoda da digitare e da incollare nei link, e GitHub la mostra così com'è solo
nel titolo della pagina.

Il nome mostrato nel titolo può restare quello che hai scritto, spazi e barra
compresi: si imposta in *Settings → About* ed è distinto dall'indirizzo. Quindi
si può avere l'indirizzo comodo e il titolo esatto.

Aggiornati di conseguenza i due pulsanti di `index.html` (righe 95-96), il
titolo della finestra e la finestra *Informazioni*.

### Sviluppo con IA

Dichiarato in quattro posti, perché ognuno raggiunge un pubblico diverso:

| Dove | Per chi |
|---|---|
| README, in cima | chi arriva dal repository e legge prima di scaricare |
| `index.html` | chi arriva dal sito, con una nota dedicata |
| `NOTICE.md` | chi riusa il codice e ha bisogno dei dettagli legali |
| finestra *Informazioni* del programma | chi usa il programma e non ha mai visto il repository |

Non è una formalità: GitHub richiede di dichiarare l'uso di strumenti di IA
generativa per i contenuti pubblicati, e alcune giurisdizioni trattano le opere
generate con IA in modo diverso quanto alla protezione del diritto d'autore.
Chi riusa questo codice deve poterlo sapere.

### Avvertenze d'uso

Scritte una volta per esteso nel README e richiamate in breve altrove. Coprono
tre cose:

1. **La moderazione.** Un editor di salvataggi usato per riempire tutto subito
   toglie il divertimento al gioco stesso — è il danno più probabile e il più
   sottovalutato.
2. **Il multigiocatore.** L'editor è per la partita in singolo; portare valori
   alterati in sessioni condivise rovina la partita di altri.
3. **Il rischio dell'account.** Hello Games non ha approvato lo strumento e non
   esiste una garanzia ufficiale. Il README lo dice senza allarmismo e senza
   false rassicurazioni: il rischio è basso se il file resta coerente e si
   gioca in singolo, ma la decisione è di chi usa il programma.

---

## 8. La pubblicazione, e quello che è saltato fuori dopo

Il repository è online: **https://github.com/Sbri0110/atlante-no-mans-sky-save-editor**
(40 commit, `main`, licenza MIT riconosciuta, argomenti `no-mans-sky`,
`save-editor`, `java`, `swing`, `flatlaf`, `italiano`).

### 8.1 I dati privati nella cronologia, non nei file

I file correnti erano puliti da giorni. La cronologia no: **23 righe** con dati
che non devono stare in un repository pubblico, in quattro file, nelle versioni
*intermedie*. Git conserva ogni versione passata, quindi chi clonava le vedeva.

| File | Cosa esponeva |
|---|---|
| `assets/schermate/elenca.svg` | il percorso completo dei salvataggi e il nome della partita |
| `src/it/atlante/nms/ContenitoreWgs.java` | il nome della partita in un commento |
| `.workbuddy-ai/memory/MEMORY.md` (2 versioni) | il percorso del disco |
| `compila.bat`, `avvia.bat` | il percorso del vecchio editor |

**Due errori miei, da non ripetere.** Il primo elenco dei file da pulire l'avevo
*indovinato* (`compila.bat`, `avvia.bat`, `.gitignore`) invece che misurato, e
mancavano proprio i tre peggiori. E `Salvataggio.java`, che avevo indicato come
colpevole, non c'entrava niente: il nome della partita sta in
`ContenitoreWgs.java`. Infine avevo bollato `elenca.svg` come falso positivo del
grep: era vero positivo. La lezione è sempre la stessa, la terza volta in questo
progetto: **la lista si ricava dal contenuto, non da un nome di file.**

### 8.2 Come è stata ripulita

`git filter-branch` su questa macchina non parte (il sandbox gli impedisce di
creare `.git-rewrite`), e `filter-repo` non è installato. Ricostruire gli alberi
a mano con `ls-tree` + `mktree` + `commit-tree` funziona ma è lentissimo: oltre
otto minuti e non era finito.

La strada giusta è **`git fast-export | filtro | git fast-import`**: 1,4 secondi.
Il filtro legge i blocchi `blob` del flusso e ne riscrive il contenuto *in
loco*, correggendo la lunghezza; i marcatori restano gli stessi, quindi i commit
che li citano usano da soli la versione pulita.

**Risultato misurato:** 3.597 blob esaminati, 6 riscritti, 13 sostituzioni,
residui **da 23 a 0**. La punta è passata da `01326fb` a `d566918a`, ma l'albero
di HEAD è rimasto **`2a071d090dc7f7407e91e3484da530336e9d7fa5`** — lo stesso hash
di prima. Stesso hash significa stessi file con gli stessi byte: è una garanzia
crittografica, non una promessa. I 40 commit sono conservati.

### 8.3 Il pacchetto pronto all'uso

Il repository contiene i sorgenti, ma `avvia.bat` avvia dalle classi compilate e
`Atlante.jar` non è nel repository. Chi clonava doveva avere un **JDK** e
compilare: funziona, ma per usare il programma è una richiesta assurda.

Quindi c'è una **release** con `Atlante-1.0.zip` (89 MB):

- `Atlante.jar` con le 223 classi già compilate dentro
- `lib/flatlaf.jar` per il tema
- `risorse/` con il catalogo e le 3.518 icone
- `assets/marchio/` con il logo, che serve all'icona della finestra
- `avvia.bat`, riscritto per il pacchetto: avvia il jar, non compila

Serve **solo un JRE**. Il JDK portatile non è incluso: servirebbe a compilare, e
non c'è niente da compilare. Così il pacchetto resta sotto i 100 MB.

### 8.4 La verifica del pacchetto

Non ho dato per buona la struttura: ho estratto lo ZIP in una cartella separata
e l'ho provato come farebbe chi lo scarica.

- tutte le 16 schermate generate **dal pacchetto estratto** sono risultate
  **identiche byte per byte** a quelle pubblicate;
- il collaudo di scrittura passa: **26 prove su 26**;
- il giro completo conserva i dati.

Trovato così un difetto: mancava `assets/marchio/`, e il logo dell'applicazione
non compariva (in alto a sinistra). Aggiunto, e la schermata è tornata identica
a quella pubblicata. **Senza questo controllo il pacchetto sarebbe uscito senza
il marchio.**

### 8.5 Il README

La tabella diceva di poter aprire `Atlante.jar` come se fosse nel repository: non
c'è, e chi lo cercava perdeva tempo. Ora la sezione *Come si prova* ha due
strade separate: **usare** il programma (pacchetto dalla release, serve un JRE) o
**compilare** dai sorgenti (serve un JDK). Con l'avvertenza che il pacchetto va
estratto intero e `avvia.bat` lanciato da dentro la sua cartella, perché tema e
icone si caricano da lì.

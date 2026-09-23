<p align="center">
  <img src="assets/marchio/logo-512.png" width="150" alt="Marchio di Atlante">
</p>

<h1 align="center">Atlante</h1>

<p align="center">
  <strong>No Man's Sky | Save Editor</strong><br>
  Scritto da zero in Java, senza dipendenze esterne
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/licenza-MIT-E5484D.svg" alt="Licenza MIT"></a>
  <a href="https://adoptium.net/"><img src="https://img.shields.io/badge/Java-8%2B-FF7A7A.svg" alt="Java 8 o superiore"></a>
  <img src="https://img.shields.io/badge/dipendenze-nessuna-4ADE80.svg" alt="Nessuna dipendenza">
  <img src="https://img.shields.io/badge/catalogo-5587%20voci-FEBC2E.svg" alt="Catalogo di 5587 voci">
  <img src="https://img.shields.io/badge/sviluppato%20con-IA-8B5CF6.svg" alt="Sviluppato con l'intelligenza artificiale">
</p>

> [!WARNING]
> **Modifica i tuoi salvataggi a tuo rischio, e con moderazione.**
>
> - **Puoi rovinarti il gioco.** No Man's Sky è fatto di scoperta, attesa e
>   conquista. Darsi duemila milioni di unità o tutti gli oggetti appiattisce
>   l'esperienza fino a renderla noiosa, e quello non si annulla con un pulsante.
> - **Il multigiocatore è un'altra cosa.** Questo editor è per la partita **in
>   singolo**. Non usarlo per portare valori alterati in sessioni con altri.
> - **Hello Games non ha approvato questo strumento.** Non esiste una garanzia
>   ufficiale che l'uso di un editor non comporti conseguenze sul tuo account. Il
>   rischio è piccolo se il file resta coerente e giochi in singolo, ma **è
>   tuo**: valuta tu se vale la pena.
>
> Fai una copia del salvataggio prima di aprirlo la prima volta, e tieni il gioco
> chiuso mentre l'editor è aperto. L'editor fa da sé una copia di sicurezza prima
> di ogni scrittura, ma una copia tua non fa mai male.
>
> Le stesse cose, spiegate per esteso, sono in [documenti/AVVERTENZE.md](documenti/AVVERTENZE.md).

> [!NOTE]
> **Questo software è stato sviluppato con l'intelligenza artificiale.**
>
> Il codice, l'interfaccia, la documentazione e i commenti sono nati da sessioni
> di lavoro con un assistente di IA, sotto la direzione, la verifica e le
> correzioni di un autore umano. È dichiarato apertamente perché chi legge il
> codice ha il diritto di saperlo.
>
> **In bene:** il progetto è stato scritto in tempi molto più brevi del solito,
> con una copertura di prove superiore alla media — 26 prove automatiche, il
> giro completo che confronta i dati campo per campo, e una verifica che nessuna
> scrittura tocchi il file se il contenuto non torna identico.
>
> **In male:** l'IA sbaglia, e sbaglia in modo convincente. Un difetto può
> essersi nascosto dove le prove non arrivano. Buona parte di questo lavoro è
> consistita proprio nel cercare e correggere errori già scritti.
>
> **Perciò:** se trovi qualcosa di storto,
> [apri una segnalazione](https://github.com/Sbri0110/atlante-no-mans-sky-save-editor/issues).
> È il contributo più utile che puoi dare.

**Vuoi solo usare il programma?** C'è una pagina che lo presenta e il pacchetto
pronto da scaricare: <https://sbri0110.github.io/atlante-no-mans-sky-save-editor/>

---

Legge i salvataggi di No Man's Sky, ne decodifica le chiavi cifrate, li mostra in
un albero con **le icone e i nomi di gioco** e li riscrive **verificando che i
dati non cambino**.

Prima di ogni scrittura il contenuto viene ricompattato, riscompattato e
confrontato campo per campo con l'originale: se anche un solo valore differisce,
il file non viene toccato.

<p align="center">
  <img src="assets/schermate/atlante-partita.png" width="100%" alt="Finestra di Atlante: elenco dei salvataggi a sinistra, albero dei campi con le icone degli oggetti al centro, scheda dell'oggetto a destra">
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

**Misure sul salvataggio di prova** (Xbox Game Pass, settembre 2026): 1.743.369
byte compressi → 9.842.314 byte decompressi, 19 blocchi, durata di gioco
185 h 58 min.

---

## Come si prova

Ci sono due strade, e servono cose diverse.

### 1. Usare il programma

Scarica `Atlante-1.0.zip` dalla pagina delle
[release](https://github.com/Sbri0110/atlante-no-mans-sky-save-editor/releases),
estrailo dove vuoi e fai doppio clic su `avvia.bat`.

L'unica cosa che serve è **Java 8 o superiore** installato
([Adoptium](https://adoptium.net/) va benissimo): basta il JRE, non serve il JDK,
perché le classi sono già compilate dentro `Atlante.jar`. Il pacchetto si porta
dietro il tema, il catalogo e le 3.518 icone del gioco.

> Il pacchetto va estratto tutto intero e `avvia.bat` va lanciato da dentro la
> sua cartella: il tema e le icone si caricano da lì. Se sposti solo il `.jar`,
> il programma parte ma si presenta con gli identificatori al posto delle
> immagini.

### 2. Compilare dai sorgenti

Se hai clonato il repository, `Atlante.jar` non c'è: va costruito. Serve un
**JDK** (non basta il JRE: serve `javac`).

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
In alternativa `avvia.bat` fa tutto da solo: cerca Java, compila se le classi non
ci sono e apre la finestra.

### Cosa fa all'avvio

Trova i salvataggi sulla macchina e apre il primo. Se non ne trova, usa
**File → Apri file...**.

Le icone degli oggetti e dell'interfaccia sono incluse: le trovi già in
`risorse/icone/` appena cloni. Se le togli, il programma funziona lo stesso
mostrando gli identificatori al posto delle immagini — vedi
[NOTICE.md](NOTICE.md) per il perché di quella cartella.

### Dove sono i salvataggi

| Piattaforma | Percorso |
|---|---|
| Steam / GOG | `%USERPROFILE%\AppData\Roaming\HelloGames\NMS\` |
| Xbox Game Pass | `%LOCALAPPDATA%\Packages\HelloGames.NoMansSky_bs190hzg1sesy\SystemAppData\wgs\` |

---

## La dichiarazione, dentro il programma

Le avvertenze e la dichiarazione sull'IA non stanno solo qui: sono anche nella
finestra **Informazioni** del programma, perché chi lo apre può non aver mai
visto questo repository, e quelle sono proprio le cose da sapere prima di
toccare il salvataggio.

<p align="center">
  <img src="assets/schermate/atlante-informazioni.png" width="70%" alt="Finestra Informazioni di Atlante: catalogo e icone di gioco, la garanzia sul confronto campo per campo, la dichiarazione SVILUPPATO CON L'INTELLIGENZA ARTIFICIALE e le quattro avvertenze d'uso">
</p>

---

## Documentazione

| Documento | Cosa c'è dentro |
|---|---|
| [documenti/FORMATO.md](documenti/FORMATO.md) | il formato dei salvataggi, verificato sui byte: blocchi, contenitori Xbox, chiavi cifrate |
| [documenti/ARCHITETTURA.md](documenti/ARCHITETTURA.md) | com'è organizzato il codice e quattro decisioni di progetto |
| [documenti/COLLAUDO.md](documenti/COLLAUDO.md) | la riga di comando e il collaudo completo |
| [documenti/AVVERTENZE.md](documenti/AVVERTENZE.md) | uso responsabile, multigiocatore, rischio del ban, sviluppo con IA |
| [NOTICE.md](NOTICE.md) | librerie incluse, dati di gioco e marchi |

---

## Come contribuire

Il contributo più utile è **segnalare una discrepanza**: un salvataggio che non
si legge, un valore che cambia dopo il giro completo, una chiave ignota di cui
conosci il nome.

Il comando `giro` è lo strumento con cui verificare qualsiasi modifica alla
lettura o alla scrittura: **se non passa, la modifica è sbagliata.** Vedi
[documenti/COLLAUDO.md](documenti/COLLAUDO.md).

---

## Licenza

Codice rilasciato con licenza **MIT** — vedi [LICENSE](LICENSE).
Le librerie incluse, i dati di gioco e i marchi sono documentati in
[NOTICE.md](NOTICE.md).

No Man's Sky e tutti i nomi correlati sono marchi di Hello Games Limited.
Questo progetto non è affiliato, approvato o sponsorizzato da Hello Games.

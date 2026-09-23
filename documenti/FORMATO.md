# Il formato dei salvataggi di No Man's Sky

Documentazione verificata sui byte, non ricavata da supposizioni. Serve anche a
chi vuole scrivere un editor per conto proprio.

---

## 1. I due formati

No Man's Sky scrive i dati in due modi diversi, e vanno riconosciuti dal
**contenuto**, perché nulla nel nome del file li distingue:

| Formato | Dove | Com'è fatto |
|---|---|---|
| **Blocchi** | salvataggi principali | sequenza di blocchi, ognuno con intestazione di 16 byte e un blocco LZ4 |
| **LZ4 grezzo** | dati account | un solo blocco LZ4 senza intestazione |

---

## 2. Il flusso a blocchi

```
ripetuto fino a fine file:
  magic             4 byte    E5 A1 ED FE
  dimensioneComp    4 byte    uint32 little endian
  dimensioneDecomp  4 byte    uint32 little endian   (524288 = 512 KB)
  riservato         4 byte    sempre zero
  dati              dimensioneComp byte, blocco LZ4 grezzo
```

Non c'è intestazione di file: il primo blocco comincia subito. La dimensione
decomposta è sempre 524.288 byte, tranne nell'ultimo blocco.

---

## 3. Il contenitore Xbox Game Pass

```
<radice>/containers.index                   elenco dei contenitori
<radice>/<GUID 32 caratteri>/container.NN   descrittore, contiene il nome "data"
<radice>/<GUID>/<GUID>                      payload: flusso a blocchi
<radice>/<GUID>/<GUID>                      metadati: durata, nome del salvataggio
```

I due file con nome a GUID si distinguono dal contenuto, non dal nome: il
payload comincia con il magic del flusso a blocchi, i metadati con
un'intestazione di 20 byte.

Il suffisso di `container.NN` **non è fisso**: il gioco lo incrementa
(`container.23`, `container.27`, `container.31`). Va sempre cercato il più
recente presente su disco.

---

## 4. `containers.index`

Serializzazione .NET. Ogni stringa è preceduta da un `uint32` con il numero di
caratteri, seguita da UTF-16LE. Ogni voce ha questa forma:

```
<nome slot>   <identificativo fra virgolette>   <GUID binario di 16 byte>
```

Il nome dello slot **precede** il GUID, che compare in forma binaria con i primi
tre campi invertiti (ordine .NET). Verificato: nell'indice compare prima il
contenitore dei dati account, poi i due slot di salvataggio (Automatico e
Manuale), nell'ordine in cui sono elencati.

---

## 5. I metadati

```
0   uint32   costante 4225 nei salvataggi di prova
4   uint32   costante 1
8   uint32   durata di gioco in secondi
12  uint32   zero
16  uint32   dimensione del payload decompresso
20  ...      nome del salvataggio, testo a terminazione nulla
```

---

## 6. Le chiavi cifrate

Il gioco non scrive i nomi dei campi. Scrive terne di tre caratteri prodotte da
un hash del nome: `{"F2P":4737}` invece di `{"Version":4737}`.

La corrispondenza fra terna e nome è **un dato, non un algoritmo**: le terne sono
calcolabili, i nomi si scoprono leggendo i salvataggi.
`risorse/db/jsonmap.txt` contiene quell'elenco, una voce per riga nel formato
`terna<TAB>NomeProprieta`.

Le 57 chiavi ancora ignote sono proprietà introdotte da aggiornamenti recenti
del gioco. Restano visibili con la loro terna invece di essere perse.

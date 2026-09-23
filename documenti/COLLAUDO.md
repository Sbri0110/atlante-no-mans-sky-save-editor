# Riga di comando e collaudo

## Riga di comando

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

---

## Il collaudo completo

`giro` lavora tutto in memoria e non tocca mai il disco. Lo strumento
`Collaudo` copre le parti che invece ci scrivono: apre il file, lo salva,
controlla la copia di sicurezza, modifica un valore e verifica che sopravviva,
poi prova i casi storti (un file qualunque, un file vuoto, una cartella, un file
che non esiste) per accertarsi che diano un errore e non un arresto.

L'ultima prova riguarda il **contesto attivo**. Un salvataggio tiene due copie
dello stato del giocatore — la partita e la spedizione — e `ActiveContext` dice
quale delle due è in corso. La prova mette il contesto a spedizione, in memoria,
e controlla che il percorso di ogni scheda segua quel ramo: se una scheda
restasse attaccata alla partita principale, in spedizione si vedrebbero i nomi
di una partita con dentro gli oggetti dell'altra.

Lavora su una copia, in una cartella a parte: **il salvataggio vero non viene
mai toccato.** Esce con codice 1 se una prova fallisce, quindi si può usare in
una catena automatica. Si può rilanciare quante volte si vuole: la copia di
lavoro viene rifatta da capo ogni volta.

```bash
java -cp "classi;lib/flatlaf.jar" it.atlante.strumenti.Collaudo <file> [cartella]
```

---

## Verificare una modifica

Il comando `giro` è lo strumento con cui verificare qualsiasi modifica alla
lettura o alla scrittura: **se non passa, la modifica è sbagliata.**

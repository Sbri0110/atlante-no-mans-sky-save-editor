# Note su licenze e marchi

## Codice di questo progetto

Rilasciato con licenza MIT. Vedi [LICENSE](LICENSE).

### Sviluppo con l'intelligenza artificiale

Il codice, l'interfaccia, la documentazione e i commenti sono stati scritti con
l'aiuto di un assistente di **intelligenza artificiale**, sotto la direzione, la
verifica e le correzioni di un autore umano.

La dichiarazione è esplicita perché riguarda i diritti di chi legge e usa il
programma: le opere generate con il supporto di strumenti di IA possono avere un
regime diverso, per esempio, quanto alla protezione del diritto d'autore in
alcune giurisdizioni e alle condizioni d'uso di piattaforme come GitHub o gli
store. Chi riusa questo codice deve saperlo prima di farlo.

Per lo stesso motivo il progetto è accompagnato da **prove automatiche
verificabili** (`strumenti.Collaudo`, il comando `giro`): l'origine del codice
non è una garanzia di correttezza, e il modo per fidarsi è poterlo controllare ed
eseguire, non fidarsi della dichiarazione.

## Librerie incluse

### FlatLaf

`lib/flatlaf.jar` — libreria per il tema dell'interfaccia grafica.
Copyright di FormDev Software GmbH e altri contributori.
Rilasciata con **Apache License 2.0** (https://www.apache.org/licenses/LICENSE-2.0).

Il file viene distribuito insieme al programma perché l'interfaccia lo usa per
angoli arrotondati, temi scuri e componenti moderni. Il programma funziona anche
senza: in quel caso ripiega sul tema Nimbus incluso nella JVM e l'aspetto è più
spartano, ma tutte le funzioni restano disponibili.

## Dati di gioco

I file in `risorse/db/` descrivono il formato dei salvataggi: nomi delle
proprietà, elenco degli oggetti, dimensioni degli inventari. Sono **dati**
ricavati dai file di gioco, non codice, e servono a mostrare nomi leggibili al
posto degli identificatori interni.

### Le icone

`risorse/icone/` contiene **3.518 immagini PNG** (82 MB) estratte
dall'installazione di No Man's Sky: le icone degli oggetti e quelle
dell'interfaccia — i tipi di nave, le classi delle fregate, i biomi, i glifi dei
portali, i potenziamenti dei compagni. Servono tutte, nessuna esclusa: il
catalogo ne cita 3.428 e il codice ne usa altre 98 per gli elementi che non
descrivono un oggetto ma una categoria.

Sono **arte di gioco**, e restano di Hello Games. Qui sono incluse nella loro
forma originale, senza modifiche, per una ragione pratica: sono ciò che rende
l'interfaccia leggibile a colpo d'occhio. Un editor che mostra
`SUBSTANCE-FUEL2` al posto dell'icona del carbonio condensato è un editor che
non si capisce.

Chi non è d'accordo con questa scelta può cancellare la cartella: il programma
funziona lo stesso e ripiega sugli identificatori testuali. Chi la pensa
diversamente, e ritiene che l'inclusione non sia opportuna, può aprire una
segnalazione — la questione è aperta e si discute volentieri.

## Marchi

No Man's Sky e tutti i nomi correlati sono marchi di **Hello Games Limited**.

Questo progetto non è affiliato, approvato, sponsorizzato o in alcun modo
collegato a Hello Games. È uno strumento non ufficiale scritto da un giocatore
per modificare i propri salvataggi locali.

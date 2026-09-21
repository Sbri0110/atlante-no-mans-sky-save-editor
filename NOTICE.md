# Note su licenze e marchi

## Codice di questo progetto

Rilasciato con licenza MIT. Vedi [LICENSE](LICENSE).

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

Le icone degli oggetti **non sono incluse** in questo repository: sono arte di
gioco e pesano circa 73 MB. Il programma le usa se presenti in `risorse/icone/`
e funziona ugualmente quando mancano, mostrando gli identificatori.

## Marchi

No Man's Sky e tutti i nomi correlati sono marchi di **Hello Games Limited**.

Questo progetto non è affiliato, approvato, sponsorizzato o in alcun modo
collegato a Hello Games. È uno strumento non ufficiale scritto da un giocatore
per modificare i propri salvataggi locali.

package it.atlante.nms;

import it.atlante.io.FlussoBlocchi;
import it.atlante.io.Lz4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * I formati con cui No Man's Sky scrive i dati.
 *
 * Ne esistono due, e vanno riconosciuti dal contenuto perche' non c'e' nulla
 * nel nome del file che li distingua:
 *
 * <ul>
 *   <li><b>BLOCCHI</b> — il salvataggio principale. Sequenza di blocchi, ognuno
 *       con intestazione di 16 byte (magic {@code E5 A1 ED FE}, dimensione
 *       compressa, dimensione decompressa, riservato) seguita da un blocco LZ4.
 *       Le dimensioni decompresse sono da 512 KB, tranne l'ultimo blocco.</li>
 *   <li><b>LZ4_GREZZO</b> — i dati account. Un solo blocco LZ4 senza alcuna
 *       intestazione: il file comincia direttamente con il primo token LZ4.
 *       Misurato: 27.977 byte che si scompattano in 82.814 byte di JSON.</li>
 * </ul>
 *
 * Un terzo caso, JSON in chiaro, non e' mai stato osservato sui salvataggi reali
 * ma viene riconosciuto per sicurezza: se un file comincia con una graffa e'
 * gia' leggibile e non va toccato.
 */
public enum Formato {

    BLOCCHI("blocchi con intestazione"),
    LZ4_GREZZO("blocco LZ4 senza intestazione"),
    JSON_CHIARO("JSON in chiaro"),
    SCONOSCIUTO("non riconosciuto");

    private final String descrizione;

    Formato(String descrizione) {
        this.descrizione = descrizione;
    }

    public String descrizione() {
        return descrizione;
    }

    /** Riconosce il formato dai primi byte del file. */
    public static Formato rileva(byte[] dati) {
        if (dati == null || dati.length < 8) {
            return SCONOSCIUTO;
        }
        if (FlussoBlocchi.riconosci(dati)) {
            return BLOCCHI;
        }
        if (primoByteUtile(dati) == '{') {
            return JSON_CHIARO;
        }
        // Un blocco LZ4 grezzo non ha firma: l'unico modo di riconoscerlo e'
        // provare a scompattarlo e guardare se il risultato e' JSON.
        try {
            byte[] fuori = Lz4.scompatta(dati, 0);
            if (fuori.length > 0 && primoByteUtile(fuori) == '{') {
                return LZ4_GREZZO;
            }
        } catch (RuntimeException e) {
            // non e' un blocco valido: si prosegue
        }
        return SCONOSCIUTO;
    }

    private static byte primoByteUtile(byte[] dati) {
        for (int i = 0; i < Math.min(dati.length, 64); i++) {
            byte b = dati[i];
            if (b == ' ' || b == '\n' || b == '\r' || b == '\t' || b == 0) {
                continue;
            }
            return b;
        }
        return 0;
    }

    /** Scompatta il contenuto secondo il formato. */
    public byte[] leggi(byte[] dati, FlussoBlocchi.Resoconto resoconto) throws IOException {
        switch (this) {
            case BLOCCHI:
                return FlussoBlocchi.leggi(new java.io.ByteArrayInputStream(dati), resoconto);
            case LZ4_GREZZO: {
                byte[] fuori = Lz4.scompatta(dati, 0);
                if (resoconto != null) {
                    resoconto.blocchi = 1;
                }
                return fuori;
            }
            case JSON_CHIARO:
                if (resoconto != null) {
                    resoconto.blocchi = 0;
                }
                return dati;
            default:
                throw new IOException("formato non riconosciuto: impossibile leggere il contenuto");
        }
    }

    /** Ricompatta il contenuto secondo il formato. */
    public byte[] scrivi(byte[] dati, FlussoBlocchi.Resoconto resoconto) throws IOException {
        switch (this) {
            case BLOCCHI: {
                ByteArrayOutputStream out = new ByteArrayOutputStream(dati.length / 4);
                FlussoBlocchi.scrivi(out, dati, resoconto);
                return out.toByteArray();
            }
            case LZ4_GREZZO: {
                if (resoconto != null) {
                    resoconto.blocchi = 1;
                }
                return Lz4.compatta(dati);
            }
            case JSON_CHIARO:
                if (resoconto != null) {
                    resoconto.blocchi = 0;
                }
                return dati;
            default:
                throw new IOException("formato non riconosciuto: impossibile scrivere il contenuto");
        }
    }
}

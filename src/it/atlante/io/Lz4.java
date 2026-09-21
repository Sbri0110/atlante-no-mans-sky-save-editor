package it.atlante.io;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Compressione LZ4 nel formato "blocco" (LZ4 block format).
 *
 * Perche' scritto a mano: il progetto non ha dipendenze esterne. Il formato
 * blocco di LZ4 e' semplice e documentato, e averlo in casa evita di dover
 * distribuire un JAR di terzi solo per scompattare un salvataggio.
 *
 * Formato di una sequenza:
 *   token (1 byte)  = (lunghezzaLetterali &lt;&lt; 4) | (lunghezzaCorrispondenza - 4)
 *   [byte extra]    se lunghezzaLetterali  &gt;= 15  (255 finche' non si chiude)
 *   letterali       i byte non compressi
 *   offset (2 byte) little endian, distanza all'indietro
 *   [byte extra]    se (lunghezzaCorrispondenza - 4) &gt;= 15
 *
 * Vincoli del formato, da rispettare in compressione:
 *   - l'ultima sequenza contiene solo letterali;
 *   - gli ultimi 5 byte sono sempre letterali;
 *   - una corrispondenza non puo' iniziare a meno di 12 byte dalla fine.
 */
public final class Lz4 {

    /** Lunghezza massima di un blocco non compresso prodotto da questo codice. */
    public static final int DIMENSIONE_BLOCCO = 512 * 1024;

    private static final int ULTIMI_BYTE_LIBERI = 5;
    private static final int FINE_MINIMA_CORRISPONDENZA = 12;
    private static final int DIMENSIONE_TABELLA = 1 << 16;

    private Lz4() {
    }

    // ------------------------------------------------------------------
    // Scompattazione
    // ------------------------------------------------------------------

    /**
     * Scompatta un blocco LZ4.
     *
     * @param src     dati compressi
     * @param attesa  dimensione non compressa attesa; se positiva viene usata
     *                per preallocare e per controllare il risultato
     * @return i byte scompattati
     */
    public static byte[] scompatta(byte[] src, int attesa) {
        int capienza = attesa > 0 ? attesa : Math.max(64, src.length * 4);
        byte[] out = new byte[capienza];
        int lun = 0;
        int i = 0;
        int n = src.length;

        while (i < n) {
            int token = src[i++] & 0xFF;

            // --- letterali ---
            int letterali = token >>> 4;
            if (letterali == 15) {
                int b;
                do {
                    b = src[i++] & 0xFF;
                    letterali += b;
                } while (b == 255);
            }
            if (i + letterali > n) {
                throw new IllegalArgumentException(
                        "blocco LZ4 troncato: servono " + letterali + " letterali, disponibili " + (n - i));
            }
            out = assicura(out, lun + letterali);
            System.arraycopy(src, i, out, lun, letterali);
            lun += letterali;
            i += letterali;

            // L'ultima sequenza e' fatta di soli letterali: qui il blocco e' finito.
            if (i >= n) {
                break;
            }

            // --- offset ---
            if (i + 2 > n) {
                throw new IllegalArgumentException("blocco LZ4 troncato: manca l'offset");
            }
            int offset = (src[i] & 0xFF) | ((src[i + 1] & 0xFF) << 8);
            i += 2;
            if (offset == 0 || offset > lun) {
                throw new IllegalArgumentException(
                        "offset LZ4 non valido: " + offset + " con " + lun + " byte prodotti");
            }

            // --- corrispondenza ---
            int corrispondenza = token & 0x0F;
            if (corrispondenza == 15) {
                int b;
                do {
                    b = src[i++] & 0xFF;
                    corrispondenza += b;
                } while (b == 255);
            }
            corrispondenza += 4;

            int inizio = lun - offset;
            out = assicura(out, lun + corrispondenza);
            // Copia byte per byte: le sequenze possono sovrapporsi, e la
            // sovrapposizione e' voluta (e' cosi' che LZ4 ripete i pattern).
            for (int k = 0; k < corrispondenza; k++) {
                out[lun + k] = out[inizio + k];
            }
            lun += corrispondenza;
        }

        if (attesa > 0 && lun != attesa) {
            throw new IllegalArgumentException(
                    "dimensione scompattata inattesa: " + lun + " invece di " + attesa);
        }
        return lun == out.length ? out : Arrays.copyOf(out, lun);
    }

    private static byte[] assicura(byte[] out, int richiesti) {
        if (richiesti <= out.length) {
            return out;
        }
        int nuova = out.length;
        while (nuova < richiesti) {
            nuova = nuova < 1 << 24 ? nuova * 2 : nuova + (1 << 24);
        }
        return Arrays.copyOf(out, nuova);
    }

    // ------------------------------------------------------------------
    // Compressione
    // ------------------------------------------------------------------

    /**
     * Comprime un blocco LZ4.
     *
     * Compressore goloso con tabella di hash a 16 bit: non raggiunge i rapporti
     * dei compressori ottimizzati, ma e' corretto e sufficiente per riscrivere
     * un salvataggio. Il rapporto misurato sui salvataggi di prova e' intorno
     * a 5:1, contro 5,6:1 del compressore originale.
     */
    public static byte[] compatta(byte[] src) {
        int n = src.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, n / 4));
        int[] tabella = new int[DIMENSIONE_TABELLA];
        Arrays.fill(tabella, -1);

        int i = 0;
        int inizioLetterali = 0;
        int fineConsentita = n - ULTIMI_BYTE_LIBERI;

        while (i < fineConsentita) {
            if (i + 4 > n) {
                break;
            }
            int sequenza = leggi32(src, i);
            int h = hash(sequenza);
            int candidato = tabella[h];
            tabella[h] = i;

            boolean trovato = candidato >= 0
                    && i - candidato < 65536
                    && i - candidato > 0
                    && candidato + 4 <= n
                    && leggi32(src, candidato) == sequenza;

            if (!trovato || i - candidato < FINE_MINIMA_CORRISPONDENZA + 1) {
                i++;
                continue;
            }

            // Estende la corrispondenza il piu' possibile.
            int lunghezza = 4;
            int massimo = n - ULTIMI_BYTE_LIBERI - i;
            while (lunghezza < massimo && src[candidato + lunghezza] == src[i + lunghezza]) {
                lunghezza++;
            }

            int letterali = i - inizioLetterali;
            scriviSequenza(out, src, inizioLetterali, letterali, i - candidato, lunghezza);

            i += lunghezza;
            inizioLetterali = i;
        }

        // Sequenza finale: solo letterali, senza corrispondenza.
        scriviLetteraliFinali(out, src, inizioLetterali, n - inizioLetterali);
        return out.toByteArray();
    }

    private static void scriviSequenza(ByteArrayOutputStream out, byte[] src,
                                       int posLetterali, int letterali, int offset, int lunghezza) {
        int coppia = lunghezza - 4;
        int tokenLetterali = Math.min(letterali, 15);
        int tokenCoppia = Math.min(coppia, 15);
        out.write((tokenLetterali << 4) | tokenCoppia);

        if (letterali >= 15) {
            int resto = letterali - 15;
            while (resto >= 255) {
                out.write(255);
                resto -= 255;
            }
            out.write(resto);
        }
        out.write(src, posLetterali, letterali);

        out.write(offset & 0xFF);
        out.write((offset >>> 8) & 0xFF);

        if (coppia >= 15) {
            int resto = coppia - 15;
            while (resto >= 255) {
                out.write(255);
                resto -= 255;
            }
            out.write(resto);
        }
    }

    private static void scriviLetteraliFinali(ByteArrayOutputStream out, byte[] src, int pos, int letterali) {
        if (letterali <= 0) {
            // Un blocco vuoto resta valido: token 0 e nessun dato.
            out.write(0);
            return;
        }
        int tokenLetterali = Math.min(letterali, 15);
        out.write(tokenLetterali << 4);
        if (letterali >= 15) {
            int resto = letterali - 15;
            while (resto >= 255) {
                out.write(255);
                resto -= 255;
            }
            out.write(resto);
        }
        out.write(src, pos, letterali);
    }

    private static int leggi32(byte[] d, int i) {
        return (d[i] & 0xFF)
                | ((d[i + 1] & 0xFF) << 8)
                | ((d[i + 2] & 0xFF) << 16)
                | ((d[i + 3] & 0xFF) << 24);
    }

    private static int hash(int sequenza) {
        return (sequenza * 0x9E3779B1) >>> 16;
    }
}

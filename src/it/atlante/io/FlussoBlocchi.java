package it.atlante.io;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Il flusso a blocchi con cui No Man's Sky scrive i salvataggi.
 *
 * Struttura verificata sui salvataggi reali (Xbox Game Pass, settembre 2026):
 *
 * <pre>
 *   ripetuto fino a fine file:
 *     magic            4 byte   E5 A1 ED FE
 *     dimensioneComp   4 byte   uint32 little endian
 *     dimensioneDecomp 4 byte   uint32 little endian  (524288 = 512 KB)
 *     riservato        4 byte   sempre zero
 *     dati             dimensioneComp byte, blocco LZ4
 * </pre>
 *
 * Non c'e' un'intestazione di file: il primo blocco inizia subito. Il
 * salvataggio di prova e' lungo 1.743.369 byte e si scompatta in 9.842.314
 * byte distribuiti su 19 blocchi, tutti della dimensione dichiarata.
 *
 * La scrittura conserva la stessa suddivisione in blocchi da 512 KB: e' la
 * dimensione che il gioco usa, e non c'e' motivo di cambiarla.
 */
public final class FlussoBlocchi {

    public static final byte[] MAGIC = {(byte) 0xE5, (byte) 0xA1, (byte) 0xED, (byte) 0xFE};
    public static final int DIMENSIONE_BLOCCO = 512 * 1024;
    private static final int INTESTAZIONE_BLOCCO = 16;

    private FlussoBlocchi() {
    }

    /** Riconosce l'inizio di un flusso a blocchi guardando i primi byte. */
    public static boolean riconosci(byte[] testa) {
        if (testa == null || testa.length < 4) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (testa[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Legge un flusso a blocchi e restituisce il contenuto scompattato.
     *
     * @param in     flusso di ingresso
     * @param esito  se non nullo, riceve il resoconto dei blocchi letti
     */
    public static byte[] leggi(InputStream in, Resoconto esito) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 22);
        byte[] testa = new byte[INTESTAZIONE_BLOCCO];
        int numero = 0;

        while (true) {
            int letti = leggiPieno(in, testa, INTESTAZIONE_BLOCCO);
            if (letti == 0) {
                break;
            }
            if (letti < INTESTAZIONE_BLOCCO) {
                throw new EOFException("intestazione di blocco troncata dopo " + letti + " byte");
            }
            for (int i = 0; i < 4; i++) {
                if (testa[i] != MAGIC[i]) {
                    throw new IOException("magic inatteso nel blocco " + (numero + 1));
                }
            }
            int dimensioneComp = leggi32(testa, 4);
            int dimensioneDecomp = leggi32(testa, 8);
            if (dimensioneComp <= 0 || dimensioneComp > (1 << 26)) {
                throw new IOException("dimensione compressa implausibile: " + dimensioneComp);
            }

            byte[] compresso = new byte[dimensioneComp];
            if (leggiPieno(in, compresso, dimensioneComp) != dimensioneComp) {
                throw new EOFException("blocco " + (numero + 1) + " troncato");
            }
            byte[] decompresso = Lz4.scompatta(compresso, dimensioneDecomp);
            out.write(decompresso, 0, decompresso.length);
            numero++;
        }

        if (esito != null) {
            esito.blocchi = numero;
        }
        return out.toByteArray();
    }

    /**
     * Scrive un flusso a blocchi.
     *
     * @param out    flusso di uscita
     * @param dati   contenuto da comprimere
     * @param esito  se non nullo, riceve il resoconto dei blocchi scritti
     */
    public static void scrivi(OutputStream out, byte[] dati, Resoconto esito) throws IOException {
        int posizione = 0;
        int numero = 0;
        while (posizione < dati.length) {
            int quanti = Math.min(DIMENSIONE_BLOCCO, dati.length - posizione);
            byte[] pezzo = new byte[quanti];
            System.arraycopy(dati, posizione, pezzo, 0, quanti);
            byte[] compresso = Lz4.compatta(pezzo);

            byte[] testa = new byte[INTESTAZIONE_BLOCCO];
            System.arraycopy(MAGIC, 0, testa, 0, 4);
            scrivi32(testa, 4, compresso.length);
            scrivi32(testa, 8, quanti);
            scrivi32(testa, 12, 0);
            out.write(testa);
            out.write(compresso);

            posizione += quanti;
            numero++;
        }
        // Un contenuto vuoto produce comunque un blocco vuoto: un file di zero
        // byte non e' un salvataggio valido.
        if (numero == 0) {
            byte[] testa = new byte[INTESTAZIONE_BLOCCO];
            System.arraycopy(MAGIC, 0, testa, 0, 4);
            scrivi32(testa, 4, 1);
            scrivi32(testa, 8, 0);
            out.write(testa);
            out.write(0);
        }
        if (esito != null) {
            esito.blocchi = Math.max(1, numero);
        }
    }

    /** Resoconto di una lettura o scrittura, per i messaggi a video. */
    public static final class Resoconto {
        public int blocchi;

        @Override
        public String toString() {
            return blocchi + " blocchi";
        }
    }

    // ------------------------------------------------------------------

    private static int leggiPieno(InputStream in, byte[] buffer, int quanti) throws IOException {
        int totale = 0;
        while (totale < quanti) {
            int n = in.read(buffer, totale, quanti - totale);
            if (n < 0) {
                break;
            }
            totale += n;
        }
        return totale;
    }

    private static int leggi32(byte[] d, int i) {
        return (d[i] & 0xFF)
                | ((d[i + 1] & 0xFF) << 8)
                | ((d[i + 2] & 0xFF) << 16)
                | ((d[i + 3] & 0xFF) << 24);
    }

    private static void scrivi32(byte[] d, int i, int v) {
        d[i] = (byte) (v & 0xFF);
        d[i + 1] = (byte) ((v >>> 8) & 0xFF);
        d[i + 2] = (byte) ((v >>> 16) & 0xFF);
        d[i + 3] = (byte) ((v >>> 24) & 0xFF);
    }
}

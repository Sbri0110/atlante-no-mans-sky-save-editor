package it.atlante.nms;

import it.atlante.io.FlussoBlocchi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Lettura dei salvataggi nel formato Xbox Game Pass (WGS).
 *
 * Come e' fatto, verificato sul campo:
 *
 * <pre>
 *   &lt;radice&gt;/containers.index          elenco dei contenitori
 *   &lt;radice&gt;/&lt;GUID&gt;/container.NN        descrittore, contiene il nome "data"
 *   &lt;radice&gt;/&lt;GUID&gt;/&lt;GUID&gt;              payload: flusso a blocchi LZ4
 *   &lt;radice&gt;/&lt;GUID&gt;/&lt;GUID&gt;              metadati: durata di gioco, nome del salvataggio
 * </pre>
 *
 * Il suffisso di {@code container.NN} cambia nel tempo: il gioco lo incrementa
 * (container.27, container.31, container.23). Va sempre cercato il piu' recente
 * presente su disco, mai dato per fisso.
 *
 * I due file con nome a GUID si distinguono dal contenuto, non dal nome: il
 * payload comincia con il magic del flusso a blocchi, i metadati con una
 * intestazione di 20 byte.
 */
public final class ContenitoreWgs {

    private static final Charset UTF16 = Charset.forName("UTF-16LE");

    private ContenitoreWgs() {
    }

    /** Uno slot di salvataggio trovato su disco. */
    public static final class Slot {
        public final String guid;
        public final File cartella;
        public final File payload;
        public final File metadati;

        /** Nome dello slot, letto da containers.index ("Slot3Manual"). */
        public String nomeSlot = "";
        /** Nome del salvataggio dato dal giocatore ("La mia partita"). */
        public String nomeSalvataggio = "";
        /** Durata di gioco in secondi, dai metadati. */
        public long durataGioco = -1;
        /** Dimensione del payload scompattato, dai metadati. */
        public long dimensionePayload = -1;

        Slot(String guid, File cartella, File payload, File metadati) {
            this.guid = guid;
            this.cartella = cartella;
            this.payload = payload;
            this.metadati = metadati;
        }

        public String etichetta() {
            String s = nomeSlot.isEmpty() ? guid.substring(0, 8) : nomeSlot;
            if (!nomeSalvataggio.isEmpty()) {
                s = s + "  \"" + nomeSalvataggio + "\"";
            }
            return s;
        }

        @Override
        public String toString() {
            return etichetta();
        }
    }

    /**
     * Elenca gli slot presenti in una cartella WGS.
     *
     * @param radiceWgs la cartella che contiene containers.index
     */
    public static List<Slot> elenca(File radiceWgs) throws IOException {
        if (radiceWgs == null || !radiceWgs.isDirectory()) {
            throw new IOException("cartella WGS non trovata: " + radiceWgs);
        }
        File[] cartelle = radiceWgs.listFiles();
        if (cartelle == null) {
            return Collections.emptyList();
        }
        List<Slot> trovati = new ArrayList<Slot>();
        for (File c : cartelle) {
            if (!c.isDirectory() || c.getName().length() != 32) {
                continue;
            }
            Slot slot = leggiCartella(c);
            if (slot != null) {
                trovati.add(slot);
            }
        }

        // I nomi degli slot stanno in containers.index: si associano ai GUID.
        File indice = new File(radiceWgs, "containers.index");
        if (indice.isFile()) {
            associaNomiSlot(leggiFile(indice), trovati);
        }

        // Metadati: nome del salvataggio, durata, dimensione del payload.
        for (Slot s : trovati) {
            if (s.metadati != null) {
                leggiMetadati(s);
            }
        }
        Collections.sort(trovati, new java.util.Comparator<Slot>() {
            public int compare(Slot a, Slot b) {
                return a.guid.compareTo(b.guid);
            }
        });
        return trovati;
    }

    private static Slot leggiCartella(File cartella) throws IOException {
        File[] files = cartella.listFiles();
        if (files == null) {
            return null;
        }
        File payload = null;
        File metadati = null;
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            byte[] testa = leggiTesta(f, 8);
            if (FlussoBlocchi.riconosci(testa)) {
                payload = f;
            } else if (f.getName().length() == 32) {
                metadati = f;
            }
        }
        if (payload == null) {
            return null;
        }
        return new Slot(cartella.getName(), cartella, payload, metadati);
    }

    /** Legge e scompatta il payload di uno slot. */
    public static byte[] leggiPayload(Slot slot, FlussoBlocchi.Resoconto resoconto) throws IOException {
        InputStream in = new FileInputStream(slot.payload);
        try {
            return FlussoBlocchi.leggi(in, resoconto);
        } finally {
            in.close();
        }
    }

    // ------------------------------------------------------------------
    // Metadati
    // ------------------------------------------------------------------

    /**
     * I metadati hanno un'intestazione fissa di 20 byte:
     * <pre>
     *   0   uint32   (costante 4225 nei salvataggi di prova)
     *   4   uint32   (costante 1)
     *   8   uint32   durata di gioco in secondi
     *   12  uint32   zero
     *   16  uint32   dimensione del payload scompattato
     *   20  ...      nome del salvataggio, testo a terminazione nulla
     * </pre>
     */
    private static void leggiMetadati(Slot slot) throws IOException {
        byte[] d = leggiFile(slot.metadati);
        if (d.length < 24) {
            return;
        }
        slot.durataGioco = leggi32(d, 8);
        slot.dimensionePayload = leggi32(d, 16) & 0xFFFFFFFFL;
        int fine = 20;
        while (fine < d.length && d[fine] != 0) {
            fine++;
        }
        String nome = new String(d, 20, fine - 20, Charset.forName("UTF-8"));
        slot.nomeSalvataggio = nome.trim();
    }

    // ------------------------------------------------------------------
    // containers.index
    // ------------------------------------------------------------------

    /**
     * Estrae le stringhe con prefisso di lunghezza in un indice WGS.
     *
     * L'indice e' una serializzazione .NET: ogni stringa e' preceduta da un
     * uint32 con il numero di caratteri, seguita da UTF-16LE. Non conoscendo
     * lo schema completo, si estraggono tutte le stringhe plausibili in ordine
     * e si usano per etichettare i contenitori.
     */
    static List<String> stringheUtf16(byte[] d) {
        List<String> fuori = new ArrayList<String>();
        int i = 0;
        while (i + 6 <= d.length) {
            long lunghezza = leggi32(d, i) & 0xFFFFFFFFL;
            if (lunghezza >= 1 && lunghezza <= 400) {
                int byteStringa = (int) lunghezza * 2;
                if (i + 4 + byteStringa <= d.length) {
                    String s = new String(d, i + 4, byteStringa, UTF16);
                    if (plausibile(s)) {
                        fuori.add(s);
                        i += 4 + byteStringa;
                        continue;
                    }
                }
            }
            i++;
        }
        return fuori;
    }

    private static boolean plausibile(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 && c != '\t') {
                return false;
            }
        }
        return !s.trim().isEmpty();
    }

    /**
     * Associa a ogni contenitore il proprio nome di slot.
     *
     * Ogni voce dell'indice ha questa forma:
     * <pre>
     *   &lt;nome slot&gt;  &lt;identificativo fra virgolette&gt;  &lt;GUID binario 16 byte&gt;
     * </pre>
     * Il nome quindi <b>precede</b> il GUID. Per ogni contenitore si prende il
     * nome non ancora usato piu' vicino che sta prima del proprio GUID.
     *
     * Due tentativi sbagliati prima di arrivarci, entrambi utili:
     * <ol>
     *   <li>cercare il nome piu' vicino in valore assoluto: i tre contenitori
     *       finiscono per prendere tutti lo stesso nome;</li>
     *   <li>cercare il primo nome <em>dopo</em> il GUID: l'associazione slitta
     *       di una posizione e Slot3Auto diventa Slot3Manual.</li>
     * </ol>
     * Verificato sui salvataggi di prova: il contenitore dei dati account,
     * poi i due slot (Automatico e Manuale), nell'ordine in cui compaiono
     * nell'indice.
     */
    static void associaNomiSlot(byte[] indice, List<Slot> slot) {
        // Posizione nell'indice di ogni GUID, con l'indice dello slot.
        List<int[]> posizioniGuid = new ArrayList<int[]>();
        for (int i = 0; i < slot.size(); i++) {
            byte[] g = guidBinario(slot.get(i).guid);
            int p = -1;
            if (g != null) {
                p = indiceDi(indice, g);
                if (p < 0) {
                    // .NET memorizza i primi tre campi del GUID invertiti:
                    // si prova anche quella forma.
                    p = indiceDi(indice, invertiGuid(g));
                }
            }
            posizioniGuid.add(new int[]{p, i});
        }
        Collections.sort(posizioniGuid, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                return a[0] - b[0];
            }
        });

        // Nomi di slot con la loro posizione, in ordine crescente. Il terzo
        // elemento tiene traccia dell'uso: un nome vale per un solo contenitore.
        final List<Object[]> nomi = new ArrayList<Object[]>();
        for (String s : stringheUtf16(indice)) {
            if (!sembraNomeSlot(s)) {
                continue;
            }
            int p = posizioneStringa(indice, s);
            if (p >= 0) {
                nomi.add(new Object[]{Integer.valueOf(p), s, Boolean.FALSE});
            }
        }
        Collections.sort(nomi, new java.util.Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                return ((Integer) a[0]).intValue() - ((Integer) b[0]).intValue();
            }
        });

        for (int[] coppia : posizioniGuid) {
            int pos = coppia[0];
            if (pos < 0) {
                continue;
            }
            // Il nome piu' vicino che sta prima del GUID e non e' ancora usato.
            Object[] scelto = null;
            for (Object[] nome : nomi) {
                if (Boolean.TRUE.equals(nome[2])) {
                    continue;
                }
                if (((Integer) nome[0]).intValue() < pos) {
                    scelto = nome;
                }
            }
            if (scelto != null) {
                scelto[2] = Boolean.TRUE;
                slot.get(coppia[1]).nomeSlot = (String) scelto[1];
            }
        }
    }

    private static boolean sembraNomeSlot(String s) {
        if (s.startsWith("Slot")) {
            return true;
        }
        return s.equals("AccountData") || s.indexOf("Slot") >= 0;
    }

    private static int posizioneStringa(byte[] indice, String s) {
        byte[] cercato = s.getBytes(UTF16);
        return indiceDi(indice, cercato);
    }

    private static int indiceDi(byte[] pagliaio, byte[] ago) {
        if (ago.length == 0 || ago.length > pagliaio.length) {
            return -1;
        }
        int ultimo = pagliaio.length - ago.length;
        for (int i = 0; i <= ultimo; i++) {
            boolean uguale = true;
            for (int j = 0; j < ago.length; j++) {
                if (pagliaio[i + j] != ago[j]) {
                    uguale = false;
                    break;
                }
            }
            if (uguale) {
                return i;
            }
        }
        return -1;
    }

    /** Da "5FFAB16A8D3448AA846F7FB66207842E" ai 16 byte corrispondenti. */
    static byte[] guidBinario(String guid) {
        String pulito = guid.replace("-", "");
        if (pulito.length() != 32) {
            return null;
        }
        byte[] out = new byte[16];
        try {
            for (int i = 0; i < 16; i++) {
                out[i] = (byte) Integer.parseInt(pulito.substring(i * 2, i * 2 + 2), 16);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return out;
    }

    /** I primi tre campi di un GUID .NET sono memorizzati a byte invertiti. */
    static byte[] invertiGuid(byte[] g) {
        byte[] out = Arrays.copyOf(g, g.length);
        inverti(out, 0, 4);
        inverti(out, 4, 2);
        inverti(out, 6, 2);
        return out;
    }

    private static void inverti(byte[] d, int inizio, int quanti) {
        for (int i = 0; i < quanti / 2; i++) {
            byte t = d[inizio + i];
            d[inizio + i] = d[inizio + quanti - 1 - i];
            d[inizio + quanti - 1 - i] = t;
        }
    }

    // ------------------------------------------------------------------

    static byte[] leggiFile(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(f.length(), 1 << 22));
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static byte[] leggiTesta(File f, int quanti) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            byte[] b = new byte[quanti];
            int letti = 0;
            while (letti < quanti) {
                int n = in.read(b, letti, quanti - letti);
                if (n < 0) {
                    break;
                }
                letti += n;
            }
            return b;
        } finally {
            in.close();
        }
    }

    private static int leggi32(byte[] d, int i) {
        return (d[i] & 0xFF)
                | ((d[i + 1] & 0xFF) << 8)
                | ((d[i + 2] & 0xFF) << 16)
                | ((d[i + 3] & 0xFF) << 24);
    }
}

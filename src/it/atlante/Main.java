package it.atlante;

import it.atlante.io.FlussoBlocchi;
import it.atlante.json.Json;
import it.atlante.nms.ContenitoreWgs;
import it.atlante.nms.MappaChiavi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Atlante - programma da riga di comando.
 *
 * Comandi:
 * <pre>
 *   elenca &lt;cartella WGS&gt;              elenca gli slot di salvataggio trovati
 *   info &lt;file payload&gt;                riassunto di un salvataggio
 *   dump &lt;file payload&gt; [uscita.json]  scrive il salvataggio in JSON leggibile
 *   giro &lt;file payload&gt;                prova il giro completo: leggi, riscrivi, rileggi
 * </pre>
 *
 * La cartella WGS di Xbox Game Pass su Windows e':
 * <pre>
 *   %LOCALAPPDATA%\Packages\HelloGames.NoMansSky_bs190hzg1sesy\SystemAppData\wgs\
 * </pre>
 * e il contenitore e' la sottocartella con il nome a 32 caratteri esadecimali.
 */
public final class Main {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static void main(String[] args) {
        try {
            System.exit(esegui(args));
        } catch (Exception e) {
            System.err.println("errore: " + e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int esegui(String[] args) throws IOException {
        if (args.length == 0 || args[0].equals("--aiuto") || args[0].equals("-h")) {
            aiuto();
            return args.length == 0 ? 1 : 0;
        }
        String comando = args[0];

        if (comando.equals("elenca")) {
            if (args.length < 2) {
                System.err.println("serve la cartella WGS");
                return 1;
            }
            return elenca(new File(args[1]));
        }
        if (comando.equals("info")) {
            if (args.length < 2) {
                System.err.println("serve il file del salvataggio");
                return 1;
            }
            return info(new File(args[1]));
        }
        if (comando.equals("dump")) {
            if (args.length < 2) {
                System.err.println("serve il file del salvataggio");
                return 1;
            }
            File uscita = args.length >= 3 ? new File(args[2]) : null;
            return dump(new File(args[1]), uscita);
        }
        if (comando.equals("giro")) {
            if (args.length < 2) {
                System.err.println("serve il file del salvataggio");
                return 1;
            }
            return giro(new File(args[1]));
        }

        System.err.println("comando sconosciuto: " + comando);
        aiuto();
        return 1;
    }

    private static void aiuto() {
        System.out.println("Atlante - lettore di salvataggi di No Man's Sky");
        System.out.println();
        System.out.println("  elenca <cartella WGS>              elenca gli slot trovati");
        System.out.println("  info <file payload>                riassunto di un salvataggio");
        System.out.println("  dump <file payload> [uscita.json]  scrive il JSON leggibile");
        System.out.println("  giro <file payload>                prova lettura, riscrittura e rilettura");
    }

    // ------------------------------------------------------------------

    private static int elenca(File radice) throws IOException {
        List<ContenitoreWgs.Slot> slot = ContenitoreWgs.elenca(radice);
        System.out.println("cartella: " + radice.getAbsolutePath());
        System.out.println("slot trovati: " + slot.size());
        System.out.println();
        for (ContenitoreWgs.Slot s : slot) {
            System.out.println("  " + s.etichetta());
            System.out.println("      guid      : " + s.guid);
            System.out.println("      payload   : " + s.payload.getName()
                    + "  (" + mb(s.payload.length()) + " compressi)");
            if (s.dimensionePayload > 0) {
                System.out.println("      decompresso: " + mb(s.dimensionePayload));
            }
            if (s.durataGioco >= 0) {
                System.out.println("      durata    : " + durata(s.durataGioco));
            }
        }
        return 0;
    }

    private static int info(File file) throws IOException {
        if (!file.isFile()) {
            System.err.println("file non trovato: " + file);
            return 1;
        }
        FlussoBlocchi.Resoconto r = new FlussoBlocchi.Resoconto();
        byte[] dati;
        java.io.InputStream in = new java.io.FileInputStream(file);
        try {
            dati = FlussoBlocchi.leggi(in, r);
        } finally {
            in.close();
        }
        System.out.println("file        : " + file.getName());
        System.out.println("compresso   : " + mb(file.length()));
        System.out.println("decompresso : " + mb(dati.length) + "   (" + r + ")");
        System.out.println("rapporto    : " + String.format("%.2f", dati.length / (double) file.length()) + ":1");

        String testo = new String(dati, UTF8).replaceAll("\\u0000+$", "");
        Object albero = Json.leggi(testo);
        MappaChiavi mappa = caricaMappa();
        MappaChiavi.Esito esito = mappa.traduci(albero);
        System.out.println();
        System.out.println("chiavi      : " + esito);
        System.out.println();
        riassunto(albero);
        return 0;
    }

    private static int dump(File file, File uscita) throws IOException {
        if (!file.isFile()) {
            System.err.println("file non trovato: " + file);
            return 1;
        }
        FlussoBlocchi.Resoconto r = new FlussoBlocchi.Resoconto();
        byte[] dati;
        java.io.InputStream in = new java.io.FileInputStream(file);
        try {
            dati = FlussoBlocchi.leggi(in, r);
        } finally {
            in.close();
        }
        String testo = new String(dati, UTF8).replaceAll("\\u0000+$", "");
        Object albero = Json.leggi(testo);
        MappaChiavi mappa = caricaMappa();
        MappaChiavi.Esito esito = mappa.traduci(albero);

        String json = Json.scrivi(albero);
        if (uscita == null) {
            System.out.println(json);
        } else {
            Writer w = new OutputStreamWriter(new FileOutputStream(uscita), UTF8);
            try {
                w.write(json);
            } finally {
                w.close();
            }
            System.out.println("scritto: " + uscita.getAbsolutePath());
            System.out.println("  " + mb(json.getBytes(UTF8).length) + ", " + esito);
        }
        return 0;
    }

    /**
     * Il collaudo che conta: leggere un salvataggio, ricomprimerlo e rileggerlo,
     * verificando che l'albero dei dati non cambi.
     *
     * Non e' un confronto fra byte: la compressione non e' deterministica
     * rispetto all'originale, quindi i byte possono differire legittimamente.
     * Quello che deve restare identico e' il contenuto.
     */
    private static int giro(File file) throws IOException {
        if (!file.isFile()) {
            System.err.println("file non trovato: " + file);
            return 1;
        }
        System.out.println("=== GIRO COMPLETO SU " + file.getName() + " ===");

        FlussoBlocchi.Resoconto r1 = new FlussoBlocchi.Resoconto();
        java.io.InputStream in = new java.io.FileInputStream(file);
        byte[] originale;
        try {
            originale = FlussoBlocchi.leggi(in, r1);
        } finally {
            in.close();
        }
        System.out.println("  1. letto        : " + mb(originale.length) + " in " + r1);

        java.io.ByteArrayOutputStream riscritto = new java.io.ByteArrayOutputStream();
        FlussoBlocchi.Resoconto r2 = new FlussoBlocchi.Resoconto();
        FlussoBlocchi.scrivi(riscritto, originale, r2);
        byte[] compresso = riscritto.toByteArray();
        System.out.println("  2. riscritto    : " + mb(compresso.length) + " in " + r2
                + "  (rapporto " + String.format("%.2f", originale.length / (double) compresso.length) + ":1)");

        FlussoBlocchi.Resoconto r3 = new FlussoBlocchi.Resoconto();
        byte[] riletto = FlussoBlocchi.leggi(new java.io.ByteArrayInputStream(compresso), r3);
        System.out.println("  3. riletto      : " + mb(riletto.length) + " in " + r3);

        boolean identici = Arrays.equals(originale, riletto);
        System.out.println("  4. byte identici: " + (identici ? "si" : "NO"));

        String a = new String(originale, UTF8).replaceAll("\\u0000+$", "");
        String b = new String(riletto, UTF8).replaceAll("\\u0000+$", "");
        String differenza = Json.confronta(Json.leggi(a), Json.leggi(b), "");
        System.out.println("  5. dati identici: " + (differenza == null ? "si" : "NO -> " + differenza));
        System.out.println();
        System.out.println(differenza == null
                ? "ESITO: il giro completo conserva i dati."
                : "ESITO: il giro completo ALTERA i dati.");
        return differenza == null ? 0 : 5;
    }

    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void riassunto(Object albero) {
        if (!(albero instanceof java.util.Map)) {
            System.out.println("  (non e' un oggetto JSON)");
            return;
        }
        java.util.Map<String, Object> m = (java.util.Map<String, Object>) albero;
        for (java.util.Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof java.util.Map) {
                System.out.println("  " + e.getKey() + "   [oggetto, " + ((java.util.Map<?, ?>) v).size() + " campi]");
            } else if (v instanceof java.util.List) {
                System.out.println("  " + e.getKey() + "   [lista, " + ((java.util.List<?>) v).size() + "]");
            } else {
                System.out.println("  " + e.getKey() + " = " + v);
            }
        }
    }

    private static MappaChiavi caricaMappa() throws IOException {
        File[] candidati = {
                new File("risorse/db/jsonmap.txt"),
                new File("risorse\\db\\jsonmap.txt"),
                new File("..", "risorse/db/jsonmap.txt"),
        };
        for (File f : candidati) {
            if (f.isFile()) {
                return MappaChiavi.carica(f.toPath());
            }
        }
        throw new IOException("risorse/db/jsonmap.txt non trovato. "
                + "Eseguire il programma dalla cartella del progetto.");
    }

    private static String mb(long byte_) {
        if (byte_ < 1024) {
            return byte_ + " B";
        }
        if (byte_ < 1024 * 1024) {
            return String.format("%.1f KB", byte_ / 1024.0);
        }
        return String.format("%.1f MB", byte_ / 1048576.0);
    }

    private static String durata(long secondi) {
        long ore = secondi / 3600;
        long minuti = (secondi % 3600) / 60;
        return String.format("%d h %02d min", ore, minuti);
    }
}

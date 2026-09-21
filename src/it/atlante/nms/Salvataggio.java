package it.atlante.nms;

import it.atlante.io.FlussoBlocchi;
import it.atlante.json.Json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Un salvataggio aperto: il contenuto, il formato, e le regole per riscriverlo
 * senza rovinarlo.
 *
 * Tre garanzie prima di toccare il file su disco:
 *
 * <ol>
 *   <li><b>Verifica del giro completo.</b> Prima di scrivere, il contenuto viene
 *       ricompattato e riscompattato e confrontato campo per campo con
 *       l'originale. Se anche un solo valore cambia, la scrittura non avviene.</li>
 *   <li><b>Copia di sicurezza.</b> Il file esistente viene copiato accanto a se'
 *       stesso con data e ora nel nome, prima di essere sostituito.</li>
 *   <li><b>Scrittura atomica.</b> Il nuovo contenuto va prima in un file
 *       temporaneo, che sostituisce l'originale solo quando e' completo. Un
 *       errore a meta' scrittura non lascia un salvataggio rotto.</li>
 * </ol>
 *
 * La verifica del punto 1 e' la ragione per cui questo editor non ha mai
 * bisogno di chiedere "sei sicuro?": se il contenuto non sopravvive al giro
 * completo, il programma si ferma da solo.
 */
public final class Salvataggio {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final File file;
    private final MappaChiavi mappa;
    private final Formato formato;
    private final Object albero;

    private boolean modificato;
    private long dimensioneCompressa;
    private long dimensioneDecompressa;
    private int blocchi;

    private Salvataggio(File file, MappaChiavi mappa, Formato formato, Object albero) {
        this.file = file;
        this.mappa = mappa;
        this.formato = formato;
        this.albero = albero;
    }

    /** Apre un salvataggio da disco. */
    public static Salvataggio apri(File file, MappaChiavi mappa) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("file non trovato: " + file);
        }
        byte[] grezzo = Files.readAllBytes(file.toPath());
        Formato formato = Formato.rileva(grezzo);
        if (formato == Formato.SCONOSCIUTO) {
            throw new IOException("formato non riconosciuto: " + file.getName());
        }
        FlussoBlocchi.Resoconto r = new FlussoBlocchi.Resoconto();
        byte[] decompresso = formato.leggi(grezzo, r);
        String testo = new String(decompresso, UTF8).replaceAll("\\u0000+$", "");
        Object albero = Json.leggi(testo);
        mappa.traduci(albero);

        Salvataggio s = new Salvataggio(file, mappa, formato, albero);
        s.dimensioneCompressa = grezzo.length;
        s.dimensioneDecompressa = decompresso.length;
        s.blocchi = r.blocchi;
        return s;
    }

    // ------------------------------------------------------------------

    public File file() {
        return file;
    }

    public String nome() {
        return file.getName();
    }

    public Formato formato() {
        return formato;
    }

    public Object albero() {
        return albero;
    }

    public boolean modificato() {
        return modificato;
    }

    public void segnaModificato() {
        modificato = true;
    }

    public long dimensioneCompressa() {
        return dimensioneCompressa;
    }

    public long dimensioneDecompressa() {
        return dimensioneDecompressa;
    }

    public int blocchi() {
        return blocchi;
    }

    /** Riassunto per la barra di stato. */
    public String riepilogo() {
        return String.format("%s · %s · %s compressi, %s decompressi, %d blocchi",
                file.getName(),
                formato.descrizione(),
                mb(dimensioneCompressa),
                mb(dimensioneDecompressa),
                blocchi);
    }

    // ------------------------------------------------------------------
    // Scrittura
    // ------------------------------------------------------------------

    /**
     * Verifica che il contenuto sopravviva a un giro completo di compressione.
     *
     * @return null se tutto coincide, altrimenti la descrizione della prima
     *         differenza trovata
     */
    public String verifica() {
        try {
            byte[] testo = testoDelSalvataggio();
            FlussoBlocchi.Resoconto r1 = new FlussoBlocchi.Resoconto();
            byte[] compresso = formato.scrivi(testo, r1);
            FlussoBlocchi.Resoconto r2 = new FlussoBlocchi.Resoconto();
            byte[] riletto = formato.leggi(compresso, r2);
            Object secondo = Json.leggi(new String(riletto, UTF8).replaceAll("\\u0000+$", ""));
            mappa.traduci(secondo);
            return Json.confronta(albero, secondo, "");
        } catch (Exception e) {
            return "verifica non eseguibile: " + e;
        }
    }

    private byte[] testoDelSalvataggio() {
        // Le chiavi tornano nella forma cifrata prima di uscire: il gioco
        // riconosce solo quella.
        mappa.cifra(albero);
        String json = Json.scrivi(albero);
        mappa.traduci(albero);
        return json.getBytes(UTF8);
    }

    /**
     * Scrive il salvataggio su disco.
     *
     * @param cartellaBackup dove mettere la copia di sicurezza; se null la copia
     *                       va accanto al file originale
     * @return il percorso della copia di sicurezza, o null se non creata
     * @throws IOException se la verifica fallisce o la scrittura non riesce
     */
    public File salva(File cartellaBackup) throws IOException {
        String problema = verifica();
        if (problema != null) {
            throw new IOException("scrittura annullata: il contenuto non sopravvive al giro completo.\n"
                    + "Differenza trovata: " + problema);
        }

        byte[] testo = testoDelSalvataggio();
        FlussoBlocchi.Resoconto r = new FlussoBlocchi.Resoconto();
        byte[] compresso = formato.scrivi(testo, r);

        File backup = creaBackup(cartellaBackup);

        File temporaneo = new File(file.getParentFile(), file.getName() + ".nuovo");
        OutputStream out = new FileOutputStream(temporaneo);
        try {
            out.write(compresso);
            out.flush();
        } finally {
            out.close();
        }
        // Sostituzione atomica: o il file nuovo e' completo, o resta il vecchio.
        if (!temporaneo.renameTo(file)) {
            Files.deleteIfExists(temporaneo.toPath());
            throw new IOException("impossibile sostituire il file: " + file);
        }

        dimensioneCompressa = compresso.length;
        dimensioneDecompressa = testo.length;
        blocchi = r.blocchi;
        modificato = false;
        return backup;
    }

    /** Scrive il contenuto in un altro file, senza copia di sicurezza. */
    public void salvaCome(File destinazione) throws IOException {
        String problema = verifica();
        if (problema != null) {
            throw new IOException("scrittura annullata: " + problema);
        }
        byte[] testo = testoDelSalvataggio();
        FlussoBlocchi.Resoconto r = new FlussoBlocchi.Resoconto();
        Files.write(destinazione.toPath(), formato.scrivi(testo, r));
        blocchi = r.blocchi;
        modificato = false;
    }

    private File creaBackup(File cartella) throws IOException {
        if (!file.isFile()) {
            return null;
        }
        String marca = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File dove = cartella != null ? cartella : file.getParentFile();
        if (dove != null && !dove.isDirectory()) {
            dove.mkdirs();
        }
        File copia = new File(dove, file.getName() + "." + marca + ".bak");
        Files.copy(file.toPath(), copia.toPath());
        return copia;
    }

    /** Formatta una dimensione in byte con l'unita' piu' leggibile. */
    public static String mb(long byte_) {
        if (byte_ < 1024) {
            return byte_ + " B";
        }
        if (byte_ < 1024 * 1024) {
            return String.format("%.1f KB", byte_ / 1024.0);
        }
        return String.format("%.1f MB", byte_ / 1048576.0);
    }
}

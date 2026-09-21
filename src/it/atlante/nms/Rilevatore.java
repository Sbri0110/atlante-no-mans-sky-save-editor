package it.atlante.nms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Trova i salvataggi presenti sulla macchina.
 *
 * Copre le piattaforme di cui si conosce il percorso:
 *
 * <ul>
 *   <li><b>Xbox Game Pass</b> — contenitori WGS con {@code containers.index},
 *       piu' i dati account in {@code SystemAppData/xgs};</li>
 *   <li><b>Steam / GOG</b> — file {@code save*.hg} e {@code accountdata.hg}
 *       nella cartella di roaming del gioco;</li>
 *   <li><b>PS4 e Switch</b> — le cartelle di esportazione, quando presenti.</li>
 * </ul>
 *
 * La ricerca non solleva eccezioni se una piattaforma manca: su una macchina
 * senza Xbox Game Pass semplicemente non trova nulla li'.
 */
public final class Rilevatore {

    /** Un salvataggio trovato. */
    public static final class Voce {
        public final String piattaforma;
        public final String etichetta;
        public final File file;
        public final Formato formato;

        Voce(String piattaforma, String etichetta, File file, Formato formato) {
            this.piattaforma = piattaforma;
            this.etichetta = etichetta;
            this.file = file;
            this.formato = formato;
        }

        public String descrizione() {
            return etichetta + "   ·   " + piattaforma + "   ·   "
                    + Salvataggio.mb(file.length());
        }

        @Override
        public String toString() {
            return etichetta;
        }
    }

    private Rilevatore() {
    }

    /** Cerca su tutte le piattaforme conosciute. */
    public static List<Voce> cercaTutti() {
        List<Voce> trovate = new ArrayList<Voce>();
        cercaWgs(trovate);
        cercaSteam(trovate);
        cercaAccountXgs(trovate);
        return trovate;
    }

    // ------------------------------------------------------------------

    /** Cartella radice dei contenitori Xbox Game Pass, o null se assente. */
    public static File cartellaWgs() {
        File pacchetti = new File(cartellaLocale(), "Packages");
        File[] cartelle = pacchetti.listFiles();
        if (cartelle == null) {
            return null;
        }
        for (File c : cartelle) {
            if (c.isDirectory() && c.getName().startsWith("HelloGames.NoMansSky")) {
                File wgs = new File(c, "SystemAppData" + File.separator + "wgs");
                if (wgs.isDirectory()) {
                    return wgs;
                }
            }
        }
        return null;
    }

    /** Cartella dei salvataggi Steam e GOG, o null se assente. */
    public static File cartellaSteam() {
        File nms = new File(cartellaRoaming(), "HelloGames" + File.separator + "NMS");
        return nms.isDirectory() ? nms : null;
    }

    /**
     * Cerca i contenitori di Xbox Game Pass.
     *
     * La struttura ha <b>due</b> livelli sotto {@code wgs}: prima una cartella
     * per utente (nome lungo, con underscore), poi i contenitori veri e propri
     * (nome di 32 caratteri esadecimali).
     *
     * <pre>
     *   wgs/000901FF2F07BBDE_29070100B936489ABCE8B9AF3980429C/   utente
     *       containers.index
     *       18C4F9F24DEB4BDF965C25D080DAF634/                    contenitore
     *       6D75D8AC158B45E099788DE4B593BD8B/
     * </pre>
     *
     * Il primo tentativo cercava i contenitori direttamente sotto {@code wgs} e
     * non ne trovava nessuno: l'unica cosa che il rilevatore riusciva a leggere
     * erano i dati account, e il programma apriva sempre quelli.
     */
    private static void cercaWgs(List<Voce> dentro) {
        File radice = cartellaWgs();
        if (radice == null) {
            return;
        }
        File[] utenti = radice.listFiles();
        if (utenti == null) {
            return;
        }
        // Nomi di riserva, letti dalla copia con nomi leggibili.
        Map<Long, String> nomiXgs = nomiDaXgs();
        for (File utente : utenti) {
            if (!utente.isDirectory()) {
                continue;
            }
            // Si accetta solo una cartella che contenga davvero dei contenitori.
            File[] possibili = utente.listFiles();
            if (possibili == null || !contieneContenitori(possibili)) {
                continue;
            }
            List<ContenitoreWgs.Slot> slot;
            try {
                slot = ContenitoreWgs.elenca(utente);
            } catch (IOException e) {
                continue;
            }
            for (ContenitoreWgs.Slot s : slot) {
                String nome = s.nomeSlot;
                if (nome.isEmpty() && !nomiXgs.isEmpty()) {
                    // L'indice non ha dato il nome: si prova con la cartella xgs.
                    String daXgs = nomiXgs.get(Long.valueOf(s.payload.length()));
                    if (daXgs != null) {
                        nome = daXgs;
                    }
                }
                String etichetta = nome.isEmpty() ? s.guid.substring(0, 8) : nome;
                if (!s.nomeSalvataggio.isEmpty()) {
                    etichetta = etichetta + " — " + s.nomeSalvataggio;
                }
                dentro.add(new Voce("Xbox Game Pass", etichetta, s.payload, Formato.BLOCCHI));
            }
        }
    }

    /**
     * Nomi degli slot letti dalla cartella {@code xgs}, indicizzati per dimensione.
     *
     * Accanto a {@code wgs} c'e' una seconda copia degli stessi salvataggi, con
     * <b>nomi di cartella leggibili</b> invece che GUID:
     *
     * <pre>
     *   xgs/&lt;utente&gt;/Slot3Auto/data       1.743.369 byte
     *   xgs/&lt;utente&gt;/Slot3Manual/data     1.743.341 byte
     *   xgs/&lt;utente&gt;/AccountData/data        27.977 byte
     * </pre>
     *
     * I file sono <b>identici byte per byte</b> a quelli dentro {@code wgs}:
     * verificato con {@code cmp}. Non si scrive li': il gioco aggiorna
     * {@code containers.index} dentro {@code wgs}, quindi quella e' la copia
     * autorevole. Serve solo a recuperare il nome di uno slot quando l'indice
     * non si riesce a leggere, associandolo per dimensione del file.
     */
    private static Map<Long, String> nomiDaXgs() {
        Map<Long, String> perDimensione = new java.util.HashMap<Long, String>();
        File wgs = cartellaWgs();
        if (wgs == null) {
            return perDimensione;
        }
        File systemAppData = wgs.getParentFile();
        if (systemAppData == null) {
            return perDimensione;
        }
        File[] utenti = new File(systemAppData, "xgs").listFiles();
        if (utenti == null) {
            return perDimensione;
        }
        for (File utente : utenti) {
            File[] slot = utente.listFiles();
            if (slot == null) {
                continue;
            }
            for (File cartella : slot) {
                if (!cartella.isDirectory()) {
                    continue;
                }
                File dati = new File(cartella, "data");
                if (dati.isFile()) {
                    perDimensione.put(Long.valueOf(dati.length()), cartella.getName());
                }
            }
        }
        return perDimensione;
    }

    private static boolean contieneContenitori(File[] voci) {
        for (File f : voci) {
            if (f.isDirectory() && f.getName().length() == 32) {
                return true;
            }
        }
        return false;
    }

    private static void cercaSteam(List<Voce> dentro) {
        File radice = cartellaSteam();
        if (radice == null) {
            return;
        }
        File[] cartelle = radice.listFiles();
        if (cartelle == null) {
            return;
        }
        for (File c : cartelle) {
            if (!c.isDirectory()) {
                continue;
            }
            String utente = c.getName();
            for (int slot = 0; slot <= 15; slot++) {
                for (String suffisso : new String[]{"", "Auto", "Manual"}) {
                    File f = new File(c, "save" + slot + suffisso + ".hg");
                    if (f.isFile()) {
                        dentro.add(new Voce("Steam / GOG",
                                utente + " — Slot" + slot + suffisso, f, Formato.BLOCCHI));
                    }
                }
            }
            File account = new File(c, "accountdata.hg");
            if (account.isFile()) {
                dentro.add(new Voce("Steam / GOG", utente + " — dati account",
                        account, Formato.LZ4_GREZZO));
            }
        }
    }

    private static void cercaAccountXgs(List<Voce> dentro) {
        File wgs = cartellaWgs();
        if (wgs == null) {
            return;
        }
        File systemAppData = wgs.getParentFile();
        if (systemAppData == null) {
            return;
        }
        File xgs = new File(systemAppData, "xgs");
        File[] utenti = xgs.listFiles();
        if (utenti == null) {
            return;
        }
        for (File u : utenti) {
            File dati = new File(u, "AccountData" + File.separator + "data");
            if (dati.isFile()) {
                dentro.add(new Voce("Xbox Game Pass", "Dati account", dati, Formato.LZ4_GREZZO));
            }
        }
    }

    // ------------------------------------------------------------------

    private static String cartellaLocale() {
        String v = System.getenv("LOCALAPPDATA");
        return v != null ? v : System.getProperty("user.home") + File.separator + "AppData"
                + File.separator + "Local";
    }

    private static String cartellaRoaming() {
        String v = System.getenv("APPDATA");
        return v != null ? v : System.getProperty("user.home") + File.separator + "AppData"
                + File.separator + "Roaming";
    }

    /** Vero se il formato del file e' riconosciuto, senza aprirlo del tutto. */
    public static boolean riconosciuto(File f) {
        try {
            byte[] testa = new byte[64];
            java.io.InputStream in = new java.io.FileInputStream(f);
            try {
                int letti = in.read(testa);
                if (letti <= 0) {
                    return false;
                }
                byte[] esatto = new byte[letti];
                System.arraycopy(testa, 0, esatto, 0, letti);
                return Formato.rileva(esatto) != Formato.SCONOSCIUTO;
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static List<Voce> vuoto() {
        return Collections.emptyList();
    }
}

package it.atlante.nms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private static void cercaWgs(List<Voce> dentro) {
        File radice = cartellaWgs();
        if (radice == null) {
            return;
        }
        File[] contenitori = radice.listFiles();
        if (contenitori == null) {
            return;
        }
        for (File contenitore : contenitori) {
            if (!contenitore.isDirectory() || contenitore.getName().length() != 32) {
                continue;
            }
            try {
                List<ContenitoreWgs.Slot> slot = ContenitoreWgs.elenca(contenitore.getParentFile());
                for (ContenitoreWgs.Slot s : slot) {
                    if (!s.cartella.equals(contenitore)) {
                        continue;
                    }
                    String etichetta = s.nomeSlot.isEmpty() ? s.guid.substring(0, 8) : s.nomeSlot;
                    if (!s.nomeSalvataggio.isEmpty()) {
                        etichetta = etichetta + " — " + s.nomeSalvataggio;
                    }
                    dentro.add(new Voce("Xbox Game Pass", etichetta, s.payload, Formato.BLOCCHI));
                }
            } catch (IOException e) {
                // contenitore illeggibile: si passa al prossimo
            }
        }
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

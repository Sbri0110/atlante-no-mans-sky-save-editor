package it.atlante.finestra;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * I premi delle spedizioni, raggruppati per spedizione.
 *
 * L'elenco sta in {@code risorse/db/rewards.xml} ed e' un elenco piatto di 293
 * voci, in ordine di spedizione dalla piu' recente. Il raggruppamento si ricava
 * dagli identificativi: le voci che iniziano per {@code EXPD_} portano il numero
 * della spedizione, le altre (i pezzi a tema) appartengono a quella in corso.
 *
 * <pre>
 *   ^SWARM_TROPHY_B      Prismatic Core          \
 *   ^EXPD_TITLE22        Title: 'The Fragmented' |  spedizione 22
 *   ^SWARM_HAT           Direwasp Helm           |
 *   ^EXPD_POSTER22C      Weavers Poster          /
 *   ^VAULT_HELM          Heirloom Breastplate    \  spedizione 21
 *   ^EXPD_TITLE21        Title: 'Mag-Field Expert' /
 * </pre>
 *
 * Le voci senza numero si attaccano all'ultima spedizione riconosciuta: e' la
 * lettura piu' fedele all'ordine del file, e resta stabile fra una lettura e
 * l'altra.
 */
public final class Premi {

    /** Un premio: un pezzo che si sblocca. */
    public static final class Premio {
        public final String id;
        public final String nome;
        public final boolean conIcona;

        Premio(String id, String nome, boolean conIcona) {
            this.id = id;
            this.nome = nome;
            this.conIcona = conIcona;
        }
    }

    /** Una spedizione con i suoi premi. */
    public static final class Spedizione {
        public final int numero;
        public final String nome;
        public final List<Premio> premi = new ArrayList<Premio>();

        Spedizione(int numero) {
            this.numero = numero;
            this.nome = numero > 0 ? "Spedizione " + numero : "Altre ricompense";
        }
    }

    /** I premi Twitch, tenuti a parte: si sbloccano tutti insieme. */
    private static final List<Premio> TWITCH = new ArrayList<Premio>();

    private static final Pattern SEASON = Pattern.compile(
            "<season\\s+id=\"([^\"]+)\"\\s+name=\"([^\"]*)\"([^/>]*)/>");
    private static final Pattern TWITCH_VOCE = Pattern.compile(
            "<twitch\\s+id=\"([^\"]+)\"\\s+name=\"([^\"]*)\"");
    private static final Pattern NUMERO = Pattern.compile("EXPD_[A-Z]*(\\d{1,2})");

    /**
     * Legge i premi e li raggruppa.
     *
     * @param file il file rewards.xml
     * @return le spedizioni in ordine, dalla piu' recente
     */
    public static List<Spedizione> carica(File file) throws IOException {
        String testo = leggi(file);
        List<Spedizione> spedizioni = new ArrayList<Spedizione>();
        TWITCH.clear();

        Matcher m = SEASON.matcher(testo);
        Spedizione corrente = null;
        // Le prime voci del file non hanno il numero: sono i pezzi a tema della
        // spedizione piu' recente, che viene numerata subito dopo. Si tengono da
        // parte e si uniscono alla prima spedizione, invece di formare un gruppo
        // "altre ricompense" che non vuol dire niente.
        List<Premio> inAttesa = new ArrayList<Premio>();
        while (m.find()) {
            String id = m.group(1);
            String nome = m.group(2);
            boolean conIcona = m.group(3) != null && m.group(3).contains("unlock=\"true\"");
            Premio premio = new Premio(id, nome, conIcona);

            int numero = numeroDi(id);
            if (numero > 0 && (corrente == null || corrente.numero != numero)) {
                // Una voce numerata diversa apre una spedizione nuova.
                corrente = new Spedizione(numero);
                if (!inAttesa.isEmpty()) {
                    corrente.premi.addAll(inAttesa);
                    inAttesa.clear();
                }
                spedizioni.add(corrente);
            }
            if (corrente == null) {
                inAttesa.add(premio);
            } else {
                corrente.premi.add(premio);
            }
        }
        if (!inAttesa.isEmpty()) {
            Spedizione senza = new Spedizione(-1);
            senza.premi.addAll(inAttesa);
            spedizioni.add(senza);
        }

        Matcher t = TWITCH_VOCE.matcher(testo);
        while (t.find()) {
            TWITCH.add(new Premio(t.group(1), t.group(2), false));
        }

        // L'ordine si ricalcola: il file non e' in ordine di numero. Le
        // spedizioni dalla 22 alla 1 sono in ordine decrescente, ma la 23 e'
        // stata aggiunta in fondo. Senza ordinare, la piu' recente finiva
        // sotto la 1.
        java.util.Collections.sort(spedizioni, new java.util.Comparator<Spedizione>() {
            public int compare(Spedizione a, Spedizione b) {
                if (a.numero == b.numero) {
                    return 0;
                }
                // I gruppi senza numero (non dovrebbero essercene) in fondo.
                if (a.numero < 0) {
                    return 1;
                }
                if (b.numero < 0) {
                    return -1;
                }
                return b.numero - a.numero;
            }
        });
        return spedizioni;
    }

    /** Il numero di spedizione contenuto in un identificativo, o -1. */
    private static int numeroDi(String id) {
        Matcher m = NUMERO.matcher(id);
        if (!m.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Quanti premi Twitch ci sono. */
    public static int quantiTwitch() {
        return TWITCH.size();
    }

    /** I premi Twitch. */
    public static List<Premio> twitch() {
        return Collections.unmodifiableList(TWITCH);
    }

    /** Quanti premi in tutto, spedizioni e Twitch. */
    public static int totale(List<Spedizione> spedizioni) {
        int n = TWITCH.size();
        for (Spedizione s : spedizioni) {
            n += s.premi.size();
        }
        return n;
    }

    private static String leggi(File file) throws IOException {
        StringBuilder b = new StringBuilder();
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String riga;
            while ((riga = r.readLine()) != null) {
                b.append(riga).append('\n');
            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    // niente
                }
            }
        }
        return b.toString();
    }

    private Premi() {
    }
}

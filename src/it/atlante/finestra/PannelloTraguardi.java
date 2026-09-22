package it.atlante.finestra;

import it.atlante.json.Json;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traguardi e fazioni: i progressi del viaggiatore, in schede.
 *
 * Il primo tentativo era un elenco piatto di 471 righe con gli identificativi
 * del salvataggio in mezzo a quelli con un nome: illeggibile, e giustamente
 * bocciato.
 *
 * Il vecchio editor li raggruppa in SCHEDE: una per i traguardi, una per le
 * uccisioni, e una per ogni razza e ogni gilda, ognuna con la sua icona e poche
 * righe con un nome chiaro. Questa e' quella struttura.
 *
 * <b>Come sono state ricavate le corrispondenze.</b> Non traducendo gli
 * identificativi a occhio — cosi' avevo gia' inventato due valori inesistenti
 * nella scheda delle stazioni. Ho preso le coppie nome/valore che il vecchio
 * editor mostra e ho cercato nel salvataggio la statistica con quel valore.
 * Dove il valore e' unico la corrispondenza e' certa:
 *
 * <pre>
 *   Parole apprese 662            -&gt; ^WORDS_LEARNT
 *   Unita' accumulate 2147483647  -&gt; ^MONEY
 *   Pirati 265                    -&gt; ^PIRATES_KILLED
 *   Gek, sistemi visitati 138     -&gt; ^TSEEN_SYSTEMS
 *   Esplorazione a piedi          -&gt; ^DIST_WALKED   (l'unica decimale)
 * </pre>
 *
 * Dove il valore non era unico la voce e' stata <b>lasciata fuori</b>: meglio
 * una scheda con qualche riga in meno che una riga che modifica la statistica
 * sbagliata.
 */
public final class PannelloTraguardi extends JPanel {

    /** Una voce di una scheda: nome mostrato e identificativo. */
    private static final class Voce {
        final String nome;
        final String id;

        Voce(String nome, String id) {
            this.nome = nome;
            this.id = id;
        }
    }

    /** Una scheda: titolo, icona e le sue voci. */
    private static final class Scheda {
        final String titolo;
        final String icona;
        final List<Voce> voci = new ArrayList<Voce>();

        Scheda(String titolo, String icona) {
            this.titolo = titolo;
            this.icona = icona;
        }

        Scheda v(String nome, String id) {
            voci.add(new Voce(nome, id));
            return this;
        }
    }

    private static final List<Scheda> SCHEDE = new ArrayList<Scheda>();

    static {
        SCHEDE.add(new Scheda("Traguardi", "UI-MILESTONES")
                .v("Esplorazione a piedi", "^DIST_WALKED")
                .v("Incontri con coloni alieni", "^ALIENS_MET")
                .v("Parole apprese", "^WORDS_LEARNT")
                .v("Unità accumulate", "^MONEY")
                .v("Astronavi distrutte", "^ENEMIES_KILLED")
                .v("Sentinelle distrutte", "^SENTINEL_KILLS")
                .v("Esplorazione spaziale (warp)", "^DIST_WARP"));

        SCHEDE.add(new Scheda("Uccisioni", "UI-WEAPONICON")
                .v("Droni sentinella", "^DRONES_KILLED")
                .v("Quad sentinella", "^QUADS_KILLED")
                .v("Walker sentinella", "^WALKERS_KILLED")
                .v("Pirati", "^PIRATES_KILLED")
                .v("Forze dell'ordine", "^POLICE_KILLED"));

        SCHEDE.add(new Scheda("Gek", "UI-GEK")
                .v("Reputazione", "^TRA_STANDING")
                .v("Missioni completate", "^TGDONE_MISSIONS")
                .v("Parole apprese", "^BWORDS_LEARNT")
                .v("Sistemi visitati", "^TSEEN_SYSTEMS"));

        SCHEDE.add(new Scheda("Vy'keen", "UI-VYKEEN")
                .v("Reputazione", "^WAR_STANDING")
                .v("Missioni completate", "^TDONE_MISSIONS")
                .v("Parole apprese", "^WWORDS_LEARNT")
                .v("Sistemi visitati", "^WSEEN_SYSTEMS"));

        SCHEDE.add(new Scheda("Korvax", "UI-KORVAX")
                .v("Reputazione", "^EXP_STANDING")
                .v("Missioni completate", "^TDONE_MISSIONS")
                .v("Parole apprese", "^EWORDS_LEARNT")
                .v("Sistemi visitati", "^ESEEN_SYSTEMS"));

        SCHEDE.add(new Scheda("Mercanti", "UI-TRADERS")
                .v("Reputazione", "^TGUILD_STAND")
                .v("Missioni completate", "^TGDONE_MISSIONS")
                .v("Piante coltivate", "^PLANTS_PLANTED")
                .v("Unità guadagnate", "^MONEY"));

        SCHEDE.add(new Scheda("Guerrieri", "UI-WARRIORS")
                .v("Reputazione", "^WGUILD_STAND")
                .v("Missioni completate", "^TDONE_MISSIONS")
                .v("Sentinelle distrutte", "^SENTINEL_KILLS")
                .v("Pirati uccisi", "^PIRATES_KILLED"));

        SCHEDE.add(new Scheda("Esploratori", "UI-EXPLORERS")
                .v("Reputazione", "^EGUILD_STAND")
                .v("Missioni completate", "^TDONE_MISSIONS")
                .v("Creature rare scansionate", "^RARE_SCANNED")
                .v("Distanza warp", "^DIST_WARP"));
    }

    private final Icone icone;
    private final Runnable suModifica;

    private final JPanel contenitore = new Colonna();
    private final JLabel contatore = new JLabel();
    private final JTextField cerca = new JTextField();

    private Map<String, Object> stato;

    public PannelloTraguardi(Icone icone, Runnable suModifica) {
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.FONDO);
        contenitore.setBackground(Aspetto.FONDO);
        contenitore.setBorder(BorderFactory.createEmptyBorder(14, 16, 20, 16));

        add(costruisciBarra(), BorderLayout.NORTH);

        JScrollPane scorrimento = new JScrollPane(contenitore);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(18);
        add(scorrimento, BorderLayout.CENTER);
    }

    private static final class Colonna extends JPanel implements Scrollable {
        Colonna() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Aspetto.FONDO);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibile, int orientamento, int direzione) {
            return 18;
        }

        public int getScrollableBlockIncrement(Rectangle visibile, int orientamento, int direzione) {
            return visibile.height;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel costruisciBarra() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setBackground(Aspetto.FONDO_ALTO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Aspetto.BORDO),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel titolo = new JLabel("PROGRESSIONE DEL VIAGGIATORE");
        titolo.setFont(Aspetto.monospaziato(10.5f, Font.BOLD));
        titolo.setForeground(Aspetto.ACCENTO);
        barra.add(titolo, BorderLayout.WEST);

        JPanel destra = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        destra.setOpaque(false);
        contatore.setFont(Aspetto.monospaziato(11, Font.PLAIN));
        contatore.setForeground(Aspetto.TESTO_TENUE);
        destra.add(contatore);

        cerca.setPreferredSize(new Dimension(200, 28));
        cerca.setToolTipText("Cerca fra i traguardi: nasconde le schede che non contengono il testo");
        cerca.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filtra();
            }
        });
        destra.add(cerca);

        JButton massimizza = new JButton("Massimizza fazioni e gilde");
        massimizza.setFont(massimizza.getFont().deriveFont(11.5f));
        massimizza.setToolTipText("Porta tutte le reputazioni a 100");
        massimizza.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                massimizza();
            }
        });
        destra.add(massimizza);
        barra.add(destra, BorderLayout.EAST);
        return barra;
    }

    // ------------------------------------------------------------------

    /** Mostra i traguardi del gruppo globale. */
    @SuppressWarnings("unchecked")
    public void mostra(Object radice) {
        stato = null;
        if (radice instanceof Map) {
            // Il contesto attivo, non la partita principale per forza: un
            // salvataggio di spedizione ha i suoi traguardi.
            Object contesto = ((Map<String, Object>) radice).get(Inventari.contesto(radice));
            if (contesto instanceof Map) {
                Object psd = ((Map<String, Object>) contesto).get("PlayerStateData");
                if (psd instanceof Map) {
                    stato = (Map<String, Object>) psd;
                }
            }
        }
        disegna();
    }

    private void disegna() {
        contenitore.removeAll();
        if (stato == null) {
            contatore.setText("");
            contenitore.add(new JLabel("Nessun salvataggio aperto"));
            contenitore.revalidate();
            contenitore.repaint();
            return;
        }

        JPanel griglia = new JPanel(new GridLayout(0, 2, 14, 14));
        griglia.setOpaque(false);
        griglia.setAlignmentX(Component.LEFT_ALIGNMENT);

        int totale = 0;
        for (Scheda s : SCHEDE) {
            griglia.add(scheda(s));
            totale += s.voci.size();
        }
        contenitore.add(griglia);
        contatore.setText(totale + " valori");
        contenitore.revalidate();
        contenitore.repaint();
    }

    private JPanel scheda(Scheda s) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Aspetto.PANNELLO);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Aspetto.BORDO),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JPanel testa = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 9, 0));
        testa.setOpaque(false);
        JLabel immagine = new JLabel();
        ImageIcon img = icone.perFile(s.icona + ".PNG", 26);
        immagine.setIcon(img != null ? img : Icone.segno(s.titolo.substring(0, 1), 26,
                Aspetto.ACCENTO, true));
        testa.add(immagine);
        JLabel titolo = new JLabel(s.titolo);
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 13.5f));
        titolo.setForeground(Aspetto.TESTO);
        testa.add(titolo);
        testa.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        p.add(testa, BorderLayout.NORTH);

        JPanel righe = new JPanel();
        righe.setLayout(new BoxLayout(righe, BoxLayout.Y_AXIS));
        righe.setOpaque(false);
        for (Voce v : s.voci) {
            righe.add(riga(v));
        }
        p.add(righe, BorderLayout.CENTER);
        p.setToolTipText(s.titolo);
        return p;
    }

    private JPanel riga(final Voce v) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel nome = new JLabel(v.nome);
        nome.setFont(nome.getFont().deriveFont(12f));
        nome.setForeground(Aspetto.TESTO_TENUE);
        nome.setPreferredSize(new Dimension(180, 24));
        nome.setToolTipText(v.id);
        r.add(nome, BorderLayout.WEST);

        JTextField campo = new JTextField(testoDi(v.id));
        campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
        campo.setPreferredSize(new Dimension(140, 26));
        campo.setToolTipText(v.id + "  —  modifica e premi Invio");
        campo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scrivi(v.id, ((JTextField) e.getSource()).getText().trim());
            }
        });
        r.add(campo, BorderLayout.CENTER);

        // Accanto alle parole apprese, il pulsante che le impara tutte.
        if (v.id.contains("WORDS")) {
            JButton impara = new JButton("Impara tutte");
            impara.setFont(impara.getFont().deriveFont(11f));
            impara.setToolTipText("Impara tutte le parole che ti mancano, per tutte le razze");
            impara.setPreferredSize(new Dimension(120, 26));
            impara.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    imparaTutteLeParole();
                }
            });
            r.add(impara, BorderLayout.EAST);
        }
        return r;
    }

    /**
     * Impara tutte le parole che mancano.
     *
     * Le parole non sono un contatore: il salvataggio tiene
     * {@code PlayerStateData.KnownWordGroups}, una lista di circa 3800 voci
     * fatte cosi':
     *
     * <pre>
     *   { Group: "^TRA_A", Races: [true, false, false, ...] }
     * </pre>
     *
     * Il gruppo e' la parola in una lingua, e l'elenco dice quali razze la
     * conoscono: indice 0 i mercanti, 1 i guerrieri, 2 gli esploratori, 8 gli
     * autofagi. Per impararle tutte basta portare tutte le caselle a vero.
     *
     * I contatori nelle statistiche (^TWORDS_LEARNT e simili) si aggiornano di
     * conseguenza, con il numero di parole che esistono per ogni razza secondo
     * {@code risorse/db/words.xml}.
     */
    @SuppressWarnings("unchecked")
    private void imparaTutteLeParole() {
        if (stato == null) {
            return;
        }
        Object gruppi = stato.get("KnownWordGroups");
        if (!(gruppi instanceof List)) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Questo salvataggio non ha l'elenco delle parole.",
                    "Niente da fare", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int parole = 0;
        int caselle = 0;
        for (Object g : (List<Object>) gruppi) {
            if (!(g instanceof Map)) {
                continue;
            }
            Object razze = ((Map<String, Object>) g).get("Races");
            if (!(razze instanceof List)) {
                continue;
            }
            List<Object> elenco = (List<Object>) razze;
            boolean mancava = false;
            for (int i = 0; i < elenco.size(); i++) {
                if (!Boolean.TRUE.equals(elenco.get(i))) {
                    elenco.set(i, Boolean.TRUE);
                    caselle++;
                    mancava = true;
                }
            }
            if (mancava) {
                parole++;
            }
        }

        // I contatori delle statistiche: il massimo e' il numero di parole che
        // esistono per quella razza.
        int[] massimi = {contaparole("TRADERS"), contaparole("WARRIORS"),
                contaparole("EXPLORERS"), contaparole("BUILDERS")};
        impostaAlmeno("^TWORDS_LEARNT", massimi[0]);
        impostaAlmeno("^WWORDS_LEARNT", massimi[1]);
        impostaAlmeno("^EWORDS_LEARNT", massimi[2]);
        impostaAlmeno("^BWORDS_LEARNT", massimi[3]);

        if (caselle > 0) {
            suModifica.run();
        }
        disegna();
        javax.swing.JOptionPane.showMessageDialog(this,
                caselle == 0 ? "Sapevi gia' tutte le parole."
                        : "Parole imparate: " + parole + "\nCaselle impostate: " + caselle,
                "Fatto", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Quante parole esistono per una razza, secondo words.xml.
     *
     * Il file elenca le parole e, per ognuna, un gruppo per razza. Contando i
     * gruppi si ottiene il massimo che il contatore puo' raggiungere.
     */
    private int contaparole(String razza) {
        if (cacheParole == null) {
            cacheParole = new LinkedHashMap<String, Integer>();
        }
        Integer noto = cacheParole.get(razza);
        if (noto != null) {
            return noto.intValue();
        }
        int quanti = 0;
        try {
            String testo = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get("risorse", "db", "words.xml")),
                    java.nio.charset.StandardCharsets.UTF_8);
            int da = 0;
            String ago = "race=\"" + razza + "\"";
            while ((da = testo.indexOf(ago, da)) >= 0) {
                quanti++;
                da += ago.length();
            }
        } catch (Exception e) {
            quanti = 0;
        }
        cacheParole.put(razza, Integer.valueOf(quanti));
        return quanti;
    }

    private static Map<String, Integer> cacheParole;

    /** Porta una statistica almeno a un valore, senza abbassarla. */
    private void impostaAlmeno(String id, int minimo) {
        if (minimo <= 0) {
            return;
        }
        Map<String, Object> voce = voceDi(id);
        if (voce == null) {
            return;
        }
        Object contenuto = voce.get("Value");
        if (contenuto instanceof Map) {
            Map<String, Object> v = (Map<String, Object>) contenuto;
            Object attuale = v.get("IntValue");
            if (attuale == null || numero(attuale) < minimo) {
                v.put("IntValue", new Json.Numero(String.valueOf(minimo)));
            }
        }
    }

    // ------------------------------------------------------------------

    /** Nasconde le schede che non contengono la ricerca. */
    private void filtra() {
        String filtro = cerca.getText() == null ? "" : cerca.getText().trim().toLowerCase();
        for (Component c : contenitore.getComponents()) {
            if (!(c instanceof JPanel)) {
                continue;
            }
            for (Component figlio : ((JPanel) c).getComponents()) {
                if (figlio instanceof JPanel) {
                    figlio.setVisible(filtro.isEmpty() || contiene((JPanel) figlio, filtro));
                }
            }
        }
        contenitore.revalidate();
        contenitore.repaint();
    }

    private boolean contiene(JPanel scheda, String filtro) {
        for (Scheda s : SCHEDE) {
            if (s.titolo.toLowerCase().contains(filtro)) {
                return true;
            }
        }
        for (Component c : scheda.getComponents()) {
            if (!(c instanceof JPanel)) {
                continue;
            }
            for (Component r : ((JPanel) c).getComponents()) {
                if (!(r instanceof JPanel)) {
                    continue;
                }
                for (Component e : ((JPanel) r).getComponents()) {
                    if (e instanceof JLabel) {
                        String t = ((JLabel) e).getText();
                        if (t != null && t.toLowerCase().contains(filtro)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> voceDi(String id) {
        Object stats = stato.get("Stats");
        if (!(stats instanceof List)) {
            return null;
        }
        for (Object gruppo : (List<Object>) stats) {
            if (!(gruppo instanceof Map)) {
                continue;
            }
            Map<String, Object> g = (Map<String, Object>) gruppo;
            if (numero(g.get("Address")) != 0) {
                continue;
            }
            Object voci = g.get("Stats");
            if (!(voci instanceof List)) {
                continue;
            }
            for (Object voce : (List<Object>) voci) {
                if (voce instanceof Map
                        && id.equals(String.valueOf(((Map<String, Object>) voce).get("Id")))) {
                    return (Map<String, Object>) voce;
                }
            }
            // Ci sono piu' gruppi con indirizzo 0: senza fermarsi al primo le
            // stesse voci comparirebbero piu' volte.
            break;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String testoDi(String id) {
        Map<String, Object> voce = voceDi(id);
        if (voce == null) {
            return "—";
        }
        Object valore = voce.get("Value");
        if (valore instanceof Map) {
            Map<String, Object> v = (Map<String, Object>) valore;
            if (v.get("IntValue") != null) {
                return String.valueOf(numero(v.get("IntValue")));
            }
            if (v.get("FloatValue") != null) {
                return String.valueOf(v.get("FloatValue"));
            }
        }
        return "—";
    }

    @SuppressWarnings("unchecked")
    private void scrivi(String id, String valore) {
        Map<String, Object> voce = voceDi(id);
        if (voce == null) {
            return;
        }
        Object contenuto = voce.get("Value");
        if (contenuto instanceof Map) {
            Map<String, Object> v = (Map<String, Object>) contenuto;
            if (v.get("IntValue") != null) {
                v.put("IntValue", new Json.Numero(valore));
            } else if (v.get("FloatValue") != null) {
                v.put("FloatValue", new Json.Numero(valore));
            }
            suModifica.run();
        }
    }

    private static long numero(Object o) {
        if (o instanceof Json.Numero) {
            return ((Json.Numero) o).comeLong();
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private void massimizza() {
        int toccati = 0;
        for (String id : new String[]{"^TRA_STANDING", "^EXP_STANDING", "^WAR_STANDING",
                "^BUI_STANDING", "^PIR_STAND", "^TGUILD_STAND", "^EGUILD_STAND",
                "^WGUILD_STAND"}) {
            Map<String, Object> voce = voceDi(id);
            if (voce == null) {
                continue;
            }
            Object valore = voce.get("Value");
            if (valore instanceof Map) {
                ((Map<String, Object>) valore).put("IntValue", new Json.Numero("100"));
                toccati++;
            }
        }
        if (toccati > 0) {
            suModifica.run();
        }
        disegna();
        javax.swing.JOptionPane.showMessageDialog(this,
                toccati == 0 ? "Niente da cambiare." : "Reputazioni portate a 100: " + toccati,
                "Fatto", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /** Quante schede mostra: serve al collaudo. */
    public int quanteSchede() {
        return SCHEDE.size();
    }
}

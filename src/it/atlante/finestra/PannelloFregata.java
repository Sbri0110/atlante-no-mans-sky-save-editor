package it.atlante.finestra;

import it.atlante.json.Json;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.util.List;
import java.util.Map;

/**
 * La scheda di una fregata, divisa in gruppi invece che in una colonna sola.
 *
 * Il salvataggio tiene la fregata come una matassa di campi, e mostrarli tutti
 * di fila costringeva a scorrere: qui si vedono quattro gruppi, su due colonne
 * come nel vecchio editor.
 *
 * <pre>
 *   INFORMAZIONI FREGATA          STATS
 *     Nome                          Combat, Exploration, Industry, Trading,
 *     Tipo, Classe, Razza           costo per salto, carburante, durata,
 *     Seed casa, Seed modello       bottino, riparazione, danni, furtivita'
 *
 *   TRATTI                        TOTALI
 *     cinque menu a tendina         spedizioni, riuscite, non riuscite, danni
 * </pre>
 *
 * I tratti non si scrivono a mano: si scelgono da un menu con tutti quelli del
 * gioco, ognuno con il suo effetto ("Exploration Specialist (+15 Exploration)").
 */
public final class PannelloFregata extends JPanel {

    /** Le classi di fregata: il valore nel salvataggio e come si chiama. */
    private static final String[][] CLASSI = {
            {"Exploration", "Exploration"},
            {"Mining", "Mining"},
            {"Diplomacy", "Diplomacy"},
            {"Pirate", "Pirate"},
            {"DeepSpace", "DeepSpace"},
            {"GhostShip", "GhostShip"},
            {"Normandy", "Normandy"},
    };

    /** La razza: nel salvataggio e' la gilda. */
    private static final String[][] RAZZE = {
            {"Warriors", "Vy'keen"},
            {"Traders", "Gek"},
            {"Explorers", "Korvax"},
    };

    /** Quanti tratti puo' avere una fregata. */
    private static final int QUANTI_TRATTI = 5;

    private final Icone icone;
    private final Runnable suModifica;

    private final JLabel icona = new JLabel();
    private final JLabel nome = new JLabel();
    private final JLabel sotto = new JLabel();

    private final JTextField nomeDato = new JTextField();
    private final JComboBox<String> classe = new JComboBox<String>();
    private final JComboBox<String> inventario = new JComboBox<String>();
    private final JComboBox<String> razza = new JComboBox<String>();
    private final JTextField seedCasa = new JTextField();
    private final JTextField seedModello = new JTextField();

    private final List<JComboBox<Tratti.Tratto>> tratti = new ArrayList<JComboBox<Tratti.Tratto>>();
    private final List<JTextField> stats = new ArrayList<JTextField>();
    private final JTextField spedizioni = new JTextField();
    private final JTextField riuscite = new JTextField();
    private final JTextField nonRiuscite = new JTextField();
    private final JTextField danneggiata = new JTextField();

    private Object radice;
    private int indice = -1;
    private boolean caricando;

    public PannelloFregata(Icone icone, Runnable suModifica) {
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.PANNELLO);

        for (String[] c : CLASSI) {
            classe.addItem(c[1]);
        }
        for (String[] r : RAZZE) {
            razza.addItem(r[1]);
        }
        for (String c : new String[]{"S", "A", "B", "C"}) {
            inventario.addItem(c);
        }

        add(testata(), BorderLayout.NORTH);
        add(corpo(), BorderLayout.CENTER);
        collega();
        svuota();
    }

    // ------------------------------------------------------------------

    private JPanel testata() {
        JPanel testa = new JPanel(new BorderLayout(14, 0));
        testa.setBackground(Aspetto.PANNELLO);
        testa.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
        icona.setPreferredSize(new Dimension(72, 44));
        testa.add(icona, BorderLayout.WEST);
        JPanel testi = new JPanel();
        testi.setOpaque(false);
        testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
        nome.setFont(nome.getFont().deriveFont(Font.BOLD, 15f));
        nome.setForeground(Aspetto.TESTO);
        nome.setAlignmentX(Component.LEFT_ALIGNMENT);
        sotto.setFont(sotto.getFont().deriveFont(11.5f));
        sotto.setForeground(Aspetto.TESTO_TENUE);
        sotto.setAlignmentX(Component.LEFT_ALIGNMENT);
        testi.add(nome);
        testi.add(Box.createVerticalStrut(4));
        testi.add(sotto);
        testa.add(testi, BorderLayout.CENTER);
        return testa;
    }

    /** Le due colonne, ognuna con i suoi gruppi. */
    private JScrollPane corpo() {
        JPanel colonne = new Colonne();
        colonne.setBackground(Aspetto.PANNELLO);
        colonne.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 18));

        JPanel sinistra = colonna();
        sinistra.add(titolo("INFORMAZIONI FREGATA"));
        nomeDato.setFont(nomeDato.getFont().deriveFont(12f));
        sinistra.add(riga("Nome", nomeDato));
        classe.setFont(classe.getFont().deriveFont(12f));
        sinistra.add(riga("Tipo", classe));
        inventario.setFont(inventario.getFont().deriveFont(12f));
        sinistra.add(riga("Classe", inventario));
        razza.setFont(razza.getFont().deriveFont(12f));
        sinistra.add(riga("Razza NPC", razza));
        seedCasa.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        sinistra.add(riga("Seed casa", seedCasa));
        seedModello.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        sinistra.add(riga("Seed modello", conPulsante(seedModello, "Modello")));
        sinistra.add(Box.createVerticalStrut(10));
        sinistra.add(titolo("TRATTI"));
        for (int i = 0; i < QUANTI_TRATTI; i++) {
            final int posizione = i;
            JComboBox<Tratti.Tratto> menu = new JComboBox<Tratti.Tratto>();
            menu.setFont(menu.getFont().deriveFont(11.5f));
            for (Tratti.Tratto t : Tratti.tutti()) {
                menu.addItem(t);
            }
            menu.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (caricando) {
                        return;
                    }
                    Object scelta = ((JComboBox<?>) e.getSource()).getSelectedItem();
                    scriviTratto(posizione, scelta instanceof Tratti.Tratto
                            ? ((Tratti.Tratto) scelta).id : null);
                }
            });
            tratti.add(menu);
            sinistra.add(riga("Tratto " + (i + 1), menu));
        }

        JPanel destra = colonna();
        destra.add(titolo("STATS"));
        for (String nomeStat : Tratti.STATS) {
            JTextField campo = new JTextField();
            campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
            campo.setHorizontalAlignment(SwingConstants.RIGHT);
            final int posizione = stats.size();
            campo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scriviStat(posizione, ((JTextField) e.getSource()).getText().trim());
                }
            });
            stats.add(campo);
            destra.add(riga(nomeStat, campo));
        }
        destra.add(Box.createVerticalStrut(10));
        destra.add(titolo("TOTALI"));
        destra.add(riga("Spedizioni", numero(spedizioni, "TotalNumberOfExpeditions")));
        destra.add(riga("Riuscite", numero(riuscite, "TotalNumberOfSuccessfulEvents")));
        destra.add(riga("Non riuscite", numero(nonRiuscite, "TotalNumberOfFailedEvents")));
        destra.add(riga("Danneggiata", numero(danneggiata, "NumberOfTimesDamaged")));

        colonne.add(sinistra);
        colonne.add(destra);

        JScrollPane scorrimento = new JScrollPane(colonne);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.setBackground(Aspetto.PANNELLO);
        scorrimento.getVerticalScrollBar().setUnitIncrement(16);
        return scorrimento;
    }

    private JPanel colonna() {
        JPanel p = new JPanel();
        p.setBackground(Aspetto.PANNELLO);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /**
     * Le due colonne, che si adattano alla larghezza disponibile.
     *
     * Dentro un pannello scorrevole un {@code GridLayout} dichiara la larghezza
     * come somma di quello che chiedono le colonne: se chiedono piu' dello
     * spazio disponibile la seconda finisce fuori dalla vista, e i campi
     * allineati a destra sembrano vuoti perche' il loro testo resta fuori dal
     * bordo. Qui la larghezza segue quella della finestra.
     */
    private static final class Colonne extends JPanel implements Scrollable {

        Colonne() {
            setLayout(new GridLayout(1, 2, 18, 0));
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibile, int orientamento, int direzione) {
            return 16;
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

    private JLabel titolo(String testo) {
        JLabel l = new JLabel(testo);
        l.setFont(Aspetto.monospaziato(10f, Font.BOLD));
        l.setForeground(Aspetto.ACCENTO);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0));
        return l;
    }

    private JTextField numero(final JTextField campo, final String nomeCampo) {
        campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
        campo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviNumero(nomeCampo, campo.getText().trim());
            }
        });
        return campo;
    }

    private JPanel riga(String etichetta, Component editor) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(etichetta);
        l.setFont(l.getFont().deriveFont(11.5f));
        l.setForeground(Aspetto.TESTO_DEBOLE);
        l.setPreferredSize(new Dimension(136, 24));
        r.add(l, BorderLayout.WEST);
        r.add(editor, BorderLayout.CENTER);
        return r;
    }

    /** Un campo con il pulsante che ne genera il contenuto. */
    private JPanel conPulsante(final JTextField campo, final String quale) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        JButton genera = new JButton("Genera");
        genera.setFont(genera.getFont().deriveFont(11.5f));
        genera.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nuovo = Piloti.semeNuovo();
                campo.setText(nuovo);
                scriviSeme(quale, nuovo);
            }
        });
        p.add(campo, BorderLayout.CENTER);
        p.add(genera, BorderLayout.EAST);
        return p;
    }

    private void collega() {
        nomeDato.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (caricando || fregata() == null) {
                    return;
                }
                fregata().put("CustomName", nomeDato.getText().trim());
                nome.setText(nomeDato.getText().trim().isEmpty()
                        ? "Fregata " + (indice + 1) : nomeDato.getText().trim());
                suModifica.run();
            }
        });
        classe.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviAnnidato("FrigateClass", "FrigateClass", valore(CLASSI, classe.getSelectedIndex()));
            }
        });
        razza.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviAnnidato("Race", "AlienRace", valore(RAZZE, razza.getSelectedIndex()));
            }
        });
        inventario.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object scelta = inventario.getSelectedItem();
                scriviAnnidato("InventoryClass", "InventoryClass",
                        scelta == null ? null : String.valueOf(scelta));
            }
        });
        seedCasa.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviSeme("Casa", seedCasa.getText().trim());
            }
        });
    }

    private static String valore(String[][] tabella, int indice) {
        return indice < 0 || indice >= tabella.length ? null : tabella[indice][0];
    }

    private static int posizione(String[][] tabella, String valore) {
        for (int i = 0; i < tabella.length; i++) {
            if (tabella[i][0].equalsIgnoreCase(valore)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------

    public void mostra(Object radice, int i) {
        this.radice = radice;
        this.indice = i;
        caricando = true;
        try {
            Map<String, Object> f = fregata();
            if (f == null) {
                svuota();
                return;
            }
            String dato = testo(f.get("CustomName"));
            nome.setText(dato.isEmpty() ? "Fregata " + (i + 1) : dato);
            nomeDato.setText(dato);

            int pc = posizione(CLASSI, annidato(f, "FrigateClass", "FrigateClass"));
            classe.setSelectedIndex(pc >= 0 ? pc : 0);
            int pr = posizione(RAZZE, annidato(f, "Race", "AlienRace"));
            razza.setSelectedIndex(pr >= 0 ? pr : 0);
            String ci = annidato(f, "InventoryClass", "InventoryClass");
            inventario.setSelectedItem(ci == null || ci.isEmpty() ? "C" : ci);

            seedCasa.setText(seme(f, "HomeSystemSeed"));
            seedModello.setText(seme(f, "ResourceSeed"));

            List<Object> ids = trattiDi(f);
            for (int k = 0; k < tratti.size(); k++) {
                String id = k < ids.size() ? String.valueOf(ids.get(k)) : null;
                Tratti.Tratto t = Tratti.perId(id);
                tratti.get(k).setSelectedItem(t);
            }

            List<Object> valori = valoriStats(f);
            for (int k = 0; k < stats.size(); k++) {
                stats.get(k).setText(k < valori.size() ? testo(valori.get(k)) : "");
            }

            spedizioni.setText(testo(f.get("TotalNumberOfExpeditions")));
            riuscite.setText(testo(f.get("TotalNumberOfSuccessfulEvents")));
            nonRiuscite.setText(testo(f.get("TotalNumberOfFailedEvents")));
            danneggiata.setText(testo(f.get("NumberOfTimesDamaged")));

            aggiornaTesta();
        } finally {
            caricando = false;
        }
        revalidate();
        repaint();
    }

    private void aggiornaTesta() {
        Object c = classe.getSelectedItem();
        Object r = razza.getSelectedItem();
        ImageIcon img = icone.perFile(Icone.iconaDiRisorsa(c == null ? "" : String.valueOf(c)), 72);
        icona.setIcon(img != null ? img : Icone.segno("F", 44, Aspetto.ACCENTO, true));
        StringBuilder s = new StringBuilder();
        if (r != null) {
            s.append(r);
        }
        if (c != null) {
            if (s.length() > 0) {
                s.append("  ·  ");
            }
            s.append(String.valueOf(c));
        }
        Object inv = inventario.getSelectedItem();
        if (inv != null) {
            s.append("  ·  Classe ").append(inv);
        }
        sotto.setText(s.toString());
    }

    public void svuota() {
        radice = null;
        indice = -1;
        caricando = true;
        try {
            icona.setIcon(Icone.segno("·", 44, Aspetto.TESTO_DEBOLE, true));
            nome.setText("Nessuna fregata scelta");
            sotto.setText("");
            nomeDato.setText("");
            seedCasa.setText("");
            seedModello.setText("");
            for (JComboBox<Tratti.Tratto> menu : tratti) {
                menu.setSelectedItem(null);
            }
            for (JTextField campo : stats) {
                campo.setText("");
            }
            spedizioni.setText("");
            riuscite.setText("");
            nonRiuscite.setText("");
            danneggiata.setText("");
        } finally {
            caricando = false;
        }
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> fregata() {
        if (radice == null || indice < 0) {
            return null;
        }
        Object elenco = Inventari.risolvi(radice, "BaseContext.PlayerStateData.FleetFrigates");
        if (!(elenco instanceof List)) {
            return null;
        }
        List<Object> l = (List<Object>) elenco;
        if (indice >= l.size()) {
            return null;
        }
        Object f = l.get(indice);
        return f instanceof Map ? (Map<String, Object>) f : null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> trattiDi(Map<String, Object> f) {
        Object v = f.get("TraitIDs");
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
    }

    /** I valori di {@code Stats}: undici numeri in fila. */
    @SuppressWarnings("unchecked")
    private List<Object> valoriStats(Map<String, Object> f) {
        Object v = f.get("Stats");
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
    }

    @SuppressWarnings("unchecked")
    private String annidato(Map<String, Object> f, String campo, String dentro) {
        Object v = f.get(campo);
        if (v instanceof Map) {
            Object d = ((Map<String, Object>) v).get(dentro);
            return d == null ? null : String.valueOf(d);
        }
        return null;
    }

    /**
     * Scrive dentro un campo annidato.
     *
     * Il salvataggio tiene {@code FrigateClass: {FrigateClass: "Mining"}}: si
     * tocca il valore interno, non l'oggetto.
     */
    @SuppressWarnings("unchecked")
    private void scriviAnnidato(String campo, String dentro, String valore) {
        if (caricando || valore == null) {
            return;
        }
        Map<String, Object> f = fregata();
        if (f == null) {
            return;
        }
        Object v = f.get(campo);
        if (v instanceof Map) {
            ((Map<String, Object>) v).put(dentro, valore);
            aggiornaTesta();
            suModifica.run();
        }
    }

    /** Cambia un tratto nella posizione data. */
    @SuppressWarnings("unchecked")
    private void scriviTratto(int posizione, String id) {
        if (caricando || id == null) {
            return;
        }
        Map<String, Object> f = fregata();
        if (f == null) {
            return;
        }
        List<Object> lista = trattiDi(f);
        if (!(f.get("TraitIDs") instanceof List)) {
            f.put("TraitIDs", lista);
        }
        while (lista.size() <= posizione) {
            lista.add("^");
        }
        lista.set(posizione, id);
        suModifica.run();
    }

    /** Scrive una delle undici statistiche. */
    @SuppressWarnings("unchecked")
    private void scriviStat(int posizione, String valore) {
        if (caricando || valore.isEmpty()) {
            return;
        }
        Map<String, Object> f = fregata();
        if (f == null) {
            return;
        }
        Object v = f.get("Stats");
        if (!(v instanceof List)) {
            return;
        }
        List<Object> lista = (List<Object>) v;
        while (lista.size() <= posizione) {
            lista.add(new Json.Numero("0"));
        }
        lista.set(posizione, new Json.Numero(valore));
        suModifica.run();
    }

    private void scriviNumero(String campo, String valore) {
        if (caricando || valore.isEmpty()) {
            return;
        }
        Map<String, Object> f = fregata();
        if (f == null) {
            return;
        }
        f.put(campo, new Json.Numero(valore));
        suModifica.run();
    }

    private String seme(Map<String, Object> f, String campo) {
        Object s = f.get(campo);
        if (s instanceof List && ((List<Object>) s).size() >= 2) {
            Object v = ((List<Object>) s).get(1);
            return v == null ? "" : String.valueOf(v);
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void scriviSeme(String quale, String valore) {
        if (caricando || valore == null || valore.isEmpty()) {
            return;
        }
        Map<String, Object> f = fregata();
        if (f == null) {
            return;
        }
        String campo = "Casa".equals(quale) ? "HomeSystemSeed" : "ResourceSeed";
        Object s = f.get(campo);
        if (s instanceof List && ((List<Object>) s).size() >= 2) {
            ((List<Object>) s).set(1, valore);
            suModifica.run();
        }
    }

    private String testo(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Json.Numero) {
            String t = ((Json.Numero) v).testo();
            return t.endsWith(".0") ? t.substring(0, t.length() - 2) : t;
        }
        return String.valueOf(v);
    }
}

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
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * La scheda di una fregata.
 *
 * Come per il pilota dello squadrone, il salvataggio tiene la fregata come una
 * matassa di campi: mostrarli tutti come albero costringe a leggere "Seme della
 * risorsa, Elemento 1, Elemento 2" per arrivare a "Eventi riusciti". Qui ci sono
 * le cose che si guardano davvero, con i nomi del vecchio editor:
 *
 * <ul>
 *   <li><b>Nome</b> — quello dato dal giocatore ({@code CustomName});</li>
 *   <li><b>Classe</b> — Exploration, Mining, Diplomacy, Pirate, DeepSpace,
 *       GhostShip, Normandy: sono le sette che il salvataggio usa;</li>
 *   <li><b>Razza</b> — chi l'ha costruita: Warriors (Vy'keen), Traders (Gek),
 *       Explorers (Korvax);</li>
 *   <li><b>Classe di inventario</b> — S, A, B, C;</li>
 *   <li><b>Danni, riparazioni, volte danneggiata</b>;</li>
 *   <li><b>Spedizioni</b> — completate, eventi riusciti, eventi falliti;</li>
 *   <li><b>I tre semi</b> — risorsa, sistema di origine, tratti imposti, con
 *       <i>Genera</i>.</li>
 * </ul>
 *
 * I campi annidati ({@code FrigateClass}, {@code Race}, {@code InventoryClass})
 * sono oggetti con dentro un solo valore: si scrive dentro l'oggetto, non al
 * posto suo, altrimenti il gioco non riconoscerebbe piu' il campo.
 */
public final class PannelloFregata extends JPanel {

    /** Una classe di fregata: il valore nel salvataggio e come si chiama. */
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

    private final Icone icone;
    private final Runnable suModifica;

    private final JLabel icona = new JLabel();
    private final JLabel nome = new JLabel();
    private final JLabel sotto = new JLabel();

    private final JTextField nomeDato = new JTextField();
    private final JComboBox<String> classe = new JComboBox<String>();
    private final JComboBox<String> razza = new JComboBox<String>();
    private final JComboBox<String> inventario = new JComboBox<String>();
    private final JTextField danni = new JTextField();
    private final JTextField riparazioni = new JTextField();
    private final JTextField volteDanneggiata = new JTextField();
    private final JTextField spedizioni = new JTextField();
    private final JTextField riusciti = new JTextField();
    private final JTextField falliti = new JTextField();
    private final JTextField seedRisorsa = new JTextField();
    private final JTextField seedOrigine = new JTextField();
    private final JTextField seedTratti = new JTextField();

    private Object radice;
    private int indice = -1;
    private boolean caricando;

    public PannelloFregata(Icone icone, Runnable suModifica) {
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
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

        JPanel testa = new JPanel(new BorderLayout(14, 0));
        testa.setOpaque(false);
        testa.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        testa.setAlignmentX(Component.LEFT_ALIGNMENT);
        testa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        icona.setPreferredSize(new Dimension(64, 40));
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
        add(testa);

        JPanel campi = new JPanel();
        campi.setOpaque(false);
        campi.setLayout(new BoxLayout(campi, BoxLayout.Y_AXIS));
        campi.setBorder(BorderFactory.createEmptyBorder(0, 18, 12, 18));
        campi.setAlignmentX(Component.LEFT_ALIGNMENT);

        nomeDato.setFont(nomeDato.getFont().deriveFont(12f));
        campi.add(riga("Nome", nomeDato));
        classe.setFont(classe.getFont().deriveFont(12f));
        campi.add(riga("Classe", classe));
        razza.setFont(razza.getFont().deriveFont(12f));
        campi.add(riga("Razza", razza));
        inventario.setFont(inventario.getFont().deriveFont(12f));
        campi.add(riga("Classe di inventario", inventario));
        campi.add(riga("Danni subiti", numero(danni)));
        campi.add(riga("Riparazioni", numero(riparazioni)));
        campi.add(riga("Volte danneggiata", numero(volteDanneggiata)));
        campi.add(riga("Spedizioni completate", numero(spedizioni)));
        campi.add(riga("Eventi riusciti", numero(riusciti)));
        campi.add(riga("Eventi falliti", numero(falliti)));
        campi.add(riga("Seme della risorsa", conPulsante(seedRisorsa, "Risorsa")));
        campi.add(riga("Seme del sistema", conPulsante(seedOrigine, "Sistema")));
        campi.add(riga("Seme dei tratti", conPulsante(seedTratti, "Tratti")));

        add(campi);
        add(Box.createVerticalGlue());

        collega();
        svuota();
    }

    private JTextField numero(JTextField campo) {
        campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
        return campo;
    }

    private JPanel riga(String etichetta, Component editor) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel l = new JLabel(etichetta);
        l.setFont(l.getFont().deriveFont(11.5f));
        l.setForeground(Aspetto.TESTO_DEBOLE);
        l.setPreferredSize(new Dimension(150, 24));
        r.add(l, BorderLayout.WEST);
        r.add(editor, BorderLayout.CENTER);
        return r;
    }

    /** Un campo con il pulsante che ne genera il contenuto. */
    private JPanel conPulsante(final JTextField campo, final String quale) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        final JButton genera = new JButton("Genera");
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
        collegaNumero(danni, "DamageTaken");
        collegaNumero(riparazioni, "RepairsMade");
        collegaNumero(volteDanneggiata, "NumberOfTimesDamaged");
        collegaNumero(spedizioni, "TotalNumberOfExpeditions");
        collegaNumero(riusciti, "TotalNumberOfSuccessfulEvents");
        collegaNumero(falliti, "TotalNumberOfFailedEvents");
        collegaSeme(seedRisorsa, "Risorsa");
        collegaSeme(seedOrigine, "Sistema");
        collegaSeme(seedTratti, "Tratti");
    }

    private void collegaNumero(final JTextField campo, final String nomeCampo) {
        campo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Map<String, Object> f = fregata();
                if (caricando || f == null) {
                    return;
                }
                String v = campo.getText().trim();
                if (v.isEmpty()) {
                    return;
                }
                f.put(nomeCampo, new Json.Numero(v));
                suModifica.run();
            }
        });
    }

    private void collegaSeme(final JTextField campo, final String quale) {
        campo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviSeme(quale, campo.getText().trim());
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

            String c = annidato(f, "FrigateClass", "FrigateClass");
            int pc = posizione(CLASSI, c);
            classe.setSelectedIndex(pc >= 0 ? pc : 0);

            String r = annidato(f, "Race", "AlienRace");
            int pr = posizione(RAZZE, r);
            razza.setSelectedIndex(pr >= 0 ? pr : 0);

            String ci = annidato(f, "InventoryClass", "InventoryClass");
            inventario.setSelectedItem(ci == null || ci.isEmpty() ? "C" : ci);

            nomeDato.setText(dato);
            danni.setText(testo(f.get("DamageTaken")));
            riparazioni.setText(testo(f.get("RepairsMade")));
            volteDanneggiata.setText(testo(f.get("NumberOfTimesDamaged")));
            spedizioni.setText(testo(f.get("TotalNumberOfExpeditions")));
            riusciti.setText(testo(f.get("TotalNumberOfSuccessfulEvents")));
            falliti.setText(testo(f.get("TotalNumberOfFailedEvents")));
            seedRisorsa.setText(seme(f, "ResourceSeed"));
            seedOrigine.setText(seme(f, "HomeSystemSeed"));
            seedTratti.setText(seme(f, "ForcedTraitsSeed"));

            aggiornaTesta();
        } finally {
            caricando = false;
        }
        revalidate();
        repaint();
    }

    /** La testata: icona della classe, nome e riga di riconoscimento. */
    private void aggiornaTesta() {
        Object c = classe.getSelectedItem();
        Object r = razza.getSelectedItem();
        String chiave = c == null ? "" : String.valueOf(c);
        ImageIcon img = icone.perFile(Icone.iconaDiRisorsa(chiave), 64);
        icona.setIcon(img != null ? img
                : Icone.segno("F", 40, Aspetto.ACCENTO, true));
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
            icona.setIcon(Icone.segno("·", 40, Aspetto.TESTO_DEBOLE, true));
            nome.setText("Nessuna fregata scelta");
            sotto.setText("");
            nomeDato.setText("");
            danni.setText("");
            riparazioni.setText("");
            volteDanneggiata.setText("");
            spedizioni.setText("");
            riusciti.setText("");
            falliti.setText("");
            seedRisorsa.setText("");
            seedOrigine.setText("");
            seedTratti.setText("");
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

    /** Il valore di un campo annidato, per esempio {@code FrigateClass.FrigateClass}. */
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
     * tocca il valore interno, non l'oggetto. Sostituendolo con una stringa il
     * gioco non riconoscerebbe piu' la classe.
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
        String campo = "Risorsa".equals(quale) ? "ResourceSeed"
                : "Sistema".equals(quale) ? "HomeSystemSeed" : "ForcedTraitsSeed";
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

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
 * La scheda di un compagno, divisa in gruppi.
 *
 * Il salvataggio tiene un compagno come una matassa di campi — semi, descrittori,
 * stati d'animo, mosse di battaglia — e l'albero costringeva a scorrere una
 * colonna lunghissima. Qui si vedono quattro gruppi su due colonne:
 *
 * <pre>
 *   INFORMAZIONI                  SEMI
 *     Nome, Tipo, Bioma,            creatura, secondario, ossa,
 *     Predatore, Taglia             specie, genere, colore
 *
 *   STATO                         BATTAGLIE
 *     fiducia, animo, tratti        mosse, vittorie, premi
 * </pre>
 *
 * Il tipo e il bioma si scelgono da un menu: nel salvataggio sono due oggetti
 * annidati ({@code CreatureType.CreatureType}, {@code Biome.Biome}) e si scrive
 * dentro l'oggetto, non al posto suo.
 */
public final class PannelloCompagno extends JPanel {

    /** I tipi di creatura, come li elenca il gioco. */
    private static final String[] TIPI = {
            "None", "Weird", "Bird", "FlyingLizard", "FlyingSnake", "Butterfly",
            "FlyingBeetle", "Beetle", "Fish", "Shark", "Crab", "Snake", "Dino",
            "Antelope", "Rodent", "Cat", "Cow", "Fiend", "Drone", "Quad",
            "SpiderQuad", "SpiderQuadMini", "Walker", "Predator", "PlayerPredator",
            "Prey", "Passive", "FishPredator", "FishPrey", "FiendFishSmall",
            "FiendFishBig", "Jellyfish", "Proto", "Skull", "WeirdFloat",
    };

    /** I biomi: nel salvataggio e' il posto dove l'hai trovato. */
    private static final String[] BIOMI = {
            "Lush", "Barren", "Cold", "Toxic", "Radioactive", "Fire", "Weird", "Mech",
    };

    private final Icone icone;
    private final Runnable suModifica;

    private final JLabel icona = new JLabel();
    private final JLabel nome = new JLabel();
    private final JLabel sotto = new JLabel();

    private final JTextField nomeDato = new JTextField();
    private final JComboBox<String> tipo = new JComboBox<String>(TIPI);
    private final JComboBox<String> bioma = new JComboBox<String>(BIOMI);
    private final JComboBox<String> predatore = new JComboBox<String>(
            new String[]{"No", "Sì"});
    private final JTextField taglia = new JTextField();

    private final JTextField seedCreatura = new JTextField();
    private final JTextField seedSecondario = new JTextField();
    private final JTextField seedOssa = new JTextField();
    private final JTextField seedSpecie = new JTextField();
    private final JTextField seedGenere = new JTextField();
    private final JTextField seedColore = new JTextField();

    private final JTextField fiducia = new JTextField();
    private final JTextField animo1 = new JTextField();
    private final JTextField animo2 = new JTextField();
    private final JTextField tratto1 = new JTextField();
    private final JTextField tratto2 = new JTextField();
    private final JTextField tratto3 = new JTextField();

    private final JTextField vittorie = new JTextField();
    private final JTextField premi = new JTextField();
    private final List<JComboBox<String>> classiStat = new ArrayList<JComboBox<String>>();
    private final List<JComboBox<String>> mosse = new ArrayList<JComboBox<String>>();
    private final List<JLabel> iconeMosse = new ArrayList<JLabel>();
    /** Campi semplici e booleani, per nome del campo nel salvataggio. */
    private final Map<String, JTextField> semplici = new java.util.LinkedHashMap<String, JTextField>();
    private final Map<String, JComboBox<String>> booleani = new java.util.LinkedHashMap<String, JComboBox<String>>();

    private Object radice;
    private int indice = -1;
    private boolean caricando;

    public PannelloCompagno(Icone icone, Runnable suModifica) {
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.PANNELLO);
        add(testata(), BorderLayout.NORTH);
        add(corpo(), BorderLayout.CENTER);
        collega();
        svuota();
    }

    private JPanel testata() {
        JPanel testa = new JPanel(new BorderLayout(14, 0));
        testa.setBackground(Aspetto.PANNELLO);
        testa.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
        icona.setPreferredSize(new Dimension(52, 52));
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

    private JScrollPane corpo() {
        JPanel colonne = new Colonne();
        colonne.setBackground(Aspetto.PANNELLO);
        colonne.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 18));

        JPanel sinistra = colonna();
        sinistra.add(titolo("INFORMAZIONI"));
        nomeDato.setFont(nomeDato.getFont().deriveFont(12f));
        sinistra.add(riga("Nome", nomeDato));
        tipo.setFont(tipo.getFont().deriveFont(12f));
        sinistra.add(riga("Tipo", tipo));
        bioma.setFont(bioma.getFont().deriveFont(12f));
        sinistra.add(riga("Bioma", bioma));
        predatore.setFont(predatore.getFont().deriveFont(12f));
        sinistra.add(riga("Predatore", predatore));
        sinistra.add(riga("Taglia", numero(taglia, "Scale")));
        sinistra.add(Box.createVerticalStrut(10));
        sinistra.add(titolo("STATO"));
        sinistra.add(riga("Fiducia", numero(fiducia, "Trust")));
        sinistra.add(riga("Animo", numero(animo1, "Mood1")));
        sinistra.add(riga("Animo 2", numero(animo2, "Mood2")));
        sinistra.add(riga("Tratto 1", numero(tratto1, "Trait1")));
        sinistra.add(riga("Tratto 2", numero(tratto2, "Trait2")));
        sinistra.add(riga("Tratto 3", numero(tratto3, "Trait3")));
        sinistra.add(Box.createVerticalStrut(10));
        // Tutto il resto che il salvataggio tiene sul compagno e che il gioco
        // usa: l'uovo, se e' stato evocato, la pelliccia, il nome della specie.
        sinistra.add(titolo("UOVA E ALTRO"));
        sinistra.add(riga("Nome specie", semplice("CustomSpeciesName")));
        sinistra.add(riga("Creatura", semplice("CreatureID")));
        sinistra.add(riga("Indirizzo (UA)", semplice("UA")));
        sinistra.add(riga("Nascita", semplice("BirthTime")));
        sinistra.add(riga("Uovo modificato", siNo("EggModified")));
        sinistra.add(riga("Evocato", siNo("HasBeenSummoned")));
        sinistra.add(riga("Pelliccia", siNo("HasFur")));
        sinistra.add(riga("Ricomposizione libera", siNo("AllowUnmodifiedReroll")));

        JPanel destra = colonna();
        destra.add(titolo("SEMI"));
        destra.add(riga("Creatura", conPulsante(seedCreatura, "Creatura")));
        destra.add(riga("Secondario", conPulsante(seedSecondario, "Secondario")));
        destra.add(riga("Ossa", conPulsante(seedOssa, "Ossa")));
        destra.add(riga("Specie", conPulsante(seedSpecie, "Specie")));
        destra.add(riga("Genere", conPulsante(seedGenere, "Genere")));
        destra.add(riga("Colore", conPulsante(seedColore, "Colore")));
        destra.add(Box.createVerticalStrut(10));
        destra.add(titolo("BATTAGLIE"));
        destra.add(riga("Vittorie", numero(vittorie, "PetBattlerVictories")));
        destra.add(riga("Premi disponibili", numero(premi, "PetBattlerTreatsAvailable")));
        destra.add(riga("Premi mangiati 1", semplice("TreatEaten1")));
        destra.add(riga("Premi mangiati 2", semplice("TreatEaten2")));
        destra.add(riga("Premi mangiati 3", semplice("TreatEaten3")));
        destra.add(riga("Progresso premio", semplice("PetBattleProgressToTreat")));
        destra.add(riga("Usa classi proprie", siNo("PetBattlerUseCoreStatClassOverrides")));
        // Le classi delle statistiche di combattimento: nel salvataggio sono tre
        // oggetti annidati come quelli delle navi, e valgono S, A, B o C.
        for (int i = 0; i < 3; i++) {
            final int posizione = i;
            JComboBox<String> menu = new JComboBox<String>(new String[]{"S", "A", "B", "C"});
            menu.setFont(menu.getFont().deriveFont(12f));
            menu.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (caricando) {
                        return;
                    }
                    Object v = ((JComboBox<?>) e.getSource()).getSelectedItem();
                    scriviClasseStat(posizione, v == null ? null : String.valueOf(v));
                }
            });
            classiStat.add(menu);
            destra.add(riga("Classe stat " + (i + 1), menu));
        }
        // Le mosse di combattimento: ognuna con l'icona del suo tipo, che nel
        // gioco sta in icons/pets/moves (attacco, difesa, potenza, velocita',
        // furtivita', cura, precisione, ricarica).
        for (int i = 0; i < 5; i++) {
            final int posizione = i;
            final JComboBox<String> menu = new JComboBox<String>();
            menu.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
            menu.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object v = menu.getSelectedItem();
                    scriviMossa(posizione, v == null ? null : String.valueOf(v));
                    iconeMosse.get(posizione).setIcon(
                            icone.perFile(iconaMossa(String.valueOf(v)), 22));
                }
            });
            JLabel iconaMossa = new JLabel();
            iconaMossa.setPreferredSize(new Dimension(24, 22));
            iconeMosse.add(iconaMossa);
            mosse.add(menu);
            JPanel rigaMossa = new JPanel(new BorderLayout(6, 0));
            rigaMossa.setOpaque(false);
            rigaMossa.setAlignmentX(Component.LEFT_ALIGNMENT);
            rigaMossa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            rigaMossa.add(iconaMossa, BorderLayout.WEST);
            rigaMossa.add(menu, BorderLayout.CENTER);
            destra.add(rigaMossa);
        }

        colonne.add(sinistra);
        colonne.add(destra);

        JScrollPane scorrimento = new JScrollPane(colonne);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.setBackground(Aspetto.PANNELLO);
        scorrimento.getVerticalScrollBar().setUnitIncrement(16);
        return scorrimento;
    }

    /** Un campo di testo legato a un campo del salvataggio. */
    private JTextField semplice(final String nomeCampo) {
        final JTextField campo = new JTextField();
        campo.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
        campo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviNumero(nomeCampo, campo.getText().trim());
            }
        });
        semplici.put(nomeCampo, campo);
        return campo;
    }

    /** Un si'/no legato a un campo booleano del salvataggio. */
    private JComboBox<String> siNo(final String nomeCampo) {
        final JComboBox<String> menu = new JComboBox<String>(new String[]{"No", "Sì"});
        menu.setFont(menu.getFont().deriveFont(12f));
        menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (caricando || compagno() == null) {
                    return;
                }
                compagno().put(nomeCampo, Boolean.valueOf(menu.getSelectedIndex() == 1));
                suModifica.run();
            }
        });
        booleani.put(nomeCampo, menu);
        return menu;
    }

    private JPanel colonna() {
        JPanel p = new JPanel();
        p.setBackground(Aspetto.PANNELLO);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
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
        l.setPreferredSize(new Dimension(120, 24));
        r.add(l, BorderLayout.WEST);
        r.add(editor, BorderLayout.CENTER);
        return r;
    }

    private JPanel conPulsante(final JTextField campo, final String quale) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        campo.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
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
                if (caricando || compagno() == null) {
                    return;
                }
                String v = nomeDato.getText().trim();
                compagno().put("CustomName", v);
                nome.setText(v.isEmpty() ? "Compagno " + (indice + 1) : v);
                suModifica.run();
            }
        });
        tipo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object v = tipo.getSelectedItem();
                scriviAnnidato("CreatureType", "CreatureType",
                        v == null ? null : String.valueOf(v));
            }
        });
        bioma.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object v = bioma.getSelectedItem();
                scriviAnnidato("Biome", "Biome", v == null ? null : String.valueOf(v));
            }
        });
        predatore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (caricando || compagno() == null) {
                    return;
                }
                compagno().put("Predator", Boolean.valueOf(predatore.getSelectedIndex() == 1));
                suModifica.run();
            }
        });
        seedCreatura.addActionListener(seme("Creatura"));
        seedSecondario.addActionListener(seme("Secondario"));
        seedOssa.addActionListener(seme("Ossa"));
        seedSpecie.addActionListener(seme("Specie"));
        seedGenere.addActionListener(seme("Genere"));
        seedColore.addActionListener(seme("Colore"));
    }

    private ActionListener seme(final String quale) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviSeme(quale, ((JTextField) e.getSource()).getText().trim());
            }
        };
    }

    // ------------------------------------------------------------------

    public void mostra(Object radice, int i) {
        this.radice = radice;
        this.indice = i;
        caricando = true;
        try {
            Map<String, Object> c = compagno();
            if (c == null) {
                svuota();
                return;
            }
            String dato = testo(c.get("CustomName"));
            nome.setText(dato.isEmpty() ? "Compagno " + (i + 1) : dato);
            nomeDato.setText(dato);

            scegli(tipo, annidato(c, "CreatureType", "CreatureType"));
            scegli(bioma, annidato(c, "Biome", "Biome"));
            predatore.setSelectedIndex(Boolean.TRUE.equals(c.get("Predator")) ? 1 : 0);

            taglia.setText(testo(c.get("Scale")));
            fiducia.setText(testo(c.get("Trust")));
            List<Object> animi = lista(c.get("Moods"));
            animo1.setText(animi.size() > 0 ? testo(animi.get(0)) : "");
            animo2.setText(animi.size() > 1 ? testo(animi.get(1)) : "");
            List<Object> tratti = lista(c.get("Traits"));
            tratto1.setText(tratti.size() > 0 ? testo(tratti.get(0)) : "");
            tratto2.setText(tratti.size() > 1 ? testo(tratti.get(1)) : "");
            tratto3.setText(tratti.size() > 2 ? testo(tratti.get(2)) : "");

            seedCreatura.setText(seme(c, "CreatureSeed"));
            seedSecondario.setText(seme(c, "CreatureSecondarySeed"));
            seedOssa.setText(seme(c, "BoneScaleSeed"));
            seedSpecie.setText(testo(c.get("SpeciesSeed")));
            seedGenere.setText(testo(c.get("GenusSeed")));
            seedColore.setText(seme(c, "ColourBaseSeed"));

            vittorie.setText(testo(c.get("PetBattlerVictories")));
            premi.setText(testo(c.get("PetBattlerTreatsAvailable")));
            // Le classi delle statistiche: una per ognuno dei tre oggetti
            // annidati. Se il compagno non le ha, i menu restano su "C".
            List<Object> classi = lista(c.get("PetBattlerCoreStatClassOverrides"));
            for (int k = 0; k < classiStat.size(); k++) {
                String valore = "C";
                if (k < classi.size() && classi.get(k) instanceof Map) {
                    Object v = ((Map<String, Object>) classi.get(k)).get("InventoryClass");
                    if (v != null) {
                        valore = String.valueOf(v);
                    }
                }
                classiStat.get(k).setSelectedItem(valore);
            }
            List<Object> mosseLista = lista(c.get("PetBattlerMoves"));
            List<String> conosciute = mosseConosciute();
            for (int k = 0; k < mosse.size(); k++) {
                JComboBox<String> menu = mosse.get(k);
                menu.removeAllItems();
                for (String m : conosciute) {
                    menu.addItem(m);
                }
                String id = k < mosseLista.size() ? String.valueOf(mosseLista.get(k)) : null;
                menu.setSelectedItem(id);
                iconeMosse.get(k).setIcon(id == null ? null : icone.perFile(iconaMossa(id), 22));
                menu.setToolTipText(id == null ? null : nomeMossa(id));
            }

            for (Map.Entry<String, JTextField> e : semplici.entrySet()) {
                String campo = e.getKey();
                if (campo.startsWith("TreatEaten")) {
                    int posizione = Integer.parseInt(campo.substring(10)) - 1;
                    List<Object> mangiati = lista(c.get("PetBattlerTreatsEaten"));
                    e.getValue().setText(posizione < mangiati.size() ? testo(mangiati.get(posizione)) : "");
                } else {
                    e.getValue().setText(testo(c.get(campo)));
                }
            }
            for (Map.Entry<String, JComboBox<String>> e : booleani.entrySet()) {
                e.getValue().setSelectedIndex(Boolean.TRUE.equals(c.get(e.getKey())) ? 1 : 0);
            }
            aggiornaTesta();
        } finally {
            caricando = false;
        }
        revalidate();
        repaint();
    }

    private void scegli(JComboBox<String> menu, String valore) {
        if (valore == null) {
            return;
        }
        for (int i = 0; i < menu.getItemCount(); i++) {
            if (menu.getItemAt(i).equalsIgnoreCase(valore)) {
                menu.setSelectedIndex(i);
                return;
            }
        }
    }

    private void aggiornaTesta() {
        Object b = bioma.getSelectedItem();
        String chiave = b == null ? "" : String.valueOf(b).toUpperCase();
        ImageIcon img = icone.perFile("UI-BIOMA-" + chiave + ".PNG", 52);
        icona.setIcon(img != null ? img : Icone.segno("C", 52, Aspetto.ACCENTO, true));
        StringBuilder s = new StringBuilder();
        Object t = tipo.getSelectedItem();
        if (t != null) {
            s.append(String.valueOf(t));
        }
        if (b != null) {
            if (s.length() > 0) {
                s.append("  ·  ");
            }
            s.append("Bioma ").append(String.valueOf(b));
        }
        if (predatore.getSelectedIndex() == 1) {
            s.append("  ·  Predatore");
        }
        sotto.setText(s.toString());
    }

    public void svuota() {
        radice = null;
        indice = -1;
        caricando = true;
        try {
            icona.setIcon(Icone.segno("·", 52, Aspetto.TESTO_DEBOLE, true));
            nome.setText("Nessun compagno scelto");
            sotto.setText("");
            nomeDato.setText("");
            taglia.setText("");
            fiducia.setText("");
            animo1.setText("");
            animo2.setText("");
            tratto1.setText("");
            tratto2.setText("");
            tratto3.setText("");
            seedCreatura.setText("");
            seedSecondario.setText("");
            seedOssa.setText("");
            seedSpecie.setText("");
            seedGenere.setText("");
            seedColore.setText("");
            vittorie.setText("");
            premi.setText("");
            for (int k = 0; k < mosse.size(); k++) {
                mosse.get(k).setSelectedItem(null);
                iconeMosse.get(k).setIcon(null);
            }
            for (JTextField campo : semplici.values()) {
                campo.setText("");
            }
        } finally {
            caricando = false;
        }
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> compagno() {
        if (radice == null || indice < 0) {
            return null;
        }
        Object elenco = Inventari.risolvi(radice, "BaseContext.PlayerStateData.Pets");
        if (!(elenco instanceof List)) {
            return null;
        }
        List<Object> l = (List<Object>) elenco;
        if (indice >= l.size()) {
            return null;
        }
        Object c = l.get(indice);
        return c instanceof Map ? (Map<String, Object>) c : null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> lista(Object v) {
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
    }

    @SuppressWarnings("unchecked")
    private String annidato(Map<String, Object> c, String campo, String dentro) {
        Object v = c.get(campo);
        if (v instanceof Map) {
            Object d = ((Map<String, Object>) v).get(dentro);
            return d == null ? null : String.valueOf(d);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void scriviAnnidato(String campo, String dentro, String valore) {
        if (caricando || valore == null) {
            return;
        }
        Map<String, Object> c = compagno();
        if (c == null) {
            return;
        }
        Object v = c.get(campo);
        if (v instanceof Map) {
            ((Map<String, Object>) v).put(dentro, valore);
            aggiornaTesta();
            suModifica.run();
        }
    }

    private void scriviNumero(String nomeCampo, String valore) {
        if (caricando || valore.isEmpty()) {
            return;
        }
        Map<String, Object> c = compagno();
        if (c == null) {
            return;
        }
        if (nomeCampo.startsWith("TreatEaten")) {
            scriviInLista(c, "PetBattlerTreatsEaten",
                    Integer.parseInt(nomeCampo.substring(10)) - 1, valore);
        } else if ("Mood1".equals(nomeCampo) || "Mood2".equals(nomeCampo)) {
            scriviInLista(c, "Moods", "Mood1".equals(nomeCampo) ? 0 : 1, valore);
        } else if (nomeCampo.startsWith("Trait")) {
            int posizione = Integer.parseInt(nomeCampo.substring(5)) - 1;
            scriviInLista(c, "Traits", posizione, valore);
        } else {
            c.put(nomeCampo, new Json.Numero(valore));
        }
        suModifica.run();
    }

    @SuppressWarnings("unchecked")
    private void scriviInLista(Map<String, Object> c, String campo, int posizione, String valore) {
        Object v = c.get(campo);
        if (!(v instanceof List)) {
            return;
        }
        List<Object> l = (List<Object>) v;
        if (posizione < l.size()) {
            l.set(posizione, new Json.Numero(valore));
        }
    }

    /**
     * Scrive la classe di una statistica di combattimento.
     *
     * Nel salvataggio sono tre oggetti annidati come quelli delle navi:
     * {@code PetBattlerCoreStatClassOverrides: [{InventoryClass: "C"}, ...]}.
     */
    @SuppressWarnings("unchecked")
    private void scriviClasseStat(int posizione, String valore) {
        if (caricando || valore == null) {
            return;
        }
        Map<String, Object> c = compagno();
        if (c == null) {
            return;
        }
        Object v = c.get("PetBattlerCoreStatClassOverrides");
        if (!(v instanceof List)) {
            return;
        }
        List<Object> l = (List<Object>) v;
        while (l.size() <= posizione) {
            Map<String, Object> nuovo = new java.util.LinkedHashMap<String, Object>();
            nuovo.put("InventoryClass", "C");
            l.add(nuovo);
        }
        Object voce = l.get(posizione);
        if (voce instanceof Map) {
            ((Map<String, Object>) voce).put("InventoryClass", valore);
            suModifica.run();
        }
    }

    /** Il seme di un campo a due elementi, o il campo stesso se e' una stringa. */
    private String seme(Map<String, Object> c, String campo) {
        Object s = c.get(campo);
        if (s instanceof List && ((List<Object>) s).size() >= 2) {
            Object v = ((List<Object>) s).get(1);
            return v == null ? "" : String.valueOf(v);
        }
        return testo(s);
    }

    @SuppressWarnings("unchecked")
    private void scriviSeme(String quale, String valore) {
        if (caricando || valore == null || valore.isEmpty()) {
            return;
        }
        Map<String, Object> c = compagno();
        if (c == null) {
            return;
        }
        String campo = "Creatura".equals(quale) ? "CreatureSeed"
                : "Secondario".equals(quale) ? "CreatureSecondarySeed"
                : "Ossa".equals(quale) ? "BoneScaleSeed"
                : "Specie".equals(quale) ? "SpeciesSeed"
                : "Genere".equals(quale) ? "GenusSeed" : "ColourBaseSeed";
        Object s = c.get(campo);
        if (s instanceof List && ((List<Object>) s).size() >= 2) {
            ((List<Object>) s).set(1, valore);
            suModifica.run();
        } else if (s != null) {
            c.put(campo, valore);
            suModifica.run();
        }
    }

    /**
     * L'icona di una mossa, dedotta dal suo identificativo.
     *
     * Le mosse del salvataggio sono sigle — {@code ^ATTACK_AFF},
     * {@code ^SELF_HOT}, {@code ^DEBUFF_ACCURACY} — e il gioco ha un'icona per
     * ciascun tipo in icons/pets/moves e icons/pets/buffs.
     */
    private static String iconaMossa(String id) {
        String u = id == null ? "" : id.toUpperCase();
        if (u.indexOf("SELF") >= 0 || u.indexOf("HEAL") >= 0) {
            return "UI-PETMOVE-HEALTH.PNG";
        }
        if (u.indexOf("DEBUFF") >= 0 || u.indexOf("ACCUR") >= 0) {
            return "UI-PETMOVE-ACCURACY.PNG";
        }
        if (u.indexOf("DEFEN") >= 0) {
            return "UI-PETMOVE-DEFENCE.PNG";
        }
        if (u.indexOf("SPEED") >= 0) {
            return "UI-PETMOVE-SPEED.PNG";
        }
        if (u.indexOf("STEALTH") >= 0 || u.indexOf("DODGE") >= 0) {
            return "UI-PETMOVE-STEALTH.PNG";
        }
        if (u.indexOf("POWER") >= 0) {
            return "UI-PETMOVE-POWER.PNG";
        }
        if (u.indexOf("COOL") >= 0) {
            return "UI-PETMOVE-COOLDOWN.PNG";
        }
        return "UI-PETMOVE-ATTACK.PNG";
    }

    /**
     * Tutte le mosse che compaiono nei compagni di questo salvataggio.
     *
     * Il gioco tiene l'elenco delle mosse nei suoi metadata, che l'editor non
     * legge: qui si raccolgono tutte quelle che i compagni di questo salvataggio
     * hanno davvero — di solito sono piu' di quante ne abbia un compagno solo —
     * e si scelgono dal menu. Nessuna sigla da scrivere a mano.
     */
    private List<String> mosseConosciute() {
        List<String> trovate = new ArrayList<String>();
        Object elenco = radice == null ? null
                : Inventari.risolvi(radice, "BaseContext.PlayerStateData.Pets");
        if (!(elenco instanceof List)) {
            return trovate;
        }
        for (Object o : (List<Object>) elenco) {
            if (!(o instanceof Map)) {
                continue;
            }
            for (Object m : lista(((Map<String, Object>) o).get("PetBattlerMoves"))) {
                String id = String.valueOf(m);
                if (!id.isEmpty() && !"^".equals(id) && !trovate.contains(id)) {
                    trovate.add(id);
                }
            }
        }
        java.util.Collections.sort(trovate);
        return trovate;
    }

    /** Cambia la mossa in una delle cinque posizioni. */
    @SuppressWarnings("unchecked")
    private void scriviMossa(int posizione, String id) {
        if (caricando || id == null || id.isEmpty() || "^".equals(id)) {
            return;
        }
        Map<String, Object> c = compagno();
        if (c == null) {
            return;
        }
        Object v = c.get("PetBattlerMoves");
        if (!(v instanceof List)) {
            List<Object> nuova = new ArrayList<Object>();
            c.put("PetBattlerMoves", nuova);
            v = nuova;
        }
        List<Object> l = (List<Object>) v;
        while (l.size() <= posizione) {
            l.add("^");
        }
        l.set(posizione, id);
        suModifica.run();
    }

    /** Il nome leggibile di una mossa: {@code ^ATTACK_AFF} -> "Attacco affinita'". */
    private static String nomeMossa(String id) {
        if (id == null) {
            return "";
        }
        String u = id.replace("^", "").replace("_", " ").trim().toLowerCase();
        if (u.isEmpty()) {
            return "";
        }
        String[] pezzi = u.split(" ");
        StringBuilder s = new StringBuilder();
        for (String pezzo : pezzi) {
            if (s.length() > 0) {
                s.append(" ");
            }
            if ("aff".equals(pezzo)) {
                s.append("affinita'");
            } else if ("debuff".equals(pezzo)) {
                s.append("riduce");
            } else if ("self".equals(pezzo)) {
                s.append("su di se'");
            } else {
                s.append(pezzo);
            }
        }
        String t = s.toString();
        return t.substring(0, 1).toUpperCase() + t.substring(1);
    }

    private String testo(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Json.Numero) {
            return ((Json.Numero) v).testo();
        }
        return String.valueOf(v);
    }

    /** Le due colonne, che seguono la larghezza della finestra. */
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
}

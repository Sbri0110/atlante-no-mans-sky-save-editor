package it.atlante.finestra;

import it.atlante.json.Json;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La scheda di un pilota dello squadrone: un form, non un albero di campi.
 *
 * Nel salvataggio un pilota e' una matassa di oggetti annidati — la risorsa
 * dell'equipaggio con dentro il seme, la risorsa della nave con dentro il suo,
 * i tratti — e mostrarli come albero significava leggere "Procedural Texture,
 * Samplers, Alt Id" per trovare il grado. Qui si vedono le sei cose che contano,
 * con i nomi che il vecchio editor usava nella sua scheda "Wingman":
 *
 * <ul>
 *   <li><b>Attivo</b> — se il pilota e' reclutato ({@code SquadronUnlockedPilotSlots});</li>
 *   <li><b>Razza NPC</b> — un menu a tendina: il modello dell'equipaggio e'
 *       uno di quelli noti, e sceglierlo cambia la razza del pilota;</li>
 *   <li><b>Seed NPC</b> — il seme che decide il suo aspetto, con <i>Genera</i>;</li>
 *   <li><b>Tipo di astronave</b> — un menu a tendina con i tipi del gioco;</li>
 *   <li><b>Seed astronave</b> — il seme della nave, con <i>Genera</i>;</li>
 *   <li><b>Grado pilota</b> — il numero che il gioco mostra.</li>
 * </ul>
 *
 * Il seme e' il secondo elemento della lista del salvataggio: la lista e'
 * {@code [flag, "0x..."} e si tocca solo la stringa.
 */
public final class PannelloWingman extends JPanel {

    private final Icone icone;
    private final Runnable suModifica;

    private final JLabel icona = new JLabel();
    private final JLabel nome = new JLabel();
    private final JLabel sotto = new JLabel();

    private final JCheckBox attivo = new JCheckBox("Attivo");
    private final JComboBox<Piloti.Razza> razza = new JComboBox<Piloti.Razza>();
    private final JLabel iconaRazza = new JLabel();
    private final JTextField seedNpc = new JTextField();
    private final JButton generaNpc = new JButton("Genera");
    private final JComboBox<Piloti.Nave> nave = new JComboBox<Piloti.Nave>();
    private final JLabel iconaNave = new JLabel();
    private final JTextField seedNave = new JTextField();
    private final JButton generaNave = new JButton("Genera");
    private final JTextField grado = new JTextField();

    private Object radice;
    private int indice = -1;
    /** Vero mentre si riempie il form: le scritture arrivano solo dall'utente. */
    private boolean caricando;

    public PannelloWingman(Icone icone, Runnable suModifica) {
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Aspetto.PANNELLO);

        for (Piloti.Razza r : Piloti.RAZZE) {
            razza.addItem(r);
        }
        for (Piloti.Nave n : Piloti.NAVI) {
            nave.addItem(n);
        }

        // --- testa: l'icona della razza e il nome del pilota ---
        JPanel testa = new JPanel(new BorderLayout(14, 0));
        testa.setOpaque(false);
        testa.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        testa.setAlignmentX(Component.LEFT_ALIGNMENT);
        testa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        icona.setPreferredSize(new Dimension(56, 56));
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

        // --- i campi ---
        JPanel campi = new JPanel();
        campi.setOpaque(false);
        campi.setLayout(new BoxLayout(campi, BoxLayout.Y_AXIS));
        campi.setBorder(BorderFactory.createEmptyBorder(0, 18, 12, 18));
        campi.setAlignmentX(Component.LEFT_ALIGNMENT);

        attivo.setOpaque(false);
        attivo.setFont(attivo.getFont().deriveFont(12f));
        attivo.setForeground(Aspetto.TESTO);
        campi.add(rigaSemplice(attivo));

        iconaRazza.setPreferredSize(new Dimension(26, 26));
        razza.setFont(razza.getFont().deriveFont(12f));
        campi.add(riga("Razza NPC", conIcona(iconaRazza, razza)));

        seedNpc.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        campi.add(riga("Seed NPC", conPulsante(seedNpc, generaNpc)));

        iconaNave.setPreferredSize(new Dimension(26, 26));
        nave.setFont(nave.getFont().deriveFont(12f));
        campi.add(riga("Tipo di astronave", conIcona(iconaNave, nave)));

        seedNave.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        campi.add(riga("Seed astronave", conPulsante(seedNave, generaNave)));

        grado.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        grado.setHorizontalAlignment(SwingConstants.RIGHT);
        campi.add(riga("Grado pilota", grado));

        add(campi);
        add(Box.createVerticalGlue());

        collega();
        svuota();
    }

    private JPanel rigaSemplice(Component editor) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        r.add(editor, BorderLayout.WEST);
        return r;
    }

    private JPanel riga(String etichetta, Component editor) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel l = new JLabel(etichetta);
        l.setFont(l.getFont().deriveFont(11.5f));
        l.setForeground(Aspetto.TESTO_DEBOLE);
        l.setPreferredSize(new Dimension(126, 24));
        r.add(l, BorderLayout.WEST);
        r.add(editor, BorderLayout.CENTER);
        return r;
    }

    /** Un menù con la sua icona accanto. */
    private JPanel conIcona(JLabel iconaPiccola, Component editor) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.add(iconaPiccola, BorderLayout.WEST);
        p.add(editor, BorderLayout.CENTER);
        return p;
    }

    /** Un campo con il pulsante che ne genera il contenuto. */
    private JPanel conPulsante(JTextField campo, JButton pulsante) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        pulsante.setFont(pulsante.getFont().deriveFont(11.5f));
        p.add(campo, BorderLayout.CENTER);
        p.add(pulsante, BorderLayout.EAST);
        return p;
    }

    private void collega() {
        attivo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (caricando) {
                    return;
                }
                Object slots = Inventari.risolvi(radice, "BaseContext.PlayerStateData.SquadronUnlockedPilotSlots");
                if (slots instanceof List) {
                    List<Object> l = lista(slots);
                    if (indice < l.size()) {
                        l.set(indice, Boolean.valueOf(attivo.isSelected()));
                        suModifica.run();
                    }
                }
            }
        });
        razza.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (caricando) {
                    return;
                }
                Piloti.Razza scelta = (Piloti.Razza) razza.getSelectedItem();
                Map<String, Object> r = risorsa("NPCResource", true);
                if (scelta != null && r != null) {
                    r.put("Filename", scelta.risorsa);
                    iconaRazza.setIcon(icone.perFile(scelta.icona, 26));
                    icona.setIcon(icone.perFile(scelta.icona, 56));
                    aggiornaSotto();
                    suModifica.run();
                }
            }
        });
        nave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (caricando) {
                    return;
                }
                Piloti.Nave scelta = (Piloti.Nave) nave.getSelectedItem();
                Map<String, Object> r = risorsa("ShipResource", true);
                if (scelta != null && r != null) {
                    r.put("Filename", scelta.risorsa);
                    suModifica.run();
                }
            }
        });
        generaNpc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nuovo = Piloti.semeNuovo();
                seedNpc.setText(nuovo);
                scriviSeed(risorsa("NPCResource", true), nuovo);
            }
        });
        generaNave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nuovo = Piloti.semeNuovo();
                seedNave.setText(nuovo);
                scriviSeed(risorsa("ShipResource", true), nuovo);
            }
        });
        seedNpc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviSeed(risorsa("NPCResource", true), seedNpc.getText().trim());
            }
        });
        seedNave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviSeed(risorsa("ShipResource", true), seedNave.getText().trim());
            }
        });
        grado.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Map<String, Object> p = pilota();
                if (p == null) {
                    return;
                }
                String v = grado.getText().trim();
                if (v.isEmpty()) {
                    return;
                }
                p.put("PilotRank", new Json.Numero(v));
                suModifica.run();
            }
        });
    }

    // ------------------------------------------------------------------

    /** Mostra il pilota numero {@code i}. */
    public void mostra(Object radice, int i) {
        this.radice = radice;
        this.indice = i;
        caricando = true;
        try {
            Map<String, Object> p = pilota();
            if (p == null) {
                svuota();
                return;
            }
            nome.setText("Wingman " + (i + 1));

            Object slots = Inventari.risolvi(radice, "BaseContext.PlayerStateData.SquadronUnlockedPilotSlots");
            boolean haSlots = slots instanceof List && indice < lista(slots).size();
            attivo.setEnabled(haSlots);
            attivo.setSelected(haSlots && Boolean.TRUE.equals(lista(slots).get(indice)));
            attivo.setToolTipText(haSlots ? "Se il pilota e' reclutato"
                    : "Questo salvataggio non tiene l'elenco dei posti sbloccati");

            Map<String, Object> npc = risorsa("NPCResource", false);
            String nomeRazza = npc == null ? null : testo(npc.get("Filename"));
            Piloti.Razza r = Piloti.razzaDi(nomeRazza);
            razza.setEnabled(npc != null);
            razza.setSelectedItem(r != null ? r : null);
            iconaRazza.setIcon(r == null ? null : icone.perFile(r.icona, 26));
            icona.setIcon(r != null ? icone.perFile(r.icona, 56)
                    : Icone.segno("·", 56, Aspetto.TESTO_DEBOLE, true));
            seedNpc.setText(npc == null ? "" : seed(npc));

            Map<String, Object> nave2 = risorsa("ShipResource", false);
            String nomeNave = nave2 == null ? null : testo(nave2.get("Filename"));
            Piloti.Nave n = Piloti.naveDi(nomeNave);
            nave.setEnabled(nave2 != null);
            nave.setSelectedItem(n);
            iconaNave.setIcon(n == null ? null : icone.perFile(n.icona, 26));
            seedNave.setText(nave2 == null ? "" : seed(nave2));

            grado.setText(testo(p.get("PilotRank")));
            aggiornaSotto();
            abilita(true);
        } finally {
            caricando = false;
        }
        revalidate();
        repaint();
    }

    /** La riga sotto il nome: razza, tipo di nave e grado. */
    private void aggiornaSotto() {
        StringBuilder s = new StringBuilder();
        Piloti.Razza r = (Piloti.Razza) razza.getSelectedItem();
        Piloti.Nave n = (Piloti.Nave) nave.getSelectedItem();
        if (r != null) {
            s.append(r.nome);
        }
        if (n != null) {
            if (s.length() > 0) {
                s.append("  ·  ");
            }
            s.append(n.nome);
        }
        String g = grado.getText().trim();
        if (!g.isEmpty()) {
            if (s.length() > 0) {
                s.append("  ·  ");
            }
            s.append("Grado ").append(g);
        }
        sotto.setText(s.toString());
    }

    /** Svuota il form: nessun pilota scelto. */
    public void svuota() {
        radice = null;
        indice = -1;
        caricando = true;
        try {
            icona.setIcon(Icone.segno("·", 56, Aspetto.TESTO_DEBOLE, true));
            nome.setText("Nessun pilota scelto");
            sotto.setText("");
            attivo.setSelected(false);
            attivo.setEnabled(false);
            razza.setSelectedItem(null);
            razza.setEnabled(false);
            iconaRazza.setIcon(null);
            seedNpc.setText("");
            nave.setSelectedItem(null);
            nave.setEnabled(false);
            iconaNave.setIcon(null);
            seedNave.setText("");
            grado.setText("");
        } finally {
            caricando = false;
        }
        abilita(false);
        revalidate();
        repaint();
    }

    private void abilita(boolean attivo2) {
        seedNpc.setEnabled(attivo2);
        generaNpc.setEnabled(attivo2);
        seedNave.setEnabled(attivo2);
        generaNave.setEnabled(attivo2);
        grado.setEnabled(attivo2);
    }

    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<Object> lista(Object o) {
        return (List<Object>) o;
    }

    /** Il pilota che si sta modificando, o null. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> pilota() {
        if (radice == null || indice < 0) {
            return null;
        }
        Object elenco = Inventari.risolvi(radice, "BaseContext.PlayerStateData.SquadronPilots");
        if (!(elenco instanceof List)) {
            return null;
        }
        List<Object> l = lista(elenco);
        if (indice >= l.size()) {
            return null;
        }
        Object p = l.get(indice);
        return p instanceof Map ? (Map<String, Object>) p : null;
    }

    /**
     * La risorsa del pilota ({@code NPCResource} o {@code ShipResource}).
     *
     * @param crea se vero e la risorsa non c'e', la crea vuota: serve per i
     *             salvataggi in cui il pilota non ha ancora una nave
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> risorsa(String campo, boolean crea) {
        Map<String, Object> p = pilota();
        if (p == null) {
            return null;
        }
        Object v = p.get(campo);
        if (v instanceof Map) {
            return (Map<String, Object>) v;
        }
        if (!crea) {
            return null;
        }
        Map<String, Object> nuova = new LinkedHashMap<String, Object>();
        nuova.put("Filename", "");
        nuova.put("Seed", new ArrayList<Object>());
        p.put(campo, nuova);
        return nuova;
    }

    /** Il seme di una risorsa: il secondo elemento della lista. */
    @SuppressWarnings("unchecked")
    private String seed(Map<String, Object> risorsa) {
        Object s = risorsa.get("Seed");
        if (s instanceof List && ((List<Object>) s).size() >= 2) {
            Object v = ((List<Object>) s).get(1);
            return v == null ? "" : String.valueOf(v);
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void scriviSeed(Map<String, Object> risorsa, String valore) {
        if (risorsa == null || valore == null || valore.isEmpty()) {
            return;
        }
        Object s = risorsa.get("Seed");
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

package it.atlante.finestra;

import it.atlante.json.Json;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
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
 * La scheda di un insediamento.
 *
 * Il salvataggio tiene in {@code SettlementStatesV2} fino a cento insediamenti:
 * quelli che il giocatore ha visitato, con nome e seme, e i suoi — riconoscibili
 * perche' hanno popolazione, statistiche e perk. La sezione mostra solo quelli
 * che hanno davvero qualcosa dentro.
 *
 * I gruppi sono quattro, su due colonne:
 *
 * <pre>
 *   INFORMAZIONI                  STATISTICHE
 *     nome, popolazione, razza      gli otto valori del gioco
 *     proprietario
 *
 *   SEMI E LUOGO                  EDIFICI E GIUDIZI
 *     seme, identificativo,         prossimo miglioramento,
 *     indirizzo, posizione          ultimo giudizio, perk
 * </pre>
 */
public final class PannelloInsediamento extends JPanel {

    /** Le otto statistiche di un insediamento, nell'ordine del salvataggio. */
    private static final String[] STATS = {
            "Statistica 1", "Statistica 2", "Statistica 3", "Statistica 4",
            "Statistica 5", "Statistica 6", "Statistica 7", "Statistica 8",
    };

    private static final String[] CLASSI = {
            "None", "A", "B", "C", "D", "E", "F", "S",
    };

    private static final String[] GIUDIZI = {
            "None", "Positive", "Negative", "Choice", "Crime", "Dispute",
    };

    private final Icone icone;
    private final Runnable suModifica;

    private final JLabel icona = new JLabel();
    private final JLabel nome = new JLabel();
    private final JLabel sotto = new JLabel();

    private final JTextField nomeDato = new JTextField();
    private final JTextField popolazione = new JTextField();
    private final JTextField proprietario = new JTextField();
    private final JComboBox<String> razza = new JComboBox<String>(
            new String[]{"None", "Gek", "Korvax", "Vy'keen"});

    private final List<JTextField> stats = new ArrayList<JTextField>();

    private final JTextField seed = new JTextField();
    private final JTextField identificativo = new JTextField();
    private final JTextField indirizzo = new JTextField();
    private final JTextField posizioneX = new JTextField();
    private final JTextField posizioneY = new JTextField();
    private final JTextField posizioneZ = new JTextField();

    private final JComboBox<String> prossimaClasse = new JComboBox<String>(CLASSI);
    private final JTextField prossimoIndice = new JTextField();
    private final JTextField prossimoSeme = new JTextField();
    private final JComboBox<String> giudizio = new JComboBox<String>(GIUDIZI);
    private final JTextField perk = new JTextField();
    private final JLabel elencoPerk = new JLabel();

    private Object radice;
    private int indice = -1;
    private boolean caricando;

    public PannelloInsediamento(Icone icone, Runnable suModifica) {
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
        icona.setPreferredSize(new Dimension(48, 48));
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
        sinistra.add(riga("Popolazione", numero(popolazione, "Population")));
        razza.setFont(razza.getFont().deriveFont(12f));
        sinistra.add(riga("Razza", razza));
        proprietario.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        proprietario.setEditable(false);
        sinistra.add(riga("Proprietario", proprietario));

        sinistra.add(Box.createVerticalStrut(10));
        sinistra.add(titolo("SEMI E LUOGO"));
        seed.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        sinistra.add(riga("Seme", seed));
        identificativo.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        sinistra.add(riga("Identificativo", identificativo));
        indirizzo.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        sinistra.add(riga("Indirizzo", indirizzo));
        sinistra.add(riga("Posizione X", numero(posizioneX, "PosX")));
        sinistra.add(riga("Posizione Y", numero(posizioneY, "PosY")));
        sinistra.add(riga("Posizione Z", numero(posizioneZ, "PosZ")));

        JPanel destra = colonna();
        destra.add(titolo("STATISTICHE"));
        for (String nomeStat : STATS) {
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
        destra.add(titolo("EDIFICI E GIUDIZI"));
        prossimaClasse.setFont(prossimaClasse.getFont().deriveFont(12f));
        destra.add(riga("Prossima classe", prossimaClasse));
        destra.add(riga("Prossimo indice", numero(prossimoIndice, "UpgradeIndex")));
        destra.add(riga("Prossimo seme", numero(prossimoSeme, "UpgradeSeed")));
        giudizio.setFont(giudizio.getFont().deriveFont(12f));
        destra.add(riga("Giudizio in attesa", giudizio));
        perk.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        destra.add(riga("Ultimo perk", perk));
        elencoPerk.setFont(elencoPerk.getFont().deriveFont(10.5f));
        elencoPerk.setForeground(Aspetto.TESTO_DEBOLE);
        elencoPerk.setAlignmentX(Component.LEFT_ALIGNMENT);
        elencoPerk.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        destra.add(elencoPerk);

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
        l.setPreferredSize(new Dimension(126, 24));
        r.add(l, BorderLayout.WEST);
        r.add(editor, BorderLayout.CENTER);
        return r;
    }

    private void collega() {
        nomeDato.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (caricando || insediamento() == null) {
                    return;
                }
                String v = nomeDato.getText().trim();
                insediamento().put("Name", v);
                nome.setText(v.isEmpty() ? "Insediamento " + (indice + 1) : v);
                suModifica.run();
            }
        });
        razza.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviAnnidato("Race", "AlienRace");
            }
        });
        prossimaClasse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviAnnidato("NextBuildingUpgradeClass", "BuildingClass");
            }
        });
        giudizio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviAnnidato("PendingJudgementType", "SettlementJudgementType");
            }
        });
        seed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviTesto("SeedValue", seed.getText().trim());
            }
        });
        identificativo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviTesto("UniqueId", identificativo.getText().trim());
            }
        });
        indirizzo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviTesto("UniverseAddress", indirizzo.getText().trim());
            }
        });
        perk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scriviTesto("LastJudgementPerkID", perk.getText().trim());
            }
        });
    }

    // ------------------------------------------------------------------

    public void mostra(Object radice, int i) {
        this.radice = radice;
        this.indice = i;
        caricando = true;
        try {
            Map<String, Object> s = insediamento();
            if (s == null) {
                svuota();
                return;
            }
            String dato = testo(s.get("Name"));
            nome.setText(dato.isEmpty() ? "Insediamento " + (i + 1) : dato);
            nomeDato.setText(dato);
            popolazione.setText(testo(s.get("Population")));
            scegli(razza, annidato(s, "Race", "AlienRace"));
            proprietario.setText(testo(annidato(s, "Owner", "USN")));

            seed.setText(testo(s.get("SeedValue")));
            identificativo.setText(testo(s.get("UniqueId")));
            indirizzo.setText(testo(s.get("UniverseAddress")));
            List<Object> pos = lista(s.get("Position"));
            posizioneX.setText(pos.size() > 0 ? testo(pos.get(0)) : "");
            posizioneY.setText(pos.size() > 1 ? testo(pos.get(1)) : "");
            posizioneZ.setText(pos.size() > 2 ? testo(pos.get(2)) : "");

            List<Object> valori = lista(s.get("Stats"));
            for (int k = 0; k < stats.size(); k++) {
                stats.get(k).setText(k < valori.size() ? testo(valori.get(k)) : "");
            }

            scegli(prossimaClasse, annidato(s, "NextBuildingUpgradeClass", "BuildingClass"));
            prossimoIndice.setText(testo(s.get("NextBuildingUpgradeIndex")));
            prossimoSeme.setText(testo(s.get("NextBuildingUpgradeSeedValue")));
            scegli(giudizio, annidato(s, "PendingJudgementType", "SettlementJudgementType"));
            perk.setText(testo(s.get("LastJudgementPerkID")));

            List<Object> perks = lista(s.get("Perks"));
            StringBuilder p = new StringBuilder("Perk: ");
            for (Object o : perks) {
                if (p.length() > 6) {
                    p.append(", ");
                }
                p.append(String.valueOf(o).replace("^", ""));
            }
            elencoPerk.setText(perks.isEmpty() ? "" : p.toString());

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
        // Nel gioco l'insediamento non ha un'illustrazione: il segno e' il
        // simbolo della razza che ci abita.
        String r = String.valueOf(razza.getSelectedItem());
        String iconaRazza = Icone.iconaDiRisorsa(r);
        ImageIcon img = iconaRazza == null ? null : icone.perFile(iconaRazza, 48);
        icona.setIcon(img != null ? img : Icone.segno("I", 48, Aspetto.ACCENTO, true));
        StringBuilder s = new StringBuilder();
        String pop = popolazione.getText().trim();
        if (!pop.isEmpty()) {
            s.append("Popolazione ").append(pop);
        }
        if (r != null && !"None".equals(r)) {
            if (s.length() > 0) {
                s.append("  ·  ");
            }
            s.append(r);
        }
        sotto.setText(s.toString());
    }

    public void svuota() {
        radice = null;
        indice = -1;
        caricando = true;
        try {
            icona.setIcon(Icone.segno("·", 48, Aspetto.TESTO_DEBOLE, true));
            nome.setText("Nessun insediamento scelto");
            sotto.setText("");
            nomeDato.setText("");
            popolazione.setText("");
            proprietario.setText("");
            seed.setText("");
            identificativo.setText("");
            indirizzo.setText("");
            posizioneX.setText("");
            posizioneY.setText("");
            posizioneZ.setText("");
            prossimoIndice.setText("");
            prossimoSeme.setText("");
            perk.setText("");
            elencoPerk.setText("");
            for (JTextField campo : stats) {
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
    private Map<String, Object> insediamento() {
        if (radice == null || indice < 0) {
            return null;
        }
        Object elenco = Inventari.risolvi(radice,
                "BaseContext.PlayerStateData.SettlementStatesV2");
        if (!(elenco instanceof List)) {
            return null;
        }
        List<Object> l = (List<Object>) elenco;
        if (indice >= l.size()) {
            return null;
        }
        Object s = l.get(indice);
        return s instanceof Map ? (Map<String, Object>) s : null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> lista(Object v) {
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
    }

    @SuppressWarnings("unchecked")
    private String annidato(Map<String, Object> s, String campo, String dentro) {
        Object v = s.get(campo);
        if (v instanceof Map) {
            Object d = ((Map<String, Object>) v).get(dentro);
            return d == null ? null : String.valueOf(d);
        }
        return null;
    }

    /** Scrive dentro un campo annidato, senza sostituire l'oggetto. */
    @SuppressWarnings("unchecked")
    private void scriviAnnidato(String campo, String dentro) {
        if (caricando) {
            return;
        }
        Map<String, Object> s = insediamento();
        if (s == null) {
            return;
        }
        Object v = s.get(campo);
        if (!(v instanceof Map)) {
            return;
        }
        JComboBox<String> menu = "Race".equals(campo) ? razza
                : "NextBuildingUpgradeClass".equals(campo) ? prossimaClasse : giudizio;
        Object scelta = menu.getSelectedItem();
        if (scelta == null) {
            return;
        }
        ((Map<String, Object>) v).put(dentro, String.valueOf(scelta));
        aggiornaTesta();
        suModifica.run();
    }

    private void scriviTesto(String campo, String valore) {
        if (caricando || valore.isEmpty()) {
            return;
        }
        Map<String, Object> s = insediamento();
        if (s != null) {
            s.put(campo, valore);
            suModifica.run();
        }
    }

    @SuppressWarnings("unchecked")
    private void scriviStat(int posizione, String valore) {
        if (caricando || valore.isEmpty()) {
            return;
        }
        Map<String, Object> s = insediamento();
        if (s == null) {
            return;
        }
        Object v = s.get("Stats");
        if (!(v instanceof List)) {
            return;
        }
        List<Object> l = (List<Object>) v;
        if (posizione < l.size()) {
            l.set(posizione, new Json.Numero(valore));
            suModifica.run();
        }
    }

    private void scriviNumero(String campo, String valore) {
        if (caricando || valore.isEmpty()) {
            return;
        }
        Map<String, Object> s = insediamento();
        if (s == null) {
            return;
        }
        if (campo.startsWith("Pos")) {
            Object v = s.get("Position");
            if (v instanceof List) {
                int posizione = "PosX".equals(campo) ? 0 : "PosY".equals(campo) ? 1 : 2;
                List<Object> l = lista(v);
                if (posizione < l.size()) {
                    l.set(posizione, new Json.Numero(valore));
                    suModifica.run();
                }
            }
            return;
        }
        s.put(campo, new Json.Numero(valore));
        suModifica.run();
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

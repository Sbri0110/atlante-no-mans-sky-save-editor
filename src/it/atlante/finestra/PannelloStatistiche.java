package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
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
 * Le statistiche delle cose: navi, multi-tool e veicoli.
 *
 * Danno, scudo, iperguida, manovrabilita': i valori che nel gioco si vedono
 * nella scheda di una nave e che il vecchio editor permette di modificare.
 *
 * <b>Dove stanno, verificato su un salvataggio vero:</b>
 * <ul>
 *   <li>le navi: {@code ShipOwnership[i].Inventory.BaseStatValues}, con gli
 *       identificativi {@code ^SHIP_DAMAGE}, {@code ^SHIP_SHIELD},
 *       {@code ^SHIP_HYPERDRIVE}, {@code ^SHIP_AGILE};</li>
 *   <li>i multi-tool: {@code Multitools[i].Store.BaseStatValues}, con
 *       {@code ^WEAPON_DAMAGE}, {@code ^WEAPON_MINING}, {@code ^WEAPON_SCAN};</li>
 *   <li>i veicoli: {@code VehicleOwnership[i].Inventory.BaseStatValues}.</li>
 * </ul>
 *
 * Gli identificativi non sono tradotti a occhio: sono quelli che il salvataggio
 * usa davvero, letti dal file. Le etichette italiane vengono da li'.
 */
public final class PannelloStatistiche extends JPanel {

    /** Un valore da modificare: etichetta e identificativo. */
    private static final class Valore {
        final String nome;
        final String id;

        Valore(String nome, String id) {
            this.nome = nome;
            this.id = id;
        }
    }

    /** Le statistiche di una nave. */
    private static final List<Valore> NAVI = new ArrayList<Valore>();

    /** Le statistiche di un multi-tool. */
    private static final List<Valore> ARMI = new ArrayList<Valore>();

    /** Le statistiche di un veicolo. */
    private static final List<Valore> VEICOLI = new ArrayList<Valore>();

    static {
        NAVI.add(new Valore("Danno", "^SHIP_DAMAGE"));
        NAVI.add(new Valore("Scudo", "^SHIP_SHIELD"));
        NAVI.add(new Valore("Iperguida", "^SHIP_HYPERDRIVE"));
        NAVI.add(new Valore("Manovrabilità", "^SHIP_AGILE"));

        ARMI.add(new Valore("Danno", "^WEAPON_DAMAGE"));
        ARMI.add(new Valore("Estrazione", "^WEAPON_MINING"));
        ARMI.add(new Valore("Scansione", "^WEAPON_SCAN"));

        VEICOLI.add(new Valore("Danno", "^VEHICLE_DAMAGE"));
        VEICOLI.add(new Valore("Scudo", "^VEHICLE_SHIELD"));
        VEICOLI.add(new Valore("Manovrabilità", "^VEHICLE_AGILE"));
    }

    private final Catalogo catalogo;
    private final Icone icone;
    private final Runnable suModifica;

    private final JPanel contenitore = new Colonna();
    private final JLabel contatore = new JLabel();

    private Map<String, Object> stato;

    public PannelloStatistiche(Catalogo catalogo, Icone icone, Runnable suModifica) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.FONDO);
        contenitore.setBackground(Aspetto.FONDO);
        contenitore.setBorder(BorderFactory.createEmptyBorder(14, 16, 20, 16));

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Aspetto.FONDO_ALTO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Aspetto.BORDO),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        JLabel titolo = new JLabel("STATISTICHE");
        titolo.setFont(Aspetto.monospaziato(10.5f, Font.BOLD));
        titolo.setForeground(Aspetto.ACCENTO);
        barra.add(titolo, BorderLayout.WEST);
        contatore.setFont(Aspetto.monospaziato(11, Font.PLAIN));
        contatore.setForeground(Aspetto.TESTO_TENUE);
        barra.add(contatore, BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);

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

    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public void mostra(Object radice) {
        stato = null;
        if (radice instanceof Map) {
            // Il contesto attivo: navi, multi-tool e veicoli di una spedizione
            // hanno le loro statistiche, non quelle della partita principale.
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

    @SuppressWarnings("unchecked")
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

        int quante = 0;
        quante += gruppo(griglia, "ShipOwnership", "Inventory", NAVI, "UI-SHIPICON", "Name");
        quante += gruppo(griglia, "Multitools", "Store", ARMI, "UI-WEAPONICON", "Name");
        quante += gruppo(griglia, "VehicleOwnership", "Inventory", VEICOLI,
                "PRODUCT-AM_EXOCRAFTTREE", "Name");

        if (quante == 0) {
            contenitore.add(new JLabel("Nessuna statistica da mostrare."));
            contatore.setText("");
        } else {
            contenitore.add(griglia);
            contatore.setText(quante + " schede");
        }
        contenitore.revalidate();
        contenitore.repaint();
    }

    /**
     * Aggiunge una scheda per ogni elemento di una lista.
     *
     * @param contenitoreLista il campo che contiene la lista (ShipOwnership...)
     * @param doveStanno       il campo interno con BaseStatValues (Inventory, Store)
     * @return quante schede ha aggiunto
     */
    @SuppressWarnings("unchecked")
    private int gruppo(JPanel griglia, String contenitoreLista, String doveStanno,
                       List<Valore> valori, String icona, String campoNome) {
        Object elenco = stato.get(contenitoreLista);
        if (!(elenco instanceof List)) {
            return 0;
        }
        int quante = 0;
        int indice = 0;
        for (Object elemento : (List<Object>) elenco) {
            indice++;
            if (!(elemento instanceof Map)) {
                continue;
            }
            Map<String, Object> e = (Map<String, Object>) elemento;
            Object dentro = e.get(doveStanno);
            if (!(dentro instanceof Map)) {
                continue;
            }
            Object statistiche = ((Map<String, Object>) dentro).get("BaseStatValues");
            if (!(statistiche instanceof List) || ((List<Object>) statistiche).isEmpty()) {
                continue;
            }
            String nome = String.valueOf(e.get(campoNome));
            if (nome == null || nome.trim().isEmpty() || "null".equals(nome)) {
                nome = "Senza nome " + indice;
            }
            griglia.add(scheda(nome.trim(), icona, (List<Object>) statistiche, valori));
            quante++;
        }
        return quante;
    }

    @SuppressWarnings("unchecked")
    private JPanel scheda(String nome, String icona, List<Object> statistiche, List<Valore> valori) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Aspetto.PANNELLO);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Aspetto.BORDO),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JPanel testa = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 9, 0));
        testa.setOpaque(false);
        JLabel immagine = new JLabel();
        ImageIcon img = icone.perFile(icona + ".PNG", 26);
        immagine.setIcon(img != null ? img : Icone.segno(nome.substring(0, 1), 26,
                Aspetto.ACCENTO, true));
        testa.add(immagine);
        JLabel titolo = new JLabel(nome);
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 13f));
        titolo.setForeground(Aspetto.TESTO);
        testa.add(titolo);
        testa.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        p.add(testa, BorderLayout.NORTH);

        JPanel righe = new JPanel();
        righe.setLayout(new BoxLayout(righe, BoxLayout.Y_AXIS));
        righe.setOpaque(false);

        for (Valore v : valori) {
            Map<String, Object> voce = cercaStatistica(statistiche, v.id);
            if (voce == null) {
                continue;
            }
            righe.add(riga(v, voce));
        }
        p.add(righe, BorderLayout.CENTER);
        return p;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cercaStatistica(List<Object> statistiche, String id) {
        for (Object o : statistiche) {
            if (o instanceof Map
                    && id.equals(String.valueOf(((Map<String, Object>) o).get("BaseStatID")))) {
                return (Map<String, Object>) o;
            }
        }
        return null;
    }

    private JPanel riga(Valore v, final Map<String, Object> voce) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel nome = new JLabel(v.nome);
        nome.setFont(nome.getFont().deriveFont(12f));
        nome.setForeground(Aspetto.TESTO_TENUE);
        nome.setPreferredSize(new Dimension(150, 24));
        nome.setToolTipText(v.id);
        r.add(nome, BorderLayout.WEST);

        final JTextField campo = new JTextField(testoDi(voce));
        campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
        campo.setPreferredSize(new Dimension(130, 26));
        campo.setToolTipText(v.id + "  —  modifica e premi Invio");
        campo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scrivi(voce, campo.getText().trim());
            }
        });
        r.add(campo, BorderLayout.CENTER);
        return r;
    }

    private String testoDi(Map<String, Object> voce) {
        Object valore = voce.get("Value");
        if (valore instanceof Json.Numero) {
            String t = ((Json.Numero) valore).testo();
            return t.endsWith(".0") ? t.substring(0, t.length() - 2) : t;
        }
        return String.valueOf(valore);
    }

    /**
     * Scrive il valore.
     *
     * I valori di queste statistiche sono decimali: scrivendo "1000" al posto di
     * "1000.0" il salvataggio cambierebbe tipo e il gioco potrebbe non
     * riconoscerlo piu'. Si conserva quindi la forma decimale.
     */
    private void scrivi(Map<String, Object> voce, String testo) {
        String valore = testo;
        if (!valore.contains(".") && !valore.contains(",")) {
            valore = valore + ".0";
        }
        valore = valore.replace(',', '.');
        voce.put("Value", new Json.Numero(valore));
        suModifica.run();
    }

    /** Quante schede mostra: serve al collaudo. */
    public int quanteSchede() {
        int n = 0;
        for (Component c : contenitore.getComponents()) {
            if (c instanceof JPanel) {
                n += ((JPanel) c).getComponentCount();
            }
        }
        return n;
    }
}

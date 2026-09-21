package it.atlante.finestra;

import it.atlante.json.Json;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traguardi e fazioni: i progressi del viaggiatore, uno per uno.
 *
 * Nel salvataggio i traguardi non sono campi con un nome leggibile: sono voci di
 * una tabella di statistiche, ognuna con un identificativo in stile
 * programmatore ({@code ^DIST_WARP}, {@code ^PIRATES_KILLED}) e un valore.
 *
 * Due scelte, entrambe per non ripetere un errore gia' fatto oggi:
 *
 * <ul>
 *   <li>l'<b>identificativo vero</b> si vede sempre, accanto all'etichetta. Le
 *       etichette in italiano ci sono solo dove la corrispondenza e' sicura:
 *       tradurre a caso aveva gia' prodotto due identificativi inventati nella
 *       scheda delle stazioni, che non esistevano;</li>
 *   <li>si mostrano <b>tutte</b> le statistiche, non solo quelle con
 *       un'etichetta. Sono 471 nel gruppo globale: nasconderne una parte
 *       significa togliere la possibilita' di modificarle.</li>
 * </ul>
 *
 * Le statistiche stanno in {@code PlayerStateData.Stats}: una lista di gruppi,
 * il primo con indirizzo 0 (globale), gli altri con l'indirizzo di un sistema.
 * Qui si mostra il gruppo globale.
 */
public final class PannelloTraguardi extends JPanel {

    /** Le etichette di cui sono sicuro. Le altre restano con l'identificativo. */
    private static final Map<String, String> ETICHETTE = new LinkedHashMap<String, String>();

    static {
        etichetta("^DIST_WARP", "Distanza percorsa in warp");
        etichetta("^DIST_WALKED", "Distanza percorsa a piedi");
        etichetta("^EXTREME_WALK", "Camminata in condizioni estreme");
        etichetta("^EX_HOT_WALK", "Camminata su mondi torridi");
        etichetta("^EX_COLD_WALK", "Camminata su mondi ghiacciati");
        etichetta("^TIMES_IN_SPACE", "Volte nello spazio");
        etichetta("^BLACKHOLE_WARPS", "Salti attraverso buchi neri");
        etichetta("^SHIPS_BOUGHT", "Astronavi acquistate");
        etichetta("^SENT_SHIP_CLAIM", "Astronavi abbandonate rivendicate");
        etichetta("^PIRATES_KILLED", "Pirati uccisi");
        etichetta("^ENEMIES_KILLED", "Nemici uccisi");
        etichetta("^WALKERS_KILLED", "Walker sentinella distrutti");
        etichetta("^QUADS_KILLED", "Quad sentinella distrutti");
        etichetta("^SPIDERS_KILLED", "Ragni distrutti");
        etichetta("^FLORA_KILLED", "Piante distrutte");
        etichetta("^ROAD_KILL", "Creature investite");
        etichetta("^DEATHS", "Volte in cui sei morto");
        etichetta("^DEATH_ROBOT", "Morti per sentinelle");
        etichetta("^WORDS_LEARNT", "Parole apprese");
        etichetta("^TWORDS_LEARNT", "Parole apprese (viaggiatore)");
        etichetta("^PLANTS_GATHERED", "Piante raccolte");
        etichetta("^RARE_SCANNED", "Creature rare scansionate");
        etichetta("^BIG_SCAN_MIN", "Unità guadagnate scansionando");
        etichetta("^TDONE_MISSIONS", "Missioni completate");
        etichetta("^TGDONE_MISSIONS", "Missioni di gilda completate");
        etichetta("^SP_POI_MISSIONS", "Contratti di recupero");
        etichetta("^PIRATE_MISSIONS", "Missioni contro i pirati");
        etichetta("^PIRATE_MYSTERY", "Misteri dei pirati risolti");
        etichetta("^PIRATES_LORE", "Storie dei pirati raccolte");
        etichetta("^TRA_STANDING", "Reputazione con i Gek");
        etichetta("^EXP_STANDING", "Reputazione con i Korvax");
        etichetta("^WAR_STANDING", "Reputazione con i Vy'keen");
        etichetta("^BUI_STANDING", "Reputazione con gli Autofagi");
        etichetta("^PIR_STAND", "Reputazione con i Fuorilegge");
        etichetta("^TGUILD_STAND", "Gilda dei mercanti");
        etichetta("^EGUILD_STAND", "Gilda degli esploratori");
        etichetta("^WGUILD_STAND", "Gilda dei guerrieri");
        etichetta("^STATIONS_OWNED", "Stazioni possedute");
        etichetta("^STATION_VISITED", "Stazioni visitate");
        etichetta("^SENT_SHIP_CLAIM", "Astronavi sentinella rivendicate");
    }

    private static void etichetta(String id, String nome) {
        ETICHETTE.put(id, nome);
    }

    private final Runnable suModifica;

    private final JPanel contenitore = new Colonna();
    private final JLabel contatore = new JLabel();
    private final JTextField cerca = new JTextField();

    private Object radice;
    private Map<String, Object> stato;
    private final List<String> identificativi = new ArrayList<String>();

    public PannelloTraguardi(Runnable suModifica) {
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.PANNELLO);
        contenitore.setBackground(Aspetto.PANNELLO);
        contenitore.setBorder(BorderFactory.createEmptyBorder(10, 14, 20, 14));

        add(costruisciBarra(), BorderLayout.NORTH);

        JScrollPane scorrimento = new JScrollPane(contenitore);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(18);
        add(scorrimento, BorderLayout.CENTER);
    }

    private static final class Colonna extends JPanel implements Scrollable {
        Colonna() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Aspetto.PANNELLO);
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

        cerca.setPreferredSize(new Dimension(190, 28));
        cerca.setToolTipText("Cerca un traguardo per nome o identificativo");
        cerca.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disegna();
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
        this.radice = radice;
        stato = null;
        identificativi.clear();
        if (radice instanceof Map) {
            Object base = ((Map<String, Object>) radice).get("BaseContext");
            Object psd = base instanceof Map ? ((Map<String, Object>) base).get("PlayerStateData") : null;
            if (psd instanceof Map) {
                stato = (Map<String, Object>) psd;
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

        List<Map<String, Object>> voci = vociGlobali();
        String filtro = cerca.getText() == null ? "" : cerca.getText().trim().toLowerCase();
        int mostrate = 0;

        for (Map<String, Object> voce : voci) {
            String id = String.valueOf(voce.get("Id"));
            String etichetta = ETICHETTE.get(id);
            String cercabile = (etichetta == null ? "" : etichetta + " ") + id;
            if (!filtro.isEmpty() && !cercabile.toLowerCase().contains(filtro)) {
                continue;
            }
            contenitore.add(new Riga(id, etichetta, voce));
            mostrate++;
        }
        if (mostrate == 0) {
            contatore.setText("");
            contenitore.add(new JLabel("Nessun traguardo corrisponde alla ricerca."));
        } else {
            contatore.setText(mostrate + (mostrate == 1 ? " traguardo" : " traguardi"));
        }
        contenitore.revalidate();
        contenitore.repaint();
    }

    /** Le voci di statistica del gruppo con indirizzo 0. */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> vociGlobali() {
        List<Map<String, Object>> fuori = new ArrayList<Map<String, Object>>();
        Object stats = stato.get("Stats");
        if (!(stats instanceof List)) {
            return fuori;
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
                if (voce instanceof Map) {
                    fuori.add((Map<String, Object>) voce);
                }
            }
            // Ci sono piu' gruppi con indirizzo 0: senza fermarsi al primo le
            // stesse voci comparivano piu' volte, e l'elenco risultava di 3645
            // righe invece di 471.
            break;
        }
        return fuori;
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

    // ------------------------------------------------------------------

    private void massimizza() {
        int toccati = 0;
        for (String id : new String[]{"^TRA_STANDING", "^EXP_STANDING", "^WAR_STANDING",
                "^BUI_STANDING", "^PIR_STAND", "^TGUILD_STAND", "^EGUILD_STAND",
                "^WGUILD_STAND"}) {
            for (Map<String, Object> voce : vociGlobali()) {
                if (!id.equals(String.valueOf(voce.get("Id")))) {
                    continue;
                }
                Object valore = voce.get("Value");
                if (valore instanceof Map) {
                    ((Map<String, Object>) valore).put("IntValue", new Json.Numero("100"));
                    toccati++;
                }
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

    /** Quante statistiche mostra: serve al collaudo. */
    public int quante() {
        return contenitore.getComponentCount();
    }

    // ------------------------------------------------------------------

    /** Una riga: un traguardo con il suo valore modificabile. */
    private final class Riga extends JPanel {

        Riga(final String id, String etichetta, final Map<String, Object> voce) {
            setLayout(new BorderLayout(12, 0));
            setBackground(Aspetto.PANNELLO);
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 8));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

            JPanel testi = new JPanel(new BorderLayout(10, 0));
            testi.setOpaque(false);
            // Con un'etichetta: nome in chiaro a sinistra e identificativo a
            // destra. Senza: solo l'identificativo, che ripeterlo due volte
            // non aiuta nessuno.
            if (etichetta != null) {
                JLabel nome = new JLabel(etichetta);
                nome.setFont(nome.getFont().deriveFont(Font.PLAIN, 12f));
                nome.setForeground(Aspetto.TESTO);
                nome.setPreferredSize(new Dimension(280, 24));
                testi.add(nome, BorderLayout.WEST);
                JLabel identificativo = new JLabel(id);
                identificativo.setFont(Aspetto.monospaziato(10.5f, Font.PLAIN));
                identificativo.setForeground(Aspetto.TESTO_DEBOLE);
                testi.add(identificativo, BorderLayout.CENTER);
            } else {
                JLabel identificativo = new JLabel(id);
                identificativo.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
                identificativo.setForeground(Aspetto.TESTO_TENUE);
                testi.add(identificativo, BorderLayout.WEST);
            }
            add(testi, BorderLayout.CENTER);

            final JTextField campo = new JTextField(String.valueOf(valoreDi(voce)));
            campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
            campo.setHorizontalAlignment(SwingConstants.RIGHT);
            campo.setPreferredSize(new Dimension(150, 26));
            campo.setToolTipText("Modifica e premi Invio");
            campo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object valore = voce.get("Value");
                    if (valore instanceof Map) {
                        ((Map<String, Object>) valore).put("IntValue",
                                new Json.Numero(campo.getText().trim()));
                        suModifica.run();
                    }
                }
            });
            add(campo, BorderLayout.EAST);
        }

        @SuppressWarnings("unchecked")
        private long valoreDi(Map<String, Object> voce) {
            Object valore = voce.get("Value");
            if (valore instanceof Map) {
                return numero(((Map<String, Object>) valore).get("IntValue"));
            }
            return numero(valore);
        }
    }
}

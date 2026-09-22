package it.atlante.finestra;

import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * I premi delle spedizioni, con la casella per sbloccarli uno per uno.
 *
 * Nel vecchio editor erano una tabella piatta di 293 righe: per sbloccare un
 * premio preciso bisognava cercarlo in mezzo a tutti. Qui sono raggruppati per
 * spedizione, cosi' si vede a colpo d'occhio cosa manca a una spedizione, e
 * ogni gruppo ha il suo pulsante per sbloccarla tutta.
 *
 * Tre livelli di azione, dal piu' fine al piu' grosso:
 * <ul>
 *   <li>la <b>casella</b> di una riga: sblocca quel singolo premio;</li>
 *   <li>il pulsante <b>sblocca questa spedizione</b>: tutta la spedizione;</li>
 *   <li>il pulsante in alto: tutte le spedizioni e i drop Twitch.</li>
 * </ul>
 *
 * Lo stato di sblocco sta nel salvataggio, in
 * {@code CommonStateData.EarnedSeasonSpecialRewards}: l'elenco degli
 * identificativi gia' ottenuti. Non si tiene una copia: si legge e si scrive
 * direttamente quella lista, cosi' non puo' andare fuori sincrono.
 */
public final class PannelloPremi extends JPanel {

    private final Catalogo catalogo;
    private final Icone icone;
    private final Runnable suModifica;

    private final JPanel contenitore = new Colonna();
    private final JLabel contatore = new JLabel();
    private final JTextField cerca = new JTextField();
    private final JLabel vuoto = new JLabel("", JLabel.CENTER);

    private List<Premi.Spedizione> spedizioni = new ArrayList<Premi.Spedizione>();
    private Set<String> sbloccati = new LinkedHashSet<String>();
    /** Le liste del salvataggio su cui scrivere, per poterle aggiornare. */
    private List<List<String>> listeDaScrivere = new ArrayList<List<String>>();
    private boolean mostraTwitch;

    /**
     * Vero se il premio risulta gia' ottenuto.
     *
     * Si guardano due cose: l'identificativo di rewards.xml e quello che il
     * salvataggio usa davvero per la stessa ricompensa. Sono elenchi diversi —
     * {@code ^EXPD_EGG_23} contro {@code ^RS_S23_EGG} — e senza il secondo
     * confronto il pannello non riconosceva nemmeno le spedizioni finite.
     */
    private boolean sbloccato(Premi.Premio premio) {
        if (sbloccati.contains(premio.id)) {
            return true;
        }
        return premio.idGioco != null && sbloccati.contains(premio.idGioco);
    }

    /** L'identificativo da scrivere per un premio: quello del gioco se c'e'. */
    private String idDaScrivere(Premi.Premio premio) {
        return premio.idGioco != null ? premio.idGioco : premio.id;
    }

    public PannelloPremi(Catalogo catalogo, Icone icone, Runnable suModifica) {
        this.catalogo = catalogo;
        this.icone = icone;
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

        vuoto.setForeground(Aspetto.TESTO_DEBOLE);
        vuoto.setFont(vuoto.getFont().deriveFont(12.5f));
    }

    /** Contenitore che si adatta in larghezza ma non in altezza. */
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

    // ------------------------------------------------------------------

    private JPanel costruisciBarra() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setBackground(Aspetto.FONDO_ALTO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Aspetto.BORDO),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JPanel schede = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        schede.setOpaque(false);
        final javax.swing.JToggleButton sped = new javax.swing.JToggleButton("Spedizioni");
        final javax.swing.JToggleButton twit = new javax.swing.JToggleButton("Twitch Drops");
        sped.setSelected(true);
        sped.setFont(sped.getFont().deriveFont(12f));
        twit.setFont(twit.getFont().deriveFont(12f));
        javax.swing.ButtonGroup gruppo = new javax.swing.ButtonGroup();
        gruppo.add(sped);
        gruppo.add(twit);
        sped.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mostraTwitch = false;
                disegna();
            }
        });
        twit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mostraTwitch = true;
                disegna();
            }
        });
        schede.add(sped);
        schede.add(twit);
        barra.add(schede, BorderLayout.WEST);

        JPanel destra = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        destra.setOpaque(false);
        contatore.setFont(Aspetto.monospaziato(11, Font.PLAIN));
        contatore.setForeground(Aspetto.TESTO_TENUE);
        destra.add(contatore);

        cerca.setPreferredSize(new Dimension(190, 28));
        cerca.setToolTipText("Cerca un premio per nome");
        cerca.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disegna();
            }
        });
        destra.add(cerca);

        JButton tutto = new JButton("Sblocca tutti i mancanti");
        tutto.setFont(tutto.getFont().deriveFont(11.5f));
        tutto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sbloccaTutto();
            }
        });
        destra.add(tutto);
        barra.add(destra, BorderLayout.EAST);
        return barra;
    }

    // ------------------------------------------------------------------

    /**
     * Mostra i premi, leggendo lo stato dal salvataggio aperto.
     *
     * Lo stato delle ricompense NON sta in un solo posto. Cercarlo solo in
     * EarnedSeasonSpecialRewards — che contiene le 24 ricompense speciali —
     * faceva apparire 11 premi ottenuti su 293 quando l'utente li aveva tutti:
     * il vecchio editor, che li legge tutti, ne mostrava 293 su 293.
     *
     * Le liste da guardare, in entrambi i contesti (partita e spedizione):
     *   - RedeemedSeasonRewards: le ricompense riscattate, con gli stessi
     *     identificativi di rewards.xml (^EXPD_*);
     *   - KnownProducts e KnownSpecials: i pezzi conosciuti, dove il gioco
     *     registra la maggior parte delle ricompense;
     *   - EarnedSeasonSpecialRewards: le ricompense speciali, con
     *     identificativi propri (^RS_S23_EGG).
     */
    public void mostra(Object radice, File filePremi) {
        sbloccati = new LinkedHashSet<String>();
        listeDaScrivere = new ArrayList<List<String>>();

        // Si guardano tutti e due i contesti, perche' una ricompensa riscattata
        // in partita vale anche in spedizione e viceversa. Il contesto attivo
        // viene pero' per primo, cosi' l'ordine delle liste scritte segue
        // quello del salvataggio aperto.
        String attivo = Inventari.contesto(radice);
        for (String contesto : new String[]{attivo,
                "BaseContext".equals(attivo) ? "ExpeditionContext" : "BaseContext"}) {
            Object c = radice instanceof Map
                    ? ((Map<String, Object>) radice).get(contesto) : null;
            Object psd = c instanceof Map
                    ? ((Map<String, Object>) c).get("PlayerStateData") : null;
            if (psd instanceof Map) {
                raccogli((Map<String, Object>) psd, "RedeemedSeasonRewards");
                raccogli((Map<String, Object>) psd, "KnownProducts");
                raccogli((Map<String, Object>) psd, "KnownSpecials");
            }
        }
        Object comune = radice instanceof Map
                ? ((Map<String, Object>) radice).get("CommonStateData") : null;
        if (comune instanceof Map) {
            raccogli((Map<String, Object>) comune, "EarnedSeasonSpecialRewards");
        }

        try {
            spedizioni = Premi.carica(filePremi);
        } catch (IOException e) {
            spedizioni = new ArrayList<Premi.Spedizione>();
        }
        disegna();
    }

    /** Aggiunge al gruppo degli sbloccati il contenuto di una lista del salvataggio. */
    @SuppressWarnings("unchecked")
    private void raccogli(Map<String, Object> dove, String nome) {
        Object elenco = dove.get(nome);
        if (!(elenco instanceof List)) {
            return;
        }
        List<Object> lista = (List<Object>) elenco;
        List<String> testi = new ArrayList<String>();
        for (Object o : lista) {
            String testo = String.valueOf(o);
            sbloccati.add(testo);
            testi.add(testo);
        }
        listeDaScrivere.add(testi);
    }

    private void disegna() {
        contenitore.removeAll();
        String filtro = cerca.getText() == null ? "" : cerca.getText().trim().toLowerCase();

        if (mostraTwitch) {
            List<Premi.Premio> tw = Premi.twitch();
            JPanel gruppo = new Gruppo("Twitch Drops", null, tw);
            contenitore.add(gruppo);
        } else {
            for (Premi.Spedizione s : spedizioni) {
                if (filtro.isEmpty() || contiene(s, filtro)) {
                    contenitore.add(new Gruppo(s.nome, s, s.premi));
                }
            }
        }
        if (contenitore.getComponentCount() == 0) {
            vuoto.setText("Nessun premio corrisponde alla ricerca.");
            contenitore.add(vuoto);
        }
        aggiornaContatore();
        contenitore.revalidate();
        contenitore.repaint();
    }

    private boolean contiene(Premi.Spedizione s, String filtro) {
        for (Premi.Premio p : s.premi) {
            if (p.nome.toLowerCase().contains(filtro) || p.id.toLowerCase().contains(filtro)) {
                return true;
            }
        }
        return s.nome.toLowerCase().contains(filtro);
    }

    private void aggiornaContatore() {
        int totale = 0;
        int fatti = 0;
        for (Premi.Spedizione s : spedizioni) {
            for (Premi.Premio p : s.premi) {
                totale++;
                if (sbloccato(p)) {
                    fatti++;
                }
            }
        }
        if (mostraTwitch) {
            List<Premi.Premio> tw = Premi.twitch();
            int tf = 0;
            for (Premi.Premio p : tw) {
                if (sbloccato(p)) {
                    tf++;
                }
            }
            contatore.setText("Twitch Drops  " + tf + " / " + tw.size());
        } else {
            contatore.setText("Spedizioni  " + fatti + " / " + totale
                    + "   (mancanti " + (totale - fatti) + ")");
        }
    }

    // ------------------------------------------------------------------

    private void cambia(Premi.Premio p, boolean sbloccare) {
        if (listeDaScrivere.isEmpty()) {
            return;
        }
        // Si scrive nella prima lista disponibile. RedeemedSeasonRewards e' la
        // piu' adatta: e' quella che il gioco usa per le ricompense di
        // spedizione, e accetta gli stessi identificativi di rewards.xml.
        List<String> destinazione = listeDaScrivere.get(0);
        String id = p.idGioco != null ? p.idGioco : p.id;
        if (sbloccare) {
            if (!sbloccati.contains(id)) {
                sbloccati.add(id);
                destinazione.add(id);
            }
        } else {
            sbloccati.remove(id);
            destinazione.remove(id);
        }
        suModifica.run();
    }

    private void sbloccaGruppo(Premi.Spedizione s, List<Premi.Premio> premi) {
        for (Premi.Premio p : premi) {
            if (!sbloccato(p)) {
                cambia(p, true);
            }
        }
        disegna();
    }

    private void sbloccaTutto() {
        for (Premi.Spedizione s : spedizioni) {
            for (Premi.Premio p : s.premi) {
                if (!sbloccato(p)) {
                    cambia(p, true);
                }
            }
        }
        for (Premi.Premio p : Premi.twitch()) {
            if (!sbloccato(p)) {
                cambia(p, true);
            }
        }
        disegna();
    }

    /** Quanti premi sbloccati: serve al collaudo. */
    public int quantiSbloccati() {
        return sbloccati.size();
    }

    // ------------------------------------------------------------------

    /** Un gruppo richiudibile: una spedizione con i suoi premi. */
    private final class Gruppo extends JPanel {

        private final JPanel corpo = new JPanel();
        private final JLabel freccia = new JLabel("▾");
        private final boolean costruito;

        Gruppo(String titolo, final Premi.Spedizione spedizione, List<Premi.Premio> premi) {
            setLayout(new BorderLayout());
            setBackground(Aspetto.PANNELLO);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

            int fatti = 0;
            for (Premi.Premio p : premi) {
                if (sbloccato(p)) {
                    fatti++;
                }
            }

            JPanel testa = new JPanel(new BorderLayout(10, 0));
            testa.setBackground(Aspetto.FONDO_ALTO);
            testa.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Aspetto.BORDO),
                    BorderFactory.createEmptyBorder(9, 12, 9, 12)));

            JPanel sinistra = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
            sinistra.setOpaque(false);
            freccia.setForeground(Aspetto.TESTO_DEBOLE);
            sinistra.add(freccia);
            JLabel nome = new JLabel(titolo);
            nome.setFont(nome.getFont().deriveFont(Font.BOLD, 13f));
            nome.setForeground(Aspetto.TESTO);
            sinistra.add(nome);
            JLabel stato = new JLabel(fatti + " / " + premi.size()
                    + (fatti == premi.size() ? "   completo" : ""));
            stato.setFont(Aspetto.monospaziato(10.5f, Font.PLAIN));
            stato.setForeground(fatti == premi.size() ? Aspetto.OK : Aspetto.TESTO_DEBOLE);
            sinistra.add(stato);
            testa.add(sinistra, BorderLayout.WEST);

            if (fatti < premi.size()) {
                JButton sblocca = new JButton("Sblocca questa spedizione");
                sblocca.setFont(sblocca.getFont().deriveFont(11f));
                sblocca.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        sbloccaGruppo(spedizione, premiDelGruppo(spedizione));
                    }
                });
                testa.add(sblocca, BorderLayout.EAST);
            }
            add(testa, BorderLayout.NORTH);

            corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
            corpo.setBackground(Aspetto.PANNELLO);
            for (Premi.Premio p : premi) {
                corpo.add(new Riga(p));
            }
            add(corpo, BorderLayout.CENTER);
            costruito = true;

            testa.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            testa.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    boolean visibile = corpo.isVisible();
                    corpo.setVisible(!visibile);
                    freccia.setText(visibile ? "▸" : "▾");
                    revalidate();
                    repaint();
                }
            });
        }

        private List<Premi.Premio> premiDelGruppo(Premi.Spedizione s) {
            return s == null ? Premi.twitch() : s.premi;
        }
    }

    // ------------------------------------------------------------------

    /** Una riga: un premio con la sua casella. */
    private final class Riga extends JPanel {

        Riga(final Premi.Premio premio) {
            setLayout(new BorderLayout(10, 0));
            setBackground(Aspetto.PANNELLO);
            setBorder(BorderFactory.createEmptyBorder(4, 26, 4, 14));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

            JLabel immagine = new JLabel();
            Catalogo.Voce voce = catalogo.voce(premio.id);
            ImageIcon img = voce == null ? null : icone.perFile(voce.icona, 26);
            immagine.setIcon(img != null ? img : Icone.segno("◆", 26, Aspetto.TESTO_DEBOLE, true));
            immagine.setPreferredSize(new Dimension(30, 28));
            add(immagine, BorderLayout.WEST);

            JLabel nome = new JLabel(premio.nome);
            nome.setFont(nome.getFont().deriveFont(12.5f));
            nome.setForeground(Aspetto.TESTO);
            nome.setToolTipText(premio.id);
            add(nome, BorderLayout.CENTER);

            JCheckBox casella = new JCheckBox();
            casella.setOpaque(false);
            casella.setSelected(sbloccati.contains(premio.id));
            casella.setToolTipText("Sblocca solo questo premio");
            casella.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cambia(premio, ((JCheckBox) e.getSource()).isSelected());
                }
            });
            add(casella, BorderLayout.EAST);
        }
    }
}

package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * I campi di una sezione, in forma leggibile.
 *
 * Il primo tentativo usava un albero: corretto e completo, ma illeggibile.
 * Un campo appariva come {@code SquadronPilots} con valore
 * {@code [0x013700C854F21221, PilotRank=10]}, cioe' il {@code toString} di Java.
 * Chi apre un editor vuole leggere "Piloti dello squadrone" e vedere un
 * interruttore dove c'e' un vero/falso, non un numero dove c'e' una quantita'.
 *
 * Qui ogni campo e' una riga con tre cose:
 * <ul>
 *   <li>l'<b>etichetta</b> in italiano, presa da {@link Etichette};</li>
 *   <li>il <b>valore</b>, formattato secondo il tipo;</li>
 *   <li>l'<b>editor</b> adatto: interruttore per i veri/falso, campo numerico
 *       per i numeri, scheda con icona di gioco per gli oggetti.</li>
 * </ul>
 *
 * I gruppi si aprono a richiesta: il salvataggio ha 739.012 campi, costruirli
 * tutti insieme non e' possibile. Si costruisce il primo livello e i figli
 * nascono al primo clic.
 */
public final class PannelloCampi extends JPanel {

    private final Catalogo catalogo;
    private final Icone icone;
    private final Runnable suModifica;
    private final Colonna contenitore = new Colonna();
    private final JLabel vuoto = new JLabel("", SwingConstants.CENTER);

    private int larghezzaEtichette = 230;

    /**
     * Colonna che si adatta alla larghezza della finestra ma non all'altezza.
     *
     * Serve a due cose insieme: le intestazioni dei gruppi devono arrivare fino
     * al bordo destro, e i gruppi non devono stirarsi in verticale quando il
     * contenuto e' piu' corto della finestra. Un {@code BorderLayout} risolve la
     * prima ma non la seconda; {@link javax.swing.Scrollable} risolve entrambe.
     */
    private static final class Colonna extends JPanel implements javax.swing.Scrollable {
        Colonna() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Aspetto.PANNELLO);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(java.awt.Rectangle visibile, int orientamento, int direzione) {
            return 18;
        }

        public int getScrollableBlockIncrement(java.awt.Rectangle visibile, int orientamento, int direzione) {
            return visibile.height;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public PannelloCampi(Catalogo catalogo, Icone icone, Runnable suModifica) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.PANNELLO);

        JPanel involucro = new JPanel(new BorderLayout());
        involucro.setBackground(Aspetto.PANNELLO);
        // In alto: la Colonna si adatta da sola alla larghezza della finestra.
        involucro.add(contenitore, BorderLayout.NORTH);

        JScrollPane scorrimento = new JScrollPane(involucro);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(18);
        scorrimento.setBackground(Aspetto.PANNELLO);
        add(scorrimento, BorderLayout.CENTER);

        vuoto.setForeground(Aspetto.TESTO_DEBOLE);
        vuoto.setFont(vuoto.getFont().deriveFont(12.5f));
    }

    // ------------------------------------------------------------------

    /** Mostra il contenuto di una sezione. */
    @SuppressWarnings("unchecked")
    public void mostra(Object radice) {
        contenitore.removeAll();
        if (!(radice instanceof Map)) {
            vuoto.setText("Questa sezione non ha campi da mostrare.");
            contenitore.add(vuoto);
            contenitore.revalidate();
            contenitore.repaint();
            return;
        }
        Map<String, Object> mappa = (Map<String, Object>) radice;
        if (mappa.isEmpty()) {
            vuoto.setText("Questa sezione non ha campi in questo salvataggio.");
            contenitore.add(vuoto);
            contenitore.revalidate();
            contenitore.repaint();
            return;
        }
        for (Map.Entry<String, Object> e : mappa.entrySet()) {
            contenitore.add(riga(e.getKey(), e.getValue(), 0, mappa));
        }
        contenitore.add(Box.createVerticalStrut(24));
        contenitore.revalidate();
        contenitore.repaint();
        // Si aprono i primi due livelli: partendo tutto chiuso la sezione
        // sembrerebbe vuota, e chi apre il programma la prima volta non sa che
        // i gruppi si aprono cliccando.
        espandiFinoA(2);
    }

    /** Apre i gruppi fino a una profondita', per le schermate e per la ricerca. */
    public void espandiFinoA(int profondita) {
        for (Component c : contenitore.getComponents()) {
            if (c instanceof Gruppo) {
                ((Gruppo) c).apriFinoA(profondita - 1);
            }
        }
    }

    /**
     * Porta in vista il primo campo che contiene il testo cercato.
     *
     * @return l'etichetta del campo trovato, o null
     */
    public String cerca(String testo) {
        if (testo == null || testo.trim().isEmpty()) {
            return null;
        }
        return cercaIn(contenitore, testo.trim().toLowerCase());
    }

    private String cercaIn(JPanel dove, String cercato) {
        for (Component c : dove.getComponents()) {
            if (c instanceof Gruppo) {
                Gruppo g = (Gruppo) c;
                if (g.etichetta.toLowerCase().contains(cercato)) {
                    g.apri();
                    g.scrollRectToVisible(new java.awt.Rectangle(0, 0, g.getWidth(), g.getHeight()));
                    return g.etichetta;
                }
                g.apri();
                String dentro = cercaIn(g.corpo, cercato);
                if (dentro != null) {
                    return dentro;
                }
            } else if (c instanceof Riga) {
                Riga r = (Riga) c;
                if (r.etichetta.toLowerCase().contains(cercato)) {
                    r.scrollRectToVisible(new java.awt.Rectangle(0, 0, r.getWidth(), r.getHeight()));
                    return r.etichetta;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Costruzione delle righe
    // ------------------------------------------------------------------

    /**
     * Costruisce la riga adatta a un valore.
     *
     * @param genitore il contenitore del valore (mappa o lista): serve a
     *                 scrivere la modifica nell'oggetto giusto. Senza di esso
     *                 l'editor cambierebbe una copia e il salvataggio resterebbe
     *                 invariato, senza alcun errore visibile.
     */
    private JComponent riga(String chiave, Object valore, int livello, Object genitore) {
        if (valore instanceof Map) {
            return new Gruppo(chiave, (Map<?, ?>) valore, livello);
        }
        if (valore instanceof List) {
            return new Gruppo(chiave, (List<?>) valore, livello);
        }
        return new Riga(chiave, valore, livello, genitore);
    }

    private int rientro(int livello) {
        return 14 + livello * 18;
    }

    // ------------------------------------------------------------------

    /** Un gruppo richiudibile: un campo che contiene altri campi. */
    private final class Gruppo extends JPanel {

        private final String etichetta;
        private final Object contenuto;
        private final int livello;
        final JPanel corpo = new JPanel();
        private final JLabel freccia = new JLabel();
        private final JLabel titolo = new JLabel();
        private final JLabel conteggio = new JLabel();
        private boolean costruito;
        private boolean aperto;

        Gruppo(String chiave, Map<?, ?> mappa, int livello) {
            this.etichetta = Etichette.leggi(chiave);
            this.contenuto = mappa;
            this.livello = livello;
            costruisci();
        }

        Gruppo(String chiave, List<?> lista, int livello) {
            this.etichetta = Etichette.leggi(chiave);
            this.contenuto = lista;
            this.livello = livello;
            costruisci();
        }

        private void costruisci() {
            setLayout(new BorderLayout());
            setBackground(Aspetto.PANNELLO);
            setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel testa = new JPanel(new BorderLayout(8, 0));
            testa.setBackground(Aspetto.PANNELLO);
            testa.setBorder(BorderFactory.createEmptyBorder(6, rientro(livello), 6, 14));
            testa.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

            freccia.setText("▸");
            freccia.setForeground(Aspetto.TESTO_DEBOLE);
            freccia.setFont(freccia.getFont().deriveFont(12f));
            freccia.setPreferredSize(new Dimension(14, 16));
            testa.add(freccia, BorderLayout.WEST);

            titolo.setText(etichetta);
            titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 12.5f));
            titolo.setForeground(Aspetto.TESTO);
            testa.add(titolo, BorderLayout.CENTER);

            int quanti = contenuto instanceof Map ? ((Map<?, ?>) contenuto).size() : ((List<?>) contenuto).size();
            conteggio.setText(quanti + (quanti == 1 ? " campo" : " campi"));
            conteggio.setFont(Aspetto.monospaziato(10.5f, Font.PLAIN));
            conteggio.setForeground(Aspetto.TESTO_DEBOLE);
            testa.add(conteggio, BorderLayout.EAST);

            testa.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    alterna();
                }
            });

            corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
            corpo.setBackground(Aspetto.PANNELLO);
            corpo.setVisible(false);

            add(testa, BorderLayout.NORTH);
            add(corpo, BorderLayout.CENTER);
        }

        void alterna() {
            if (aperto) {
                corpo.setVisible(false);
                freccia.setText("▸");
                aperto = false;
            } else {
                apri();
            }
            revalidate();
            repaint();
        }

        @SuppressWarnings("unchecked")
        void apri() {
            if (!costruito) {
                costruito = true;
                if (contenuto instanceof Map) {
                    Map<String, Object> mappa = (Map<String, Object>) contenuto;
                    for (Map.Entry<String, Object> e : mappa.entrySet()) {
                        corpo.add(riga(e.getKey(), e.getValue(), livello + 1, mappa));
                    }
                } else {
                    List<Object> lista = (List<Object>) contenuto;
                    // Le liste lunghe si troncano: cento slot identici non
                    // aggiungono niente da vedere, e costruirli tutti costa.
                    int mostrati = Math.min(lista.size(), 60);
                    for (int i = 0; i < mostrati; i++) {
                        corpo.add(riga(String.valueOf(i), lista.get(i), livello + 1, lista));
                    }
                    if (lista.size() > mostrati) {
                        JLabel resto = new JLabel("   ... e altri " + (lista.size() - mostrati) + " elementi");
                        resto.setFont(Aspetto.monospaziato(11, Font.PLAIN));
                        resto.setForeground(Aspetto.TESTO_DEBOLE);
                        resto.setBorder(BorderFactory.createEmptyBorder(6, rientro(livello + 1), 6, 14));
                        corpo.add(resto);
                    }
                }
            }
            corpo.setVisible(true);
            freccia.setText("▾");
            aperto = true;
            revalidate();
            repaint();
        }

        void apriFinoA(int profondita) {
            apri();
            if (profondita <= 0) {
                return;
            }
            for (Component c : corpo.getComponents()) {
                if (c instanceof Gruppo) {
                    ((Gruppo) c).apriFinoA(profondita - 1);
                }
            }
        }
    }

    // ------------------------------------------------------------------

    /** Una riga: un campo con un valore semplice e il suo editor. */
    private final class Riga extends JPanel {

        final String etichetta;
        private final Object originale;
        private final Object genitore;
        private final String nomeCampo;

        Riga(String chiave, Object valore, int livello, Object genitore) {
            this.etichetta = Etichette.leggi(chiave);
            this.originale = valore;
            this.genitore = genitore;
            this.nomeCampo = chiave;
            setLayout(new BorderLayout(12, 0));
            setBackground(Aspetto.PANNELLO);
            setBorder(BorderFactory.createEmptyBorder(3, rientro(livello), 3, 14));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

            JLabel nome = new JLabel(etichetta);
            nome.setFont(nome.getFont().deriveFont(12f));
            nome.setForeground(Aspetto.TESTO_TENUE);
            nome.setPreferredSize(new Dimension(larghezzaEtichette - rientro(livello), 26));
            nome.setToolTipText(chiave + (Etichette.conosciuto(chiave) ? "" : "  (nome non tradotto)"));
            add(nome, BorderLayout.WEST);

            add(creaEditor(chiave, valore), BorderLayout.CENTER);
        }

        private JComponent creaEditor(String chiave, Object valore) {
            if (valore instanceof Boolean) {
                JToggleButton interruttore = new JToggleButton();
                interruttore.setSelected(((Boolean) valore).booleanValue());
                interruttore.setText(((Boolean) valore).booleanValue() ? "sì" : "no");
                interruttore.setFont(interruttore.getFont().deriveFont(11.5f));
                interruttore.setPreferredSize(new Dimension(72, 26));
                interruttore.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JToggleButton t = (JToggleButton) e.getSource();
                        t.setText(t.isSelected() ? "sì" : "no");
                        scrivi(Boolean.valueOf(t.isSelected()));
                    }
                });
                JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
                p.setOpaque(false);
                p.add(interruttore);
                return p;
            }

            if (valore instanceof String) {
                String testo = (String) valore;
                Catalogo.Voce voce = catalogo.voce(testo);
                if (voce != null) {
                    return creaRigaOggetto(voce);
                }
            }

            JTextField campo = new JTextField(valore == null ? "" : testoDi(valore));
            campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
            campo.setHorizontalAlignment(SwingConstants.RIGHT);
            campo.setPreferredSize(new Dimension(240, 26));
            campo.setMaximumSize(new Dimension(240, 26));
            campo.setToolTipText("Modifica e premi Invio");
            campo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scrivi(AlberoDati.converti(originale, campo.getText()));
                }
            });
            JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
            p.setOpaque(false);
            p.add(campo);
            return p;
        }

        private JComponent creaRigaOggetto(Catalogo.Voce voce) {
            JPanel p = new JPanel(new BorderLayout(9, 0));
            p.setOpaque(false);

            JLabel immagine = new JLabel();
            ImageIcon img = icone.perFile(voce.icona, 26);
            immagine.setIcon(img != null ? img : Icone.segno("?", 26, Aspetto.TESTO_DEBOLE, true));
            immagine.setPreferredSize(new Dimension(28, 28));
            p.add(immagine, BorderLayout.WEST);

            JLabel nome = new JLabel(voce.etichetta());
            nome.setFont(nome.getFont().deriveFont(Font.BOLD, 12.5f));
            nome.setForeground(Aspetto.ATTENZIONE);
            p.add(nome, BorderLayout.CENTER);

            JButton cambia = new JButton("Cambia...");
            cambia.setFont(cambia.getFont().deriveFont(11.5f));
            cambia.setPreferredSize(new Dimension(96, 26));
            cambia.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PannelloDettagli.SelettoreOggetto s = new PannelloDettagli.SelettoreOggetto(
                            javax.swing.SwingUtilities.getWindowAncestor(Riga.this), catalogo, icone);
                    Catalogo.Voce scelta = s.apri(String.valueOf(originale));
                    if (scelta != null) {
                        scrivi(scelta.id);
                    }
                }
            });
            p.add(cambia, BorderLayout.EAST);
            return p;
        }

        /**
         * Scrive il nuovo valore nell'oggetto originale.
         *
         * La mappa mostrata e' una vista filtrata di quella del salvataggio, e
         * la sua {@code put} delega all'originale. Per gli elementi di una lista
         * si scrive invece per indice. Sbagliando questo passaggio la modifica
         * resterebbe nella copia mostrata: l'utente vedrebbe il valore nuovo e
         * il file resterebbe vecchio.
         */
        @SuppressWarnings("unchecked")
        private void scrivi(Object nuovo) {
            if (nuovo == null) {
                return;
            }
            if (genitore instanceof Map) {
                ((Map<String, Object>) genitore).put(nomeCampo, nuovo);
            } else if (genitore instanceof List) {
                try {
                    ((List<Object>) genitore).set(Integer.parseInt(nomeCampo), nuovo);
                } catch (NumberFormatException e) {
                    return;
                } catch (IndexOutOfBoundsException e) {
                    return;
                }
            }
            suModifica.run();
        }
    }

    private static String testoDi(Object valore) {
        if (valore == null) {
            return "";
        }
        if (valore instanceof Json.Numero) {
            return ((Json.Numero) valore).testo();
        }
        return String.valueOf(valore);
    }
}

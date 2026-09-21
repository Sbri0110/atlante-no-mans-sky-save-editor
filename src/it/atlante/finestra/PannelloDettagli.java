package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Il pannello di destra: cosa e' il campo selezionato e come si cambia.
 *
 * Per un oggetto di gioco mostra la scheda completa: icona grande, nome,
 * sottotitolo, categoria e descrizione, presi dal catalogo. Per un numero mostra
 * un campo monospaziato, per un valore vero/falso un interruttore.
 *
 * Il pulsante "cambia oggetto" apre una ricerca sul catalogo: si scrive parte
 * del nome e si sceglie, invece di digitare a memoria un identificatore come
 * {@code ^CASING}.
 */
public final class PannelloDettagli extends JPanel {

    private final Catalogo catalogo;
    private final Icone icone;
    private final java.util.function.Consumer<Object> suApplica;

    private final JLabel iconaGrande = new JLabel();
    private final JLabel nome = new JLabel();
    private final JLabel sottotitolo = new JLabel();
    private final Etichetta categoria = new Etichetta();
    private final JTextArea descrizione = new JTextArea();
    private final JTextField campo = new JTextField();
    private final JToggleButton interruttore = new JToggleButton("disattivo");
    private final JButton applica = new JButton("Applica");
    private final JButton scegli = new JButton("Cambia oggetto...");
    private final JLabel percorso = new JLabel();
    private final JPanel riquadroValore = new JPanel(new BorderLayout(8, 0));

    private AlberoDati.Nodo nodo;

    public PannelloDettagli(Catalogo catalogo, Icone icone,
                            java.util.function.Consumer<Object> suApplica) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.suApplica = suApplica;
        setLayout(new BorderLayout(0, 0));
        setBackground(Aspetto.PANNELLO);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Aspetto.BORDO));
        setPreferredSize(new Dimension(380, 600));

        add(costruisciTesta(), BorderLayout.NORTH);
        add(costruisciCorpo(), BorderLayout.CENTER);
        add(costruisciPiede(), BorderLayout.SOUTH);
        svuota();
    }

    // ------------------------------------------------------------------

    private JComponent costruisciTesta() {
        JPanel testa = new JPanel();
        testa.setLayout(new BoxLayout(testa, BoxLayout.Y_AXIS));
        testa.setBackground(Aspetto.PANNELLO_ALTO);
        testa.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        iconaGrande.setAlignmentX(Component.LEFT_ALIGNMENT);
        iconaGrande.setPreferredSize(new Dimension(88, 88));
        iconaGrande.setMaximumSize(new Dimension(88, 88));
        iconaGrande.setHorizontalAlignment(SwingConstants.LEFT);

        nome.setFont(nome.getFont().deriveFont(Font.BOLD, 17f));
        nome.setForeground(Aspetto.TESTO);
        nome.setAlignmentX(Component.LEFT_ALIGNMENT);

        sottotitolo.setFont(sottotitolo.getFont().deriveFont(12f));
        sottotitolo.setForeground(Aspetto.TESTO_TENUE);
        sottotitolo.setAlignmentX(Component.LEFT_ALIGNMENT);

        categoria.setAlignmentX(Component.LEFT_ALIGNMENT);

        testa.add(iconaGrande);
        testa.add(Box.createVerticalStrut(10));
        testa.add(nome);
        testa.add(Box.createVerticalStrut(3));
        testa.add(sottotitolo);
        testa.add(Box.createVerticalStrut(8));
        testa.add(categoria);
        return testa;
    }

    private JComponent costruisciCorpo() {
        JPanel corpo = new JPanel();
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.setBackground(Aspetto.PANNELLO);
        corpo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        percorso.setFont(Aspetto.monospaziato(11, Font.PLAIN));
        percorso.setForeground(Aspetto.TESTO_DEBOLE);
        percorso.setAlignmentX(Component.LEFT_ALIGNMENT);
        corpo.add(percorso);
        corpo.add(Box.createVerticalStrut(14));

        JLabel etichettaValore = new JLabel("Valore");
        etichettaValore.setFont(etichettaValore.getFont().deriveFont(Font.BOLD, 11f));
        etichettaValore.setForeground(Aspetto.TESTO_DEBOLE);
        etichettaValore.setAlignmentX(Component.LEFT_ALIGNMENT);
        corpo.add(etichettaValore);
        corpo.add(Box.createVerticalStrut(6));

        riquadroValore.setOpaque(false);
        riquadroValore.setAlignmentX(Component.LEFT_ALIGNMENT);
        riquadroValore.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        campo.setFont(Aspetto.monospaziato(13, Font.PLAIN));
        interruttore.setFont(interruttore.getFont().deriveFont(13f));
        riquadroValore.add(campo, BorderLayout.CENTER);
        corpo.add(riquadroValore);
        corpo.add(Box.createVerticalStrut(10));

        JPanel comandi = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        comandi.setOpaque(false);
        comandi.setAlignmentX(Component.LEFT_ALIGNMENT);
        comandi.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        applica.setFont(applica.getFont().deriveFont(Font.BOLD, 12.5f));
        scegli.setFont(scegli.getFont().deriveFont(12.5f));
        comandi.add(applica);
        comandi.add(scegli);
        corpo.add(comandi);
        corpo.add(Box.createVerticalStrut(18));

        JLabel etichettaDescrizione = new JLabel("Descrizione");
        etichettaDescrizione.setFont(etichettaDescrizione.getFont().deriveFont(Font.BOLD, 11f));
        etichettaDescrizione.setForeground(Aspetto.TESTO_DEBOLE);
        etichettaDescrizione.setAlignmentX(Component.LEFT_ALIGNMENT);
        corpo.add(etichettaDescrizione);
        corpo.add(Box.createVerticalStrut(6));

        descrizione.setEditable(false);
        descrizione.setLineWrap(true);
        descrizione.setWrapStyleWord(true);
        descrizione.setBackground(Aspetto.PANNELLO);
        descrizione.setForeground(Aspetto.TESTO_TENUE);
        descrizione.setFont(descrizione.getFont().deriveFont(12.5f));
        descrizione.setBorder(null);
        JScrollPane scorrimento = new JScrollPane(descrizione);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.setOpaque(false);
        scorrimento.getViewport().setOpaque(false);
        scorrimento.getVerticalScrollBar().setUnitIncrement(16);
        scorrimento.setAlignmentX(Component.LEFT_ALIGNMENT);
        corpo.add(scorrimento);
        return corpo;
    }

    private JComponent costruisciPiede() {
        JPanel piede = new JPanel(new BorderLayout());
        piede.setBackground(Aspetto.PANNELLO_ALTO);
        piede.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        JLabel suggerimento = new JLabel("Doppio clic su un valore per modificarlo in linea");
        suggerimento.setFont(suggerimento.getFont().deriveFont(11.5f));
        suggerimento.setForeground(Aspetto.TESTO_DEBOLE);
        piede.add(suggerimento, BorderLayout.WEST);
        return piede;
    }

    // ------------------------------------------------------------------

    public void mostra(AlberoDati.Nodo nodo) {
        this.nodo = nodo;
        if (nodo == null) {
            svuota();
            return;
        }

        percorso.setText(percorsoDi(nodo));
        Catalogo.Voce voce = nodo.valore() instanceof String
                ? catalogo.voce((String) nodo.valore()) : null;

        if (voce != null) {
            ImageIcon grande = icone.perFile(voce.icona, 88);
            iconaGrande.setIcon(grande != null ? grande
                    : Icone.segno(voce.etichetta().substring(0, 1), 88, Aspetto.ATTENZIONE, true));
            nome.setText(voce.etichetta());
            sottotitolo.setText(voce.sottotitolo == null ? "" : voce.sottotitolo);
            categoria.testo(voce.categoria);
            descrizione.setText(voce.descrizione == null ? "" : voce.descrizione);
        } else {
            iconaGrande.setIcon(Icone.segno(tipoDi(nodo), 88, Aspetto.TESTO_DEBOLE, true));
            nome.setText(nodo.chiave() == null ? "Salvataggio" : nodo.chiave());
            sottotitolo.setText(tipoEsteso(nodo));
            categoria.testo(null);
            descrizione.setText("");
        }

        // Editor adatto al tipo del valore.
        riquadroValore.removeAll();
        boolean booleano = nodo.valore() instanceof Boolean;
        boolean modificabile = nodo.foglia();
        if (booleano) {
            boolean vero = ((Boolean) nodo.valore()).booleanValue();
            interruttore.setSelected(vero);
            interruttore.setText(vero ? "attivo" : "disattivo");
            riquadroValore.add(interruttore, BorderLayout.CENTER);
        } else {
            campo.setText(nodo.valoreTestuale());
            campo.setEditable(modificabile);
            riquadroValore.add(campo, BorderLayout.CENTER);
        }
        applica.setEnabled(modificabile);
        scegli.setVisible(modificabile && (voce != null
                || (nodo.valore() instanceof String && nodo.chiave() != null
                    && sembraIdentificatore(nodo.chiave()))));
        riquadroValore.revalidate();
        riquadroValore.repaint();
    }

    private static boolean sembraIdentificatore(String chiave) {
        String c = chiave.toLowerCase();
        return c.contains("id") || c.contains("item") || c.contains("type")
                || c.contains("tech") || c.contains("product") || c.contains("substance");
    }

    private void svuota() {
        nodo = null;
        iconaGrande.setIcon(Icone.segno("?", 88, Aspetto.TESTO_DEBOLE, true));
        nome.setText("Nessun campo selezionato");
        sottotitolo.setText("Scegli un campo dall'albero");
        categoria.testo(null);
        descrizione.setText("");
        percorso.setText("");
        campo.setText("");
        campo.setEditable(false);
        applica.setEnabled(false);
        scegli.setVisible(false);
        riquadroValore.removeAll();
        riquadroValore.add(campo, BorderLayout.CENTER);
        riquadroValore.revalidate();
        riquadroValore.repaint();
    }

    private static String percorsoDi(AlberoDati.Nodo nodo) {
        StringBuilder b = new StringBuilder();
        AlberoDati.Nodo n = nodo;
        while (n != null && n.chiave() != null) {
            b.insert(0, "/" + n.chiave());
            n = n.genitore();
        }
        return b.length() == 0 ? "/" : b.toString();
    }

    private static String tipoDi(AlberoDati.Nodo nodo) {
        Object v = nodo.valore();
        if (v instanceof Json.Numero) {
            return "#";
        }
        if (v instanceof Boolean) {
            return "1";
        }
        if (v instanceof java.util.Map) {
            return "{";
        }
        if (v instanceof java.util.List) {
            return "[";
        }
        return "T";
    }

    private static String tipoEsteso(AlberoDati.Nodo nodo) {
        Object v = nodo.valore();
        if (v instanceof Json.Numero) {
            return ((Json.Numero) v).intero() ? "numero intero" : "numero decimale";
        }
        if (v instanceof Boolean) {
            return "vero / falso";
        }
        if (v instanceof java.util.Map) {
            return ((java.util.Map<?, ?>) v).size() + " campi";
        }
        if (v instanceof java.util.List) {
            return ((java.util.List<?>) v).size() + " elementi";
        }
        if (v == null) {
            return "valore assente";
        }
        return "testo";
    }

    // ------------------------------------------------------------------

    public void collegaAscoltatori() {
        applica.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applica();
            }
        });
        campo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applica();
            }
        });
        interruttore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean vero = interruttore.isSelected();
                interruttore.setText(vero ? "attivo" : "disattivo");
                applica();
            }
        });
        scegli.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scegliOggetto();
            }
        });
    }

    private void applica() {
        if (nodo == null || !nodo.foglia()) {
            return;
        }
        Object nuovo;
        if (nodo.valore() instanceof Boolean) {
            nuovo = Boolean.valueOf(interruttore.isSelected());
        } else {
            nuovo = AlberoDati.converti(nodo.valore(), campo.getText());
        }
        suApplica.accept(nuovo);
    }

    private void scegliOggetto() {
        SelettoreOggetto s = new SelettoreOggetto(
                javax.swing.SwingUtilities.getWindowAncestor(this), catalogo, icone);
        Catalogo.Voce scelta = s.apri(nodo == null ? "" : nodo.valoreTestuale());
        if (scelta != null) {
            campo.setText(scelta.id);
            applica();
        }
    }

    // ------------------------------------------------------------------
    // Selettore di oggetto
    // ------------------------------------------------------------------

    /** Finestra di ricerca sul catalogo: si sceglie un oggetto invece di digitarlo. */
    static final class SelettoreOggetto extends JDialog {

        private final Catalogo catalogo;
        private final Icone icone;
        private final JTextField ricerca = new JTextField();
        private final javax.swing.DefaultListModel<Catalogo.Voce> modello =
                new javax.swing.DefaultListModel<Catalogo.Voce>();
        private final JList<Catalogo.Voce> elenco = new JList<Catalogo.Voce>(modello);
        private final JLabel conteggio = new JLabel();
        private Catalogo.Voce scelta;

        SelettoreOggetto(java.awt.Window genitore, Catalogo catalogo, Icone icone) {
            super(genitore, "Scegli un oggetto", ModalityType.APPLICATION_MODAL);
            this.catalogo = catalogo;
            this.icone = icone;
            setSize(560, 560);
            setLocationRelativeTo(genitore);
            setLayout(new BorderLayout(0, 0));
            getContentPane().setBackground(Aspetto.PANNELLO);

            ricerca.setFont(ricerca.getFont().deriveFont(14f));
            ricerca.putClientProperty("JTextField.placeholderText", "Cerca per nome o identificatore...");
            ricerca.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            JPanel testa = new JPanel(new BorderLayout());
            testa.setBackground(Aspetto.PANNELLO_ALTO);
            testa.add(ricerca, BorderLayout.CENTER);
            conteggio.setBorder(BorderFactory.createEmptyBorder(0, 14, 8, 14));
            conteggio.setFont(conteggio.getFont().deriveFont(11.5f));
            conteggio.setForeground(Aspetto.TESTO_DEBOLE);
            testa.add(conteggio, BorderLayout.SOUTH);
            add(testa, BorderLayout.NORTH);

            elenco.setBackground(Aspetto.PANNELLO);
            elenco.setCellRenderer(new DisegnatoreVoce());
            elenco.setFixedCellHeight(34);
            JScrollPane scorrimento = new JScrollPane(elenco);
            scorrimento.setBorder(BorderFactory.createEmptyBorder());
            add(scorrimento, BorderLayout.CENTER);

            JPanel piede = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 10));
            piede.setBackground(Aspetto.PANNELLO_ALTO);
            JButton annulla = new JButton("Annulla");
            JButton conferma = new JButton("Usa questo");
            conferma.setFont(conferma.getFont().deriveFont(Font.BOLD, 12.5f));
            piede.add(annulla);
            piede.add(conferma);
            add(piede, BorderLayout.SOUTH);

            ricerca.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cerca(ricerca.getText());
                }
            });
            annulla.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scelta = null;
                    dispose();
                }
            });
            conferma.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scelta = elenco.getSelectedValue();
                    dispose();
                }
            });
            elenco.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        scelta = elenco.getSelectedValue();
                        dispose();
                    }
                }
            });
        }

        Catalogo.Voce apri(String iniziale) {
            if (iniziale != null && !iniziale.isEmpty()) {
                ricerca.setText(iniziale);
            }
            cerca(ricerca.getText());
            setVisible(true);
            return scelta;
        }

        private void cerca(String testo) {
            modello.clear();
            List<Catalogo.Voce> trovate = catalogo.cerca(testo, 400);
            if (trovate.isEmpty() && (testo == null || testo.trim().isEmpty())) {
                conteggio.setText("Scrivi per cercare fra " + catalogo.dimensione() + " oggetti");
            } else {
                conteggio.setText(trovate.size() + (trovate.size() == 1 ? " risultato" : " risultati")
                        + " su " + catalogo.dimensione() + " oggetti");
            }
            for (Catalogo.Voce v : trovate) {
                modello.addElement(v);
            }
            if (!modello.isEmpty()) {
                elenco.setSelectedIndex(0);
            }
        }

        private final class DisegnatoreVoce extends JPanel
                implements ListCellRenderer<Catalogo.Voce> {

            private final JLabel icona = new JLabel();
            private final JLabel titolo = new JLabel();
            private final JLabel dettaglio = new JLabel();

            DisegnatoreVoce() {
                setLayout(new BorderLayout(10, 0));
                setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
                add(icona, BorderLayout.WEST);
                JPanel testi = new JPanel();
                testi.setOpaque(false);
                testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
                titolo.setFont(titolo.getFont().deriveFont(13f));
                dettaglio.setFont(dettaglio.getFont().deriveFont(11f));
                dettaglio.setForeground(Aspetto.TESTO_DEBOLE);
                testi.add(titolo);
                testi.add(dettaglio);
                add(testi, BorderLayout.CENTER);
            }

            public Component getListCellRendererComponent(JList<? extends Catalogo.Voce> lista,
                                                          Catalogo.Voce valore, int indice,
                                                          boolean selezionato, boolean conFuoco) {
                setBackground(selezionato ? Aspetto.ACCENTO_SCURO : Aspetto.PANNELLO);
                titolo.setForeground(selezionato ? new Color(0xFFE9E9) : Aspetto.TESTO);
                ImageIcon img = icone.perFile(valore.icona, 26);
                icona.setIcon(img != null ? img : Icone.segno("?", 26, Aspetto.TESTO_DEBOLE, true));
                titolo.setText(valore.etichetta());
                dettaglio.setText((valore.categoria == null ? "" : valore.categoria + "  ·  ")
                        + (valore.sottotitolo == null ? "" : valore.sottotitolo));
                return this;
            }
        }
    }

    /**
     * Piccola etichetta a pillola per la categoria.
     *
     * Il primo tentativo la disegnava a mano calcolando la larghezza dalle
     * metriche del carattere: la pillola usciva stretta e il testo tagliato,
     * perche' le metriche usate per misurare non erano quelle usate per
     * disegnare. Lasciando il calcolo a Swing e usando un bordo arrotondato il
     * problema non si pone.
     */
    private static final class Etichetta extends JLabel {

        Etichetta() {
            setOpaque(false);
            setFont(getFont().deriveFont(Font.BOLD, 10.5f));
            setForeground(Aspetto.ATTENZIONE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Aspetto.ATTENZIONE, 1, true),
                    BorderFactory.createEmptyBorder(2, 9, 2, 9)));
            setVisible(false);
        }

        void testo(String valore) {
            boolean visibile = valore != null && !valore.isEmpty();
            setText(visibile ? valore : "");
            setVisible(visibile);
            revalidate();
            repaint();
        }
    }
}

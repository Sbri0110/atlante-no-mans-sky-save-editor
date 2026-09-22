package it.atlante.finestra;

import it.atlante.nms.Catalogo;
import it.atlante.nms.Rilevatore;
import it.atlante.nms.Salvataggio;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * La finestra principale.
 *
 * Disposizione:
 * <pre>
 *   +----------------------------------------------------------+
 *   |  testata: marchio, nome del salvataggio, dati in evidenza |
 *   +----------------------------------------------------------+
 *   |  barra comandi                                            |
 *   +---------------+-------------------------+----------------+
 *   |  elenco slot  |  albero dei campi       |  dettaglio     |
 *   +---------------+-------------------------+----------------+
 *   |  barra di stato                                           |
 *   +----------------------------------------------------------+
 * </pre>
 */
public final class Finestra extends JFrame {

    private final Catalogo catalogo;
    private final Icone icone;
    private final File cartellaIcone;
    private final File cartellaBackup;

    private final DefaultListModel<Navigazione.Sezione> modelloSezioni =
            new DefaultListModel<Navigazione.Sezione>();
    private final JList<Navigazione.Sezione> elencoSezioni = new JList<Navigazione.Sezione>(modelloSezioni);
    private final javax.swing.JComboBox<Rilevatore.Voce> comboSalvataggi =
            new javax.swing.JComboBox<Rilevatore.Voce>();
    private Navigazione.Sezione sezioneCorrente;
    private final JPanel centro = new JPanel(new BorderLayout());
    private final JLabel stato = new JLabel();
    private final JLabel messaggio = new JLabel();
    private final JTextField cerca = new JTextField();

    private final Testata testata;
    private PannelloDettagli dettagli;
    private PannelloCampi campi;
    private PannelloInventario inventario;
    private PannelloElenco elenco;
    private PannelloPremi premi;
    private PannelloStazioni stazioni;
    private PannelloTraguardi traguardi;
    private PannelloStatistiche statistiche;
    private PannelloPrincipale principale;
    private Salvataggio salvataggio;
    private Rilevatore.Voce voceCorrente;

    private final JButton apri = new JButton("Apri file...");
    private final JButton salva = new JButton("Salva");
    private final JButton salvaCome = new JButton("Salva con nome...");

    public Finestra(Catalogo catalogo, File cartellaIcone, File cartellaBackup) {
        super("Atlante — No Man's Sky | Save Editor");
        this.catalogo = catalogo;
        this.icone = new Icone(cartellaIcone, catalogo);
        this.cartellaIcone = cartellaIcone;
        this.cartellaBackup = cartellaBackup;
        this.testata = new Testata();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        // Il marchio rosso su finestra, barra delle applicazioni e anteprime.
        Marchio.applicaA(this);

        JPanel radice = new JPanel(new BorderLayout());
        radice.setBackground(Aspetto.FONDO);
        radice.add(testata, BorderLayout.NORTH);
        radice.add(costruisciCorpo(), BorderLayout.CENTER);
        radice.add(costruisciStato(), BorderLayout.SOUTH);
        setContentPane(radice);

        setJMenuBar(costruisciMenu());
        collegaAscoltatori();
        aggiornaStato("Pronto. Scegli un salvataggio dall'elenco o apri un file.");
        // La ricerca dei salvataggi e' una lettura di cartelle: pochi
        // millisecondi, e va fatta subito, non rimandata. Rimandandola
        // l'elenco restava vuoto finche' la finestra non tornava libera.
        cercaSlot();
    }

    // ------------------------------------------------------------------
    // Costruzione
    // ------------------------------------------------------------------

    private JComponent costruisciCorpo() {
        JPanel sinistra = costruisciNavigazione();

        centro.setBackground(Aspetto.PANNELLO);
        centro.add(pannelloVuoto(), BorderLayout.CENTER);

        principale = new PannelloPrincipale(catalogo, icone);
        principale.collega(new Runnable() {
            public void run() {
                if (voceCorrente != null) {
                    apri(voceCorrente.file);
                }
            }
        }, new Runnable() {
            public void run() {
                salva();
            }
        }, new Runnable() {
            public void run() {
                salvaCome();
            }
        }, new Runnable() {
            public void run() {
                testata.segnaModificato();
            }
        }, new java.util.function.Consumer<String>() {
            public void accept(String testo) {
                messaggio.setText(testo);
            }
        });

        campi = new PannelloCampi(catalogo, icone, new Runnable() {
            public void run() {
                salvataggio.segnaModificato();
                testata.segnaModificato();
                messaggio.setText("Modifiche non salvate");
            }
        });

        inventario = new PannelloInventario(catalogo, icone, new Runnable() {
            public void run() {
                salvataggio.segnaModificato();
                testata.segnaModificato();
                messaggio.setText("Modifiche non salvate");
            }
        });

        elenco = new PannelloElenco(catalogo, icone, new Runnable() {
            public void run() {
                salvataggio.segnaModificato();
                testata.segnaModificato();
                messaggio.setText("Modifiche non salvate");
            }
        });

        statistiche = new PannelloStatistiche(catalogo, icone, new Runnable() {
            public void run() {
                salvataggio.segnaModificato();
                testata.segnaModificato();
                messaggio.setText("Modifiche non salvate");
            }
        });

        traguardi = new PannelloTraguardi(icone, new Runnable() {
            public void run() {
                salvataggio.segnaModificato();
                testata.segnaModificato();
                messaggio.setText("Modifiche non salvate");
            }
        });

        stazioni = new PannelloStazioni(catalogo, icone, new Runnable() {
            public void run() {
                salvataggio.segnaModificato();
                testata.segnaModificato();
                messaggio.setText("Modifiche non salvate");
            }
        });

        premi = new PannelloPremi(catalogo, icone, new Runnable() {
            public void run() {
                salvataggio.segnaModificato();
                testata.segnaModificato();
                messaggio.setText("Modifiche non salvate");
            }
        });

        JPanel corpo = new JPanel(new BorderLayout());
        corpo.setBackground(Aspetto.FONDO);
        corpo.add(costruisciBarraComandi(), BorderLayout.NORTH);

        JSplitPane principaleSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sinistra, centro);
        principaleSplit.setResizeWeight(0.0);
        principaleSplit.setDividerLocation(252);
        principaleSplit.setBorder(BorderFactory.createEmptyBorder());
        principaleSplit.setDividerSize(6);
        corpo.add(principaleSplit, BorderLayout.CENTER);
        return corpo;
    }

    private JComponent costruisciBarraComandi() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setBackground(Aspetto.FONDO_ALTO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Aspetto.BORDO),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JPanel comandi = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        comandi.setOpaque(false);
        apri.setFont(apri.getFont().deriveFont(12.5f));
        salva.setFont(salva.getFont().deriveFont(Font.BOLD, 12.5f));
        salvaCome.setFont(salvaCome.getFont().deriveFont(12.5f));
        salva.setEnabled(false);
        salvaCome.setEnabled(false);

        // I salvataggi trovati stanno qui invece che in una colonna: si scelgono
        // una volta all'inizio, mentre le sezioni si cambiano di continuo.
        comboSalvataggi.setPreferredSize(new Dimension(330, 30));
        comboSalvataggi.setFont(comboSalvataggi.getFont().deriveFont(12.5f));
        comboSalvataggi.setRenderer(new DisegnatoreSalvataggio());
        comboSalvataggi.setToolTipText("Salvataggi trovati sulla macchina");

        comandi.add(apri);
        comandi.add(comboSalvataggi);
        comandi.add(salva);
        comandi.add(salvaCome);
        barra.add(comandi, BorderLayout.WEST);

        cerca.setPreferredSize(new Dimension(300, 30));
        cerca.setFont(cerca.getFont().deriveFont(12.5f));
        cerca.putClientProperty("JTextField.placeholderText", "Cerca un campo o un oggetto...");
        cerca.putClientProperty("JTextField.leadingIcon",
                Icone.segno("?", 16, Aspetto.TESTO_DEBOLE, false));
        JPanel destra = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        destra.setOpaque(false);
        destra.add(cerca);
        barra.add(destra, BorderLayout.EAST);
        return barra;
    }

    /**
     * La colonna di sinistra: le sezioni del salvataggio.
     *
     * E' la navigazione vera del programma. Prima qui c'era l'elenco dei
     * salvataggi, che ora sta nel menu a tendina della barra comandi: il
     * salvataggio si scegle una volta, la sezione si cambia di continuo.
     */
    private JPanel costruisciNavigazione() {
        elencoSezioni.setBackground(Aspetto.PANNELLO);
        elencoSezioni.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        elencoSezioni.setCellRenderer(new DisegnatoreSezione());
        elencoSezioni.setFixedCellHeight(46);

        JScrollPane scorrimento = new JScrollPane(elencoSezioni);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(16);

        JLabel titolo = new JLabel("  SEZIONI");
        titolo.setFont(Aspetto.monospaziato(10, Font.BOLD));
        titolo.setForeground(Aspetto.TESTO_DEBOLE);
        titolo.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));

        JPanel pannello = new JPanel(new BorderLayout());
        pannello.setBackground(Aspetto.PANNELLO);
        pannello.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Aspetto.BORDO));
        pannello.add(titolo, BorderLayout.NORTH);
        pannello.add(scorrimento, BorderLayout.CENTER);
        pannello.setPreferredSize(new Dimension(252, 600));

        for (Navigazione.Sezione s : Navigazione.elenco()) {
            modelloSezioni.addElement(s);
        }
        return pannello;
    }

    private JComponent costruisciStato() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setBackground(Aspetto.FONDO_ALTO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Aspetto.BORDO),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        stato.setFont(Aspetto.monospaziato(11.5f, Font.PLAIN));
        stato.setForeground(Aspetto.TESTO_TENUE);
        messaggio.setFont(messaggio.getFont().deriveFont(11.5f));
        messaggio.setForeground(Aspetto.TESTO_DEBOLE);
        barra.add(stato, BorderLayout.WEST);
        barra.add(messaggio, BorderLayout.EAST);
        return barra;
    }

    private JMenuBar costruisciMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(voce("Apri file...", KeyEvent.VK_O, new Runnable() {
            public void run() {
                apriFile();
            }
        }));
        file.add(voce("Rileggi elenco", KeyEvent.VK_R, new Runnable() {
            public void run() {
                cercaSlot();
            }
        }));
        file.addSeparator();
        file.add(voce("Salva", KeyEvent.VK_S, new Runnable() {
            public void run() {
                salva();
            }
        }));
        file.add(voce("Salva con nome...", 0, new Runnable() {
            public void run() {
                salvaCome();
            }
        }));
        file.addSeparator();
        file.add(voce("Esci", 0, new Runnable() {
            public void run() {
                dispose();
                System.exit(0);
            }
        }));
        barra.add(file);

        JMenu vista = new JMenu("Vista");
        vista.add(voce("Espandi tutto", KeyEvent.VK_E, new Runnable() {
            public void run() {
                if (campi != null) {
                    campi.espandiFinoA(12);
                }
            }
        }));
        vista.add(voce("Comprimi tutto", KeyEvent.VK_C, new Runnable() {
            public void run() {
                // Ricostruire il contenuto richiude tutti i gruppi.
                if (salvataggio != null) {
                    mostraContenuto();
                }
            }
        }));
        vista.addSeparator();
        vista.add(costruisciMenuColori());
        barra.add(vista);

        JMenu aiuto = new JMenu("?");
        aiuto.add(voce("Informazioni", 0, new Runnable() {
            public void run() {
                informazioni();
            }
        }));
        barra.add(aiuto);
        return barra;
    }

    /**
     * Sottomenu per cambiare la tavolozza a programma avviato.
     *
     * Il cambio e' immediato: i colori vengono rimessi nella tavolozza dei
     * componenti e l'interfaccia viene ridipinta, senza riavviare. La scelta
     * viene salvata in {@code risorse/aspetto.properties}, quindi resta anche
     * alla riapertura successiva.
     */
    private JMenu costruisciMenuColori() {
        JMenu colori = new JMenu("Colori");
        javax.swing.ButtonGroup gruppo = new javax.swing.ButtonGroup();
        for (final String nome : Aspetto.nomiTavolozze()) {
            javax.swing.JRadioButtonMenuItem v = new javax.swing.JRadioButtonMenuItem(nome);
            v.setSelected(nome.equals(Aspetto.nomeTavolozza()));
            v.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Aspetto.cambiaTavolozza(nome);
                    messaggio.setText("Tavolozza \"" + nome + "\" applicata e salvata");
                    repaint();
                }
            });
            gruppo.add(v);
            colori.add(v);
        }
        colori.addSeparator();
        JMenuItem dove = new JMenuItem("Dove sono i colori...");
        dove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File f = new File("risorse" + File.separator + "aspetto.properties");
                JOptionPane.showMessageDialog(Finestra.this,
                        "I colori stanno in:\n\n" + f.getAbsolutePath()
                        + "\n\nI valori sono esadecimali (#RRGGBB). Per cambiare un colore\n"
                        + "solo, cancella la riga \"tavolozza\" e lascia le altre: quelle\n"
                        + "che restano vincono sulla tavolozza scelta.",
                        "Dove sono i colori", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        colori.add(dove);
        return colori;
    }

    private JMenuItem voce(String testo, int tasto, final Runnable azione) {
        JMenuItem v = new JMenuItem(testo);
        if (tasto != 0) {
            v.setAccelerator(KeyStroke.getKeyStroke(tasto, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        v.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                azione.run();
            }
        });
        return v;
    }

    private JComponent pannelloVuoto() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Aspetto.FONDO);
        JLabel t = new JLabel("<html><div style='text-align:center'>"
                + "<span style='font-size:15px'>Nessun salvataggio aperto</span><br><br>"
                + "<span style='font-size:12px;color:#A29194'>Scegli uno slot dall'elenco a sinistra,<br>"
                + "oppure usa <b>File &gt; Apri file...</b></span></div></html>", SwingConstants.CENTER);
        t.setForeground(Aspetto.TESTO_TENUE);
        p.add(t, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------
    // Azioni
    // ------------------------------------------------------------------

    /**
     * Cerca i salvataggi e ne apre uno.
     *
     * L'apertura automatica va fatta con cautela: riempiendo il menu a tendina
     * a mano, ogni aggiunta fa scattare l'ascoltatore e finiva per aprirsi
     * l'ultimo elemento aggiunto — nella pratica i dati account, che non hanno
     * la struttura di un salvataggio di gioco e lasciavano tutte le sezioni
     * vuote. Qui l'ascoltatore viene staccato durante il riempimento, e alla
     * fine si apre di proposito un salvataggio di gioco.
     */
    private void cercaSlot() {
        ActionListener[] ascoltatori = comboSalvataggi.getActionListeners();
        for (ActionListener a : ascoltatori) {
            comboSalvataggi.removeActionListener(a);
        }
        comboSalvataggi.removeAllItems();

        List<Rilevatore.Voce> trovate = Rilevatore.cercaTutti();
        for (Rilevatore.Voce v : trovate) {
            comboSalvataggi.addItem(v);
        }
        for (ActionListener a : ascoltatori) {
            comboSalvataggi.addActionListener(a);
        }

        if (trovate.isEmpty()) {
            aggiornaStato("Nessun salvataggio trovato automaticamente. Usa File > Apri file...");
            return;
        }

        // Si preferisce un salvataggio di gioco: i dati account non hanno le
        // sezioni di gioco e vanno aperti solo se non c'e' altro.
        int scelto = -1;
        for (int i = 0; i < trovate.size(); i++) {
            if (trovate.get(i).formato == it.atlante.nms.Formato.BLOCCHI) {
                scelto = i;
                break;
            }
        }
        if (scelto < 0) {
            scelto = 0;
        }
        comboSalvataggi.setSelectedIndex(scelto);
        aggiornaStato(trovate.size() + " salvataggi trovati.");
        apri(trovate.get(scelto).file);
    }

    private void apriFile() {
        JFileChooser scelta = new JFileChooser();
        scelta.setDialogTitle("Apri un salvataggio di No Man's Sky");
        File radice = Rilevatore.cartellaWgs();
        if (radice == null) {
            radice = Rilevatore.cartellaSteam();
        }
        if (radice != null) {
            scelta.setCurrentDirectory(radice);
        }
        scelta.setFileFilter(new FileNameExtensionFilter("Salvataggi (*.hg, container)", "hg"));
        if (scelta.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        apri(scelta.getSelectedFile());
    }

    /**
     * Apre un salvataggio indicato dal chiamante.
     *
     * Serve allo strumento che genera le schermate per la documentazione: apre
     * un salvataggio vero e fotografa la finestra, cosi' le immagini pubblicate
     * mostrano l'interfaccia reale e non un disegno preparato a parte.
     */
    public void apriDaPercorso(File file) {
        apri(file);
    }

    /** Apre il primo salvataggio trovato: usato dal generatore di schermate. */
    public boolean apriPrimoTrovato() {
        List<Rilevatore.Voce> trovate = Rilevatore.cercaTutti();
        if (trovate.isEmpty()) {
            return false;
        }
        apri(trovate.get(0).file);
        return true;
    }

    /** Porta i campi a una profondita' di apertura, per le schermate. */
    public void espandiA(int profondita) {
        if (campi != null) {
            campi.espandiFinoA(profondita);
        }
    }

    /** Adatta la griglia allo spazio disponibile, per le schermate. */
    public void ridisponi() {
        if (inventario != null) {
            inventario.ridisponi();
        }
    }

    /**
     * Fissa la data di "Ultima modifica", per le schermate.
     *
     * Senza, la stessa immagine rigenerata il giorno dopo risulterebbe
     * diversa per via dell'ora del file, e il confronto fra due generazioni
     * non direbbe piu' niente di utile.
     */
    public void fissaDataModifica(long millisecondi) {
        PannelloPrincipale.fissaDataModifica(millisecondi);
    }

    /** Collaudo della selezione degli slot. */
    public String provaSelezione() {
        return inventario == null ? "nessun pannello inventario" : inventario.provaSelezione();
    }

    public int quantiSlot() {
        return inventario == null ? 0 : inventario.quantiSlot();
    }

    public int primoOccupato() {
        return inventario == null ? -1 : inventario.primoOccupato();
    }

    /** Collaudo: apre una scheda di una sezione a inventario, per nome. */
    public boolean apriScheda(String gruppo, String etichetta) {
        return inventario != null && inventario.apriScheda(gruppo, etichetta);
    }

    /** Sceglie uno slot senza passare dal mouse. */
    public void selezionaSlot(int indice) {
        if (inventario != null && indice >= 0) {
            inventario.selezionaDirettamente(indice);
        }
    }

    public int slotSelezionato() {
        return inventario == null ? -1 : inventario.slotSelezionato();
    }

    /** Seleziona una sezione per nome: usato dal generatore di schermate. */
    public boolean mostraSezionePerNome(String nome) {
        for (int i = 0; i < modelloSezioni.size(); i++) {
            if (modelloSezioni.get(i).nome.equalsIgnoreCase(nome)) {
                elencoSezioni.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    /** Nomi delle sezioni, per chi vuole sceglierne una. */
    public String[] nomiSezioni() {
        String[] nomi = new String[modelloSezioni.size()];
        for (int i = 0; i < modelloSezioni.size(); i++) {
            nomi[i] = modelloSezioni.get(i).nome;
        }
        return nomi;
    }

    /** Scrive un messaggio nella barra di stato. */
    public void messaggioStato(String testo) {
        messaggio.setText(testo);
    }

    private void apri(File file) {
        try {
            messaggio.setText("Apertura in corso...");
            Salvataggio nuovo = Salvataggio.apri(file, mappaChiavi());
            salvataggio = nuovo;

            // Si tiene traccia di quale voce dell'elenco e' aperta: la schermata
            // iniziale mostra la piattaforma e lo slot.
            voceCorrente = null;
            for (int i = 0; i < comboSalvataggi.getItemCount(); i++) {
                Rilevatore.Voce v = comboSalvataggi.getItemAt(i);
                if (v != null && v.file.equals(file)) {
                    voceCorrente = v;
                    break;
                }
            }

            // La sezione scelta resta; all'apertura si parte dalla schermata
            // iniziale, che dice cosa si e' aperto.
            if (sezioneCorrente == null) {
                sezioneCorrente = Navigazione.elenco().get(0);
            }
            elencoSezioni.setSelectedValue(sezioneCorrente, true);

            mostraContenuto();
            // I conteggi per sezione cambiano con il salvataggio: il disegnatore
            // li rilegge a ogni disegno, quindi basta forzare un ridisegno.
            elencoSezioni.repaint();
            salva.setEnabled(true);
            salvaCome.setEnabled(true);
            testata.aggiorna(nuovo);
            aggiornaStato(nuovo.riepilogo());
            messaggio.setText("Sezione: " + sezioneCorrente.nome);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Non riesco ad aprire il file.\n\n" + e.getMessage(),
                    "Errore di apertura", JOptionPane.ERROR_MESSAGE);
            aggiornaStato("Apertura non riuscita.");
        }
    }

    /**
     * Cambia la sezione mostrata.
     *
     * L'albero viene ricostruito sul ramo della sezione. Le modifiche gia'
     * fatte restano: i valori dell'albero ridotto sono gli stessi oggetti del
     * salvataggio, non copie.
     */
    private void mostraSezione(Navigazione.Sezione sezione) {
        sezioneCorrente = sezione;
        if (salvataggio == null) {
            messaggio.setText("Sezione scelta: " + sezione.nome + ". Apri prima un salvataggio.");
            return;
        }
        mostraContenuto();
        int quanti = Navigazione.conta(salvataggio.albero(), sezione);
        if (sezione.contenitore == Navigazione.Contenitore.PRINCIPALE) {
            messaggio.setText("Principale — " + sezione.descrizione);
        } else if (quanti == 0) {
            messaggio.setText(sezione.nome + " — nessun campo di questa sezione nel file aperto"
                    + " (forse sono dati account, non un salvataggio di gioco)");
        } else {
            messaggio.setText(sezione.nome + " — " + quanti + " campi · " + sezione.descrizione);
        }
    }

    /**
     * Mostra il contenuto della sezione scelta.
     *
     * Due pannelli diversi: la schermata iniziale con le informazioni del file e
     * le azioni rapide, e il pannello dei campi per tutte le altre sezioni.
     */
    private void mostraContenuto() {
        if (salvataggio == null) {
            return;
        }
        centro.removeAll();
        if (sezioneCorrente != null
                && sezioneCorrente.contenitore == Navigazione.Contenitore.PRINCIPALE) {
            principale.aggiorna(salvataggio,
                    voceCorrente == null ? null : voceCorrente.piattaforma,
                    voceCorrente == null ? null : voceCorrente.etichetta);
            centro.add(principale, BorderLayout.CENTER);
        } else if (sezioneCorrente != null && Inventari.aGriglia(sezioneCorrente.nome)) {
            // Le sezioni a inventario si mostrano come griglie di slot, non
            // come elenchi di campi: e' la struttura del vecchio editor.
            inventario.mostra(salvataggio.albero(), sezioneCorrente.nome);
            centro.add(inventario, BorderLayout.CENTER);
        } else if (sezioneCorrente != null && Elenchi.aElenco(sezioneCorrente.nome)) {
            // Squadrone, Fregate, Compagni, Veicoli: pochi elementi ricchi di
            // campi. Si sceglie l'elemento a sinistra e si modifica a destra.
            elenco.mostra(salvataggio.albero(), sezioneCorrente.nome);
            centro.add(elenco, BorderLayout.CENTER);
        } else if (sezioneCorrente != null && "Statistiche".equals(sezioneCorrente.nome)) {
            statistiche.mostra(salvataggio.albero());
            centro.add(statistiche, BorderLayout.CENTER);
        } else if (sezioneCorrente != null && "Traguardi e fazioni".equals(sezioneCorrente.nome)) {
            traguardi.mostra(salvataggio.albero());
            centro.add(traguardi, BorderLayout.CENTER);
        } else if (sezioneCorrente != null && "Conquista stazioni".equals(sezioneCorrente.nome)) {
            stazioni.mostra(salvataggio.albero());
            centro.add(stazioni, BorderLayout.CENTER);
        } else if (sezioneCorrente != null && "Spedizioni".equals(sezioneCorrente.nome)) {
            // I premi delle spedizioni, raggruppati, con la casella per ognuno.
            premi.mostra(salvataggio.albero(),
                    new File("risorse" + File.separator + "db" + File.separator + "rewards.xml"));
            centro.add(premi, BorderLayout.CENTER);
        } else {
            Navigazione.Sezione sezione = sezioneCorrente != null
                    ? sezioneCorrente : Navigazione.elenco().get(0);
            campi.mostra(Navigazione.filtro(salvataggio.albero(), sezione));
            centro.add(campi, BorderLayout.CENTER);
        }
        centro.revalidate();
        centro.repaint();
    }

    private void salva() {
        if (salvataggio == null) {
            return;
        }
        try {
            messaggio.setText("Verifica e scrittura...");
            File backup = salvataggio.salva(cartellaBackup);
            testata.aggiorna(salvataggio);
            aggiornaStato(salvataggio.riepilogo());
            messaggio.setText(backup == null
                    ? "Salvato"
                    : "Salvato. Copia di sicurezza: " + backup.getName());
            JOptionPane.showMessageDialog(this,
                    "Salvataggio scritto e verificato.\n\n"
                            + (backup == null ? "" : "Copia di sicurezza:\n" + backup.getAbsolutePath()),
                    "Fatto", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Scrittura non eseguita.\n\n" + e.getMessage(),
                    "Il salvataggio non e' stato toccato", JOptionPane.ERROR_MESSAGE);
            aggiornaStato("Scrittura annullata dalla verifica.");
        }
    }

    private void salvaCome() {
        if (salvataggio == null) {
            return;
        }
        JFileChooser scelta = new JFileChooser(salvataggio.file().getParentFile());
        scelta.setDialogTitle("Salva il salvataggio come...");
        scelta.setSelectedFile(new File(salvataggio.file().getName()));
        if (scelta.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            salvataggio.salvaCome(scelta.getSelectedFile());
            aggiornaStato("Scritto: " + scelta.getSelectedFile().getAbsolutePath());
            messaggio.setText("Salvato");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void informazioni() {
        JOptionPane.showMessageDialog(this, testoInformazioni(), "Informazioni",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Il testo della finestra "Informazioni".
     *
     * Le avvertenze e la dichiarazione sull'intelligenza artificiale stanno
     * anche qui, non solo nel README: chi apre il programma puo' non aver mai
     * visto il repository, e le cose che deve sapere prima di toccare il
     * salvataggio sono proprio queste.
     */
    private String testoInformazioni() {
        String testo = "Atlante - No Man's Sky | Save Editor\n\n"
                + "Catalogo di gioco: " + catalogo.dimensione() + " oggetti\n"
                + "Icone di gioco: " + (cartellaIcone.isDirectory() ? cartellaIcone.getAbsolutePath() : "assenti") + "\n"
                + "Tema: " + (Aspetto.conFlatlaf() ? "FlatLaf" : "Nimbus (FlatLaf non trovato)") + "\n"
                + "Copia di sicurezza: " + (cartellaBackup == null ? "accanto al salvataggio" : cartellaBackup.getAbsolutePath()) + "\n\n"
                + "Prima di ogni scrittura il contenuto viene ricompattato, riscompattato\n"
                + "e confrontato campo per campo con l'originale. Se qualcosa cambia,\n"
                + "il file non viene toccato.\n\n"
                + "------------------------------------------------------------\n"
                + "SVILUPPATO CON L'INTELLIGENZA ARTIFICIALE\n\n"
                + "Codice, interfaccia e documentazione sono nati da sessioni di lavoro\n"
                + "con un assistente di IA, sotto la direzione e la verifica di un autore\n"
                + "umano. E' dichiarato apertamente: chi usa il programma ha il diritto\n"
                + "di saperlo. L'IA sbaglia in modo convincente, quindi un difetto puo'\n"
                + "essere sfuggito anche alle prove automatiche. Se ne trovi uno,\n"
                + "segnalalo invece di pensare di aver sbagliato tu.\n\n"
                + "------------------------------------------------------------\n"
                + "AVVERTENZE - LEGGILE\n\n"
                + "Modifica i salvataggi a tuo rischio, e con moderazione.\n\n"
                + "1. Puoi rovinarti il gioco. No Man's Sky e' fatto di scoperta, attesa\n"
                + "   e conquista: darsi unita' illimitate o tutti gli oggetti appiattisce\n"
                + "   l'esperienza fino a renderla noiosa, e non si annulla con un\n"
                + "   pulsante. Usalo per togliere un ostacolo, non per saltare il gioco.\n\n"
                + "2. Solo in partita singola. Non portare valori alterati in sessioni\n"
                + "   multigiocatore: rovinerebbe la partita di chi gioca con te.\n\n"
                + "3. Il rischio e' tuo. Hello Games non ha approvato questo strumento e\n"
                + "   non esiste una garanzia ufficiale per il tuo account. Il rischio e'\n"
                + "   basso se il file resta coerente e giochi in singolo, ma nessuno\n"
                + "   puo' prometterti niente.\n\n"
                + "4. Tieni il gioco chiuso mentre l'editor e' aperto, e fai una copia\n"
                + "   tua del salvataggio prima di aprirlo la prima volta.\n\n"
                + "------------------------------------------------------------\n"
                + "No Man's Sky e' un marchio di Hello Games. Questo progetto non e'\n"
                + "affiliato ne' approvato da Hello Games. Licenza MIT.";
        return testo;
    }

    private it.atlante.nms.MappaChiavi mappaChiavi() throws IOException {
        File m = new File("risorse/db/jsonmap.txt");
        if (!m.isFile()) {
            m = new File("risorse" + File.separator + "db" + File.separator + "jsonmap.txt");
        }
        if (!m.isFile()) {
            throw new IOException("mappa delle chiavi non trovata in risorse/db/jsonmap.txt");
        }
        return it.atlante.nms.MappaChiavi.carica(m.toPath());
    }

    private void aggiornaStato(String testo) {
        stato.setText(testo);
    }

    // ------------------------------------------------------------------
    // Testata
    // ------------------------------------------------------------------

    /** La fascia in alto: marchio, nome del salvataggio, dati in evidenza. */
    private final class Testata extends JPanel {

        private final JLabel nomeSalvataggio = new JLabel("Nessun salvataggio aperto");
        private final JLabel percorso = new JLabel("");
        private final Pillola slot = new Pillola(Pillola.ACCENTO);
        private final Pillola durata = new Pillola(Pillola.INFO);
        private final Pillola dimensione = new Pillola(Pillola.ATTENZIONE);
        private final Pillola modificato = new Pillola(Pillola.ERRORE);
        private ImageIcon marchio;

        Testata() {
            setLayout(new BorderLayout(16, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
            setPreferredSize(new Dimension(1000, 104));

            // Il marchio occupa una colonna propria: dipingerlo sopra il testo
            // lo faceva sovrapporre al titolo.
            JPanel colonnaMarchio = new JPanel(new BorderLayout());
            colonnaMarchio.setOpaque(false);
            File logo = new File("assets" + File.separator + "marchio" + File.separator + "logo-128.png");
            if (!logo.isFile()) {
                logo = new File("assets/marchio/logo-128.png");
            }
            if (logo.isFile()) {
                marchio = new ImageIcon(logo.getAbsolutePath());
                JLabel etichettaMarchio = new JLabel(marchio);
                etichettaMarchio.setVerticalAlignment(SwingConstants.CENTER);
                etichettaMarchio.setPreferredSize(new Dimension(72, 72));
                colonnaMarchio.add(etichettaMarchio, BorderLayout.CENTER);
            }
            colonnaMarchio.setPreferredSize(new Dimension(80, 72));
            add(colonnaMarchio, BorderLayout.WEST);

            JPanel testi = new JPanel();
            testi.setOpaque(false);
            testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
            JLabel titolo = new JLabel("ATLANTE");
            titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 20f));
            titolo.setForeground(Aspetto.TESTO);
            titolo.setAlignmentX(Component.LEFT_ALIGNMENT);
            nomeSalvataggio.setFont(nomeSalvataggio.getFont().deriveFont(14f));
            nomeSalvataggio.setForeground(Aspetto.ACCENTO_CHIARO);
            nomeSalvataggio.setAlignmentX(Component.LEFT_ALIGNMENT);
            percorso.setFont(Aspetto.monospaziato(10.5f, Font.PLAIN));
            percorso.setForeground(Aspetto.TESTO_DEBOLE);
            percorso.setAlignmentX(Component.LEFT_ALIGNMENT);
            testi.add(javax.swing.Box.createVerticalGlue());
            testi.add(titolo);
            testi.add(Box.createVerticalStrut(4));
            testi.add(nomeSalvataggio);
            testi.add(Box.createVerticalStrut(3));
            testi.add(percorso);
            testi.add(javax.swing.Box.createVerticalGlue());
            add(testi, BorderLayout.CENTER);

            JPanel pillole = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 22));
            pillole.setOpaque(false);
            modificato.testo("modifiche non salvate");
            modificato.setVisible(false);
            pillole.add(modificato);
            pillole.add(slot);
            pillole.add(durata);
            pillole.add(dimensione);
            add(pillole, BorderLayout.EAST);

            slot.testo(null);
            durata.testo(null);
            dimensione.testo(null);
        }

        void aggiorna(Salvataggio s) {
            nomeSalvataggio.setText(s.file().getName());
            percorso.setText(s.file().getParent());
            slot.testo(s.formato().descrizione());
            durata.testo(s.blocchi() + " blocchi");
            dimensione.testo(Salvataggio.mb(s.dimensioneDecompressa()) + " decompressi");
            modificato.setVisible(false);
            repaint();
        }

        void segnaModificato() {
            modificato.setVisible(true);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, Aspetto.PANNELLO_ALTO, getWidth(), getHeight(), Aspetto.FONDO));
            g2.fillRect(0, 0, getWidth(), getHeight());
            // filo di accento sul bordo inferiore
            g2.setColor(Aspetto.ACCENTO_SCURO);
            g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    }

    /**
     * Etichetta a pillola, per i dati in evidenza nella testata.
     *
     * Il colore non viene memorizzato: viene risolto a ogni disegno dalla
     * tavolozza corrente. Memorizzandolo, cambiando tema le pillole restavano
     * del colore vecchio fino al riavvio.
     */
    private static final class Pillola extends JLabel {

        static final int ACCENTO = 1;
        static final int INFO = 2;
        static final int ATTENZIONE = 3;
        static final int ERRORE = 4;

        private final int tipo;
        private String testo;

        Pillola(int tipo) {
            this.tipo = tipo;
            setOpaque(false);
            setFont(Aspetto.monospaziato(11, Font.PLAIN));
        }

        private Color colore() {
            switch (tipo) {
                case INFO:       return Aspetto.INFO;
                case ATTENZIONE: return Aspetto.ATTENZIONE;
                case ERRORE:     return Aspetto.ERRORE;
                default:         return Aspetto.ACCENTO;
            }
        }

        void testo(String valore) {
            this.testo = valore == null || valore.isEmpty() ? null : valore;
            setVisible(this.testo != null);
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (testo == null) {
                return new Dimension(0, 0);
            }
            java.awt.FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(testo) + 24, 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (testo == null) {
                return;
            }
            Color c = colore();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(c);
            g2.setFont(getFont());
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(testo, 12, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Disegnatore dell'elenco delle sezioni. */
    private final class DisegnatoreSezione extends JPanel
            implements ListCellRenderer<Navigazione.Sezione> {

        private final JLabel icona = new JLabel();
        private final JLabel titolo = new JLabel();
        private final JLabel dettaglio = new JLabel();

        DisegnatoreSezione() {
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 8));
            add(icona, BorderLayout.WEST);
            JPanel testi = new JPanel();
            testi.setOpaque(false);
            testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
            titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 12.5f));
            dettaglio.setFont(dettaglio.getFont().deriveFont(10.5f));
            dettaglio.setForeground(Aspetto.TESTO_DEBOLE);
            testi.add(titolo);
            testi.add(dettaglio);
            add(testi, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList<? extends Navigazione.Sezione> lista,
                                                      Navigazione.Sezione valore, int indice,
                                                      boolean selezionato, boolean conFuoco) {
            setBackground(selezionato ? Aspetto.SELEZIONE : Aspetto.PANNELLO);
            titolo.setForeground(selezionato ? Aspetto.TESTO : Aspetto.TESTO_TENUE);
            // Icona di gioco, non una lettera: ogni sezione ha la sua.
            ImageIcon img = icone.perFile(valore.segno + ".PNG", 26);
            icona.setIcon(img != null ? img
                    : Icone.segno(valore.nome.substring(0, 1), 26,
                            selezionato ? Aspetto.ACCENTO : Aspetto.TESTO_DEBOLE, true));
            titolo.setText(valore.nome);
            int quanti = salvataggio == null ? 0 : Navigazione.conta(salvataggio.albero(), valore);
            dettaglio.setText(quanti == 0 ? valore.descrizione : quanti + " campi");
            setToolTipText(valore.descrizione);
            return this;
        }
    }

    /** Disegnatore delle voci del menu a tendina dei salvataggi. */
    private final class DisegnatoreSalvataggio extends JPanel
            implements ListCellRenderer<Rilevatore.Voce> {

        private final JLabel icona = new JLabel();
        private final JLabel titolo = new JLabel();
        private final JLabel dettaglio = new JLabel();

        DisegnatoreSalvataggio() {
            setLayout(new BorderLayout(9, 0));
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            add(icona, BorderLayout.WEST);
            JPanel testi = new JPanel();
            testi.setOpaque(false);
            testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
            titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 12f));
            dettaglio.setFont(dettaglio.getFont().deriveFont(10.5f));
            dettaglio.setForeground(Aspetto.TESTO_DEBOLE);
            testi.add(titolo);
            testi.add(dettaglio);
            add(testi, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList<? extends Rilevatore.Voce> lista,
                                                      Rilevatore.Voce valore, int indice,
                                                      boolean selezionato, boolean conFuoco) {
            setBackground(selezionato ? Aspetto.SELEZIONE : Aspetto.PANNELLO_ALTO);
            titolo.setForeground(selezionato ? Aspetto.TESTO : Aspetto.TESTO_TENUE);
            if (valore == null) {
                titolo.setText("nessun salvataggio trovato");
                dettaglio.setText("");
                icona.setIcon(null);
                return this;
            }
            icona.setIcon(Icone.segno(valore.formato == it.atlante.nms.Formato.BLOCCHI ? "S" : "A",
                    22, selezionato ? Aspetto.ACCENTO : Aspetto.TESTO_DEBOLE, true));
            titolo.setText(valore.etichetta);
            dettaglio.setText(valore.piattaforma + "  ·  " + Salvataggio.mb(valore.file.length()));
            return this;
        }
    }

    // ------------------------------------------------------------------

    private void collegaAscoltatori() {
        apri.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                apriFile();
            }
        });
        salva.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                salva();
            }
        });
        salvaCome.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                salvaCome();
            }
        });
        elencoSezioni.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                Navigazione.Sezione s = elencoSezioni.getSelectedValue();
                if (s != null) {
                    mostraSezione(s);
                }
            }
        });
        comboSalvataggi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object scelto = comboSalvataggi.getSelectedItem();
                if (scelto instanceof Rilevatore.Voce) {
                    apri(((Rilevatore.Voce) scelto).file);
                }
            }
        });
        cerca.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (campi == null) {
                    return;
                }
                String trovato = campi.cerca(cerca.getText());
                messaggio.setText(trovato != null
                        ? "Trovato: " + trovato
                        : "Nessun campo corrisponde a \"" + cerca.getText() + "\"");
            }
        });
    }
}

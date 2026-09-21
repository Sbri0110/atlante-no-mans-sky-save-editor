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

    private final DefaultListModel<Rilevatore.Voce> modelloSlot = new DefaultListModel<Rilevatore.Voce>();
    private final JList<Rilevatore.Voce> elencoSlot = new JList<Rilevatore.Voce>(modelloSlot);
    private final JPanel centro = new JPanel(new BorderLayout());
    private final JLabel stato = new JLabel();
    private final JLabel messaggio = new JLabel();
    private final JTextField cerca = new JTextField();

    private final Testata testata;
    private PannelloDettagli dettagli;
    private AlberoDati albero;
    private Salvataggio salvataggio;
    private JScrollPane scorrimentoAlbero;

    private final JButton apri = new JButton("Apri file...");
    private final JButton salva = new JButton("Salva");
    private final JButton salvaCome = new JButton("Salva con nome...");

    public Finestra(Catalogo catalogo, File cartellaIcone, File cartellaBackup) {
        super("Atlante — editor di salvataggi per No Man's Sky");
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
        JPanel sinistra = costruisciElencoSlot();

        centro.setBackground(Aspetto.FONDO);
        centro.add(pannelloVuoto(), BorderLayout.CENTER);

        dettagli = new PannelloDettagli(catalogo, icone, new java.util.function.Consumer<Object>() {
            public void accept(Object nuovoValore) {
                applicaModifica(nuovoValore);
            }
        });
        dettagli.collegaAscoltatori();

        JSplitPane destro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centro, dettagli);
        destro.setResizeWeight(1.0);
        destro.setDividerLocation(820);
        destro.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane principale = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sinistra, destro);
        principale.setResizeWeight(0.0);
        principale.setDividerLocation(290);
        principale.setBorder(BorderFactory.createEmptyBorder());
        principale.setDividerSize(6);

        JPanel corpo = new JPanel(new BorderLayout());
        corpo.setBackground(Aspetto.FONDO);
        corpo.add(costruisciBarraComandi(), BorderLayout.NORTH);
        corpo.add(principale, BorderLayout.CENTER);
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
        comandi.add(apri);
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

    private JPanel costruisciElencoSlot() {
        elencoSlot.setBackground(Aspetto.PANNELLO);
        elencoSlot.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        elencoSlot.setCellRenderer(new DisegnatoreSlot());
        elencoSlot.setFixedCellHeight(52);

        JScrollPane scorrimento = new JScrollPane(elencoSlot);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(16);

        JLabel titolo = new JLabel("  SALVATAGGI TROVATI");
        titolo.setFont(Aspetto.monospaziato(10, Font.BOLD));
        titolo.setForeground(Aspetto.TESTO_DEBOLE);
        titolo.setBorder(BorderFactory.createEmptyBorder(12, 6, 8, 6));

        JPanel pannello = new JPanel(new BorderLayout());
        pannello.setBackground(Aspetto.PANNELLO);
        pannello.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Aspetto.BORDO));
        pannello.add(titolo, BorderLayout.NORTH);
        pannello.add(scorrimento, BorderLayout.CENTER);
        pannello.setPreferredSize(new Dimension(290, 600));
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
                if (albero != null) {
                    albero.espandiFinoA(12);
                }
            }
        }));
        vista.add(voce("Comprimi tutto", KeyEvent.VK_C, new Runnable() {
            public void run() {
                if (albero != null) {
                    albero.collapseRow(0);
                    albero.espandiFinoA(1);
                }
            }
        }));
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

    private void cercaSlot() {
        modelloSlot.clear();
        final List<Rilevatore.Voce> trovate = Rilevatore.cercaTutti();
        for (Rilevatore.Voce v : trovate) {
            modelloSlot.addElement(v);
        }
        if (trovate.isEmpty()) {
            aggiornaStato("Nessun salvataggio trovato automaticamente. Usa File > Apri file...");
        } else {
            aggiornaStato(trovate.size() + " salvataggi trovati. Scegline uno.");
        }
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

    /**
     * Seleziona il primo campo che contiene un oggetto di gioco riconosciuto.
     *
     * Serve a mostrare il pannello di dettaglio popolato in una schermata: senza
     * selezione il pannello di destra resta vuoto e l'immagine non racconta
     * cosa sa fare il programma.
     */
    public boolean selezionaPrimoOggetto() {
        if (albero == null) {
            return false;
        }
        AlberoDati.Nodo trovato = primoOggetto(albero.radice());
        if (trovato == null) {
            return false;
        }
        albero.rivela(trovato);
        dettagli.mostra(trovato);
        return true;
    }

    private AlberoDati.Nodo primoOggetto(AlberoDati.Nodo nodo) {
        if (nodo.foglia() && nodo.valore() instanceof String
                && catalogo.conosciuto((String) nodo.valore())) {
            return nodo;
        }
        for (int i = 0; i < nodo.getChildCount(); i++) {
            AlberoDati.Nodo trovato = primoOggetto(nodo.getChildAt(i));
            if (trovato != null) {
                return trovato;
            }
        }
        return null;
    }

    /** Porta l'albero a una profondita' di apertura, per le schermate. */
    public void espandiA(int profondita) {
        if (albero != null) {
            albero.espandiFinoA(profondita);
        }
    }

    /** Scrive un messaggio nella barra di stato. */
    public void messaggioStato(String testo) {
        messaggio.setText(testo);
    }

    private void apri(File file) {        try {
            messaggio.setText("Apertura in corso...");
            Salvataggio nuovo = Salvataggio.apri(file, mappaChiavi());
            salvataggio = nuovo;
            costruisciAlbero();
            salva.setEnabled(true);
            salvaCome.setEnabled(true);
            testata.aggiorna(nuovo);
            aggiornaStato(nuovo.riepilogo());
            messaggio.setText("Verifica automatica attiva prima di ogni scrittura");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Non riesco ad aprire il file.\n\n" + e.getMessage(),
                    "Errore di apertura", JOptionPane.ERROR_MESSAGE);
            aggiornaStato("Apertura non riuscita.");
        }
    }

    private void costruisciAlbero() {
        albero = new AlberoDati(salvataggio.albero(), catalogo, icone, new Runnable() {
            public void run() {
                salvataggio.segnaModificato();
                testata.segnaModificato();
                messaggio.setText("Modifiche non salvate");
            }
        });
        albero.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                Object ultimo = e.getPath() == null ? null : e.getPath().getLastPathComponent();
                dettagli.mostra(ultimo instanceof AlberoDati.Nodo ? (AlberoDati.Nodo) ultimo : null);
            }
        });
        scorrimentoAlbero = new JScrollPane(albero);
        scorrimentoAlbero.setBorder(BorderFactory.createEmptyBorder());
        scorrimentoAlbero.getVerticalScrollBar().setUnitIncrement(18);
        scorrimentoAlbero.setBackground(Aspetto.PANNELLO);

        centro.removeAll();
        centro.add(scorrimentoAlbero, BorderLayout.CENTER);
        centro.revalidate();
        centro.repaint();
        albero.espandiFinoA(2);
    }

    private void applicaModifica(Object nuovoValore) {
        if (albero == null) {
            return;
        }
        AlberoDati.Nodo nodo = (AlberoDati.Nodo) albero.getLastSelectedPathComponent();
        if (nodo == null || !nodo.foglia()) {
            return;
        }
        nodo.scrivi(nuovoValore);
        salvataggio.segnaModificato();
        testata.segnaModificato();
        dettagli.mostra(nodo);
        albero.repaint();
        messaggio.setText("Modifiche non salvate");
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
        String testo = "Atlante — editor di salvataggi per No Man's Sky\n\n"
                + "Catalogo di gioco: " + catalogo.dimensione() + " oggetti\n"
                + "Icone di gioco: " + (cartellaIcone.isDirectory() ? cartellaIcone.getAbsolutePath() : "assenti") + "\n"
                + "Tema: " + (Aspetto.conFlatlaf() ? "FlatLaf" : "Nimbus (FlatLaf non trovato)") + "\n"
                + "Copia di sicurezza: " + (cartellaBackup == null ? "accanto al salvataggio" : cartellaBackup.getAbsolutePath()) + "\n\n"
                + "Prima di ogni scrittura il contenuto viene ricompattato, riscompattato\n"
                + "e confrontato campo per campo con l'originale. Se qualcosa cambia,\n"
                + "il file non viene toccato.\n\n"
                + "No Man's Sky e' un marchio di Hello Games. Questo progetto non e'\n"
                + "affiliato ne' approvato da Hello Games.";
        JOptionPane.showMessageDialog(this, testo, "Informazioni", JOptionPane.INFORMATION_MESSAGE);
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
        private final Pillola slot = new Pillola(Aspetto.ACCENTO);
        private final Pillola durata = new Pillola(Aspetto.INFO);
        private final Pillola dimensione = new Pillola(Aspetto.ATTENZIONE);
        private final Pillola modificato = new Pillola(Aspetto.ERRORE);
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

    /** Etichetta a pillola, per i dati in evidenza nella testata. */
    private static final class Pillola extends JLabel {
        private final Color colore;
        private String testo;

        Pillola(Color colore) {
            this.colore = colore;
            setOpaque(false);
            setFont(Aspetto.monospaziato(11, Font.PLAIN));
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
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(colore.getRed(), colore.getGreen(), colore.getBlue(), 40));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(colore);
            g2.setFont(getFont());
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(testo, 12, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Disegnatore dell'elenco slot a sinistra. */
    private final class DisegnatoreSlot extends JPanel
            implements ListCellRenderer<Rilevatore.Voce> {

        private final JLabel icona = new JLabel();
        private final JLabel titolo = new JLabel();
        private final JLabel dettaglio = new JLabel();

        DisegnatoreSlot() {
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 10));
            add(icona, BorderLayout.WEST);
            JPanel testi = new JPanel();
            testi.setOpaque(false);
            testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
            titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 12.5f));
            dettaglio.setFont(dettaglio.getFont().deriveFont(11f));
            dettaglio.setForeground(Aspetto.TESTO_DEBOLE);
            testi.add(titolo);
            testi.add(dettaglio);
            add(testi, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList<? extends Rilevatore.Voce> lista,
                                                      Rilevatore.Voce valore, int indice,
                                                      boolean selezionato, boolean conFuoco) {
            setBackground(selezionato ? Aspetto.ACCENTO_SCURO : Aspetto.PANNELLO);
            titolo.setForeground(selezionato ? new Color(0xFFE9E9) : Aspetto.TESTO);
            icona.setIcon(Icone.segno(valore.formato == it.atlante.nms.Formato.BLOCCHI ? "S" : "A",
                    28, selezionato ? Aspetto.ACCENTO_CHIARO : Aspetto.TESTO_DEBOLE, true));
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
        elencoSlot.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                Rilevatore.Voce v = elencoSlot.getSelectedValue();
                if (v != null) {
                    apri(v.file);
                }
            }
        });
        cerca.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (albero == null) {
                    return;
                }
                AlberoDati.Nodo trovato = albero.cerca(cerca.getText());
                if (trovato != null) {
                    albero.rivela(trovato);
                    dettagli.mostra(trovato);
                    messaggio.setText("Trovato: " + trovato.chiave());
                } else {
                    messaggio.setText("Nessun campo corrisponde a \"" + cerca.getText() + "\"");
                }
            }
        });
    }
}

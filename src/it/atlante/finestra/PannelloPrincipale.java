package it.atlante.finestra;

import it.atlante.nms.Catalogo;
import it.atlante.nms.Salvataggio;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * La schermata iniziale: le informazioni del file e le azioni rapide.
 *
 * E' la prima cosa che si vede aprendo un salvataggio, e risponde alle domande
 * che uno si fa davvero: quale slot ho aperto, dove sta il file, quanto e'
 * grande, quando e' stato salvato l'ultima volta.
 *
 * Sotto stanno le azioni rapide: quello che nel vecchio editor si faceva con
 * un pulsante. Ognuna riferisce quante cose ha cambiato, cosi' si sa cosa e'
 * successo prima di salvare.
 */
public final class PannelloPrincipale extends JPanel {

    private final Catalogo catalogo;
    private final Icone icone;

    private final JLabel deposito = new JLabel("—");
    private final JLabel percorso = new JLabel("—");
    private final JLabel slotGioco = new JLabel("—");
    private final JLabel file = new JLabel("—");
    private final JLabel modificato = new JLabel("—");
    private final JLabel nomeSalvataggio = new JLabel("—");
    private final JLabel formato = new JLabel("—");
    private final JLabel dimensioni = new JLabel("—");

    /**
     * Data finta per "Ultima modifica", quando serve una schermata ripetibile.
     *
     * L'ora vera del file di prova cambia a ogni copia, quindi la stessa
     * schermata rigenerata il giorno dopo risulterebbe diversa senza che sia
     * cambiato niente nell'interfaccia. Con una data fissata, il confronto fra
     * due generazioni dice qualcosa di vero. Vale solo per le schermate: nel
     * programma la data e' quella del file.
     */
    private static Long dataFinta;

    private final JButton ricarica = new JButton("Ricarica");
    private final JButton salva = new JButton("Salva modifiche");
    private final JButton salvaCome = new JButton("Salva con nome...");

    private Salvataggio salvataggio;
    private Runnable suModifica;
    private Runnable suRicarica;
    private Runnable suSalva;
    private Runnable suSalvaCome;
    private java.util.function.Consumer<String> suMessaggio;

    public PannelloPrincipale(Catalogo catalogo, Icone icone) {        this.catalogo = catalogo;
        this.icone = icone;
        setLayout(new BorderLayout());
        setBackground(Aspetto.FONDO);

        JPanel colonna = new JPanel();
        colonna.setLayout(new BoxLayout(colonna, BoxLayout.Y_AXIS));
        colonna.setBackground(Aspetto.FONDO);
        colonna.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        colonna.add(costruisciSchedaFile());
        colonna.add(Box.createVerticalStrut(18));
        colonna.add(costruisciAzioniRapide());
        colonna.add(Box.createVerticalGlue());

        JPanel involucro = new JPanel(new BorderLayout());
        involucro.setBackground(Aspetto.FONDO);
        // Il contenuto va al centro, non in alto: in alto il pannello userebbe
        // la sua larghezza preferita e i pulsanti delle azioni rapide
        // uscirebbero dallo schermo a destra.
        involucro.add(colonna, BorderLayout.CENTER);

        javax.swing.JScrollPane scorrimento = new javax.swing.JScrollPane(involucro);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(18);
        add(scorrimento, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------

    private JPanel costruisciSchedaFile() {
        JPanel scheda = scheda("Dettagli del file");

        JPanel righe = new JPanel();
        righe.setLayout(new BoxLayout(righe, BoxLayout.Y_AXIS));
        righe.setOpaque(false);
        aggiungi(righe, "Deposito", deposito);
        aggiungi(righe, "Percorso del salvataggio", percorso);
        aggiungi(righe, "Slot di gioco", slotGioco);
        aggiungi(righe, "File", file);
        aggiungi(righe, "Ultima modifica", modificato);
        aggiungi(righe, "Formato", formato);
        aggiungi(righe, "Dimensioni", dimensioni);
        aggiungi(righe, "Nome del salvataggio", nomeSalvataggio);
        scheda.add(righe, BorderLayout.CENTER);

        JPanel comandi = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        comandi.setOpaque(false);
        comandi.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        ricarica.setFont(ricarica.getFont().deriveFont(12.5f));
        salva.setFont(salva.getFont().deriveFont(Font.BOLD, 12.5f));
        salvaCome.setFont(salvaCome.getFont().deriveFont(12.5f));
        comandi.add(ricarica);
        comandi.add(salva);
        comandi.add(salvaCome);
        scheda.add(comandi, BorderLayout.SOUTH);
        return scheda;
    }

    private JPanel costruisciAzioniRapide() {
        JPanel scheda = scheda("Azioni rapide");

        JLabel nota = new JLabel("Modificano il salvataggio aperto. Niente viene scritto su disco "
                + "finché non premi Salva.");
        nota.setFont(nota.getFont().deriveFont(11.5f));
        nota.setForeground(Aspetto.TESTO_DEBOLE);
        nota.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        scheda.add(nota, BorderLayout.NORTH);

        JPanel griglia = new JPanel(new java.awt.GridLayout(0, 3, 10, 10));
        griglia.setOpaque(false);

        griglia.add(pulsanteAzione("Ripara tutti gli slot",
                "Riporta a zero il danno di ogni slot di ogni inventario", "ripara"));
        griglia.add(pulsanteAzione("Ricarica le tecnologie",
                "Porta al massimo la carica di ogni tecnologia installata", "ricarica"));
        griglia.add(pulsanteAzione("Unità al massimo",
                "Imposta le unità a 4.294.967.295", "unita"));
        griglia.add(pulsanteAzione("Naniti al massimo",
                "Imposta i naniti a 4.294.967.295", "naniti"));
        griglia.add(pulsanteAzione("Quicksilver al massimo",
                "Imposta il Quicksilver a 4.294.967.295", "quicksilver"));
        griglia.add(pulsanteAzione("Aggiungi 100.000.000 unità",
                "Somma alle unità esistenti invece di sostituirle", "aggiungiunita"));

        scheda.add(griglia, BorderLayout.CENTER);
        return scheda;
    }

    private JButton pulsanteAzione(String titolo, String spiegazione, final String codice) {
        JButton b = new JButton(titolo);
        b.setFont(b.getFont().deriveFont(12f));
        b.setToolTipText(spiegazione);
        b.setPreferredSize(new Dimension(220, 44));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                esegui(codice);
            }
        });
        return b;
    }

    private JPanel scheda(String titolo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Aspetto.PANNELLO);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Aspetto.BORDO),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = new JLabel(titolo.toUpperCase());
        t.setFont(Aspetto.monospaziato(10.5f, Font.BOLD));
        t.setForeground(Aspetto.ACCENTO);
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        p.add(t, BorderLayout.NORTH);
        return p;
    }

    private void aggiungi(JPanel dove, String etichetta, JLabel valore) {
        JPanel riga = new JPanel(new BorderLayout(14, 0));
        riga.setOpaque(false);
        riga.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JLabel nome = new JLabel(etichetta);
        nome.setFont(nome.getFont().deriveFont(12f));
        nome.setForeground(Aspetto.TESTO_DEBOLE);
        nome.setPreferredSize(new Dimension(190, 20));
        riga.add(nome, BorderLayout.WEST);
        valore.setFont(Aspetto.monospaziato(12, Font.PLAIN));
        valore.setForeground(Aspetto.TESTO);
        riga.add(valore, BorderLayout.CENTER);
        dove.add(riga);
    }

    // ------------------------------------------------------------------

    public void collega(Runnable suRicarica, Runnable suSalva, Runnable suSalvaCome,
                        Runnable suModifica, java.util.function.Consumer<String> suMessaggio) {
        this.suRicarica = suRicarica;
        this.suSalva = suSalva;
        this.suSalvaCome = suSalvaCome;
        this.suModifica = suModifica;
        this.suMessaggio = suMessaggio;

        ricarica.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (PannelloPrincipale.this.suRicarica != null) {
                    PannelloPrincipale.this.suRicarica.run();
                }
            }
        });
        salva.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (PannelloPrincipale.this.suSalva != null) {
                    PannelloPrincipale.this.suSalva.run();
                }
            }
        });
        salvaCome.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (PannelloPrincipale.this.suSalvaCome != null) {
                    PannelloPrincipale.this.suSalvaCome.run();
                }
            }
        });
    }

    /** Aggiorna le informazioni del file. */
    public void aggiorna(Salvataggio s, String piattaforma, String etichettaSlot) {
        this.salvataggio = s;
        if (s == null) {
            deposito.setText("—");
            percorso.setText("—");
            slotGioco.setText("—");
            file.setText("—");
            modificato.setText("—");
            formato.setText("—");
            dimensioni.setText("—");
            nomeSalvataggio.setText("—");
            return;
        }
        deposito.setText(piattaforma == null ? "—" : piattaforma);
        percorso.setText(s.file().getParent());
        percorso.setToolTipText(s.file().getParent());
        slotGioco.setText(etichettaSlot == null ? "—" : etichettaSlot);
        file.setText(s.file().getName());
        modificato.setText(new SimpleDateFormat("d MMMM yyyy, HH:mm", java.util.Locale.ITALIAN)
                .format(new Date(dataFinta == null ? s.file().lastModified() : dataFinta.longValue())));
        formato.setText(s.formato().descrizione() + " · " + s.blocchi() + " blocchi");
        dimensioni.setText(Salvataggio.mb(s.dimensioneCompressa()) + " compressi, "
                + Salvataggio.mb(s.dimensioneDecompressa()) + " decompressi");
        nomeSalvataggio.setText(nomeDalSalvataggio());
        salva.setEnabled(true);
        salvaCome.setEnabled(true);
    }

    /**
     * Fissa la data mostrata in "Ultima modifica".
     *
     * La usano gli strumenti che generano le schermate della documentazione:
     * senza, l'immagine cambierebbe ogni giorno da sola.
     */
    static void fissaDataModifica(long millisecondi) {
        dataFinta = Long.valueOf(millisecondi);
    }

    /** Il nome del salvataggio, letto dal campo SaveName. */
    @SuppressWarnings("unchecked")
    private String nomeDalSalvataggio() {
        if (salvataggio == null || !(salvataggio.albero() instanceof java.util.Map)) {
            return "—";
        }
        Object comune = ((java.util.Map<String, Object>) salvataggio.albero()).get("CommonStateData");
        if (comune instanceof java.util.Map) {
            Object nome = ((java.util.Map<String, Object>) comune).get("SaveName");
            if (nome != null) {
                return String.valueOf(nome);
            }
        }
        return "—";
    }

    // ------------------------------------------------------------------

    private void esegui(String codice) {
        if (salvataggio == null) {
            avvisa("Nessun salvataggio aperto", "Apri prima un salvataggio.");
            return;
        }
        Object albero = salvataggio.albero();
        Azioni.Esito esito;

        if ("ripara".equals(codice)) {
            esito = Azioni.riparaTutto(albero);
        } else if ("ricarica".equals(codice)) {
            esito = Azioni.ricaricaTutto(albero);
        } else if ("unita".equals(codice)) {
            esito = Azioni.impostaNumero(albero, "Units", "4294967295");
        } else if ("naniti".equals(codice)) {
            esito = Azioni.impostaNumero(albero, "Nanites", "4294967295");
        } else if ("quicksilver".equals(codice)) {
            esito = Azioni.impostaNumero(albero, "Specials", "4294967295");
        } else if ("aggiungiunita".equals(codice)) {
            esito = Azioni.aggiungiNumero(albero, "Units", "100000000");
        } else {
            return;
        }

        if (esito.toccati > 0) {
            salvataggio.segnaModificato();
            if (suModifica != null) {
                suModifica.run();
            }
        }
        if (suMessaggio != null) {
            suMessaggio.accept(esito.descrizione + " — " + esito.toccati
                    + (esito.toccati == 1 ? " punto" : " punti"));
        }
        avvisa(esito.toccati > 0 ? "Fatto" : "Niente da fare",
                esito.descrizione + ".\n\nPunti modificati: " + esito.toccati
                + (esito.toccati > 0 ? "\n\nRicorda di salvare per scrivere il file." : ""));
    }

    private void avvisa(String titolo, String testo) {
        JOptionPane.showMessageDialog(this, testo, titolo, JOptionPane.INFORMATION_MESSAGE);
    }
}

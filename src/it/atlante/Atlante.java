package it.atlante;

import it.atlante.finestra.Aspetto;
import it.atlante.finestra.Finestra;
import it.atlante.nms.Catalogo;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

/**
 * Avvio dell'interfaccia grafica.
 *
 * Prima di mostrare la finestra carica il catalogo di gioco (2,2 MB di XML,
 * 5.240 voci) mostrando un avviso di attesa: senza catalogo l'editor
 * funzionerebbe lo stesso, ma mostrerebbe sigle invece di nomi e icone, e non
 * vale la pena far aspettare l'utente davanti a una finestra vuota.
 *
 * Se il catalogo manca o e' illeggibile il programma parte comunque, in modo
 * ridotto: e' meglio un editor senza icone che nessun editor.
 */
public final class Atlante {

    public static void main(String[] args) {
        Aspetto.installa();

        final JWindow attesa = avvisoAttesa();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Catalogo catalogo = caricaCatalogo();
                File cartellaIcone = cartella("risorse/icone");
                File cartellaBackup = cartella("backup");

                Finestra finestra = new Finestra(catalogo, cartellaIcone, cartellaBackup);
                finestra.setVisible(true);

                if (attesa != null) {
                    attesa.dispose();
                }
            }
        });
    }

    private static Catalogo caricaCatalogo() {
        File items = file("risorse/db/items.xml");
        if (items == null) {
            System.err.println("catalogo non trovato: risorse/db/items.xml — si prosegue senza nomi di gioco");
            return Catalogo.vuoto();
        }
        try {
            return Catalogo.carica(items);
        } catch (Exception e) {
            System.err.println("catalogo illeggibile: " + e.getMessage() + " — si prosegue senza nomi di gioco");
            return Catalogo.vuoto();
        }
    }

    /** Cerca un file sia con la barra sia con il separatore di sistema. */
    private static File file(String percorso) {
        File f = new File(percorso);
        if (f.isFile()) {
            return f;
        }
        f = new File(percorso.replace('/', File.separatorChar));
        return f.isFile() ? f : null;
    }

    private static File cartella(String percorso) {
        File f = new File(percorso.replace('/', File.separatorChar));
        return f.isDirectory() ? f : new File(percorso);
    }

    private static JWindow avvisoAttesa() {
        JWindow finestra = new JWindow();
        JPanel pannello = new JPanel(new BorderLayout(14, 0));
        pannello.setBackground(Aspetto.PANNELLO);
        pannello.setBorder(new EmptyBorder(26, 38, 26, 38));

        // Il marchio compare anche durante il caricamento: e' la prima cosa che
        // l'utente vede dell'applicazione.
        java.awt.Image icona = it.atlante.finestra.Marchio.icona();
        if (icona != null) {
            JLabel marchio = new JLabel(new javax.swing.ImageIcon(
                    icona.getScaledInstance(56, 56, java.awt.Image.SCALE_SMOOTH)));
            pannello.add(marchio, BorderLayout.WEST);
        }

        JPanel testi = new JPanel(new BorderLayout(0, 6));
        testi.setOpaque(false);
        JLabel titolo = new JLabel("ATLANTE", SwingConstants.LEFT);
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 22f));
        titolo.setForeground(Aspetto.TESTO);
        JLabel sotto = new JLabel("caricamento del catalogo di gioco...", SwingConstants.LEFT);
        sotto.setFont(sotto.getFont().deriveFont(12f));
        sotto.setForeground(Aspetto.TESTO_TENUE);
        testi.add(titolo, BorderLayout.NORTH);
        testi.add(sotto, BorderLayout.SOUTH);
        pannello.add(testi, BorderLayout.CENTER);

        pannello.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(0x4A3238)),
                new EmptyBorder(24, 34, 24, 40)));

        finestra.setContentPane(pannello);
        it.atlante.finestra.Marchio.applicaA(finestra);
        finestra.pack();
        finestra.setLocationRelativeTo(null);
        finestra.setVisible(true);
        return finestra;
    }

    private Atlante() {
    }
}

package it.atlante.strumenti;

import it.atlante.finestra.Aspetto;
import it.atlante.finestra.Finestra;
import it.atlante.nms.Catalogo;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Genera le schermate per la documentazione fotografando l'interfaccia vera.
 *
 * Non sono disegni preparati a parte: il programma apre un salvataggio reale,
 * apre l'albero, seleziona un oggetto di gioco e fotografa la finestra. Se
 * l'interfaccia cambia, le immagini si rigenerano e restano vere.
 *
 * Uso:
 * <pre>
 *   java -cp "classi;lib/flatlaf.jar" it.atlante.strumenti.Schermate [cartella]
 * </pre>
 */
public final class Schermate {

    private Schermate() {
    }

    public static void main(String[] args) throws Exception {
        final File uscita = new File(args.length > 0 ? args[0] : "schermate");
        if (!uscita.isDirectory() && !uscita.mkdirs()) {
            throw new IllegalStateException("non riesco a creare la cartella " + uscita);
        }
        Aspetto.installa();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    genera(uscita);
                } catch (Exception e) {
                    System.err.println("errore: " + e);
                    e.printStackTrace();
                }
            }
        });
        System.out.println("schermate scritte in " + uscita.getAbsolutePath());
    }

    private static void genera(File uscita) throws Exception {
        File items = new File("risorse/db/items.xml");
        if (!items.isFile()) {
            items = new File("risorse" + File.separator + "db" + File.separator + "items.xml");
        }
        Catalogo catalogo = Catalogo.carica(items);
        System.out.println("catalogo: " + catalogo.dimensione() + " oggetti");

        Finestra finestra = new Finestra(catalogo,
                new File("risorse" + File.separator + "icone"),
                new File("backup"));
        finestra.setSize(1500, 940);
        finestra.setVisible(true);

        if (!finestra.apriPrimoTrovato()) {
            System.err.println("nessun salvataggio trovato: la schermata sara' vuota");
        }
        finestra.espandiA(3);
        finestra.selezionaPrimoOggetto();
        finestra.messaggioStato("Verifica automatica attiva prima di ogni scrittura");

        // Il layout ha bisogno di un giro completo prima di essere dipinto.
        for (int i = 0; i < 6; i++) {
            finestra.validate();
            finestra.doLayout();
            Thread.sleep(90);
        }

        BufferedImage immagine = new BufferedImage(
                finestra.getWidth(), finestra.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = immagine.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        finestra.paintAll(g);
        g.dispose();

        File file = new File(uscita, "atlante-principale.png");
        ImageIO.write(immagine, "png", file);
        System.out.println("scritto " + file.getAbsolutePath()
                + "  (" + finestra.getWidth() + "x" + finestra.getHeight() + ")");

        finestra.dispose();
    }
}

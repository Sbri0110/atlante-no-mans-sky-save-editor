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

        // Diagnostica: cosa trova il rilevatore e in che ordine.
        System.out.println("salvataggi trovati dal rilevatore:");
        for (it.atlante.nms.Rilevatore.Voce v : it.atlante.nms.Rilevatore.cercaTutti()) {
            System.out.println("   " + v.formato + "  " + v.piattaforma + "  " + v.etichetta
                    + "  -> " + v.file.getName());
        }
        System.out.println("cartella WGS: " + it.atlante.nms.Rilevatore.cartellaWgs());
        System.out.println("cartella Steam: " + it.atlante.nms.Rilevatore.cartellaSteam());

        // Diagnostica del catalogo: l'icona di un oggetto noto.
        it.atlante.nms.Catalogo.Voce prova = catalogo.voce("^FUEL2");
        System.out.println("catalogo ^FUEL2: " + (prova == null ? "NON TROVATO"
                : prova.etichetta() + " / icona=" + prova.icona));
        File cartellaIcone = new File("risorse" + File.separator + "icone");
        System.out.println("cartella icone: " + cartellaIcone.getAbsolutePath()
                + "  esiste=" + cartellaIcone.isDirectory());
        if (prova != null && prova.icona != null) {
            File f = new File(cartellaIcone, prova.icona);
            System.out.println("file icona: " + f.getAbsolutePath() + "  esiste=" + f.isFile());
            javax.swing.ImageIcon ii = new javax.swing.ImageIcon(f.getAbsolutePath());
            System.out.println("decodifica: " + ii.getIconWidth() + "x" + ii.getIconHeight());
        }

        Finestra finestra = new Finestra(catalogo,
                new File("risorse" + File.separator + "icone"),
                new File("backup"));
        finestra.setSize(1500, 940);
        finestra.setVisible(true);

        if (!finestra.apriPrimoTrovato()) {
            System.err.println("nessun salvataggio trovato: la schermata sara' vuota");
        }

        // Una schermata per sezione: mostrano la navigazione in uso.
        String[][] viste = {
                {"Principale", "atlante-principale"},
                {"Tuta", "atlante-tuta"},
                {"Navi", "atlante-navi"},
                {"Squadrone", "atlante-squadrone"},
                {"Fregate", "atlante-fregate"},
        };
        for (String[] vista : viste) {
            finestra.mostraSezionePerNome(vista[0]);
            // Tre livelli bastano per arrivare ai valori in tutte le sezioni:
            // contenitore, gruppo, campo.
            finestra.espandiA(3);
            if (vista[0].equals("Tuta")) {
                System.out.println("collaudo selezione Tuta:");
                System.out.println("  slot: " + finestra.quantiSlot()
                        + ", primo occupato: " + finestra.primoOccupato());
                System.out.println("  clic simulato -> " + finestra.provaSelezione());
                finestra.selezionaSlot(finestra.primoOccupato());
                System.out.println("  selezionato dopo il clic: " + finestra.slotSelezionato());
            }
            scatta(finestra, new File(uscita, vista[1] + ".png"), vista[0]);
        }

        finestra.dispose();
    }

    private static void scatta(Finestra finestra, File destinazione, String sezione) throws Exception {
        for (int i = 0; i < 6; i++) {
            finestra.validate();
            finestra.doLayout();
            Thread.sleep(80);
        }
        // La griglia sceglie le colonne in base allo spazio: va rifatto qui,
        // perche' durante lo scatto il ridimensionamento e' gia' avvenuto.
        finestra.ridisponi();
        finestra.validate();
        Thread.sleep(150);
        BufferedImage immagine = new BufferedImage(
                finestra.getWidth(), finestra.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = immagine.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        finestra.paintAll(g);
        g.dispose();
        ImageIO.write(immagine, "png", destinazione);
        System.out.println("scritto " + destinazione.getName()
                + "  (" + finestra.getWidth() + "x" + finestra.getHeight() + ", sezione " + sezione + ")");
    }
}

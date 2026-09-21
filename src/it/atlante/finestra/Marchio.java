package it.atlante.finestra;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Window;
import java.io.File;

/**
 * Il marchio dell'applicazione.
 *
 * L'icona della finestra e della barra delle applicazioni deve essere sempre il
 * rombo rosso di Atlante: viene cercata in piu' formati, cosi' funziona sia
 * eseguendo dal progetto sia da un pacchetto costruito.
 *
 * Nota sul formato: l'icona di Windows ({@code .ico}) contiene piu' risoluzioni
 * e serve al pacchetto eseguibile, ma {@code ImageIO} non la legge. Per la
 * finestra si usa quindi un PNG, che Java decodifica senza problemi; l'ICO resta
 * per il sistema operativo e per il browser.
 */
public final class Marchio {

    /** Percorsi provati in ordine, dal piu' grande al piu' piccolo. */
    private static final String[] CANDIDATI = {
            "assets/marchio/app-icon-256.png",
            "assets/marchio/logo-256.png",
            "assets/marchio/logo-128.png",
            "assets/marchio/logo-64.png",
            "assets/marchio/logo-48.png",
            "assets/marchio/logo-32.png",
    };

    private static Image icona;
    private static boolean cercata;

    private Marchio() {
    }

    /** L'immagine del marchio, o null se i file non si trovano. */
    public static synchronized Image icona() {
        if (!cercata) {
            cercata = true;
            for (String percorso : CANDIDATI) {
                File f = new File(percorso);
                if (!f.isFile()) {
                    f = new File(percorso.replace('/', File.separatorChar));
                }
                if (f.isFile()) {
                    ImageIcon i = new ImageIcon(f.getAbsolutePath());
                    if (i.getIconWidth() > 0) {
                        icona = i.getImage();
                        break;
                    }
                }
            }
        }
        return icona;
    }

    /**
     * Applica il marchio a una finestra.
     *
     * Java accetta piu' immagini di risoluzioni diverse: il sistema sceglie
     * quella giusta per la barra delle applicazioni, il selettore di finestre e
     * le anteprime. Passandone una sola, Windows riscala e perde i tratti
     * sottili del rombo.
     */
    public static void applicaA(Window finestra) {
        if (finestra == null) {
            return;
        }
        Image principale = icona();
        if (principale == null) {
            return;
        }
        java.util.List<Image> insieme = new java.util.ArrayList<Image>();
        insieme.add(principale);
        for (String percorso : new String[]{
                "assets/marchio/logo-64.png", "assets/marchio/logo-48.png",
                "assets/marchio/logo-32.png", "assets/marchio/logo-16.png"}) {
            File f = new File(percorso.replace('/', File.separatorChar));
            if (f.isFile()) {
                ImageIcon i = new ImageIcon(f.getAbsolutePath());
                if (i.getIconWidth() > 0) {
                    insieme.add(i.getImage());
                }
            }
        }
        try {
            finestra.setIconImages(insieme);
        } catch (Throwable e) {
            // Su finestre senza supporto alle icone multiple si ripiega.
            finestra.setIconImage(principale);
        }
    }

    /** Vero se il marchio e' stato trovato. */
    public static boolean disponibile() {
        return icona() != null;
    }
}

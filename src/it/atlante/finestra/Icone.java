package it.atlante.finestra;

import it.atlante.nms.Catalogo;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Le icone dell'interfaccia.
 *
 * Due origini: le icone degli oggetti di gioco, che stanno in una cartella di
 * PNG con il nome indicato dal catalogo, e i segni disegnati a mano per gli
 * elementi che il gioco non illustra (nodi di struttura, cartelle, valori).
 *
 * Le immagini vengono ridimensionate una volta sola e tenute in memoria: un
 * albero con qualche migliaio di nodi visibili non deve ridimensionare la stessa
 * icona a ogni ridisegno.
 */
public final class Icone {

    private static final Map<String, ImageIcon> CACHE = new HashMap<String, ImageIcon>();

    private final File cartella;
    private final Catalogo catalogo;

    public Icone(File cartella, Catalogo catalogo) {
        this.cartella = cartella;
        this.catalogo = catalogo;
    }

    /**
     * Icona di un oggetto a partire dal suo identificatore di salvataggio.
     *
     * @param id    identificatore, per esempio {@code ^CASING}
     * @param lato  lato in pixel
     * @return l'icona, oppure null se l'oggetto non ha un'immagine
     */
    public ImageIcon perId(String id, int lato) {
        if (id == null) {
            return null;
        }
        Catalogo.Voce voce = catalogo.voce(id);
        if (voce == null || voce.icona == null) {
            return null;
        }
        return perFile(voce.icona, lato);
    }

    /** Icona a partire dal nome del file, per esempio {@code PRODUCT-CASING.PNG}. */
    public ImageIcon perFile(String nomeFile, int lato) {
        if (nomeFile == null || nomeFile.isEmpty()) {
            return null;
        }
        String chiave = nomeFile + "@" + lato;
        ImageIcon inCache = CACHE.get(chiave);
        if (inCache != null) {
            return inCache;
        }
        File f = new File(cartella, nomeFile);
        if (!f.isFile()) {
            // I nomi nel catalogo sono in maiuscolo: si prova anche il minuscolo.
            f = new File(cartella, nomeFile.toLowerCase());
            if (!f.isFile()) {
                return null;
            }
        }
        ImageIcon originale = new ImageIcon(f.getAbsolutePath());
        if (originale.getIconWidth() <= 0) {
            return null;
        }
        ImageIcon ridotta = ridimensiona(originale, lato);
        CACHE.put(chiave, ridotta);
        return ridotta;
    }

    /**
     * Segno disegnato per gli elementi senza illustrazione.
     *
     * @param segno     uno o due caratteri
     * @param lato      lato in pixel
     * @param colore    colore del segno
     * @param riempito  se vero il fondo e' pieno, altrimenti solo il contorno
     */
    public static ImageIcon segno(String segno, int lato, Color colore, boolean riempito) {
        String chiave = "segno:" + segno + "@" + lato + ":" + colore.getRGB() + ":" + riempito;
        ImageIcon inCache = CACHE.get(chiave);
        if (inCache != null) {
            return inCache;
        }
        BufferedImage img = new BufferedImage(lato, lato, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (riempito) {
            g.setColor(new Color(colore.getRed(), colore.getGreen(), colore.getBlue(), 46));
            g.fillRoundRect(0, 0, lato, lato, lato / 3, lato / 3);
        }
        g.setColor(colore);
        g.setStroke(new java.awt.BasicStroke(1.4f));
        g.drawRoundRect(0, 0, lato - 1, lato - 1, lato / 3, lato / 3);

        if (segno != null && !segno.isEmpty()) {
            int dimensione = Math.max(8, Math.round(lato * 0.52f));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, dimensione));
            java.awt.FontMetrics fm = g.getFontMetrics();
            int x = (lato - fm.stringWidth(segno)) / 2;
            int y = (lato - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(segno, x, y);
        }
        g.dispose();
        ImageIcon icona = new ImageIcon(img);
        CACHE.put(chiave, icona);
        return icona;
    }

    private static ImageIcon ridimensiona(ImageIcon originale, int lato) {
        int larghezza = originale.getIconWidth();
        int altezza = originale.getIconHeight();
        // Si conserva la proporzione: le icone di gioco sono quadrate, ma se una
        // non lo fosse non va deformata.
        double fattore = Math.min(lato / (double) larghezza, lato / (double) altezza);
        int nuovaLarghezza = Math.max(1, (int) Math.round(larghezza * fattore));
        int nuovaAltezza = Math.max(1, (int) Math.round(altezza * fattore));

        BufferedImage img = new BufferedImage(lato, lato, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = (lato - nuovaLarghezza) / 2;
        int y = (lato - nuovaAltezza) / 2;
        g.drawImage(originale.getImage(), x, y, nuovaLarghezza, nuovaAltezza, null);
        g.dispose();
        return new ImageIcon(img);
    }

    /** Svuota la memoria delle icone: si usa cambiando catalogo. */
    public static void svuotaCache() {
        CACHE.clear();
    }
}

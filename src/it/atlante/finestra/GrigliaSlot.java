package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * La griglia degli slot di un inventario.
 *
 * Struttura presa dal vecchio editor: gli slot si vedono tutti insieme, con
 * l'icona dell'oggetto, il nome di gioco e la quantita'. Aspetto nostro: ogni
 * slot e' una scheda con angoli arrotondati, la quantita' sta in una pillola,
 * e lo stato si legge a colpo d'occhio senza aprire niente.
 *
 * Tre informazioni che il vecchio editor non dava e che qui si vedono subito:
 * <ul>
 *   <li>gli slot <b>super-caricati</b> hanno un angolo d'accento: sono quelli
 *       che nel gioco danno il bonus, e cercarli scorrendo cento caselle era
 *       il modo piu' lento possibile;</li>
 *   <li>gli oggetti <b>danneggiati</b> mostrano una pillola rossa con il danno:
 *       si vedono a colpo d'occhio invece di doverli aprire uno per uno;</li>
 *   <li>gli slot <b>liberi</b> hanno il contorno tratteggiato, cosi' si distingue
 *       "vuoto" da "non esiste".</li>
 * </ul>
 */
public final class GrigliaSlot extends JPanel {

    /** Chi vuole sapere cosa succede nella griglia. */
    public interface Ascoltatore {
        /** Uno slot e' stato scelto. */
        void scelto(int indice);

        /** Il contenuto di uno slot e' cambiato. */
        void modificato();
    }

    private static final int LATO = 112;
    private static final int DISTANZA = 7;
    private static final int COLONNE_PREDEFINITE = 10;

    private final Catalogo catalogo;
    private final Icone icone;
    private final Ascoltatore ascoltatore;
    private final Colonna contenitore = new Colonna();
    private final JScrollPane scorrimento;

    private List<Object> slot = new ArrayList<Object>();
    private List<Integer> superCaricati = new ArrayList<Integer>();
    private Cella selezionata;
    private String nomeInventario = "";
    private int larghezzaInventario = COLONNE_PREDEFINITE;

    public GrigliaSlot(Catalogo catalogo, Icone icone, Ascoltatore ascoltatore) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.ascoltatore = ascoltatore;

        setLayout(new java.awt.BorderLayout());
        setBackground(Aspetto.PANNELLO);

        contenitore.setBackground(Aspetto.PANNELLO);
        contenitore.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        scorrimento = new JScrollPane(contenitore);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(20);
        scorrimento.setBackground(Aspetto.PANNELLO);
        add(scorrimento, java.awt.BorderLayout.CENTER);
    }

    /**
     * Non serve piu' calcolare le colonne: il layout va a capo da solo.
     *
     * Il metodo resta perche' chi lo chiama (la finestra, lo strumento delle
     * schermate) non deve sapere come e' fatto il layout interno.
     */
    public void ricalcolaColonne() {
        contenitore.revalidate();
        contenitore.repaint();
    }

    /**
     * Layout che dispone le schede su piu' righe e sa dire quante ne occupa.
     *
     * {@code FlowLayout} manda a capo correttamente, ma il suo metodo di
     * {@code getPreferredSize} calcola la dimensione come se tutto stesse su una
     * riga sola. Dentro un pannello scorrevole questo significa che il
     * contenitore dichiara l'altezza di una riga, la barra di scorrimento non
     * compare, e si vede solo la prima riga di slot: esattamente quello che
     * succedeva.
     *
     * Qui la dimensione preferita viene ricalcolata contando le righe in base
     * alla larghezza disponibile.
     */
    private static final class LayoutACapo extends java.awt.FlowLayout {

        LayoutACapo(int allineamento, int distanzaOrizzontale, int distanzaVerticale) {
            super(allineamento, distanzaOrizzontale, distanzaVerticale);
        }

        @Override
        public Dimension preferredLayoutSize(Container destinazione) {
            return misura(destinazione, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container destinazione) {
            return misura(destinazione, false);
        }

        private Dimension misura(Container destinazione, boolean preferito) {
            synchronized (destinazione.getTreeLock()) {
                int disponibile = destinazione.getWidth();
                if (disponibile <= 0) {
                    disponibile = Integer.MAX_VALUE;
                }
                int larghezzaRiga = 0;
                int altezzaTotale = 0;
                int altezzaRiga = 0;
                for (Component c : destinazione.getComponents()) {
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = preferito ? c.getPreferredSize() : c.getMinimumSize();
                    if (larghezzaRiga > 0 && larghezzaRiga + d.width > disponibile) {
                        altezzaTotale += altezzaRiga + getVgap();
                        larghezzaRiga = 0;
                        altezzaRiga = 0;
                    }
                    larghezzaRiga += d.width + getHgap();
                    altezzaRiga = Math.max(altezzaRiga, d.height);
                }
                altezzaTotale += altezzaRiga;
                java.awt.Insets margini = destinazione.getInsets();
                return new Dimension(destinazione.getWidth(),
                        altezzaTotale + margini.top + margini.bottom);
            }
        }
    }

    /** Colonna che si adatta in larghezza e dispone le schede andando a capo. */
    private static final class Colonna extends JPanel implements Scrollable {
        Colonna() {
            setLayout(new LayoutACapo(java.awt.FlowLayout.LEFT, DISTANZA, DISTANZA));
            setBackground(Aspetto.PANNELLO);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibile, int orientamento, int direzione) {
            return 20;
        }

        public int getScrollableBlockIncrement(Rectangle visibile, int orientamento, int direzione) {
            return visibile.height;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ------------------------------------------------------------------

    /**
     * Mostra un inventario.
     *
     * @param inventario la mappa dell'inventario (con Slots, Width, SpecialSlots)
     * @param nome       il nome da mostrare sopra la griglia
     */
    @SuppressWarnings("unchecked")
    public void mostra(Object inventario, String nome) {
        this.nomeInventario = nome;
        contenitore.removeAll();
        selezionata = null;

        if (!(inventario instanceof Map)) {
            contenitore.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            javax.swing.JLabel vuoto = new javax.swing.JLabel("Questo inventario non c'e' in questo salvataggio.");
            vuoto.setForeground(Aspetto.TESTO_DEBOLE);
            contenitore.add(vuoto);
            contenitore.revalidate();
            contenitore.repaint();
            return;
        }
        Map<String, Object> mappa = (Map<String, Object>) inventario;

        Object elenco = mappa.get("Slots");
        slot = elenco instanceof List ? (List<Object>) elenco : new ArrayList<Object>();

        superCaricati = new ArrayList<Integer>();
        Object speciali = mappa.get("SpecialSlots");
        if (speciali instanceof List) {
            for (Object o : (List<Object>) speciali) {
                if (o instanceof Json.Numero) {
                    superCaricati.add(Integer.valueOf((int) ((Json.Numero) o).comeLong()));
                }
            }
        }

        for (int i = 0; i < slot.size(); i++) {
            contenitore.add(new Cella(i));
        }
        contenitore.revalidate();
        contenitore.repaint();
        scorrimento.getVerticalScrollBar().setValue(0);
    }

    private static int numero(Object o, int ripiego) {
        if (o instanceof Json.Numero) {
            return (int) ((Json.Numero) o).comeLong();
        }
        return ripiego;
    }

    /** Lo slot scelto, o -1. */
    public int selezionato() {
        return selezionata == null ? -1 : selezionata.indice;
    }

    /**
     * Simula un clic su uno slot. Serve al collaudo: verifica che la catena
     * dall'evento del mouse fino al form funzioni, senza dover cliccare a mano.
     */
    public boolean provaClick(int indice) {
        for (Component c : contenitore.getComponents()) {
            if (c instanceof Cella && ((Cella) c).indice == indice) {
                Cella cella = (Cella) c;
                cella.dispatchEvent(new MouseEvent(cella, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0,
                        Math.max(1, cella.getWidth() / 2), Math.max(1, cella.getHeight() / 2),
                        1, false));
                return true;
            }
        }
        return false;
    }

    /** Quante schede sono state costruite: serve al collaudo. */
    public int quanteSchede() {
        return contenitore.getComponentCount();
    }

    /** Il primo slot occupato, o -1 se sono tutti liberi. */
    public int primoOccupato() {
        for (int i = 0; i < slot.size(); i++) {
            String id = testo(i, "Id");
            if (id != null && !id.isEmpty() && !"^".equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** Sceglie uno slot senza passare dal mouse. */
    public void seleziona(int indice) {
        for (Component c : contenitore.getComponents()) {
            if (c instanceof Cella && ((Cella) c).indice == indice) {
                selezionata = (Cella) c;
                contenitore.repaint();
                return;
            }
        }
    }

    /** Il valore di un campo di uno slot, con ripiego. */
    @SuppressWarnings("unchecked")
    private Object campo(int indice, String nome) {
        if (indice < 0 || indice >= slot.size()) {
            return null;
        }
        Object s = slot.get(indice);
        return s instanceof Map ? ((Map<String, Object>) s).get(nome) : null;
    }

    private String testo(int indice, String nome) {
        Object v = campo(indice, nome);
        if (v == null) {
            return "";
        }
        if (v instanceof Json.Numero) {
            return ((Json.Numero) v).testo();
        }
        return String.valueOf(v);
    }

    private boolean booleano(int indice, String nome) {
        Object v = campo(indice, nome);
        return v instanceof Boolean && ((Boolean) v).booleanValue();
    }

    /** Sostituisce l'oggetto di uno slot, conservando la quantita' se possibile. */
    @SuppressWarnings("unchecked")
    public void cambiaOggetto(int indice, String nuovoId) {
        if (indice < 0 || indice >= slot.size()) {
            return;
        }
        Object s = slot.get(indice);
        if (!(s instanceof Map)) {
            return;
        }
        Map<String, Object> mappa = (Map<String, Object>) s;
        mappa.put("Id", nuovoId);
        if (ascoltatore != null) {
            ascoltatore.modificato();
        }
        contenitore.repaint();
    }

    /** Aggiunge o toglie il contrassegno di slot super-caricato. */
    public void alternaSuperCaricato(int indice) {
        Integer valore = Integer.valueOf(indice);
        if (superCaricati.contains(valore)) {
            superCaricati.remove(valore);
        } else {
            superCaricati.add(valore);
        }
        // Il salvataggio tiene l'elenco dentro l'inventario, non negli slot:
        // chi chiama si occupa di scriverlo.
        contenitore.repaint();
    }

    public List<Integer> superCaricati() {
        return superCaricati;
    }

    // ------------------------------------------------------------------

    /** Una scheda della griglia: un singolo slot. */
    private final class Cella extends JPanel {

        final int indice;
        private boolean sottoIlPuntatore;

        Cella(int indice) {
            this.indice = indice;
            setPreferredSize(new Dimension(LATO, LATO));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String id = testo(indice, "Id");
            Catalogo.Voce voce = catalogo.voce(id);
            setToolTipText("<html>" + (voce != null ? "<b>" + voce.etichetta() + "</b><br>"
                    + (voce.sottotitolo == null ? "" : voce.sottotitolo + "<br>")
                    : "") + "<span style='color:#888'>" + id + "</span></html>");

            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    selezionata = Cella.this;
                    contenitore.repaint();
                    if (ascoltatore != null) {
                        ascoltatore.scelto(Cella.this.indice);
                    }
                    if (e.getClickCount() == 2) {
                        scegliOggetto();
                    }
                }

                public void mouseEntered(MouseEvent e) {
                    sottoIlPuntatore = true;
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    sottoIlPuntatore = false;
                    repaint();
                }
            });
        }

        private void scegliOggetto() {
            PannelloDettagli.SelettoreOggetto s = new PannelloDettagli.SelettoreOggetto(
                    javax.swing.SwingUtilities.getWindowAncestor(this), catalogo, icone);
            Catalogo.Voce scelta = s.apri(testo(indice, "Id"));
            if (scelta != null) {
                cambiaOggetto(indice, scelta.id);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            String id = testo(indice, "Id");
            boolean vuoto = id == null || id.isEmpty() || "^".equals(id);
            boolean scelto = selezionata == this;
            boolean speciale = superCaricati.contains(Integer.valueOf(indice));

            // --- sfondo ---
            g2.setColor(scelto ? Aspetto.SELEZIONE
                    : sottoIlPuntatore ? Aspetto.PANNELLO_ALTO : Aspetto.FONDO_ALTO);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

            // --- contorno ---
            if (vuoto) {
                g2.setColor(Aspetto.BORDO);
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{4f, 4f}, 0f));
            } else {
                g2.setColor(scelto ? Aspetto.ACCENTO : Aspetto.BORDO_VIVO);
                g2.setStroke(new BasicStroke(scelto ? 1.8f : 1f));
            }
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            if (vuoto) {
                g2.setColor(Aspetto.TESTO_DEBOLE);
                g2.setFont(Aspetto.monospaziato(9.5f, Font.PLAIN));
                String t = "libero";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2, getHeight() - 12);
                g2.dispose();
                return;
            }

            // --- angolo dello slot super-caricato ---
            if (speciale) {
                int r = 15;
                g2.setColor(Aspetto.ACCENTO);
                g2.fillArc(getWidth() - r * 2 - 2, 2, r * 2, r * 2, 0, 360);
                g2.setColor(Aspetto.FONDO);
                g2.setFont(Aspetto.monospaziato(10f, Font.BOLD));
                g2.drawString("S", getWidth() - r - 5, 15);
            }

            // --- icona ---
            Catalogo.Voce voce = catalogo.voce(id);
            javax.swing.ImageIcon icona = icone.perFile(voce == null ? null : voce.icona, 46);
            int centroX = getWidth() / 2;
            if (icona != null) {
                g2.drawImage(icona.getImage(), centroX - 23, 8, 46, 46, null);
            } else {
                g2.setColor(Aspetto.TESTO_DEBOLE);
                g2.setFont(Aspetto.monospaziato(11f, Font.BOLD));
                String sigla = id.length() > 4 ? id.substring(0, 4) : id;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(sigla, centroX - fm.stringWidth(sigla) / 2, 36);
            }

            // --- nome di gioco, su due righe ---
            String nome = voce != null ? voce.etichetta() : id;
            g2.setColor(scelto ? Aspetto.TESTO : Aspetto.TESTO_TENUE);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9.5f));
            List<String> righe = spezza(g2, nome, getWidth() - 12, 2);
            int y = 66;
            for (String riga : righe) {
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(riga, (getWidth() - fm.stringWidth(riga)) / 2, y);
                y += fm.getHeight() - 1;
            }

            // --- pillola della quantita' ---
            String quantita = testo(indice, "Amount");
            String massimo = testo(indice, "MaxAmount");
            if (!quantita.isEmpty()) {
                if (quantita.endsWith(".0")) {
                    quantita = quantita.substring(0, quantita.length() - 2);
                }
                if (massimo.endsWith(".0")) {
                    massimo = massimo.substring(0, massimo.length() - 2);
                }
                String testoQ = massimo.isEmpty() ? quantita : quantita + "/" + massimo;
                g2.setFont(Aspetto.monospaziato(9.5f, Font.PLAIN));
                FontMetrics fm = g2.getFontMetrics();
                int larghezza = fm.stringWidth(testoQ) + 12;
                int x = (getWidth() - larghezza) / 2;
                int yy = getHeight() - 18;
                g2.setColor(Aspetto.velato(Aspetto.INFO, 40));
                g2.fillRoundRect(x, yy, larghezza, 15, 15, 15);
                g2.setColor(Aspetto.INFO);
                g2.drawString(testoQ, x + 6, yy + 11);
            }

            // --- danno ---
            String danno = testo(indice, "DamageFactor");
            double valoreDanno = 0;
            try {
                valoreDanno = Double.parseDouble(danno);
            } catch (NumberFormatException e) {
                valoreDanno = 0;
            }
            if (valoreDanno > 0) {
                g2.setColor(Aspetto.ERRORE);
                g2.fillRoundRect(5, 5, 46, 14, 14, 14);
                g2.setColor(new Color(0xFFF2F2));
                g2.setFont(Aspetto.monospaziato(9f, Font.BOLD));
                g2.drawString("danno", 10, 15);
            }

            g2.dispose();
        }

        /** Spezza un testo in righe che stiano nella larghezza data. */
        private List<String> spezza(Graphics2D g2, String testo, int larghezza, int massimoRighe) {
            List<String> righe = new ArrayList<String>();
            if (testo == null || testo.isEmpty()) {
                return righe;
            }
            FontMetrics fm = g2.getFontMetrics();
            String[] parole = testo.split(" ");
            StringBuilder corrente = new StringBuilder();
            for (String parola : parole) {
                String prova = corrente.length() == 0 ? parola : corrente + " " + parola;
                if (fm.stringWidth(prova) <= larghezza) {
                    corrente.setLength(0);
                    corrente.append(prova);
                } else {
                    if (corrente.length() > 0) {
                        righe.add(corrente.toString());
                    }
                    corrente.setLength(0);
                    corrente.append(parola);
                    if (righe.size() == massimoRighe - 1) {
                        break;
                    }
                }
            }
            if (corrente.length() > 0 && righe.size() < massimoRighe) {
                righe.add(corrente.toString());
            }
            // Se il nome non ci sta, si taglia con i puntini.
            if (righe.size() == massimoRighe) {
                String ultima = righe.get(massimoRighe - 1);
                while (fm.stringWidth(ultima + "…") > larghezza && ultima.length() > 1) {
                    ultima = ultima.substring(0, ultima.length() - 1);
                }
                boolean tagliato = false;
                int usate = 0;
                for (String r : righe) {
                    usate += r.length();
                }
                if (usate < testo.replace(" ", "").length()) {
                    tagliato = true;
                }
                if (tagliato) {
                    righe.set(massimoRighe - 1, ultima + "…");
                }
            }
            return righe;
        }
    }
}

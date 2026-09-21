package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La griglia degli slot di un inventario.
 *
 * <b>La griglia e' quella del gioco, non un elenco degli oggetti.</b> Nel
 * salvataggio {@code Slots} contiene solo gli slot <i>occupati</i>, ognuno con la
 * sua posizione {@code Index.X} / {@code Index.Y}: la stiva di una nave puo'
 * avere sette oggetti dentro centodiciotto caselle, e mostrarne sette faceva
 * sembrare la nave vuota. Qui si disegnano tutte le caselle dichiarate da
 * {@code Width} e {@code Height}, si sbloccano quelle elencate in
 * {@code ValidSlotIndices}, e ogni oggetto va al suo posto. Lo spazio libero si
 * vede, ed e' lo spazio vero.
 *
 * Struttura presa dal vecchio editor — si vede tutto l'inventario insieme e si
 * modifica il singolo slot accanto — aspetto e dettagli nostri:
 *
 * <ul>
 *   <li>gli slot <b>super-caricati</b> hanno un angolo d'accento: sono quelli
 *       che nel gioco danno il bonus, e cercarli scorrendo cento caselle era
 *       il modo piu' lento possibile;</li>
 *   <li>gli oggetti <b>danneggiati</b> mostrano una pillola rossa con il danno;</li>
 *   <li>gli slot <b>liberi</b> hanno il contorno tratteggiato, quelli <b>non
 *       sbloccati</b> sono spenti: si distingue "vuoto" da "non esiste".</li>
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

    private static final int LATO_MASSIMO = 112;
    private static final int LATO_MINIMO = 56;
    private static final int DISTANZA = 6;

    private final Catalogo catalogo;
    private final Icone icone;
    private final Ascoltatore ascoltatore;
    private final Contenitore contenitore = new Contenitore();
    private final JScrollPane scorrimento;

    /** L'inventario aperto: la mappa con Slots, Width, Height, SpecialSlots. */
    private Map<String, Object> inventario;
    private String nomeInventario = "";

    /** Le celle, in ordine di lettura: riga per riga, da sinistra a destra. */
    private final List<Cella> celle = new ArrayList<Cella>();

    /** Gli oggetti per posizione: la chiave e' la coppia (x, y) appiattita. */
    private final Map<Long, Map<String, Object>> oggetti =
            new HashMap<Long, Map<String, Object>>();

    /** Le posizioni sbloccate in questo inventario. */
    private final Set<Long> attivi = new HashSet<Long>();

    /** Le posizioni super-caricate. */
    private final Set<Long> speciali = new HashSet<Long>();

    private Cella selezionata;
    private int colonne = 10;
    private int righe = 1;

    public GrigliaSlot(Catalogo catalogo, Icone icone, Ascoltatore ascoltatore) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.ascoltatore = ascoltatore;

        setLayout(new java.awt.BorderLayout());
        setBackground(Aspetto.PANNELLO);

        contenitore.setBackground(Aspetto.PANNELLO);
        contenitore.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        scorrimento = new JScrollPane(contenitore);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(20);
        scorrimento.setBackground(Aspetto.PANNELLO);
        add(scorrimento, java.awt.BorderLayout.CENTER);
    }

    /**
     * Ricalcola le dimensioni dopo un ridimensionamento della finestra.
     *
     * Il layout a griglia divide lo spazio disponibile, quindi basta far
     * ricalcolare: le celle si allargano o si stringono da sole, e l'altezza
     * preferita segue.
     */
    public void ricalcolaColonne() {
        contenitore.revalidate();
        contenitore.repaint();
    }

    // ------------------------------------------------------------------
    // Lettura del salvataggio
    // ------------------------------------------------------------------

    /**
     * Mostra un inventario.
     *
     * @param inventario la mappa dell'inventario (con Slots, Width, Height,
     *                   ValidSlotIndices, SpecialSlots)
     * @param nome       il nome da mostrare sopra la griglia
     */
    @SuppressWarnings("unchecked")
    public void mostra(Object inventario, String nome) {
        this.nomeInventario = nome == null ? "" : nome;
        this.inventario = inventario instanceof Map ? (Map<String, Object>) inventario : null;
        contenitore.removeAll();
        celle.clear();
        oggetti.clear();
        attivi.clear();
        speciali.clear();
        selezionata = null;

        if (this.inventario == null) {
            contenitore.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            JLabel vuoto = new JLabel("Questo inventario non c'e' in questo salvataggio.");
            vuoto.setForeground(Aspetto.TESTO_DEBOLE);
            contenitore.add(vuoto);
            contenitore.revalidate();
            contenitore.repaint();
            return;
        }

        Map<String, Object> mappa = this.inventario;

        // --- gli oggetti, ciascuno alla sua posizione ---
        Object elenco = mappa.get("Slots");
        if (elenco instanceof List) {
            for (Object o : (List<Object>) elenco) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<String, Object> slot = (Map<String, Object>) o;
                int[] pos = posizioneDi(slot);
                if (pos == null) {
                    continue;
                }
                oggetti.put(chiave(pos[0], pos[1]), slot);
            }
        }

        // --- quali caselle sono sbloccate ---
        Object validi = mappa.get("ValidSlotIndices");
        int massimoX = -1;
        int massimoY = -1;
        if (validi instanceof List && !((List<Object>) validi).isEmpty()) {
            for (Object v : (List<Object>) validi) {
                int[] pos = posizioneDi(v);
                if (pos == null) {
                    continue;
                }
                attivi.add(chiave(pos[0], pos[1]));
                massimoX = Math.max(massimoX, pos[0]);
                massimoY = Math.max(massimoY, pos[1]);
            }
        }

        // --- la geometria: Width e Height, con ripiego su quello che si vede ---
        colonne = Inventari.larghezza(mappa);
        righe = Inventari.altezza(mappa);
        if (colonne <= 0 || righe <= 0) {
            colonne = Math.max(1, massimoX + 1);
            righe = Math.max(1, massimoY + 1);
        }
        // Se il salvataggio non elenca le caselle sbloccate, la griglia intera
        // e' sbloccata: e' il caso dei depositi, che non hanno ValidSlotIndices.
        if (attivi.isEmpty()) {
            for (int y = 0; y < righe; y++) {
                for (int x = 0; x < colonne; x++) {
                    attivi.add(chiave(x, y));
                }
            }
        }

        // --- gli slot super-caricati ---
        Object specialiSalvati = mappa.get("SpecialSlots");
        if (specialiSalvati instanceof List) {
            for (Object v : (List<Object>) specialiSalvati) {
                int[] pos = posizioneDi(v);
                if (pos != null) {
                    speciali.add(chiave(pos[0], pos[1]));
                }
            }
        }

        // --- le celle ---
        contenitore.setLayout(new GridLayout(Math.max(1, righe), Math.max(1, colonne),
                DISTANZA, DISTANZA));
        for (int y = 0; y < righe; y++) {
            for (int x = 0; x < colonne; x++) {
                Cella cella = new Cella(celle.size(), x, y);
                celle.add(cella);
                contenitore.add(cella);
            }
        }
        contenitore.revalidate();
        contenitore.repaint();
        scorrimento.getVerticalScrollBar().setValue(0);
        scorrimento.getHorizontalScrollBar().setValue(0);
    }

    /**
     * La posizione (x, y) di un oggetto o di un indice di slot, o null.
     *
     * Il salvataggio usa due forme: gli slot mettono la posizione dentro
     * {@code Index}, mentre {@code ValidSlotIndices} la scrive sciolta come
     * {@code {X, Y}}. Vanno lette tutte e due, altrimenti l'elenco delle
     * caselle sbloccate risulta vuoto e la griglia mostra spazio che non c'e'.
     */
    @SuppressWarnings("unchecked")
    private static int[] posizioneDi(Object o) {
        if (!(o instanceof Map)) {
            return null;
        }
        Map<String, Object> m = (Map<String, Object>) o;
        Object indice = m.get("Index");
        if (indice instanceof Map) {
            m = (Map<String, Object>) indice;
        }
        int x = numero(m.get("X"), -1);
        int y = numero(m.get("Y"), -1);
        if (x < 0 || y < 0) {
            return null;
        }
        return new int[]{x, y};
    }

    private static long chiave(int x, int y) {
        return ((long) y << 20) | (x & 0xFFFFFL);
    }

    private static int numero(Object o, int ripiego) {
        if (o instanceof Json.Numero) {
            return (int) ((Json.Numero) o).comeLong();
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return ripiego;
    }

    // ------------------------------------------------------------------
    // Stato
    // ------------------------------------------------------------------

    /** Quante colonne ha la griglia disegnata. */
    public int colonne() {
        return colonne;
    }

    /** Quante righe ha la griglia disegnata. */
    public int righe() {
        return righe;
    }

    /** Quante caselle sono sbloccate. */
    public int slotAttivi() {
        return attivi.size();
    }

    /** Quanti oggetti ci sono. */
    public int occupati() {
        return oggetti.size();
    }

    /** Lo slot scelto, o -1. */
    public int selezionato() {
        return selezionata == null ? -1 : selezionata.indice;
    }

    /** Quante schede sono state costruite: serve al collaudo. */
    public int quanteSchede() {
        return celle.size();
    }

    /** Vero se la casella e' sbloccata. */
    public boolean sbloccato(int indice) {
        Cella c = cella(indice);
        return c != null && attivi.contains(chiave(c.x, c.y));
    }

    /** L'oggetto che sta in una casella, o null se la casella e' libera. */
    public Map<String, Object> oggettoDi(int indice) {
        Cella c = cella(indice);
        if (c == null) {
            return null;
        }
        return oggetti.get(chiave(c.x, c.y));
    }

    private Cella cella(int indice) {
        if (indice < 0 || indice >= celle.size()) {
            return null;
        }
        return celle.get(indice);
    }

    /** Il primo slot occupato, o -1 se sono tutti liberi. */
    public int primoOccupato() {
        for (int i = 0; i < celle.size(); i++) {
            if (oggettoDi(i) != null) {
                return i;
            }
        }
        return -1;
    }

    /** Sceglie uno slot senza passare dal mouse. */
    public void seleziona(int indice) {
        Cella c = cella(indice);
        if (c == null) {
            return;
        }
        selezionata = c;
        contenitore.repaint();
    }

    /**
     * Simula un clic su uno slot. Serve al collaudo: verifica che la catena
     * dall'evento del mouse fino al form funzioni, senza dover cliccare a mano.
     */
    public boolean provaClick(int indice) {
        Cella c = cella(indice);
        if (c == null) {
            return false;
        }
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0,
                Math.max(1, c.getWidth() / 2), Math.max(1, c.getHeight() / 2),
                1, false));
        return true;
    }

    // ------------------------------------------------------------------
    // Modifica
    // ------------------------------------------------------------------

    /**
     * Scrive un oggetto in una casella, creandolo se la casella era libera.
     *
     * Nel salvataggio gli slot vuoti non esistono: {@code Slots} elenca solo
     * quelli occupati. Riempire una casella significa quindi aggiungere una
     * voce all'elenco, con la sua posizione e i campi che il gioco si aspetta.
     */
    @SuppressWarnings("unchecked")
    public void imposta(int indice, String id) {
        Cella c = cella(indice);
        if (c == null || inventario == null || !attivi.contains(chiave(c.x, c.y))) {
            return;
        }
        long k = chiave(c.x, c.y);
        Map<String, Object> esistente = oggetti.get(k);
        if (esistente != null) {
            esistente.put("Id", id);
        } else {
            Map<String, Object> nuovo = nuovoSlot(c.x, c.y, id);
            Object elenco = inventario.get("Slots");
            if (!(elenco instanceof List)) {
                elenco = new ArrayList<Object>();
                inventario.put("Slots", elenco);
            }
            ((List<Object>) elenco).add(nuovo);
            oggetti.put(k, nuovo);
        }
        if (ascoltatore != null) {
            ascoltatore.modificato();
        }
        contenitore.repaint();
    }

    /** Toglie l'oggetto da una casella, lasciandola libera. */
    @SuppressWarnings("unchecked")
    public void rimuovi(int indice) {
        Cella c = cella(indice);
        if (c == null || inventario == null) {
            return;
        }
        long k = chiave(c.x, c.y);
        Map<String, Object> esistente = oggetti.remove(k);
        if (esistente == null) {
            return;
        }
        Object elenco = inventario.get("Slots");
        if (elenco instanceof List) {
            ((List<Object>) elenco).remove(esistente);
        }
        if (speciali.remove(k)) {
            scriviSpeciali();
        }
        if (ascoltatore != null) {
            ascoltatore.modificato();
        }
        contenitore.repaint();
    }

    /**
     * Costruisce la voce di uno slot nuovo.
     *
     * I campi sono quelli che il salvataggio usa davvero, nello stesso ordine:
     * tipo di inventario, identificativo, quantita', massimo, danno, installato,
     * posizione. Il tipo si copia da un altro slot dello stesso inventario,
     * cosi' non si inventa niente: uno slot di tecnologia deve restare di
     * tecnologia.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> nuovoSlot(int x, int y, String id) {
        Map<String, Object> slot = new LinkedHashMap<String, Object>();

        Map<String, Object> tipo = new LinkedHashMap<String, Object>();
        tipo.put("InventoryType", tipoInventario(id));
        slot.put("Type", tipo);
        slot.put("Id", id);
        slot.put("Amount", new Json.Numero("1"));
        slot.put("MaxAmount", new Json.Numero(String.valueOf(massimo(id))));
        slot.put("DamageFactor", new Json.Numero("0.0"));
        slot.put("FullyInstalled", Boolean.TRUE);
        slot.put("AddedAutomatically", Boolean.FALSE);

        Map<String, Object> indice = new LinkedHashMap<String, Object>();
        indice.put("X", new Json.Numero(String.valueOf(x)));
        indice.put("Y", new Json.Numero(String.valueOf(y)));
        slot.put("Index", indice);
        return slot;
    }

    /** Il tipo di inventario da usare per un oggetto nuovo. */
    @SuppressWarnings("unchecked")
    private String tipoInventario(String id) {
        for (Map<String, Object> s : oggetti.values()) {
            Object t = s.get("Type");
            if (t instanceof Map) {
                Object v = ((Map<String, Object>) t).get("InventoryType");
                if (v != null) {
                    return String.valueOf(v);
                }
            }
        }
        if (nomeInventario.toLowerCase().indexOf("tecnolog") >= 0) {
            return "Technology";
        }
        Catalogo.Voce voce = catalogo == null ? null : catalogo.voce(id);
        if (voce != null && "technology".equalsIgnoreCase(voce.tipo)) {
            return "Technology";
        }
        if (voce != null && "substance".equalsIgnoreCase(voce.tipo)) {
            return "Substance";
        }
        return "Product";
    }

    /**
     * Quanto puo' stare in uno slot.
     *
     * Il salvataggio lo tiene per slot. Per un oggetto nuovo si copia da un
     * altro slot con lo stesso identificativo, se c'e'; altrimenti si ripiega
     * su un valore alto, che l'utente puo' correggere dal form.
     */
    private int massimo(String id) {
        for (Map<String, Object> s : oggetti.values()) {
            if (id.equals(String.valueOf(s.get("Id")))) {
                int m = numero(s.get("MaxAmount"), 0);
                if (m > 0) {
                    return m;
                }
            }
        }
        return 9999;
    }

    // ------------------------------------------------------------------
    // Super-caricati
    // ------------------------------------------------------------------

    /** Gli slot super-caricati, come indici di cella. */
    public List<Integer> superCaricati() {
        List<Integer> indici = new ArrayList<Integer>();
        for (Cella c : celle) {
            if (speciali.contains(chiave(c.x, c.y))) {
                indici.add(Integer.valueOf(c.indice));
            }
        }
        return indici;
    }

    /** Aggiunge o toglie il contrassegno di slot super-caricato. */
    public void alternaSuperCaricato(int indice) {
        Cella c = cella(indice);
        if (c == null) {
            return;
        }
        long k = chiave(c.x, c.y);
        if (!speciali.remove(k)) {
            speciali.add(k);
        }
        contenitore.repaint();
    }

    /**
     * Riscrive l'elenco degli slot super-caricati dentro l'inventario.
     *
     * Il salvataggio non tiene indici sciolti ma coppie di posizioni con il
     * tipo di bonus: {@code {"Type": {"InventorySpecialSlotType": "TechBonus"},
     * "Index": {"X": .., "Y": ..}}}. Scrivere numeri al posto di queste mappe
     * lasciava il campo in una forma che il gioco non riconosce.
     */
    @SuppressWarnings("unchecked")
    public void scriviSpeciali() {
        if (inventario == null) {
            return;
        }
        List<Object> elenco = new ArrayList<Object>();
        List<Long> posizioni = new ArrayList<Long>(speciali);
        java.util.Collections.sort(posizioni);
        for (Long k : posizioni) {
            int x = (int) (k & 0xFFFFFL);
            int y = (int) (k >>> 20);
            Map<String, Object> voce = new LinkedHashMap<String, Object>();
            Map<String, Object> tipo = new LinkedHashMap<String, Object>();
            tipo.put("InventorySpecialSlotType", "TechBonus");
            voce.put("Type", tipo);
            Map<String, Object> indice = new LinkedHashMap<String, Object>();
            indice.put("X", new Json.Numero(String.valueOf(x)));
            indice.put("Y", new Json.Numero(String.valueOf(y)));
            voce.put("Index", indice);
            elenco.add(voce);
        }
        inventario.put("SpecialSlots", elenco);
    }

    // ------------------------------------------------------------------
    // Contenitore
    // ------------------------------------------------------------------

    /**
     * Il pannello che contiene le celle.
     *
     * Il layout a griglia divide lo spazio disponibile in parti uguali, quindi
     * le celle si adattano da sole alla larghezza della finestra. Quello che il
     * layout non sa fare e' dichiarare l'altezza giusta: dentro un pannello
     * scorrevole serve sapere quante righe occupa la griglia, altrimenti la
     * barra di scorrimento non compare e si vede solo la prima riga.
     */
    private final class Contenitore extends JPanel implements Scrollable {

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            java.awt.Insets m = getInsets();
            int disponibile = getWidth();
            if (disponibile <= 0) {
                disponibile = LATO_MASSIMO * Math.max(1, colonne) + DISTANZA * Math.max(0, colonne - 1);
            }
            int lato = latoCella(disponibile);
            int larghezza = Math.max(disponibile,
                    colonne * LATO_MINIMO + DISTANZA * Math.max(0, colonne - 1));
            int altezza = righe * lato + DISTANZA * Math.max(0, righe - 1) + m.top + m.bottom;
            return new Dimension(larghezza, altezza);
        }

        public int getScrollableUnitIncrement(Rectangle visibile, int orientamento, int direzione) {
            return 20;
        }

        public int getScrollableBlockIncrement(Rectangle visibile, int orientamento, int direzione) {
            return orientamento == javax.swing.SwingConstants.VERTICAL
                    ? visibile.height : visibile.width;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Quanto e' larga una cella, dato lo spazio disponibile. */
    private int latoCella(int disponibile) {
        int utili = disponibile - DISTANZA * Math.max(0, colonne - 1);
        int lato = utili / Math.max(1, colonne);
        if (lato > LATO_MASSIMO) {
            return LATO_MASSIMO;
        }
        if (lato < LATO_MINIMO) {
            return LATO_MINIMO;
        }
        return lato;
    }

    // ------------------------------------------------------------------
    // La singola cella
    // ------------------------------------------------------------------

    /** Una scheda della griglia: un singolo slot, con la sua posizione. */
    private final class Cella extends JPanel {

        final int indice;
        final int x;
        final int y;
        private boolean sottoIlPuntatore;

        Cella(int indice, int x, int y) {
            this.indice = indice;
            this.x = x;
            this.y = y;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(attivi.contains(chiave(x, y))
                    ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            aggiornaSuggerimento();

            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (!attivi.contains(chiave(Cella.this.x, Cella.this.y))) {
                        return;
                    }
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

        private void aggiornaSuggerimento() {
            String id = idDi();
            Catalogo.Voce voce = (id.isEmpty() || catalogo == null) ? null : catalogo.voce(id);
            String posizione = "slot " + (y + 1) + " di riga, colonna " + (x + 1);
            if (!attivi.contains(chiave(x, y))) {
                setToolTipText("<html><span style='color:#888'>Non sbloccato</span><br>"
                        + posizione + "</html>");
                return;
            }
            setToolTipText("<html>" + (voce != null ? "<b>" + voce.etichetta() + "</b><br>"
                    + (voce.sottotitolo == null ? "" : voce.sottotitolo + "<br>")
                    : "") + "<span style='color:#888'>"
                    + (id.isEmpty() ? "libero" : id) + "</span><br>" + posizione + "</html>");
        }

        private void scegliOggetto() {
            PannelloDettagli.SelettoreOggetto s = new PannelloDettagli.SelettoreOggetto(
                    javax.swing.SwingUtilities.getWindowAncestor(this), catalogo, icone);
            Catalogo.Voce scelta = s.apri(idDi());
            if (scelta != null) {
                imposta(indice, scelta.id);
                if (ascoltatore != null) {
                    ascoltatore.scelto(indice);
                }
            }
        }

        private String idDi() {
            Map<String, Object> s = oggetti.get(chiave(x, y));
            if (s == null) {
                return "";
            }
            Object id = s.get("Id");
            return id == null ? "" : String.valueOf(id);
        }

        private String testo(String campo) {
            Map<String, Object> s = oggetti.get(chiave(x, y));
            if (s == null) {
                return "";
            }
            Object v = s.get(campo);
            if (v == null) {
                return "";
            }
            if (v instanceof Json.Numero) {
                return ((Json.Numero) v).testo();
            }
            return String.valueOf(v);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int larghezza = getWidth();
            int altezza = getHeight();
            boolean sbloccato = attivi.contains(chiave(x, y));
            String id = idDi();
            boolean vuoto = id.isEmpty() || "^".equals(id);
            boolean scelto = selezionata == this;
            boolean speciale = speciali.contains(chiave(x, y));

            // --- sfondo ---
            g2.setColor(scelto ? Aspetto.SELEZIONE
                    : !sbloccato ? Aspetto.FONDO
                    : sottoIlPuntatore ? Aspetto.PANNELLO_ALTO : Aspetto.FONDO_ALTO);
            g2.fillRoundRect(0, 0, larghezza, altezza, 12, 12);

            // --- contorno ---
            if (!sbloccato) {
                g2.setColor(Aspetto.BORDO);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{2f, 5f}, 0f));
            } else if (vuoto) {
                g2.setColor(Aspetto.BORDO);
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{4f, 4f}, 0f));
            } else {
                g2.setColor(scelto ? Aspetto.ACCENTO : Aspetto.BORDO_VIVO);
                g2.setStroke(new BasicStroke(scelto ? 1.8f : 1f));
            }
            g2.drawRoundRect(0, 0, larghezza - 1, altezza - 1, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            if (!sbloccato) {
                g2.setColor(Aspetto.TESTO_DEBOLE);
                g2.setFont(Aspetto.monospaziato(8.5f, Font.PLAIN));
                String t = "bloccato";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(t, (larghezza - fm.stringWidth(t)) / 2, altezza / 2 + 3);
                g2.dispose();
                return;
            }

            if (vuoto) {
                g2.setColor(Aspetto.TESTO_DEBOLE);
                g2.setFont(Aspetto.monospaziato(9.5f, Font.PLAIN));
                String t = "libero";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(t, (larghezza - fm.stringWidth(t)) / 2, altezza - 12);
                g2.dispose();
                return;
            }

            // --- angolo dello slot super-caricato ---
            if (speciale) {
                int r = 13;
                g2.setColor(Aspetto.ACCENTO);
                g2.fillArc(larghezza - r * 2 - 2, 2, r * 2, r * 2, 0, 360);
                g2.setColor(Aspetto.FONDO);
                g2.setFont(Aspetto.monospaziato(9.5f, Font.BOLD));
                g2.drawString("S", larghezza - r - 5, 14);
            }

            // --- icona ---
            Catalogo.Voce voce = catalogo == null ? null : catalogo.voce(id);
            int latoIcona = Math.max(18, Math.min(46, Math.min(larghezza - 16, (int) (altezza * 0.40))));
            javax.swing.ImageIcon icona = icone.perFile(voce == null ? null : voce.icona, latoIcona);
            int centroX = larghezza / 2;
            if (icona != null) {
                g2.drawImage(icona.getImage(), centroX - latoIcona / 2, 7, latoIcona, latoIcona, null);
            } else {
                g2.setColor(Aspetto.TESTO_DEBOLE);
                g2.setFont(Aspetto.monospaziato(Math.max(8f, latoIcona / 4f), Font.BOLD));
                String sigla = id.length() > 4 ? id.substring(0, 4) : id;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(sigla, centroX - fm.stringWidth(sigla) / 2, 7 + latoIcona / 2 + 3);
            }

            // --- nome di gioco ---
            String nome = voce != null ? voce.etichetta() : id;
            g2.setColor(scelto ? Aspetto.TESTO : Aspetto.TESTO_TENUE);
            float dimensione = larghezza < 82 ? 8.5f : 9.5f;
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, dimensione));
            int righeNome = larghezza < 92 ? 1 : 2;
            List<String> righeTesto = spezza(g2, nome, larghezza - 10, righeNome);
            int yTesto = 7 + latoIcona + g2.getFontMetrics().getAscent() + 3;
            for (String riga : righeTesto) {
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(riga, (larghezza - fm.stringWidth(riga)) / 2, yTesto);
                yTesto += fm.getHeight() - 1;
            }

            // --- pillola della quantita' ---
            String quantita = testo("Amount");
            String massimo = testo("MaxAmount");
            if (!quantita.isEmpty()) {
                if (quantita.endsWith(".0")) {
                    quantita = quantita.substring(0, quantita.length() - 2);
                }
                if (massimo.endsWith(".0")) {
                    massimo = massimo.substring(0, massimo.length() - 2);
                }
                String testoQ = massimo.isEmpty() ? quantita : quantita + "/" + massimo;
                g2.setFont(Aspetto.monospaziato(9f, Font.PLAIN));
                FontMetrics fm = g2.getFontMetrics();
                int larghezzaQ = Math.min(fm.stringWidth(testoQ) + 12, larghezza - 6);
                int xq = (larghezza - larghezzaQ) / 2;
                int yq = altezza - 17;
                g2.setColor(Aspetto.velato(Aspetto.INFO, 40));
                g2.fillRoundRect(xq, yq, larghezzaQ, 14, 14, 14);
                g2.setColor(Aspetto.INFO);
                g2.drawString(testoQ, xq + 6, yq + 10);
            }

            // --- danno ---
            double valoreDanno = 0;
            try {
                valoreDanno = Double.parseDouble(testo("DamageFactor"));
            } catch (NumberFormatException e) {
                valoreDanno = 0;
            }
            if (valoreDanno > 0) {
                g2.setColor(Aspetto.ERRORE);
                g2.fillRoundRect(5, 5, Math.min(46, larghezza - 10), 13, 13, 13);
                g2.setColor(new Color(0xFFF2F2));
                g2.setFont(Aspetto.monospaziato(8.5f, Font.BOLD));
                g2.drawString("danno", 10, 14);
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
                while (fm.stringWidth(ultima + "\u2026") > larghezza && ultima.length() > 1) {
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
                    righe.set(massimoRighe - 1, ultima + "\u2026");
                }
            }
            return righe;
        }
    }
}

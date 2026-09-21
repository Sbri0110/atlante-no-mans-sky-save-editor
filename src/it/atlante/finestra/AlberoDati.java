package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * L'albero del salvataggio.
 *
 * Ogni nodo e' un campo. Le mappe diventano rami con il nome del campo, le liste
 * rami numerati, i valori foglie modificabili.
 *
 * Il disegno fa una cosa che cambia tutto nell'uso: quando il valore e' un
 * identificatore noto al catalogo (per esempio {@code ^CASING}) il nodo mostra
 * l'icona dell'oggetto, il suo nome di gioco e la categoria, invece della sigla.
 * Un albero di {@code ^CASING} non si legge; un albero con l'icona del rivestimento
 * metallico e scritto "Metal Plating" si legge.
 */
public final class AlberoDati extends JTree {

    private final Catalogo catalogo;
    private final Icone icone;
    private final Runnable suModifica;

    public AlberoDati(Object radice, Catalogo catalogo, Icone icone, Runnable suModifica) {
        super(new ModelloAlbero(new Nodo(null, radice, null, -1)));
        this.catalogo = catalogo;
        this.icone = icone;
        this.suModifica = suModifica;

        setRootVisible(true);
        setShowsRootHandles(true);
        setRowHeight(0);              // altezza decisa dal renderer
        setBackground(Aspetto.PANNELLO);
        setForeground(Aspetto.TESTO);
        setCellRenderer(new Disegnatore());
        setToggleClickCount(2);
        putClientProperty("JTree.lineStyle", "None");
    }

    public Nodo radice() {
        return (Nodo) getModel().getRoot();
    }

    /** Apre tutti i nodi fino a una certa profondita'. */
    public void espandiFinoA(int profondita) {
        Nodo r = radice();
        espandi(r, 0, profondita);
    }

    private void espandi(Nodo nodo, int livello, int massimo) {
        if (livello >= massimo) {
            return;
        }
        expandPath(new TreePath(nodo.percorso()));
        for (int i = 0; i < nodo.getChildCount(); i++) {
            espandi((Nodo) nodo.getChildAt(i), livello + 1, massimo);
        }
    }

    /** Apre il percorso che porta a un nodo e lo seleziona. */
    public void rivela(Nodo nodo) {
        TreePath percorso = new TreePath(nodo.percorso());
        expandPath(percorso.getParentPath());
        setSelectionPath(percorso);
        scrollPathToVisible(percorso);
    }

    /** Cerca il primo nodo il cui nome o valore contiene il testo. */
    public Nodo cerca(String testo) {
        if (testo == null || testo.trim().isEmpty()) {
            return null;
        }
        return cerca(radice(), testo.trim().toLowerCase());
    }

    private Nodo cerca(Nodo nodo, String cercato) {
        if (nodo.chiave != null && nodo.chiave.toLowerCase().contains(cercato)) {
            return nodo;
        }
        if (nodo.valore instanceof String
                && ((String) nodo.valore).toLowerCase().contains(cercato)) {
            return nodo;
        }
        if (nodo.valore instanceof String) {
            Catalogo.Voce v = catalogo.voce((String) nodo.valore);
            if (v != null && v.nome != null && v.nome.toLowerCase().contains(cercato)) {
                return nodo;
            }
        }
        for (int i = 0; i < nodo.getChildCount(); i++) {
            Nodo trovato = cerca((Nodo) nodo.getChildAt(i), cercato);
            if (trovato != null) {
                return trovato;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Nodo
    // ------------------------------------------------------------------

    /** Un nodo dell'albero: un campo del salvataggio. */
    public static final class Nodo {
        final String chiave;
        final Object valore;
        final Nodo genitore;
        final int indice;
        private List<Nodo> figli;
        private String descrizione;

        Nodo(String chiave, Object valore, Nodo genitore, int indice) {
            this.chiave = chiave;
            this.valore = valore;
            this.genitore = genitore;
            this.indice = indice;
        }

        public String chiave() {
            return chiave;
        }

        public Object valore() {
            return valore;
        }

        public Nodo genitore() {
            return genitore;
        }

        public boolean foglia() {
            return !(valore instanceof Map) && !(valore instanceof List);
        }

        public String valoreTestuale() {
            if (valore == null) {
                return "null";
            }
            if (valore instanceof Json.Numero) {
                return ((Json.Numero) valore).testo();
            }
            return String.valueOf(valore);
        }

        public int getChildCount() {
            if (figli == null) {
                figli = costruisci();
            }
            return figli.size();
        }

        public Nodo getChildAt(int i) {
            if (figli == null) {
                figli = costruisci();
            }
            return figli.get(i);
        }

        public int indiceDi(Nodo figlio) {
            if (figli == null) {
                figli = costruisci();
            }
            return figli.indexOf(figlio);
        }

        @SuppressWarnings("unchecked")
        private List<Nodo> costruisci() {
            List<Nodo> costruiti = new ArrayList<Nodo>();
            if (valore instanceof Map) {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) valore).entrySet()) {
                    costruiti.add(new Nodo(e.getKey(), e.getValue(), this, -1));
                }
            } else if (valore instanceof List) {
                List<Object> lista = (List<Object>) valore;
                for (int i = 0; i < lista.size(); i++) {
                    costruiti.add(new Nodo(String.valueOf(i), lista.get(i), this, i));
                }
            }
            return costruiti;
        }

        /** Percorso dalla radice a questo nodo, per l'albero. */
        Object[] percorso() {
            List<Object> cammino = new ArrayList<Object>();
            Nodo n = this;
            while (n != null) {
                cammino.add(0, n);
                n = n.genitore;
            }
            return cammino.toArray();
        }

        /** Rimpiazza il valore di questo nodo dentro il genitore. */
        @SuppressWarnings("unchecked")
        void scrivi(Object nuovo) {
            if (genitore == null) {
                return;
            }
            if (genitore.valore instanceof Map) {
                ((Map<String, Object>) genitore.valore).put(chiave, nuovo);
            } else if (genitore.valore instanceof List && indice >= 0) {
                ((List<Object>) genitore.valore).set(indice, nuovo);
            }
        }

        public String descrizione() {
            if (descrizione == null) {
                descrizione = chiave == null ? "salvataggio" : chiave;
            }
            return descrizione;
        }

        @Override
        public String toString() {
            return chiave == null ? "salvataggio" : chiave;
        }
    }

    // ------------------------------------------------------------------
    // Modello
    // ------------------------------------------------------------------

    private static final class ModelloAlbero implements TreeModel {
        private final Nodo radice;
        private final List<TreeModelListener> ascoltatori = new ArrayList<TreeModelListener>();

        ModelloAlbero(Nodo radice) {
            this.radice = radice;
        }

        public Object getRoot() {
            return radice;
        }

        public Object getChild(Object genitore, int indice) {
            return ((Nodo) genitore).getChildAt(indice);
        }

        public int getChildCount(Object genitore) {
            return ((Nodo) genitore).getChildCount();
        }

        public boolean isLeaf(Object nodo) {
            return ((Nodo) nodo).foglia();
        }

        public void valueForPathChanged(TreePath percorso, Object nuovoValore) {
            Nodo nodo = (Nodo) percorso.getLastPathComponent();
            if (nuovoValore instanceof String) {
                Object convertito = converti(nodo.valore(), (String) nuovoValore);
                nodo.scrivi(convertito);
            }
        }

        public int getIndexOfChild(Object genitore, Object figlio) {
            return ((Nodo) genitore).indiceDi((Nodo) figlio);
        }

        public void addTreeModelListener(TreeModelListener l) {
            ascoltatori.add(l);
        }

        public void removeTreeModelListener(TreeModelListener l) {
            ascoltatori.remove(l);
        }
    }

    /** Converte il testo digitato nel tipo che il campo aveva. */
    static Object converti(Object originale, String testo) {
        if (originale instanceof Json.Numero) {
            String pulito = testo.trim();
            if (pulito.isEmpty()) {
                return originale;
            }
            try {
                Double.parseDouble(pulito);
                return new Json.Numero(pulito);
            } catch (NumberFormatException e) {
                return originale;
            }
        }
        if (originale instanceof Boolean) {
            String pulito = testo.trim().toLowerCase();
            return Boolean.valueOf("true".equals(pulito) || "si".equals(pulito) || "1".equals(pulito));
        }
        if (originale == null) {
            return testo;
        }
        return testo;
    }

    // ------------------------------------------------------------------
    // Disegnatore
    // ------------------------------------------------------------------

    private final class Disegnatore implements TreeCellRenderer {

        private final JPanel pannello = new JPanel(new BorderLayout(8, 0));
        private final JLabel icona = new JLabel();
        private final JLabel testo = new JLabel();

        Disegnatore() {
            pannello.setOpaque(true);
            pannello.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 6));
            pannello.add(icona, BorderLayout.WEST);
            pannello.add(testo, BorderLayout.CENTER);
        }

        public Component getTreeCellRendererComponent(JTree albero, Object valore, boolean selezionato,
                                                      boolean espanso, boolean foglia, int riga,
                                                      boolean conFuoco) {
            Nodo nodo = valore instanceof Nodo ? (Nodo) valore : null;
            Color sfondo = selezionato ? Aspetto.ACCENTO_SCURO : Aspetto.PANNELLO;
            if (!selezionato && conFuoco) {
                sfondo = Aspetto.PANNELLO_ALTO;
            }
            pannello.setBackground(sfondo);
            testo.setOpaque(false);
            icona.setIcon(null);

            if (nodo == null) {
                testo.setText(String.valueOf(valore));
                testo.setForeground(Aspetto.TESTO);
                return pannello;
            }

            testo.setText(componiTesto(nodo));
            testo.setForeground(coloreDi(nodo, selezionato));
            testo.setFont(fontDi(nodo));
            icona.setIcon(iconaDi(nodo));
            return pannello;
        }

        private String componiTesto(Nodo nodo) {
            StringBuilder b = new StringBuilder();
            if (nodo.chiave != null) {
                b.append(nodo.chiave);
            }
            if (nodo.foglia()) {
                if (nodo.chiave != null) {
                    b.append("   ");
                }
                b.append(valoreLeggibile(nodo));
            } else {
                int quanti = nodo.getChildCount();
                b.append("   ").append(quanti).append(quanti == 1 ? " campo" : " campi");
            }
            return b.toString();
        }

        /** Per un identificatore noto mostra il nome di gioco, non la sigla. */
        private String valoreLeggibile(Nodo nodo) {
            if (nodo.valore instanceof String) {
                Catalogo.Voce v = catalogo.voce((String) nodo.valore);
                if (v != null) {
                    StringBuilder b = new StringBuilder();
                    b.append(v.etichetta());
                    if (v.sottotitolo != null && !v.sottotitolo.isEmpty()) {
                        b.append("  ·  ").append(v.sottotitolo);
                    }
                    return b.toString();
                }
            }
            return nodo.valoreTestuale();
        }

        private Color coloreDi(Nodo nodo, boolean selezionato) {
            if (selezionato) {
                return new Color(0xFFE9E9);
            }
            if (!nodo.foglia()) {
                return Aspetto.TESTO;
            }
            if (nodo.valore == null) {
                return Aspetto.TESTO_DEBOLE;
            }
            if (nodo.valore instanceof Boolean) {
                return ((Boolean) nodo.valore).booleanValue() ? Aspetto.OK : Aspetto.TESTO_TENUE;
            }
            if (nodo.valore instanceof Json.Numero) {
                return Aspetto.INFO;
            }
            if (nodo.valore instanceof String && catalogo.conosciuto((String) nodo.valore)) {
                return Aspetto.ATTENZIONE;
            }
            return Aspetto.TESTO_TENUE;
        }

        private Font fontDi(Nodo nodo) {
            if (nodo.valore instanceof Json.Numero) {
                return Aspetto.monospaziato(12, Font.PLAIN);
            }
            return new JLabel().getFont();
        }

        private Icon iconaDi(Nodo nodo) {
            if (!nodo.foglia()) {
                boolean lista = nodo.valore instanceof List;
                return Icone.segno(lista ? "[" : "{", 18,
                        lista ? Aspetto.INFO : Aspetto.TESTO_DEBOLE, true);
            }
            if (nodo.valore instanceof String) {
                ImageIcon diGioco = icone.perId((String) nodo.valore, 22);
                if (diGioco != null) {
                    return diGioco;
                }
            }
            if (nodo.valore instanceof Boolean) {
                boolean vero = ((Boolean) nodo.valore).booleanValue();
                return Icone.segno(vero ? "1" : "0", 18, vero ? Aspetto.OK : Aspetto.TESTO_DEBOLE, true);
            }
            if (nodo.valore instanceof Json.Numero) {
                return Icone.segno("#", 18, Aspetto.INFO, true);
            }
            return Icone.segno("T", 18, Aspetto.TESTO_DEBOLE, true);
        }
    }

    /** Raccolta di utilita' per l'albero, usate dalla finestra. */
    public static List<Nodo> cammini(Nodo radice) {
        List<Nodo> tutti = new ArrayList<Nodo>();
        raccogli(radice, tutti);
        return Collections.unmodifiableList(tutti);
    }

    private static void raccogli(Nodo nodo, List<Nodo> dentro) {
        dentro.add(nodo);
        for (int i = 0; i < nodo.getChildCount(); i++) {
            raccogli(nodo.getChildAt(i), dentro);
        }
    }

    /** Disegna un rettangolo arrotondato: usato da piu' componenti. */
    static void rettangoloArrotondato(Graphics g, int x, int y, int larghezza, int altezza,
                                      int raggio, Color riempimento, Color bordo) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (riempimento != null) {
            g2.setColor(riempimento);
            g2.fillRoundRect(x, y, larghezza, altezza, raggio, raggio);
        }
        if (bordo != null) {
            g2.setColor(bordo);
            g2.drawRoundRect(x, y, larghezza - 1, altezza - 1, raggio, raggio);
        }
        g2.dispose();
    }

    /** Dimensione preferita dell'albero, per i calcoli di layout. */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(520, 600);
    }
}

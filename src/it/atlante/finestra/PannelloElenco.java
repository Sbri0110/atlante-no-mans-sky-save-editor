package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Un elenco di elementi: si sceglie a sinistra, si modifica a destra.
 *
 * Serve per le cose che nel salvataggio sono liste di pochi elementi ricchi di
 * campi: i quattro piloti dello squadrone, le ventitre' fregate, i compagni.
 * Non sono inventari — non hanno cento slot con un oggetto dentro — e non sono
 * nemmeno un campo solo. Nel vecchio editor si presentano come una scheda per
 * elemento con accanto i suoi dati.
 *
 * Il form a destra e' lo stesso pannello dei campi usato altrove, quindi le
 * etichette sono in italiano e ogni valore ha l'editor adatto al suo tipo senza
 * doverlo riscrivere qui.
 */
public final class PannelloElenco extends JPanel {

    /** Una voce dell'elenco. */
    private static final class Voce {
        final int indice;
        final String nome;
        final String sottotitolo;
        final Object valore;

        Voce(int indice, String nome, String sottotitolo, Object valore) {
            this.indice = indice;
            this.nome = nome;
            this.sottotitolo = sottotitolo;
            this.valore = valore;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    private final Catalogo catalogo;
    private final Icone icone;
    private final DefaultListModel<Voce> modello = new DefaultListModel<Voce>();
    private final JList<Voce> elenco = new JList<Voce>(modello);
    private final PannelloCampi campi;
    private final JLabel titolo = new JLabel();
    private final JLabel conteggio = new JLabel();

    public PannelloElenco(Catalogo catalogo, Icone icone, Runnable suModifica) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.campi = new PannelloCampi(catalogo, icone, suModifica);

        setLayout(new BorderLayout());
        setBackground(Aspetto.PANNELLO);

        // --- elenco a sinistra ---
        elenco.setBackground(Aspetto.PANNELLO);
        elenco.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        elenco.setCellRenderer(new Disegnatore());
        elenco.setFixedCellHeight(50);

        JScrollPane scorrimento = new JScrollPane(elenco);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(16);

        titolo.setFont(Aspetto.monospaziato(10.5f, Font.BOLD));
        titolo.setForeground(Aspetto.ACCENTO);
        titolo.setBorder(BorderFactory.createEmptyBorder(14, 14, 6, 14));

        conteggio.setFont(Aspetto.monospaziato(10.5f, Font.PLAIN));
        conteggio.setForeground(Aspetto.TESTO_DEBOLE);
        conteggio.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));

        JPanel testa = new JPanel(new BorderLayout());
        testa.setBackground(Aspetto.PANNELLO);
        testa.add(titolo, BorderLayout.NORTH);
        testa.add(conteggio, BorderLayout.SOUTH);

        JPanel sinistra = new JPanel(new BorderLayout());
        sinistra.setBackground(Aspetto.PANNELLO);
        sinistra.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Aspetto.BORDO));
        sinistra.add(testa, BorderLayout.NORTH);
        sinistra.add(scorrimento, BorderLayout.CENTER);
        sinistra.setPreferredSize(new Dimension(320, 500));

        add(sinistra, BorderLayout.WEST);
        add(campi, BorderLayout.CENTER);

        elenco.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                Voce v = elenco.getSelectedValue();
                if (v != null) {
                    campi.mostra(v.valore);
                }
            }
        });
    }

    // ------------------------------------------------------------------

    /** Mostra l'elenco di una sezione. */
    public void mostra(Object radice, String nomeSezione) {
        Elenchi.Elenco descrizione = Elenchi.perSezione(nomeSezione);
        modello.clear();
        titolo.setText(nomeSezione.toUpperCase());
        if (descrizione == null) {
            conteggio.setText("");
            campi.mostra(null);
            return;
        }

        Object valore = Inventari.risolvi(radice, descrizione.percorso);
        List<Object> elementi = new ArrayList<Object>();
        if (valore instanceof List) {
            elementi.addAll((List<Object>) valore);
        }

        for (int i = 0; i < elementi.size(); i++) {
            Object elemento = elementi.get(i);
            modello.addElement(new Voce(i, nomeDi(elemento, descrizione, i),
                    sottotitoloDi(elemento, descrizione), elemento));
        }
        conteggio.setText(elementi.size() + (elementi.size() == 1 ? " elemento" : " elementi"));

        if (!modello.isEmpty()) {
            elenco.setSelectedIndex(0);
        } else {
            campi.mostra(null);
        }
    }

    /** Il nome da mostrare: quello dato dal giocatore, o un numero progressivo. */
    @SuppressWarnings("unchecked")
    private String nomeDi(Object elemento, Elenchi.Elenco descrizione, int indice) {
        if (descrizione.campoNome != null && elemento instanceof Map) {
            Object n = ((Map<String, Object>) elemento).get(descrizione.campoNome);
            if (n != null && !String.valueOf(n).trim().isEmpty()) {
                return String.valueOf(n).trim();
            }
        }
        return descrizione.prefisso + " " + (indice + 1);
    }

    /** La riga sotto il nome: un campo che aiuta a riconoscere l'elemento. */
    @SuppressWarnings("unchecked")
    private String sottotitoloDi(Object elemento, Elenchi.Elenco descrizione) {
        if (descrizione.campoSottotitolo == null || !(elemento instanceof Map)) {
            return "";
        }
        Object v = ((Map<String, Object>) elemento).get(descrizione.campoSottotitolo);
        // Certe volte il campo e' a sua volta un oggetto con dentro un solo
        // valore (FrigateClass = {FrigateClass = "Exploration"}). Senza scendere
        // di un livello si mostrerebbe il toString della mappa.
        if (v instanceof Map) {
            Map<String, Object> dentro = (Map<String, Object>) v;
            if (dentro.size() == 1) {
                v = dentro.values().iterator().next();
            } else {
                return "";
            }
        }
        if (v == null) {
            return "";
        }
        String testo = v instanceof Json.Numero ? ((Json.Numero) v).testo() : String.valueOf(v);
        if (testo.isEmpty() || "0".equals(testo)) {
            return "";
        }
        return Etichette.leggi(descrizione.campoSottotitolo) + " " + testo;
    }

    /** Quanti elementi ha l'elenco: serve al collaudo. */
    public int quanti() {
        return modello.size();
    }

    /** Il nome dell'elemento scelto, o stringa vuota. */
    public String nomeScelto() {
        Voce v = elenco.getSelectedValue();
        return v == null ? "" : v.nome;
    }

    // ------------------------------------------------------------------

    /** Disegnatore delle voci dell'elenco. */
    private final class Disegnatore extends JPanel implements ListCellRenderer<Voce> {

        private final JLabel icona = new JLabel();
        private final JLabel nome = new JLabel();
        private final JLabel sotto = new JLabel();

        Disegnatore() {
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 10));
            icona.setPreferredSize(new Dimension(30, 30));
            add(icona, BorderLayout.WEST);
            JPanel testi = new JPanel();
            testi.setOpaque(false);
            testi.setLayout(new javax.swing.BoxLayout(testi, javax.swing.BoxLayout.Y_AXIS));
            nome.setFont(nome.getFont().deriveFont(Font.BOLD, 12.5f));
            sotto.setFont(sotto.getFont().deriveFont(10.5f));
            sotto.setForeground(Aspetto.TESTO_DEBOLE);
            testi.add(nome);
            testi.add(sotto);
            add(testi, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList<? extends Voce> lista, Voce valore,
                                                      int indice, boolean selezionato, boolean conFuoco) {
            setBackground(selezionato ? Aspetto.SELEZIONE : Aspetto.PANNELLO);
            nome.setForeground(selezionato ? Aspetto.TESTO : Aspetto.TESTO_TENUE);
            icona.setIcon(Icone.segno(valore.nome.substring(0, 1).toUpperCase(), 30,
                    selezionato ? Aspetto.ACCENTO : Aspetto.TESTO_DEBOLE, true));
            nome.setText(valore.nome);
            sotto.setText(valore.sottotitolo);
            return this;
        }
    }
}

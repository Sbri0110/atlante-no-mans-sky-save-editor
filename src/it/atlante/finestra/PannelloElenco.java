package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
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
        /** Il file dell'icona dell'elemento (la razza del pilota), o null. */
        final String icona;
        /** Il file dell'icona della sua cosa (la navicella), o null. */
        final String icona2;
        /** La classe della sua cosa, per esempio "S" per una nave, o null. */
        final String classe;

        Voce(int indice, String nome, String sottotitolo, Object valore,
             String icona, String icona2, String classe) {
            this.indice = indice;
            this.nome = nome;
            this.sottotitolo = sottotitolo;
            this.valore = valore;
            this.icona = icona;
            this.icona2 = icona2;
            this.classe = classe;
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
    private final PannelloWingman wingman;
    private final PannelloFregata fregata;
    private final PannelloCompagno compagno;
    /** Il pannello a schede che tiene l'albero dei campi e le schede su misura. */
    private JPanel centro;
    /** Quale scheda si sta mostrando: "campi", "scheda" o "fregata". */
    private String scheda = "campi";
    private final JLabel titolo = new JLabel();
    private final JLabel conteggio = new JLabel();
    private final JPanel testata = new JPanel();
    private final JLabel testataIcona = new JLabel();
    private final JLabel testataNome = new JLabel();
    private final JLabel testataSotto = new JLabel();

    private Object radice;
    /** Vero se questa sezione usa la scheda del pilota invece dell'albero. */
    private boolean usaScheda;

    public PannelloElenco(Catalogo catalogo, Icone icone, Runnable suModifica) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.campi = new PannelloCampi(catalogo, icone, suModifica);
        this.wingman = new PannelloWingman(icone, suModifica);
        this.fregata = new PannelloFregata(icone, suModifica);
        this.compagno = new PannelloCompagno(icone, suModifica);

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

        // --- testata a destra: quello che hai scelto, con la sua icona ---
        testata.setBackground(Aspetto.FONDO_ALTO);
        testata.setLayout(new BorderLayout(12, 0));
        testata.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Aspetto.BORDO),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        testataIcona.setPreferredSize(new Dimension(52, 52));
        testata.add(testataIcona, BorderLayout.WEST);
        JPanel testiTestata = new JPanel();
        testiTestata.setOpaque(false);
        testiTestata.setLayout(new javax.swing.BoxLayout(testiTestata, javax.swing.BoxLayout.Y_AXIS));
        testataNome.setFont(testataNome.getFont().deriveFont(Font.BOLD, 15f));
        testataNome.setForeground(Aspetto.TESTO);
        testataNome.setAlignmentX(Component.LEFT_ALIGNMENT);
        testataSotto.setFont(testataSotto.getFont().deriveFont(11.5f));
        testataSotto.setForeground(Aspetto.TESTO_TENUE);
        testataSotto.setAlignmentX(Component.LEFT_ALIGNMENT);
        testiTestata.add(testataNome);
        testiTestata.add(javax.swing.Box.createVerticalStrut(4));
        testiTestata.add(testataSotto);
        testata.add(testiTestata, BorderLayout.CENTER);

        JPanel destra = new JPanel(new BorderLayout());
        destra.setBackground(Aspetto.PANNELLO);
        destra.add(testata, BorderLayout.NORTH);
        // Due modi di modificare lo stesso elemento: l'albero dei campi, che va
        // bene per le fregate, e la scheda del pilota, che per lo squadrone e'
        // l'unico modo di non perdersi. Vanno in un pannello a schede: metterli
        // tutti e due al centro di un BorderLayout non funziona, perche' il
        // secondo prende il posto del primo e il form sparisce.
        centro = new JPanel(new java.awt.CardLayout());
        centro.setBackground(Aspetto.PANNELLO);
        centro.add(campi, "campi");
        centro.add(wingman, "scheda");
        centro.add(fregata, "fregata");
        centro.add(compagno, "compagno");
        destra.add(centro, BorderLayout.CENTER);

        add(sinistra, BorderLayout.WEST);
        add(destra, BorderLayout.CENTER);

        elenco.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                Voce v = elenco.getSelectedValue();
                if (v != null) {
                    apri(v);
                }
                aggiornaTestata(v);
            }
        });
    }

    /**
     * Apre un elemento nella scheda giusta.
     *
     * Lo squadrone e le fregate hanno una scheda su misura — i campi del gioco
     * con i loro nomi, i menu per la razza e il tipo, i pulsanti per i semi —
     * perche' il loro albero dei campi dice "Elemento 1, Elemento 2" e non si
     * capisce cosa si sta modificando. Le altre sezioni restano sull'albero.
     */
    private void apri(Voce v) {
        if ("scheda".equals(scheda)) {
            wingman.mostra(radice, v.indice);
        } else if ("fregata".equals(scheda)) {
            fregata.mostra(radice, v.indice);
        } else if ("compagno".equals(scheda)) {
            compagno.mostra(radice, v.indice);
        } else {
            campi.mostra(v.valore);
        }
    }

    /** Aggiorna la testata a destra con l'elemento scelto. */
    private void aggiornaTestata(Voce v) {
        if (v == null) {
            testataIcona.setIcon(Icone.segno("·", 52, Aspetto.TESTO_DEBOLE, true));
            testataNome.setText("Nessun elemento scelto");
            testataSotto.setText("");
            return;
        }
        ImageIcon img = v.icona == null ? null : icone.perFile(v.icona, 52);
        testataIcona.setIcon(img != null ? img
                : Icone.segno(v.nome.substring(0, 1).toUpperCase(), 52, Aspetto.ACCENTO, true));
        testataNome.setText(v.nome);
        testataSotto.setText(v.sottotitolo);
    }

    // ------------------------------------------------------------------

    /** Mostra l'elenco di una sezione. */
    public void mostra(Object radice, String nomeSezione) {
        Elenchi.Elenco descrizione = Elenchi.perSezione(nomeSezione);
        this.radice = radice;
        scheda = "Squadrone".equals(nomeSezione) ? "scheda"
                : "Fregate".equals(nomeSezione) ? "fregata"
                : "Compagni".equals(nomeSezione) ? "compagno" : "campi";
        usaScheda = !"campi".equals(scheda);
        ((java.awt.CardLayout) centro.getLayout()).show(centro, scheda);
        // Le schede su misura hanno gia' la loro testata con l'icona grande.
        testata.setVisible(!usaScheda);
        modello.clear();
        titolo.setText(nomeSezione.toUpperCase());
        if (descrizione == null) {
            conteggio.setText("");
            campi.mostra(null);
            wingman.svuota();
            aggiornaTestata(null);
            return;
        }

        Object valore = Inventari.risolvi(radice, descrizione.percorso);
        List<Object> elementi = new ArrayList<Object>();
        if (valore instanceof List) {
            elementi.addAll((List<Object>) valore);
        }

        for (int i = 0; i < elementi.size(); i++) {
            Object elemento = elementi.get(i);
            String icona = iconaDi(elemento, descrizione.campoIcona);
            if (icona == null) {
                icona = descrizione.iconaFissa;
            }
            // La navicella: se il tipo ha un'immagine vera nel catalogo — le
            // navi-premio ce l'hanno — si usa quella, altrimenti l'icona
            // dell'interfaccia.
            String icona2 = iconaDi(elemento, descrizione.campoIcona2);
            Piloti.Nave tipoNave = Piloti.naveDi(risorsaDi(elemento, descrizione.campoIcona2));
            if (tipoNave != null) {
                icona2 = tipoNave.icona;
            }
            String classe = Icone.classeDiRisorsa(risorsaDi(elemento, descrizione.campoIcona2));
            modello.addElement(new Voce(i, nomeDi(elemento, descrizione, i),
                    sottotitoloDi(elemento, descrizione, classe), elemento,
                    icona, icona2, classe));
        }
        conteggio.setText(elementi.size() + (elementi.size() == 1 ? " elemento" : " elementi"));

        if (!modello.isEmpty()) {
            elenco.setSelectedIndex(0);
            // Se l'indice era gia' zero il listener non riceve niente: la prima
            // voce va aperta lo stesso, altrimenti si apre l'elenco con la
            // scheda vuota.
            Voce prima = modello.getElementAt(0);
            apri(prima);
            aggiornaTestata(prima);
        } else {
            campi.mostra(null);
            wingman.svuota();
            fregata.svuota();
            compagno.svuota();
            aggiornaTestata(null);
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
        // Il nome dato dal giocatore c'e' solo se l'ha dato. Per le creature
        // speciali il salvataggio tiene almeno la specie (^UI_BONEPET_SPECIES):
        // meglio quella di un numero.
        if (elemento instanceof Map) {
            Object specie = ((Map<String, Object>) elemento).get("CustomSpeciesName");
            if (specie != null) {
                String t = String.valueOf(specie).replace("^", "").replace("_", " ").trim();
                if (!t.isEmpty()) {
                    return t;
                }
            }
        }
        return descrizione.prefisso + " " + (indice + 1);
    }

    /**
     * Il percorso della risorsa di un elemento.
     *
     * Certe volte il campo e' un oggetto con dentro un {@code Filename} (la
     * risorsa del pilota, quella della sua nave), certe volte e' direttamente
     * il nome della cosa (la razza di una fregata, che e' "Gek" e basta).
     */
    @SuppressWarnings("unchecked")
    private static String risorsaDi(Object elemento, String campo) {
        if (campo == null || !(elemento instanceof Map)) {
            return null;
        }
        Object v = ((Map<String, Object>) elemento).get(campo);
        if (v instanceof Map) {
            Map<String, Object> dentro = (Map<String, Object>) v;
            Object f = dentro.get("Filename");
            if (f != null && !String.valueOf(f).isEmpty()) {
                return String.valueOf(f);
            }
            // Alcuni campi hanno un solo valore annidato senza Filename: la
            // razza di una fregata sta in {AlienRace: "Gek"}.
            if (dentro.size() == 1) {
                Object solo = dentro.values().iterator().next();
                return solo == null ? null : String.valueOf(solo);
            }
            return null;
        }
        return v == null ? null : String.valueOf(v);
    }

    /** Il file dell'icona di un elemento, o null. */
    private static String iconaDi(Object elemento, String campo) {
        return Icone.iconaDiRisorsa(risorsaDi(elemento, campo));
    }

    /** La razza scritta per esteso, letta dal modello: NPCKORVAX -> Korvax. */
    private static String razzaDi(Object elemento, String campo) {
        String r = risorsaDi(elemento, campo);
        if (r == null) {
            return null;
        }
        String u = r.toUpperCase();
        if (u.indexOf("KORVAX") >= 0 || u.indexOf("EXPLORER") >= 0) {
            return "Korvax";
        }
        if (u.indexOf("VYKEEN") >= 0 || u.indexOf("VY'KEEN") >= 0 || u.indexOf("WARRIOR") >= 0) {
            return "Vy'keen";
        }
        if (u.indexOf("GEK") >= 0 || u.indexOf("TRADER") >= 0) {
            return "Gek";
        }
        return null;
    }

    /**
     * La riga sotto il nome.
     *
     * Dice le cose che servono a riconoscere l'elemento a colpo d'occhio: la
     * razza e la classe della navicella per un pilota dello squadrone, il grado
     * e cosi' via. Il campo dichiarato dalla sezione resta in fondo, con la sua
     * etichetta italiana.
     */
    private String sottotitoloDi(Object elemento, Elenchi.Elenco descrizione, String classe) {
        List<String> pezzi = new ArrayList<String>();
        String razza = razzaDi(elemento, descrizione.campoIcona);
        if (razza != null) {
            pezzi.add(razza);
        }
        if (classe != null) {
            pezzi.add("Classe " + classe);
        }
        String dichiarato = valoreSemplice(elemento, descrizione.campoSottotitolo);
        if (dichiarato != null && !dichiarato.isEmpty()) {
            pezzi.add(Etichette.leggi(descrizione.campoSottotitolo) + " " + dichiarato);
        }
        StringBuilder testo = new StringBuilder();
        for (String pezzo : pezzi) {
            if (testo.length() > 0) {
                testo.append("  ·  ");
            }
            testo.append(pezzo);
        }
        return testo.toString();
    }

    /** Il valore di un campo come testo, scendendo di un livello se e' un oggetto. */
    @SuppressWarnings("unchecked")
    private String valoreSemplice(Object elemento, String campo) {
        if (campo == null || !(elemento instanceof Map)) {
            return "";
        }
        Object v = ((Map<String, Object>) elemento).get(campo);
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
        return testo;
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
        private final JLabel icona2 = new JLabel();
        private final JLabel nome = new JLabel();
        private final JLabel sotto = new JLabel();

        Disegnatore() {
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

            // L'icona della cosa e, piu' piccola accanto, quella della sua
            // navicella: un pilota dello squadrone si riconosce dalla razza e
            // dalla nave che porta.
            JPanel immagini = new JPanel();
            immagini.setOpaque(false);
            immagini.setLayout(new javax.swing.BoxLayout(immagini, javax.swing.BoxLayout.X_AXIS));
            icona.setPreferredSize(new Dimension(34, 34));
            icona.setMaximumSize(new Dimension(34, 34));
            icona2.setPreferredSize(new Dimension(26, 26));
            icona2.setMaximumSize(new Dimension(26, 26));
            immagini.add(icona);
            immagini.add(javax.swing.Box.createHorizontalStrut(4));
            immagini.add(icona2);
            add(immagini, BorderLayout.WEST);

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

            ImageIcon img = valore.icona == null ? null : icone.perFile(valore.icona, 34);
            icona.setIcon(img != null ? img
                    : Icone.segno(valore.nome.substring(0, 1).toUpperCase(), 34,
                    selezionato ? Aspetto.ACCENTO : Aspetto.TESTO_DEBOLE, true));

            ImageIcon img2 = valore.icona2 == null ? null : icone.perFile(valore.icona2, 26);
            icona2.setIcon(img2);
            icona2.setVisible(img2 != null);

            nome.setText(valore.nome);
            sotto.setText(valore.sottotitolo);
            return this;
        }
    }
}

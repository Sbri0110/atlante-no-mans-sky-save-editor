package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Conquista stazioni: cosa serve per rivendicare una stazione spaziale.
 *
 * Nel gioco una stazione non assegnata si rivendica soddisfacendo cinque
 * requisiti. Questo pannello li mostra uno per uno, con il valore che hai e
 * quello richiesto, e permette di soddisfarli.
 *
 * Da dove vengono i dati, verificati leggendo un salvataggio vero:
 * <ul>
 *   <li>il <b>sistema attuale</b> e' {@code PlayerStateData.UniverseAddress.
 *       GalacticAddress}: le tre coordinate e l'indice del sistema;</li>
 *   <li>i <b>valori</b> stanno in {@code PlayerStateData.Stats}, che e' una lista
 *       di gruppi: il primo ha indirizzo 0 ed e' globale, gli altri hanno
 *       l'indirizzo di un sistema. E' cosi' che si distingue un valore locale da
 *       uno generale — ed e' cosi' che il vecchio editor sapeva in che stazione
 *       ti trovavi;</li>
 *   <li>le <b>gilde</b> sono {@code ^TGUILD_STAND} (mercanti), {@code ^EGUILD_STAND}
 *       (esploratori), {@code ^WGUILD_STAND} (guerrieri); le <b>razze</b> sono
 *       {@code ^TRA_STANDING}, {@code ^EXP_STANDING}, {@code ^WAR_STANDING};</li>
 *   <li>la <b>chiave</b> e' l'oggetto {@code ^STATION_KEY} nell'inventario;</li>
 *   <li>le <b>stazioni gia' possedute</b> sono il valore di {@code ^STATIONS_OWNED}.</li>
 * </ul>
 */
public final class PannelloStazioni extends JPanel {

    /** Un requisito da soddisfare. */
    private static final class Requisito {
        final String nome;
        final String spiegazione;
        final String icona;
        final int richiesto;
        /** Gli identificativi di statistica da guardare. */
        final String[] statistiche;
        /** Vero se si misura in unita' invece che in gradi. */
        final boolean inUnita;

        Requisito(String nome, String spiegazione, String icona, int richiesto,
                  boolean inUnita, String... statistiche) {
            this.nome = nome;
            this.spiegazione = spiegazione;
            this.icona = icona;
            this.richiesto = richiesto;
            this.inUnita = inUnita;
            this.statistiche = statistiche;
        }
    }

    /**
     * I cinque requisiti, con gli identificativi che usa il vecchio editor.
     *
     * Non sono indovinati: sono le costanti di testo della sua classe {@code bE},
     * estratte dal JAR. Avevo messo due identificativi inventati per i contratti
     * ({@code ^BOUNTY_CONTRACTS}, {@code ^MISSION_CONTRACTS}) e non esistono:
     * quello vero e' {@code ^SP_POI_MISSIONS}.
     */
    private static final Requisito[] REQUISITI = {
            new Requisito("1. Registrazione razza", "Il grado di reputazione con la razza che vive qui",
                    "UI-GEK", 30, false, "^TRA_STANDING", "^EXP_STANDING", "^WAR_STANDING"),
            new Requisito("2. Registrazione gilda", "Il grado di reputazione con le gilde locali",
                    "UI-TRADERS", 15, false, "^TGUILD_STAND", "^EGUILD_STAND", "^WGUILD_STAND"),
            new Requisito("3. Contratti di recupero", "I contratti di recupero completati in questo sistema",
                    "UI-MILESTONES", 5, false, "^SP_POI_MISSIONS"),
            new Requisito("4. Unità per il nucleo", "Le unità che hai, disponibili per costruire il nucleo",
                    "PRODUCT-SPECIAL", 1000000000, true, "Units"),
    };

    private final Catalogo catalogo;
    private final Icone icone;
    private final Runnable suModifica;

    private final JPanel contenitore = new Colonna();
    private final JLabel sistema = new JLabel("—");
    private final JLabel indirizzo = new JLabel("—");
    private final JLabel possedute = new JLabel("—");
    private final JLabel chiave = new JLabel("—");
    private final JLabel chiaveIcona = new JLabel();
    private final List<JLabel> valori = new ArrayList<JLabel>();

    private Object radice;

    public PannelloStazioni(Catalogo catalogo, Icone icone, Runnable suModifica) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.FONDO);
        contenitore.setBackground(Aspetto.FONDO);
        contenitore.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        JScrollPane scorrimento = new JScrollPane(contenitore);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(18);
        add(scorrimento, BorderLayout.CENTER);

        contenitore.add(schedaSistema());
        contenitore.add(Box.createVerticalStrut(16));
        contenitore.add(schedaRequisiti());
        contenitore.add(Box.createVerticalStrut(16));
        contenitore.add(schedaAzioni());
    }

    /** Contenitore che si adatta in larghezza ma non in altezza. */
    private static final class Colonna extends JPanel implements Scrollable {
        Colonna() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Aspetto.FONDO);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibile, int orientamento, int direzione) {
            return 18;
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

    private JPanel scheda(String titolo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Aspetto.PANNELLO);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Aspetto.BORDO),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t = new JLabel(titolo.toUpperCase());
        t.setFont(Aspetto.monospaziato(10.5f, Font.BOLD));
        t.setForeground(Aspetto.ACCENTO);
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        p.add(t, BorderLayout.NORTH);
        return p;
    }

    private void riga(JPanel dove, String etichetta, java.awt.Component valore) {
        JPanel r = new JPanel(new BorderLayout(14, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JLabel nome = new JLabel(etichetta);
        nome.setFont(nome.getFont().deriveFont(12f));
        nome.setForeground(Aspetto.TESTO_DEBOLE);
        nome.setPreferredSize(new Dimension(200, 22));
        r.add(nome, BorderLayout.WEST);
        if (valore instanceof JLabel) {
            JLabel v = (JLabel) valore;
            v.setFont(Aspetto.monospaziato(12.5f, Font.PLAIN));
            v.setForeground(Aspetto.TESTO);
        }
        r.add(valore, BorderLayout.CENTER);
        dove.add(r);
    }

    private JPanel schedaSistema() {
        JPanel scheda = scheda("Sistema solare attuale");
        JPanel righe = new JPanel();
        righe.setLayout(new BoxLayout(righe, BoxLayout.Y_AXIS));
        righe.setOpaque(false);
        sistema.setFont(sistema.getFont().deriveFont(Font.BOLD, 15f));
        sistema.setForeground(Aspetto.TESTO);
        riga(righe, "Sistema", sistema);
        riga(righe, "Indirizzo galattico", indirizzo);
        riga(righe, "Stazioni già possedute", possedute);

        JPanel chiaveRiga = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        chiaveRiga.setOpaque(false);
        chiaveIcona.setPreferredSize(new Dimension(26, 26));
        chiaveRiga.add(chiaveIcona);
        chiave.setFont(Aspetto.monospaziato(12.5f, Font.PLAIN));
        chiaveRiga.add(chiave);
        riga(righe, "Chiave Station Override", chiaveRiga);

        scheda.add(righe, BorderLayout.CENTER);
        return scheda;
    }

    private JPanel schedaRequisiti() {
        JPanel scheda = scheda("Requisiti per rivendicare la stazione");
        JPanel righe = new JPanel();
        righe.setLayout(new BoxLayout(righe, BoxLayout.Y_AXIS));
        righe.setOpaque(false);
        for (int i = 0; i < REQUISITI.length; i++) {
            JLabel valore = new JLabel("—");
            valori.add(valore);
            righe.add(rigaRequisito(REQUISITI[i], valore));
        }
        scheda.add(righe, BorderLayout.CENTER);
        return scheda;
    }

    private JPanel rigaRequisito(Requisito r, JLabel valore) {
        JPanel riga = new JPanel(new BorderLayout(12, 0));
        riga.setOpaque(false);
        riga.setBorder(BorderFactory.createEmptyBorder(7, 0, 7, 0));

        JLabel immagine = new JLabel();
        Catalogo.Voce voce = catalogo.voce("^" + r.icona.replace("UI-", ""));
        ImageIcon img = icone.perFile(r.icona + ".PNG", 30);
        immagine.setIcon(img != null ? img : Icone.segno("◆", 30, Aspetto.TESTO_DEBOLE, true));
        immagine.setPreferredSize(new Dimension(34, 32));
        riga.add(immagine, BorderLayout.WEST);

        JPanel testi = new JPanel();
        testi.setOpaque(false);
        testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
        JLabel nome = new JLabel(r.nome);
        nome.setFont(nome.getFont().deriveFont(Font.BOLD, 12.5f));
        nome.setForeground(Aspetto.TESTO);
        JLabel spiega = new JLabel(r.spiegazione);
        spiega.setFont(spiega.getFont().deriveFont(11f));
        spiega.setForeground(Aspetto.TESTO_DEBOLE);
        testi.add(nome);
        testi.add(spiega);
        riga.add(testi, BorderLayout.CENTER);

        valore.setFont(Aspetto.monospaziato(12f, Font.BOLD));
        valore.setHorizontalAlignment(JLabel.RIGHT);
        valore.setPreferredSize(new Dimension(190, 24));
        riga.add(valore, BorderLayout.EAST);
        return riga;
    }

    private JPanel schedaAzioni() {
        JPanel scheda = scheda("Azioni");
        JPanel griglia = new JPanel(new java.awt.GridLayout(0, 2, 10, 10));
        griglia.setOpaque(false);

        griglia.add(pulsante("Soddisfa i requisiti",
                "Porta gradi e unità ai valori richiesti", "soddisfa"));
        griglia.add(pulsante("Aggiungi 1.000.000.000 unità",
                "Somma le unità necessarie al nucleo", "unita"));
        griglia.add(pulsante("Aggiungi 10 Station Override",
                "Mette dieci chiavi nell'inventario", "chiavi"));
        griglia.add(pulsante("Massimizza gilde e razze",
                "Porta tutte le reputazioni a 100", "reputazioni"));

        scheda.add(griglia, BorderLayout.CENTER);
        return scheda;
    }

    private JButton pulsante(String titolo, String spiegazione, final String codice) {
        JButton b = new JButton(titolo);
        b.setFont(b.getFont().deriveFont(12f));
        b.setToolTipText(spiegazione);
        b.setPreferredSize(new Dimension(260, 40));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                esegui(codice);
            }
        });
        return b;
    }

    // ------------------------------------------------------------------

    /** Legge i valori dal salvataggio aperto. */
    @SuppressWarnings("unchecked")
    public void mostra(Object radice) {
        this.radice = radice;
        Object psd = statoDelGiocatore(radice);
        if (!(psd instanceof Map)) {
            sistema.setText("Nessun salvataggio aperto");
            return;
        }
        Map<String, Object> stato = (Map<String, Object>) psd;

        // --- sistema attuale ---
        Object ua = stato.get("UniverseAddress");
        String indirizzoTesto = "—";
        if (ua instanceof Map) {
            Object ga = ((Map<String, Object>) ua).get("GalacticAddress");
            if (ga instanceof Map) {
                Map<String, Object> g = (Map<String, Object>) ga;
                indirizzoTesto = "X " + intero(g.get("VoxelX"))
                        + "   Y " + intero(g.get("VoxelY"))
                        + "   Z " + intero(g.get("VoxelZ"))
                        + "   sistema " + intero(g.get("SolarSystemIndex"))
                        + "   pianeta " + intero(g.get("PlanetIndex"));
            }
        }
        indirizzo.setText(indirizzoTesto);
        sistema.setText("Sistema " + intero(indirizzoDi(ua)));

        // --- valori delle statistiche ---
        possedute.setText(valoreDi(stato, "^STATIONS_OWNED") + " stazioni");
        for (int i = 0; i < REQUISITI.length; i++) {
            Requisito r = REQUISITI[i];
            long suo = 0;
            for (String s : r.statistiche) {
                suo = Math.max(suo, numeroDi(stato, s));
            }
            boolean ok = suo >= r.richiesto;
            JLabel l = valori.get(i);
            l.setText((r.inUnita ? compatta(suo) : String.valueOf(suo))
                    + " / " + (r.inUnita ? compatta(r.richiesto) : String.valueOf(r.richiesto))
                    + (ok ? "   OK" : "   da completare"));
            l.setForeground(ok ? Aspetto.OK : Aspetto.ATTENZIONE);
        }

        // --- la chiave nell'inventario ---
        boolean haChiave = haOggetto(stato, "^STATION_KEY");
        chiave.setText(haChiave ? "presente nell'inventario" : "assente");
        chiave.setForeground(haChiave ? Aspetto.OK : Aspetto.ERRORE);
        ImageIcon img = icone.perFile(iconaDi("^STATION_KEY"), 26);
        chiaveIcona.setIcon(img != null ? img : Icone.segno("◆", 26, Aspetto.TESTO_DEBOLE, true));

        revalidate();
        repaint();
    }

    private String iconaDi(String id) {
        Catalogo.Voce v = catalogo.voce(id);
        return v == null ? null : v.icona;
    }

    private static Object statoDelGiocatore(Object radice) {
        if (!(radice instanceof Map)) {
            return null;
        }
        Object base = ((Map<?, ?>) radice).get("BaseContext");
        return base instanceof Map ? ((Map<?, ?>) base).get("PlayerStateData") : null;
    }

    /** L'indirizzo del sistema attuale, o 0. */
    @SuppressWarnings("unchecked")
    private long indirizzoDi(Object ua) {
        if (!(ua instanceof Map)) {
            return 0;
        }
        Object ga = ((Map<String, Object>) ua).get("GalacticAddress");
        if (!(ga instanceof Map)) {
            return 0;
        }
        Map<String, Object> g = (Map<String, Object>) ga;
        // Le tre coordinate stanno in 4 bit ciascuna, poi l'indice del sistema.
        long x = numero(g.get("VoxelX")) & 0xFFF;
        long y = numero(g.get("VoxelY")) & 0xFFF;
        long z = numero(g.get("VoxelZ")) & 0xFFF;
        long s = numero(g.get("SolarSystemIndex")) & 0xFFF;
        return (x << 28) | (y << 16) | (z << 4) | (s & 0xF);
    }

    private static long numero(Object o) {
        if (o instanceof Json.Numero) {
            return ((Json.Numero) o).comeLong();
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0;
    }

    private static String intero(Object o) {
        return String.valueOf(numero(o));
    }

    /**
     * Il valore di una statistica, o di un campo diretto del giocatore.
     *
     * Le unita' non sono una statistica: sono il campo {@code Units} dello stato
     * del giocatore. Cercarle fra le statistiche con un identificativo inventato
     * ({@code ^UNITS_PAID}) dava sempre zero.
     */
    @SuppressWarnings("unchecked")
    private long numeroDi(Map<String, Object> stato, String nome) {
        // Campi diretti del giocatore, non statistiche.
        if ("Units".equals(nome)) {
            long v = numero(stato.get("Units"));
            // Le unita' sono un intero senza segno a 32 bit: superata la soglia
            // diventano negative. Un valore negativo qui vuol dire "tante", non
            // "sotto zero", e va riletto come tale.
            if (v < 0) {
                v += 4294967296L;
            }
            return v;
        }
        Object stats = stato.get("Stats");
        if (!(stats instanceof List)) {
            return 0;
        }
        for (Object gruppo : (List<Object>) stats) {
            if (!(gruppo instanceof Map)) {
                continue;
            }
            Map<String, Object> g = (Map<String, Object>) gruppo;
            if (numero(g.get("Address")) != 0) {
                continue;
            }
            Object voci = g.get("Stats");
            if (!(voci instanceof List)) {
                continue;
            }
            for (Object voce : (List<Object>) voci) {
                if (!(voce instanceof Map)) {
                    continue;
                }
                Map<String, Object> v = (Map<String, Object>) voce;
                if (nome.equals(String.valueOf(v.get("Id")))) {
                    Object valore = v.get("Value");
                    if (valore instanceof Map) {
                        return numero(((Map<String, Object>) valore).get("IntValue"));
                    }
                    return numero(valore);
                }
            }
        }
        return 0;
    }

    /** Vero se l'oggetto e' presente nell'inventario. */
    @SuppressWarnings("unchecked")
    private boolean haOggetto(Map<String, Object> stato, String id) {
        Object inv = stato.get("Inventory");
        if (!(inv instanceof Map)) {
            return false;
        }
        Object slot = ((Map<String, Object>) inv).get("Slots");
        if (!(slot instanceof List)) {
            return false;
        }
        for (Object s : (List<Object>) slot) {
            if (s instanceof Map && id.equals(String.valueOf(((Map<String, Object>) s).get("Id")))) {
                return true;
            }
        }
        return false;
    }

    private String valoreDi(Map<String, Object> stato, String id) {
        return String.valueOf(numeroDi(stato, id));
    }

    /** Un numero leggibile: 269951037 diventa 269.951.037. */
    private static String compatta(long n) {
        String s = String.valueOf(Math.abs(n));
        StringBuilder b = new StringBuilder();
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            b.append(s.charAt(i));
            if (++c % 3 == 0 && i > 0) {
                b.append('.');
            }
        }
        return (n < 0 ? "-" : "") + b.reverse();
    }

    // ------------------------------------------------------------------

    private void esegui(String codice) {
        Object psd = statoDelGiocatore(radice);
        if (!(psd instanceof Map)) {
            return;
        }
        Map<String, Object> stato = (Map<String, Object>) psd;
        int toccati = 0;

        if ("soddisfa".equals(codice)) {
            // I tre requisiti numerici, con gli identificativi del vecchio
            // editor: razze a 30, gilde a 15, contratti a 5.
            for (String s : new String[]{"^TRA_STANDING", "^EXP_STANDING", "^WAR_STANDING"}) {
                toccati += impostaAlmeno(stato, s, 30);
            }
            for (String s : new String[]{"^TGUILD_STAND", "^EGUILD_STAND", "^WGUILD_STAND"}) {
                toccati += impostaAlmeno(stato, s, 15);
            }
            toccati += impostaAlmeno(stato, "^SP_POI_MISSIONS", 5);
        }
        if ("reputazioni".equals(codice)) {
            for (String s : new String[]{"^TRA_STANDING", "^EXP_STANDING", "^WAR_STANDING",
                    "^TGUILD_STAND", "^EGUILD_STAND", "^WGUILD_STAND", "^PIR_STAND"}) {
                toccati += impostaAlmeno(stato, s, 100);
            }
        }
        if ("chiavi".equals(codice)) {
            toccati += aggiungiChiavi(stato, 10);
        }

        if (toccati > 0) {
            suModifica.run();
        }
        mostra(radice);
        javax.swing.JOptionPane.showMessageDialog(this,
                toccati == 0 ? "Niente da cambiare." : "Punti modificati: " + toccati,
                "Fatto", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Porta una statistica almeno a un valore, senza abbassarla.
     *
     * Alzare e' quello che serve per soddisfare un requisito; abbassare
     * distruggerebbe reputazione che il giocatore ha guadagnato. Se il valore
     * attuale e' gia' piu' alto, non si tocca niente.
     */
    @SuppressWarnings("unchecked")
    private int impostaAlmeno(Map<String, Object> stato, String nome, long minimo) {
        long attuale = numeroDi(stato, nome);
        if (attuale >= minimo) {
            return 0;
        }
        return impostaStatistica(stato, nome, minimo);
    }

    /** Imposta una statistica nel gruppo globale. Restituisce quante ne ha toccate. */
    @SuppressWarnings("unchecked")
    private int impostaStatistica(Map<String, Object> stato, String nome, long valore) {
        Object stats = stato.get("Stats");
        if (!(stats instanceof List)) {
            return 0;
        }
        int toccati = 0;
        for (Object gruppo : (List<Object>) stats) {
            if (!(gruppo instanceof Map)) {
                continue;
            }
            Map<String, Object> g = (Map<String, Object>) gruppo;
            if (numero(g.get("Address")) != 0) {
                continue;
            }
            Object voci = g.get("Stats");
            if (!(voci instanceof List)) {
                continue;
            }
            for (Object voce : (List<Object>) voci) {
                if (!(voce instanceof Map)) {
                    continue;
                }
                Map<String, Object> v = (Map<String, Object>) voce;
                if (!nome.equals(String.valueOf(v.get("Id")))) {
                    continue;
                }
                Object valoreAttuale = v.get("Value");
                if (valoreAttuale instanceof Map) {
                    ((Map<String, Object>) valoreAttuale).put("IntValue",
                            new Json.Numero(String.valueOf(valore)));
                    toccati++;
                }
            }
        }
        return toccati;
    }

    /** Aggiunge chiavi Station Override in uno slot libero dell'inventario. */
    @SuppressWarnings("unchecked")
    private int aggiungiChiavi(Map<String, Object> stato, int quante) {
        Object inv = stato.get("Inventory");
        if (!(inv instanceof Map)) {
            return 0;
        }
        Object slot = ((Map<String, Object>) inv).get("Slots");
        if (!(slot instanceof List)) {
            return 0;
        }
        int messe = 0;
        for (Object s : (List<Object>) slot) {
            if (messe >= quante || !(s instanceof Map)) {
                break;
            }
            Map<String, Object> m = (Map<String, Object>) s;
            Object id = m.get("Id");
            if (id != null && !String.valueOf(id).isEmpty() && !"^".equals(String.valueOf(id))) {
                continue;
            }
            m.put("Id", "^STATION_KEY");
            m.put("Amount", new Json.Numero("1"));
            m.put("MaxAmount", new Json.Numero("1"));
            m.put("DamageFactor", new Json.Numero("0.0"));
            m.put("FullyInstalled", Boolean.TRUE);
            messe++;
        }
        return messe;
    }
}

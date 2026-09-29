package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conquista stazioni: cosa serve per rivendicare una stazione spaziale.
 *
 * Nel gioco una stazione non assegnata si rivendica al Nucleo della Stazione
 * soddisfacendo quattro requisiti. Questo pannello li mostra uno per uno, con
 * il valore che hai in <b>questo sistema</b> e quello richiesto, e permette sia di
 * soddisfarli che di rivendicare o rinominare la stazione direttamente.
 *
 * <p><b>I requisiti sono locali, non globali.</b> La reputazione con la razza
 * e con la gilda che contano per la stazione sono quelle accumulate
 * <i>nel sistema in cui ti trovi</i>. La reputazione galattica non viene
 * guardata dal Nucleo.
 */
public final class PannelloStazioni extends JPanel {

    /** Un requisito da soddisfare. */
    private static final class Requisito {
        final String nome;
        final String spiegazione;
        final String icona;
        final int richiesto;
        /** Vero se si misura in unita' invece che in gradi. */
        final boolean inUnita;
        /** Vero se il valore va letto nel sistema attuale invece che in globale. */
        final boolean locale;
        /** Gli identificativi di statistica da guardare. */
        final String[] statistiche;

        Requisito(String nome, String spiegazione, String icona, int richiesto,
                  boolean inUnita, boolean locale, String... statistiche) {
            this.nome = nome;
            this.spiegazione = spiegazione;
            this.icona = icona;
            this.richiesto = richiesto;
            this.inUnita = inUnita;
            this.locale = locale;
            this.statistiche = statistiche;
        }
    }

    private static final Requisito[] REQUISITI = {
            new Requisito("1. Registrazione razza", "Il grado di reputazione con la razza che vive in questo sistema",
                    "UI-GEK", 30, false, true, "^TRA_STANDING", "^EXP_STANDING", "^WAR_STANDING"),
            new Requisito("2. Registrazione gilda", "Il grado di reputazione con le gilde di questo sistema",
                    "UI-TRADERS", 15, false, true, "^TGUILD_STAND", "^EGUILD_STAND", "^WGUILD_STAND"),
            new Requisito("3. Contratti di recupero", "I contratti di recupero completati in questo sistema",
                    "UI-MILESTONES", 5, false, true, "^SP_POI_MISSIONS"),
            new Requisito("4. Unita' per il nucleo", "Le unita' disponibili per la donazione al nucleo",
                    "SUBSTANCE-UNITS", 1000000000, true, false, "Units"),
    };

    /** Le razze, per il pulsante che massimizza. */
    private static final String[] RAZZE = {
            "^TRA_STANDING", "^EXP_STANDING", "^WAR_STANDING", "^BUI_STANDING", "^PIR_STAND"};

    /** Le gilde, per il pulsante che massimizza. */
    private static final String[] GILDE = {
            "^TGUILD_STAND", "^EGUILD_STAND", "^WGUILD_STAND"};

    private final Catalogo catalogo;
    private final Icone icone;
    private final Runnable suModifica;

    private final JPanel contenitore = new Colonna();
    private final JLabel dove = new JLabel("—");
    private final JLabel stazione = new JLabel("—");
    private final JLabel indirizzo = new JLabel("—");
    private final JLabel codice = new JLabel("—");
    private final JLabel possedute = new JLabel("—");
    private final JLabel chiave = new JLabel("—");
    private final JLabel chiaveIcona = new JLabel();
    private final List<JLabel> valori = new ArrayList<JLabel>();

    private JButton btnRivendica;
    private JButton btnAbbandona;

    private Object radice;
    /** L'indirizzo del sistema aperto, 0 se non e' determinabile. */
    private long indirizzoSistema;
    private String nomeStazioneRilevato = "";
    private boolean stazioneGiaRivendicata = false;
    private Map<String, Object> baseStazioneEsistente = null;

    public PannelloStazioni(Catalogo catalogo, Icone icone, Runnable suModifica) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.FONDO);
        contenitore.setBackground(Aspetto.FONDO);
        contenitore.setBorder(Aspetto.bordo(20, 24, 24, 24));

        JScrollPane scorrimento = new JScrollPane(contenitore);
        scorrimento.setBorder(BorderFactory.createEmptyBorder());
        scorrimento.getVerticalScrollBar().setUnitIncrement(Aspetto.px(18));
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
                Aspetto.bordo(16, 18, 16, 18)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t = new JLabel(titolo.toUpperCase());
        t.setFont(Aspetto.monospaziato(10.5f, Font.BOLD));
        t.setForeground(Aspetto.ACCENTO);
        t.setBorder(Aspetto.bordo(0, 0, 12, 0));
        p.add(t, BorderLayout.NORTH);
        return p;
    }

    private void riga(JPanel dove2, String etichetta, java.awt.Component valore) {
        JPanel r = new JPanel(new BorderLayout(14, 0));
        r.setOpaque(false);
        r.setBorder(Aspetto.bordo(4, 0, 4, 0));
        JLabel nome = new JLabel(etichetta);
        nome.setFont(nome.getFont().deriveFont(Aspetto.punti(12f)));
        nome.setForeground(Aspetto.TESTO_DEBOLE);
        nome.setPreferredSize(Aspetto.dim(200, 22));
        r.add(nome, BorderLayout.WEST);
        if (valore instanceof JLabel) {
            JLabel v = (JLabel) valore;
            v.setFont(Aspetto.monospaziato(12.5f, Font.PLAIN));
            v.setForeground(Aspetto.TESTO);
        }
        r.add(valore, BorderLayout.CENTER);
        dove2.add(r);
    }

    private JPanel schedaSistema() {
        JPanel scheda = scheda("Sistema solare attuale");
        JPanel righe = new JPanel();
        righe.setLayout(new BoxLayout(righe, BoxLayout.Y_AXIS));
        righe.setOpaque(false);
        dove.setFont(dove.getFont().deriveFont(Font.BOLD, Aspetto.punti(15f)));
        dove.setForeground(Aspetto.TESTO);
        riga(righe, "Dove ti trovi", dove);
        riga(righe, "Stazione in questo sistema", stazione);
        riga(righe, "Indirizzo galattico", indirizzo);
        riga(righe, "Codice del sistema", codice);
        riga(righe, "Stazioni già possedute", possedute);

        JPanel chiaveRiga = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        chiaveRiga.setOpaque(false);
        chiaveIcona.setPreferredSize(Aspetto.dim(26, 26));
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
        riga.setBorder(Aspetto.bordo(7, 0, 7, 0));

        JLabel immagine = new JLabel();
        ImageIcon img = icone.perFile(r.icona + ".PNG", 30);
        immagine.setIcon(img != null ? img : Icone.segno("◆", 30, Aspetto.TESTO_DEBOLE, true));
        immagine.setPreferredSize(Aspetto.dim(34, 32));
        riga.add(immagine, BorderLayout.WEST);

        JPanel testi = new JPanel();
        testi.setOpaque(false);
        testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
        JLabel nome = new JLabel(r.nome);
        nome.setFont(nome.getFont().deriveFont(Font.BOLD, Aspetto.punti(12.5f)));
        nome.setForeground(Aspetto.TESTO);
        JLabel spiega = new JLabel(r.spiegazione);
        spiega.setFont(spiega.getFont().deriveFont(Aspetto.punti(11f)));
        spiega.setForeground(Aspetto.TESTO_DEBOLE);
        testi.add(nome);
        testi.add(spiega);
        riga.add(testi, BorderLayout.CENTER);

        valore.setFont(Aspetto.monospaziato(12f, Font.BOLD));
        valore.setHorizontalAlignment(JLabel.RIGHT);
        valore.setPreferredSize(Aspetto.dim(250, 24));
        riga.add(valore, BorderLayout.EAST);
        return riga;
    }

    private JPanel schedaAzioni() {
        JPanel scheda = scheda("Azioni sulla stazione");
        JPanel pannello = new JPanel();
        pannello.setLayout(new BoxLayout(pannello, BoxLayout.Y_AXIS));
        pannello.setOpaque(false);

        // Riga gestione stazione (rivendica / abbandona)
        JPanel rigaRivendica = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        rigaRivendica.setOpaque(false);
        rigaRivendica.setBorder(Aspetto.bordo(0, 0, 12, 0));

        btnRivendica = new JButton("★ Rivendica questa stazione");
        btnRivendica.setFont(btnRivendica.getFont().deriveFont(Font.BOLD, Aspetto.punti(12.5f)));
        btnRivendica.setPreferredSize(Aspetto.dim(280, 42));
        btnRivendica.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                azioneRivendicaORinomina();
            }
        });
        rigaRivendica.add(btnRivendica);

        btnAbbandona = new JButton("Abbandona rivendicazione");
        btnAbbandona.setFont(btnAbbandona.getFont().deriveFont(Aspetto.punti(12f)));
        btnAbbandona.setPreferredSize(Aspetto.dim(220, 42));
        btnAbbandona.setForeground(Aspetto.ERRORE);
        btnAbbandona.setVisible(false);
        btnAbbandona.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                azioneAbbandona();
            }
        });
        rigaRivendica.add(btnAbbandona);
        pannello.add(rigaRivendica);

        // Griglia requisiti e risorse
        JPanel griglia = new JPanel(new java.awt.GridLayout(0, 2, 10, 10));
        griglia.setOpaque(false);

        griglia.add(pulsante("Soddisfa tutti i requisiti",
                "Porta razza (30), gilda (15), contratti (5), unità (almeno 1 mld) e chiave Override ai requisiti", "soddisfa"));
        griglia.add(pulsante("Aggiungi 1.000.000.000 unità",
                "Somma un miliardo di unità al totale del giocatore", "unita"));
        griglia.add(pulsante("Aggiungi 10 Station Override",
                "Aggiunge dieci chiavi Station Override nell'inventario della tuta", "chiavi"));
        griglia.add(pulsante("Massimizza gilde e razze (100)",
                "Porta a 100 le reputazioni globali e quelle di questo sistema", "reputazioni"));

        pannello.add(griglia);
        scheda.add(pannello, BorderLayout.CENTER);
        return scheda;
    }

    private JButton pulsante(String titolo, String spiegazione, final String codice) {
        JButton b = new JButton(titolo);
        b.setFont(b.getFont().deriveFont(Aspetto.punti(12f)));
        b.setToolTipText(spiegazione);
        b.setPreferredSize(Aspetto.dim(260, 40));
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
            dove.setText("Nessun salvataggio aperto");
            stazione.setText("—");
            indirizzo.setText("—");
            codice.setText("—");
            possedute.setText("—");
            chiave.setText("—");
            btnRivendica.setEnabled(false);
            btnAbbandona.setVisible(false);
            return;
        }
        Map<String, Object> stato = (Map<String, Object>) psd;
        btnRivendica.setEnabled(true);

        // --- sistema attuale ---
        Object ua = stato.get("UniverseAddress");
        indirizzoSistema = indirizzoDelSistema(ua);
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
        codice.setText(indirizzoSistema == 0
                ? "non determinabile" : String.valueOf(indirizzoSistema));

        Object riassunto = stato.get("SaveSummary");
        String nomeBase = riassunto == null ? "" : String.valueOf(riassunto).trim();
        dove.setText(nomeBase.isEmpty() ? "(senza nome)" : nomeBase);

        // Controllo stazione del sistema
        baseStazioneEsistente = trovaBaseStazione(stato, indirizzoSistema);
        stazioneGiaRivendicata = (baseStazioneEsistente != null);

        if (stazioneGiaRivendicata) {
            nomeStazioneRilevato = String.valueOf(baseStazioneEsistente.get("Name")).trim();
            if (nomeStazioneRilevato.isEmpty()) {
                nomeStazioneRilevato = "Stazione Spaziale";
            }
            stazione.setText(nomeStazioneRilevato + "   (Rivendicata)");
            stazione.setForeground(Aspetto.OK);
            btnRivendica.setText("Rinomina stazione rivendicata");
            btnRivendica.setToolTipText("Modifica il nome assegnato a questa stazione spaziale");
            btnAbbandona.setVisible(true);
        } else {
            nomeStazioneRilevato = cercaNomeStazioneNonRivendicata(stato, indirizzoSistema);
            stazione.setText(nomeStazioneRilevato + "   (Non ancora rivendicata)");
            stazione.setForeground(Aspetto.ATTENZIONE);
            btnRivendica.setText("★ Rivendica questa stazione");
            btnRivendica.setToolTipText("Rivendica \"" + nomeStazioneRilevato + "\" e aggiungila alle tue stazioni");
            btnAbbandona.setVisible(false);
        }

        // --- valori dei requisiti ---
        possedute.setText(valoreGlobale(stato, "^STATIONS_OWNED") + " stazioni");
        for (int i = 0; i < REQUISITI.length; i++) {
            Requisito r = REQUISITI[i];
            long suo = 0;
            for (String s : r.statistiche) {
                suo = Math.max(suo, r.locale
                        ? valoreSistema(stato, indirizzoSistema, s)
                        : valoreGlobale(stato, s));
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

    /**
     * Il nome della stazione rivendicata in questo sistema, o stringa vuota se non rivendicata.
     */
    @SuppressWarnings("unchecked")
    private String nomeStazione(Map<String, Object> stato, long indirizzo) {
        Map<String, Object> base = trovaBaseStazione(stato, indirizzo);
        if (base != null) {
            Object n = base.get("Name");
            return n == null ? "" : String.valueOf(n).trim();
        }
        return "";
    }

    /**
     * Cerca la base di tipo stazione rivendicata in questo sistema.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> trovaBaseStazione(Map<String, Object> stato, long indirizzo) {
        if (indirizzo == 0) {
            return null;
        }
        Object basi = stato.get("PersistentPlayerBases");
        if (!(basi instanceof List)) {
            return null;
        }
        long sysMask = ~(0xFL << 52); // azzera indice pianeta
        for (Object b : (List<Object>) basi) {
            if (!(b instanceof Map)) {
                continue;
            }
            Map<String, Object> base = (Map<String, Object>) b;
            if (!String.valueOf(base.get("BaseType")).contains("Station")) {
                continue;
            }
            long suo = indirizzoGrezzo(base.get("GalacticAddress"));
            if (suo != 0 && (suo & sysMask) == (indirizzo & sysMask)) {
                return base;
            }
        }
        return null;
    }

    /**
     * Cerca il nome della stazione spaziale del sistema quando non e' ancora rivendicata.
     * Cerca nei TeleportEndpoints o nel SaveSummary.
     */
    @SuppressWarnings("unchecked")
    private static String cercaNomeStazioneNonRivendicata(Map<String, Object> stato, long indirizzo) {
        Object tps = stato.get("TeleportEndpoints");
        if (tps instanceof List && indirizzo != 0) {
            long sysMask = ~(0xFL << 52);
            for (Object o : (List<Object>) tps) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<String, Object> tp = (Map<String, Object>) o;
                if ("Spacestation".equalsIgnoreCase(String.valueOf(tp.get("TeleporterType")))) {
                    long tpAddr = indirizzoDelSistema(tp.get("UniverseAddress"));
                    if (tpAddr != 0 && (tpAddr & sysMask) == (indirizzo & sysMask)) {
                        Object n = tp.get("Name");
                        if (n != null && !String.valueOf(n).trim().isEmpty()) {
                            return String.valueOf(n).trim();
                        }
                    }
                }
            }
        }
        Object riassunto = stato.get("SaveSummary");
        if (riassunto != null) {
            String s = String.valueOf(riassunto).trim();
            if (s.startsWith("A bordo ")) {
                s = s.substring("A bordo ".length()).trim();
            }
            String min = s.toLowerCase();
            if (min.contains("stazione") || min.contains("station") || min.contains("orbitale")) {
                return s;
            }
        }
        return "Stazione Spaziale";
    }

    private static Object statoDelGiocatore(Object radice) {
        if (!(radice instanceof Map)) {
            return null;
        }
        Object base = ((Map<?, ?>) radice).get(Inventari.contesto(radice));
        return base instanceof Map ? ((Map<?, ?>) base).get("PlayerStateData") : null;
    }

    // ------------------------------------------------------------------
    // Indirizzi

    /**
     * L'indirizzo del sistema attuale, nella forma che il gioco usa per le
     * statistiche locali e per le basi.
     */
    private static long indirizzoDelSistema(Object universo) {
        if (!(universo instanceof Map)) {
            return 0;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> u = (Map<String, Object>) universo;
        Object ga = u.get("GalacticAddress");
        if (!(ga instanceof Map)) {
            return 0;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> g = (Map<String, Object>) ga;
        long x = numero(g.get("VoxelX")) & 0xFFF;
        long y = numero(g.get("VoxelY")) & 0xFF;
        long z = numero(g.get("VoxelZ")) & 0xFFF;
        long s = numero(g.get("SolarSystemIndex")) & 0xFFF;
        long r = numero(u.get("RealityIndex")) & 0xFF;
        return (s << 40) | (r << 32) | (y << 24) | (z << 12) | x;
    }

    /**
     * Normalizza un indirizzo letto dal file (supporta numeri, stringhe decimali o esadecimali).
     */
    private static long indirizzoGrezzo(Object valore) {
        if (valore instanceof String) {
            String s = ((String) valore).trim();
            if (s.startsWith("0x") || s.startsWith("0X")) {
                try {
                    return Long.parseUnsignedLong(s.substring(2), 16);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            try {
                return Long.parseUnsignedLong(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return numero(valore);
    }

    // ------------------------------------------------------------------
    // Lettura delle statistiche

    @SuppressWarnings("unchecked")
    private static Map<String, Object> gruppoDiSistema(Map<String, Object> stato, long indirizzo) {
        if (indirizzo == 0) {
            return null;
        }
        Object stats = stato.get("Stats");
        if (!(stats instanceof List)) {
            return null;
        }
        for (Object gruppo : (List<Object>) stats) {
            if (!(gruppo instanceof Map)) {
                continue;
            }
            Map<String, Object> g = (Map<String, Object>) gruppo;
            if (!"^SYSTEM_STATS".equals(String.valueOf(g.get("GroupId")))) {
                continue;
            }
            if (indirizzoGrezzo(g.get("Address")) == indirizzo) {
                return g;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> gruppoGlobale(Map<String, Object> stato) {
        Object stats = stato.get("Stats");
        if (!(stats instanceof List)) {
            return null;
        }
        List<Object> gruppi = (List<Object>) stats;
        for (Object gruppo : gruppi) {
            if (gruppo instanceof Map
                    && "^GLOBAL_STATS".equals(String.valueOf(((Map<String, Object>) gruppo).get("GroupId")))) {
                return (Map<String, Object>) gruppo;
            }
        }
        for (Object gruppo : gruppi) {
            if (gruppo instanceof Map
                    && indirizzoGrezzo(((Map<String, Object>) gruppo).get("Address")) == 0) {
                return (Map<String, Object>) gruppo;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static long valoreIn(Map<String, Object> gruppo, String nome) {
        if (gruppo == null) {
            return 0;
        }
        Object voci = gruppo.get("Stats");
        if (!(voci instanceof List)) {
            return 0;
        }
        for (Object voce : (List<Object>) voci) {
            if (!(voce instanceof Map)) {
                continue;
            }
            Map<String, Object> v = (Map<String, Object>) voce;
            if (!nome.equals(String.valueOf(v.get("Id")))) {
                continue;
            }
            Object valore = v.get("Value");
            if (valore instanceof Map) {
                return numero(((Map<String, Object>) valore).get("IntValue"));
            }
            return numero(valore);
        }
        return 0;
    }

    private static long valoreSistema(Map<String, Object> stato, long indirizzo, String nome) {
        return valoreIn(gruppoDiSistema(stato, indirizzo), nome);
    }

    private static long valoreGlobale(Map<String, Object> stato, String nome) {
        if ("Units".equals(nome)) {
            long v = numero(stato.get("Units"));
            if (v < 0) {
                v += 4294967296L;
            }
            return v;
        }
        return valoreIn(gruppoGlobale(stato), nome);
    }

    private static long numero(Object o) {
        if (o instanceof Json.Numero) {
            return ((Json.Numero) o).comeLong();
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            String s = ((String) o).trim();
            if (s.startsWith("0x") || s.startsWith("0X")) {
                try {
                    return Long.parseUnsignedLong(s.substring(2), 16);
                } catch (Exception ignored) {
                }
            }
            try {
                return Long.parseUnsignedLong(s);
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private static String intero(Object o) {
        return String.valueOf(numero(o));
    }

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
    // Azioni e modifiche

    @SuppressWarnings("unchecked")
    private void esegui(String codiceAzione) {
        Object psd = statoDelGiocatore(radice);
        if (!(psd instanceof Map)) {
            return;
        }
        Map<String, Object> stato = (Map<String, Object>) psd;
        long indirizzo = indirizzoDelSistema(stato.get("UniverseAddress"));
        indirizzoSistema = indirizzo;
        int toccati = 0;

        if ("soddisfa".equals(codiceAzione)) {
            if (indirizzo == 0) {
                avvisa("Non riesco a determinare il sistema attuale: niente da modificare.");
                return;
            }
            toccati = soddisfaTuttiIRequisiti(stato, indirizzo);
        } else if ("unita".equals(codiceAzione)) {
            toccati = aggiungiUnita(stato, 1000000000L);
        } else if ("reputazioni".equals(codiceAzione)) {
            for (String s : RAZZE) {
                toccati += impostaAlmenoGlobale(stato, s, 100);
            }
            for (String s : GILDE) {
                toccati += impostaAlmenoGlobale(stato, s, 100);
            }
            if (indirizzo != 0) {
                for (String s : RAZZE) {
                    if ("^BUI_STANDING".equals(s) || "^PIR_STAND".equals(s)) {
                        continue;
                    }
                    toccati += impostaAlmenoSistema(stato, indirizzo, s, 100);
                }
                for (String s : GILDE) {
                    toccati += impostaAlmenoSistema(stato, indirizzo, s, 100);
                }
                toccati += impostaAlmenoSistema(stato, indirizzo, "^SP_POI_MISSIONS", 10);
            }
        } else if ("chiavi".equals(codiceAzione)) {
            toccati = aggiungiChiavi(stato, 10);
        }

        if (toccati > 0) {
            suModifica.run();
        }
        mostra(radice);
        JOptionPane.showMessageDialog(this,
                toccati == 0 ? "Tutti i requisiti risultano già soddisfatti." : "Parametri aggiornati con successo (" + toccati + " modifiche).",
                "Operazione completata", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Rivendica la stazione corrente o la rinomina se già presente.
     */
    @SuppressWarnings("unchecked")
    private void azioneRivendicaORinomina() {
        Object psd = statoDelGiocatore(radice);
        if (!(psd instanceof Map)) {
            return;
        }
        Map<String, Object> stato = (Map<String, Object>) psd;
        long indirizzo = indirizzoDelSistema(stato.get("UniverseAddress"));
        indirizzoSistema = indirizzo;

        if (indirizzo == 0) {
            avvisa("Impossibile determinare le coordinate del sistema attuale.");
            return;
        }

        if (stazioneGiaRivendicata && baseStazioneEsistente != null) {
            String vecchioNome = String.valueOf(baseStazioneEsistente.get("Name")).trim();
            String nuovoNome = JOptionPane.showInputDialog(this,
                    "Nuovo nome per la stazione rivendicata:",
                    vecchioNome);
            if (nuovoNome == null || nuovoNome.trim().isEmpty()) {
                return;
            }
            baseStazioneEsistente.put("Name", nuovoNome.trim());
            suModifica.run();
            mostra(radice);
            JOptionPane.showMessageDialog(this, "Stazione rinominata in: " + nuovoNome.trim(),
                    "Fatto", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String nomeProposto = (nomeStazioneRilevato != null && !nomeStazioneRilevato.isEmpty())
                ? nomeStazioneRilevato : "Stazione Spaziale";
        String nomeScelto = JOptionPane.showInputDialog(this,
                "Nome con cui rivendicare questa stazione spaziale:",
                nomeProposto);
        if (nomeScelto == null || nomeScelto.trim().isEmpty()) {
            return;
        }
        nomeScelto = nomeScelto.trim();

        // 1. Trova l'Owner esistente da altre basi o creane uno
        Map<String, Object> owner = trovaOCreaOwner(stato);

        // 2. Crea la base di tipo PlayerSpaceStationBase
        Object basiObj = stato.get("PersistentPlayerBases");
        List<Object> basi;
        if (basiObj instanceof List) {
            basi = (List<Object>) basiObj;
        } else {
            basi = new ArrayList<Object>();
            stato.put("PersistentPlayerBases", basi);
        }

        Map<String, Object> nuovaBase = new LinkedHashMap<String, Object>();
        nuovaBase.put("BaseVersion", new Json.Numero("8"));
        nuovaBase.put("OriginalBaseVersion", new Json.Numero("8"));
        long addrPianeta0 = indirizzo & ~(0xFL << 52);
        nuovaBase.put("GalacticAddress", new Json.Numero(String.valueOf(addrPianeta0)));

        List<Object> pos = new ArrayList<Object>();
        pos.add(new Json.Numero("0.0"));
        pos.add(new Json.Numero("-119.78955078125"));
        pos.add(new Json.Numero("0.0"));
        nuovaBase.put("Position", pos);

        List<Object> fwd = new ArrayList<Object>();
        fwd.add(new Json.Numero("0.7477687001228333"));
        fwd.add(new Json.Numero("-0.48580285906791689"));
        fwd.add(new Json.Numero("0.4525900185108185"));
        nuovaBase.put("Forward", fwd);

        nuovaBase.put("UserData", new Json.Numero("0"));
        long ora = System.currentTimeMillis() / 1000L;
        nuovaBase.put("LastUpdateTimestamp", new Json.Numero(String.valueOf(ora)));
        nuovaBase.put("Objects", new ArrayList<Object>());
        nuovaBase.put("RID", "");
        nuovaBase.put("Owner", owner);
        nuovaBase.put("Name", nomeScelto);

        Map<String, Object> tipo = new LinkedHashMap<String, Object>();
        tipo.put("PersistentBaseTypes", "PlayerSpaceStationBase");
        nuovaBase.put("BaseType", tipo);

        nuovaBase.put("LastEditedById", "");
        nuovaBase.put("LastEditedByUsername", "");

        List<Object> scAt = new ArrayList<Object>();
        scAt.add(new Json.Numero("-0.5232313871383667"));
        scAt.add(new Json.Numero("0.11336517333984375"));
        scAt.add(new Json.Numero("0.8446165919303894"));
        nuovaBase.put("ScreenshotAt", scAt);

        List<Object> scPos = new ArrayList<Object>();
        scPos.add(new Json.Numero("-16.478759765625"));
        scPos.add(new Json.Numero("9.696029663085938"));
        scPos.add(new Json.Numero("-909.3487548828125"));
        nuovaBase.put("ScreenshotPos", scPos);

        Map<String, Object> gm = new LinkedHashMap<String, Object>();
        gm.put("PresetGameMode", "Normal");
        nuovaBase.put("GameMode", gm);

        Map<String, Object> diff = new LinkedHashMap<String, Object>();
        Map<String, Object> diffPreset = new LinkedHashMap<String, Object>();
        diffPreset.put("DifficultyPresetType", "Custom");
        diff.put("DifficultyPreset", diffPreset);
        diff.put("PersistentBaseDifficultyFlags", new Json.Numero("0"));
        nuovaBase.put("Difficulty", diff);

        nuovaBase.put("PlatformToken", "");
        nuovaBase.put("IsReported", Boolean.FALSE);
        nuovaBase.put("IsFeatured", Boolean.FALSE);

        Map<String, Object> ap = new LinkedHashMap<String, Object>();
        ap.put("BaseAutoPowerSetting", "UseDefault");
        nuovaBase.put("AutoPowerSetting", ap);

        basi.add(nuovaBase);

        // 3. Incrementa ^STATIONS_OWNED nel gruppo globale
        long vecchiePossedute = valoreGlobale(stato, "^STATIONS_OWNED");
        scriviStatistica(gruppoGlobale(stato), "^STATIONS_OWNED", vecchiePossedute + 1);

        // 4. Soddisfa anche tutti i requisiti di sistema
        soddisfaTuttiIRequisiti(stato, indirizzo);

        suModifica.run();
        mostra(radice);
        JOptionPane.showMessageDialog(this,
                "Stazione \"" + nomeScelto + "\" rivendicata con successo!\n"
                        + "Aggiunta all'elenco delle basi permanenti e contatore stazioni aggiornato a " + (vecchiePossedute + 1) + ".",
                "Stazione Rivendicata", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Abbandona la rivendicazione della stazione corrente.
     */
    @SuppressWarnings("unchecked")
    private void azioneAbbandona() {
        if (!stazioneGiaRivendicata || baseStazioneEsistente == null) {
            return;
        }
        Object psd = statoDelGiocatore(radice);
        if (!(psd instanceof Map)) {
            return;
        }
        Map<String, Object> stato = (Map<String, Object>) psd;

        int conferma = JOptionPane.showConfirmDialog(this,
                "Sei sicuro di voler abbandonare la rivendicazione di questa stazione?\n"
                        + "Verrà rimossa dalle tue stazioni possedute.",
                "Conferma abbandono", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conferma != JOptionPane.YES_OPTION) {
            return;
        }

        Object basiObj = stato.get("PersistentPlayerBases");
        if (basiObj instanceof List) {
            ((List<Object>) basiObj).remove(baseStazioneEsistente);
        }

        long posseduteVal = valoreGlobale(stato, "^STATIONS_OWNED");
        if (posseduteVal > 0) {
            scriviStatistica(gruppoGlobale(stato), "^STATIONS_OWNED", posseduteVal - 1);
        }

        suModifica.run();
        mostra(radice);
        JOptionPane.showMessageDialog(this, "Rivendicazione abbandonata.",
                "Fatto", JOptionPane.INFORMATION_MESSAGE);
    }

    private void avvisa(String testo) {
        JOptionPane.showMessageDialog(this, testo, "Attenzione", JOptionPane.WARNING_MESSAGE);
    }

    // --- statistiche --------------------------------------------------

    private int impostaAlmenoSistema(Map<String, Object> stato, long indirizzo,
                                     String nome, long minimo) {
        if (valoreSistema(stato, indirizzo, nome) >= minimo) {
            return 0;
        }
        return scriviStatistica(gruppoDiSistemaOCrealo(stato, indirizzo), nome, minimo);
    }

    private int impostaAlmenoGlobale(Map<String, Object> stato, String nome, long minimo) {
        if (valoreGlobale(stato, nome) >= minimo) {
            return 0;
        }
        return scriviStatistica(gruppoGlobale(stato), nome, minimo);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> gruppoDiSistemaOCrealo(Map<String, Object> stato, long indirizzo) {
        Map<String, Object> g = gruppoDiSistema(stato, indirizzo);
        if (g != null) {
            return g;
        }
        Object stats = stato.get("Stats");
        if (!(stats instanceof List)) {
            return null;
        }
        Map<String, Object> nuovo = new LinkedHashMap<String, Object>();
        nuovo.put("GroupId", "^SYSTEM_STATS");
        nuovo.put("Address", new Json.Numero(String.valueOf(indirizzo)));
        nuovo.put("Stats", new ArrayList<Object>());
        ((List<Object>) stats).add(nuovo);
        return nuovo;
    }

    @SuppressWarnings("unchecked")
    private static int scriviStatistica(Map<String, Object> gruppo, String nome, long valore) {
        if (gruppo == null) {
            return 0;
        }
        Object voci = gruppo.get("Stats");
        if (!(voci instanceof List)) {
            return 0;
        }
        List<Object> lista = (List<Object>) voci;
        for (Object voce : lista) {
            if (!(voce instanceof Map)) {
                continue;
            }
            Map<String, Object> v = (Map<String, Object>) voce;
            if (!nome.equals(String.valueOf(v.get("Id")))) {
                continue;
            }
            Object attuale = v.get("Value");
            if (attuale instanceof Map) {
                ((Map<String, Object>) attuale).put("IntValue", new Json.Numero(String.valueOf(valore)));
            } else {
                Map<String, Object> nuovo = new LinkedHashMap<String, Object>();
                nuovo.put("IntValue", new Json.Numero(String.valueOf(valore)));
                v.put("Value", nuovo);
            }
            return 1;
        }
        Map<String, Object> nuova = new LinkedHashMap<String, Object>();
        nuova.put("Id", nome);
        Map<String, Object> valoreNuovo = new LinkedHashMap<String, Object>();
        valoreNuovo.put("IntValue", new Json.Numero(String.valueOf(valore)));
        nuova.put("Value", valoreNuovo);
        lista.add(nuova);
        return 1;
    }

    // --- requisiti completi -------------------------------------------

    private int soddisfaTuttiIRequisiti(Map<String, Object> stato, long indirizzo) {
        int toccati = 0;
        if (indirizzo != 0) {
            for (String s : RAZZE) {
                if ("^BUI_STANDING".equals(s) || "^PIR_STAND".equals(s)) {
                    continue;
                }
                toccati += impostaAlmenoSistema(stato, indirizzo, s, 30);
            }
            for (String s : GILDE) {
                toccati += impostaAlmenoSistema(stato, indirizzo, s, 15);
            }
            toccati += impostaAlmenoSistema(stato, indirizzo, "^SP_POI_MISSIONS", 5);
        }
        // Requisito 4: unita' al nucleo (almeno 1.000.000.000)
        long unitaAttuali = valoreGlobale(stato, "Units");
        if (unitaAttuali < 1000000000L) {
            toccati += impostaUnita(stato, 1000000000L);
        }
        // Chiave Station Override (almeno 1 nell'inventario)
        if (!haOggetto(stato, "^STATION_KEY")) {
            toccati += aggiungiChiavi(stato, 1);
        }
        return toccati;
    }

    private static int impostaUnita(Map<String, Object> stato, long nuovo) {
        if (nuovo > 4294967295L) {
            nuovo = 4294967295L;
        }
        long scritto = nuovo > 2147483647L ? nuovo - 4294967296L : nuovo;
        stato.put("Units", new Json.Numero(String.valueOf(scritto)));
        return 1;
    }

    // --- unita' -------------------------------------------------------

    private int aggiungiUnita(Map<String, Object> stato, long quanto) {
        long attuale = valoreGlobale(stato, "Units");
        long nuovo = attuale + quanto;
        if (nuovo > 4294967295L) {
            nuovo = 4294967295L;
        }
        long scritto = nuovo > 2147483647L ? nuovo - 4294967296L : nuovo;
        stato.put("Units", new Json.Numero(String.valueOf(scritto)));
        return 1;
    }

    // --- owner base ---------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> trovaOCreaOwner(Map<String, Object> stato) {
        Object basi = stato.get("PersistentPlayerBases");
        if (basi instanceof List) {
            for (Object b : (List<Object>) basi) {
                if (b instanceof Map) {
                    Object o = ((Map<String, Object>) b).get("Owner");
                    if (o instanceof Map) {
                        return new LinkedHashMap<String, Object>((Map<String, Object>) o);
                    }
                }
            }
        }
        Map<String, Object> owner = new LinkedHashMap<String, Object>();
        owner.put("LID", "0000000000000001");
        owner.put("UID", "1");
        owner.put("USN", "Viaggiatore");
        owner.put("PTK", "");
        owner.put("TS", new Json.Numero(String.valueOf(System.currentTimeMillis() / 1000L)));
        return owner;
    }

    // --- chiavi Station Override e slot inventario --------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> trovaIndiceSlotLibero(Map<String, Object> stato) {
        Object inv = stato.get("Inventory");
        if (!(inv instanceof Map)) {
            return null;
        }
        Map<String, Object> mappaInv = (Map<String, Object>) inv;

        Set<String> occupati = new HashSet<String>();
        Object slots = mappaInv.get("Slots");
        int maxOccupatoX = 0;
        int maxOccupatoY = 0;
        if (slots instanceof List) {
            for (Object s : (List<Object>) slots) {
                if (!(s instanceof Map)) {
                    continue;
                }
                Object idx = ((Map<String, Object>) s).get("Index");
                if (idx instanceof Map) {
                    int x = (int) numero(((Map<String, Object>) idx).get("X"));
                    int y = (int) numero(((Map<String, Object>) idx).get("Y"));
                    occupati.add(x + "," + y);
                    if (x > maxOccupatoX) {
                        maxOccupatoX = x;
                    }
                    if (y > maxOccupatoY) {
                        maxOccupatoY = y;
                    }
                }
            }
        }

        // 1. Cerca tra gli slot sbloccati (ValidSlotIndices)
        Object validi = mappaInv.get("ValidSlotIndices");
        if (validi instanceof List) {
            for (Object v : (List<Object>) validi) {
                if (!(v instanceof Map)) {
                    continue;
                }
                int x = (int) numero(((Map<String, Object>) v).get("X"));
                int y = (int) numero(((Map<String, Object>) v).get("Y"));
                if (!occupati.contains(x + "," + y)) {
                    Map<String, Object> indice = new LinkedHashMap<String, Object>();
                    indice.put("X", new Json.Numero(String.valueOf(x)));
                    indice.put("Y", new Json.Numero(String.valueOf(y)));
                    return indice;
                }
            }
        }

        // 2. Griglia standard (Width x Height)
        int larghezza = (int) numero(mappaInv.get("Width"));
        int altezza = (int) numero(mappaInv.get("Height"));
        if (larghezza <= 0) {
            larghezza = 10;
        }
        if (altezza <= 0) {
            altezza = 12;
        }

        for (int y = 0; y < altezza; y++) {
            for (int x = 0; x < larghezza; x++) {
                if (!occupati.contains(x + "," + y)) {
                    if (validi instanceof List) {
                        Map<String, Object> valido = new LinkedHashMap<String, Object>();
                        valido.put("X", new Json.Numero(String.valueOf(x)));
                        valido.put("Y", new Json.Numero(String.valueOf(y)));
                        ((List<Object>) validi).add(valido);
                    }
                    Map<String, Object> indice = new LinkedHashMap<String, Object>();
                    indice.put("X", new Json.Numero(String.valueOf(x)));
                    indice.put("Y", new Json.Numero(String.valueOf(y)));
                    return indice;
                }
            }
        }

        Map<String, Object> indice = new LinkedHashMap<String, Object>();
        indice.put("X", new Json.Numero(String.valueOf(maxOccupatoX)));
        indice.put("Y", new Json.Numero(String.valueOf(maxOccupatoY + 1)));
        return indice;
    }

    @SuppressWarnings("unchecked")
    private int aggiungiChiavi(Map<String, Object> stato, int quante) {
        int toccati = 0;
        if (sbloccaProdottoChiave(stato)) {
            toccati++;
        }
        Object inv = stato.get("Inventory");
        if (!(inv instanceof Map)) {
            return toccati;
        }
        Object slot = ((Map<String, Object>) inv).get("Slots");
        if (!(slot instanceof List)) {
            return toccati;
        }
        List<Object> slotLista = (List<Object>) slot;

        // 1. La chiave c'e' gia': si somma la quantita'.
        for (Object s : slotLista) {
            if (!(s instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) s;
            if (!"^STATION_KEY".equals(String.valueOf(m.get("Id")))) {
                continue;
            }
            long nuovo = numero(m.get("Amount")) + quante;
            m.put("Amount", new Json.Numero(String.valueOf(nuovo)));
            if (numero(m.get("MaxAmount")) < nuovo) {
                m.put("MaxAmount", new Json.Numero(String.valueOf(nuovo)));
            }
            return toccati + 1;
        }

        // 2. Non c'e': trova uno slot libero valido nella griglia
        Map<String, Object> indice = trovaIndiceSlotLibero(stato);
        if (indice == null) {
            return toccati;
        }

        Map<String, Object> tipo = new LinkedHashMap<String, Object>();
        tipo.put("InventoryType", "Product");
        Map<String, Object> nuovoSlot = new LinkedHashMap<String, Object>();
        nuovoSlot.put("Type", tipo);
        nuovoSlot.put("Id", "^STATION_KEY");
        nuovoSlot.put("Amount", new Json.Numero(String.valueOf(quante)));
        nuovoSlot.put("MaxAmount", new Json.Numero(String.valueOf(quante)));
        nuovoSlot.put("DamageFactor", new Json.Numero("0.0"));
        nuovoSlot.put("FullyInstalled", Boolean.TRUE);
        nuovoSlot.put("AddedAutomatically", Boolean.FALSE);
        nuovoSlot.put("Index", indice);
        slotLista.add(nuovoSlot);
        return toccati + 1;
    }

    @SuppressWarnings("unchecked")
    private boolean sbloccaProdottoChiave(Map<String, Object> stato) {
        Object kp = stato.get("KnownProducts");
        if (!(kp instanceof List)) {
            return false;
        }
        List<Object> prodotti = (List<Object>) kp;
        for (Object p : prodotti) {
            if ("^STATION_KEY".equals(String.valueOf(p))) {
                return false;
            }
        }
        prodotti.add("^STATION_KEY");
        return true;
    }
}

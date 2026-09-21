package it.atlante.finestra;

import javax.swing.UIManager;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * L'aspetto dell'applicazione, con la tavolozza cambiabile.
 *
 * I colori non sono scritti nel codice: stanno in un file di configurazione
 * ({@code risorse/aspetto.properties}) e si possono cambiare da
 * <b>Vista &rarr; Colori</b> senza ricompilare e senza riavviare. Il cambio e'
 * immediato perche' i valori vengono rimessi nella tavolozza dei componenti e
 * l'interfaccia viene ridipinta.
 *
 * Il tema di base e' FlatLaf, cercato nel percorso delle classi senza
 * dichiararlo obbligatorio: se il JAR non c'e', il programma usa il tema Nimbus
 * incluso nella JVM e funziona lo stesso, con un aspetto piu' spartano.
 */
public final class Aspetto {

    /** Una tavolozza: i colori che compongono l'aspetto. */
    public static final class Tavolozza {
        public final String nome;
        public Color fondo;
        public Color fondoAlto;
        public Color pannello;
        public Color pannelloAlto;
        public Color bordo;
        public Color bordoVivo;
        public Color accento;
        public Color accentoChiaro;
        public Color accentoScuro;
        public Color selezione;
        public Color testo;
        public Color testoTenue;
        public Color testoDebole;
        public Color ok;
        public Color attenzione;
        public Color errore;
        public Color info;

        public Tavolozza(String nome) {
            this.nome = nome;
        }

        Tavolozza copia() {
            Tavolozza t = new Tavolozza(nome);
            t.fondo = fondo; t.fondoAlto = fondoAlto; t.pannello = pannello;
            t.pannelloAlto = pannelloAlto; t.bordo = bordo; t.bordoVivo = bordoVivo;
            t.accento = accento; t.accentoChiaro = accentoChiaro; t.accentoScuro = accentoScuro;
            t.selezione = selezione; t.testo = testo; t.testoTenue = testoTenue;
            t.testoDebole = testoDebole; t.ok = ok; t.attenzione = attenzione;
            t.errore = errore; t.info = info;
            return t;
        }
    }

    // ------------------------------------------------------------------
    // Tavolozze predefinite
    // ------------------------------------------------------------------

    private static final Map<String, Tavolozza> PREDEFINITE = new LinkedHashMap<String, Tavolozza>();

    private static Tavolozza costruisci(String nome,
                                        String fondo, String fondoAlto, String pannello,
                                        String pannelloAlto, String bordo, String bordoVivo,
                                        String accento, String accentoChiaro, String accentoScuro,
                                        String selezione,
                                        String testo, String testoTenue, String testoDebole) {
        Tavolozza t = new Tavolozza(nome);
        t.fondo = colore(fondo);
        t.fondoAlto = colore(fondoAlto);
        t.pannello = colore(pannello);
        t.pannelloAlto = colore(pannelloAlto);
        t.bordo = colore(bordo);
        t.bordoVivo = colore(bordoVivo);
        t.accento = colore(accento);
        t.accentoChiaro = colore(accentoChiaro);
        t.accentoScuro = colore(accentoScuro);
        t.selezione = colore(selezione);
        t.testo = colore(testo);
        t.testoTenue = colore(testoTenue);
        t.testoDebole = colore(testoDebole);
        t.ok = new Color(0x3FB950);
        t.attenzione = new Color(0xD9A62E);
        t.errore = new Color(0xE5534B);
        t.info = new Color(0x4A9EDB);
        return t;
    }

    static {
        // Neutro — aspetto predefinito.
        //
        // Fondo grigio freddo quasi nero, bordi appena visibili, un solo colore
        // d'accento. E' la tavolozza dei programmi professionali: niente tinte
        // di sfondo, il colore serve a indicare cosa e' attivo, non a decorare.
        // Il rosso del marchio compare solo nel logo.
        PREDEFINITE.put("Neutro", costruisci("Neutro",
                "#0A0B0D", "#101114", "#141518", "#1A1C21", "#24262C", "#34373F",
                "#6E7BE8", "#A5AEF5", "#3A4199", "#232A52",
                "#E9EAEC", "#8C9099", "#5E626B"));

        // Cremisi — il colore del marchio, per chi lo vuole.
        PREDEFINITE.put("Cremisi", costruisci("Cremisi",
                "#0C0A0B", "#121013", "#171417", "#1E1A1E", "#2B2529", "#3E353A",
                "#E5484D", "#FF8A8A", "#8C2226", "#3A1B1E",
                "#EDE6E7", "#A29194", "#6E5F63"));

        // Blu — freddo, contrasto alto.
        PREDEFINITE.put("Blu", costruisci("Blu",
                "#080B11", "#0E131C", "#131A25", "#1A2333", "#26303F", "#37455A",
                "#3B82F6", "#8FBAFF", "#1E3A6E", "#182A4A",
                "#E4EBF5", "#8FA0B8", "#5C6B82"));

        // Verde — riposante, buono per sessioni lunghe.
        PREDEFINITE.put("Verde", costruisci("Verde",
                "#080C0A", "#0D1310", "#121A15", "#18231C", "#243228", "#35473A",
                "#2FB866", "#7BE3A6", "#155232", "#163322",
                "#E2EFE7", "#8CA898", "#5A7064"));

        // Ambra — caldo, molto contrastato.
        PREDEFINITE.put("Ambra", costruisci("Ambra",
                "#0E0B07", "#14110B", "#1A1610", "#221D15", "#312A1F", "#463C2B",
                "#E39A20", "#F5C46B", "#6E4310", "#392B14",
                "#F1E9DC", "#AEA089", "#75684F"));

        // Viola — alternativa decisa.
        PREDEFINITE.put("Viola", costruisci("Viola",
                "#0A0910", "#100E18", "#15131F", "#1C1A29", "#28243A", "#393353",
                "#8B5CF6", "#BFA6FF", "#4C1D95", "#2A1F45",
                "#EBE7F5", "#A79CBB", "#6B6280"));

        // Chiaro — tema diurno. Serve a chi lavora in stanze illuminate:
        // un fondo scuro con luce forte costringe ad alzare la luminosita'
        // dello schermo, e il contrasto ne soffre.
        PREDEFINITE.put("Chiaro", costruisci("Chiaro",
                "#EFF1F4", "#F7F8FA", "#FFFFFF", "#F2F4F7", "#D8DCE3", "#BFC5CF",
                "#4F5BD5", "#3A44A8", "#E0E2F7", "#DDE0F5",
                "#1A1D23", "#5A6069", "#878D96"));
    }

    /** Nomi delle tavolozze disponibili, nell'ordine in cui mostrarle. */
    public static List<String> nomiTavolozze() {
        return new ArrayList<String>(PREDEFINITE.keySet());
    }

    // ------------------------------------------------------------------
    // Stato
    // ------------------------------------------------------------------

    private static Tavolozza corrente = PREDEFINITE.get("Neutro");
    private static boolean flatlaf;
    private static File fileConfigurazione;

    // Scorciatoie: il resto del codice continua a leggere Aspetto.ACCENTO ecc.
    public static Color FONDO;
    public static Color FONDO_ALTO;
    public static Color PANNELLO;
    public static Color PANNELLO_ALTO;
    public static Color BORDO;
    public static Color BORDO_VIVO;
    public static Color ACCENTO;
    public static Color ACCENTO_CHIARO;
    public static Color ACCENTO_SCURO;
    public static Color SELEZIONE;
    public static Color TESTO;
    public static Color TESTO_TENUE;
    public static Color TESTO_DEBOLE;
    public static Color OK;
    public static Color ATTENZIONE;
    public static Color ERRORE;
    public static Color INFO;

    public static String nomeTavolozza() {
        return corrente.nome;
    }

    public static boolean conFlatlaf() {
        return flatlaf;
    }

    // ------------------------------------------------------------------
    // Installazione
    // ------------------------------------------------------------------

    /** Applica il tema. Va chiamata prima di creare qualsiasi componente. */
    public static void installa() {
        caricaConfigurazione();
        sincronizzaScorciatoie();
        flatlaf = provaFlatlaf();
        if (!flatlaf) {
            provaNimbus();
        }
        ritocchiComuni();
    }

    /**
     * Cambia tavolozza a programma avviato.
     *
     * I valori vengono rimessi nella tavolozza dei componenti e l'interfaccia
     * viene ridipinta: non serve riavviare.
     */
    public static void cambiaTavolozza(String nome) {
        Tavolozza nuova = PREDEFINITE.get(nome);
        if (nuova == null) {
            return;
        }
        corrente = nuova.copia();
        sincronizzaScorciatoie();
        ritocchiComuni();
        if (flatlaf) {
            try {
                Class<?> flatlafClasse = Class.forName("com.formdev.flatlaf.FlatLaf");
                flatlafClasse.getMethod("updateUI").invoke(null);
            } catch (Throwable e) {
                aggiornaTutteLeFinestre();
            }
        } else {
            aggiornaTutteLeFinestre();
        }
        salvaConfigurazione();
    }

    private static void aggiornaTutteLeFinestre() {
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            javax.swing.SwingUtilities.updateComponentTreeUI(w);
            w.repaint();
        }
    }

    private static void sincronizzaScorciatoie() {
        FONDO = corrente.fondo;
        FONDO_ALTO = corrente.fondoAlto;
        PANNELLO = corrente.pannello;
        PANNELLO_ALTO = corrente.pannelloAlto;
        BORDO = corrente.bordo;
        BORDO_VIVO = corrente.bordoVivo;
        ACCENTO = corrente.accento;
        ACCENTO_CHIARO = corrente.accentoChiaro;
        ACCENTO_SCURO = corrente.accentoScuro;
        SELEZIONE = corrente.selezione;
        TESTO = corrente.testo;
        TESTO_TENUE = corrente.testoTenue;
        TESTO_DEBOLE = corrente.testoDebole;
        OK = corrente.ok;
        ATTENZIONE = corrente.attenzione;
        ERRORE = corrente.errore;
        INFO = corrente.info;
    }

    // ------------------------------------------------------------------
    // Configurazione su file
    // ------------------------------------------------------------------

    private static File fileConfigurazione() {
        if (fileConfigurazione != null) {
            return fileConfigurazione;
        }
        File f = new File("risorse" + File.separator + "aspetto.properties");
        fileConfigurazione = f;
        return f;
    }

    private static void caricaConfigurazione() {
        File f = fileConfigurazione();
        if (!f.isFile()) {
            return;
        }
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            p.load(in);
        } catch (IOException e) {
            return;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // niente
                }
            }
        }
        String nome = p.getProperty("tavolozza");
        if (nome != null && PREDEFINITE.containsKey(nome)) {
            corrente = PREDEFINITE.get(nome).copia();
        }
        // Colori singoli: chi vuole cambiarne uno solo non deve rifare tutto.
        corrente.fondo = leggiColore(p, "fondo", corrente.fondo);
        corrente.fondoAlto = leggiColore(p, "fondoAlto", corrente.fondoAlto);
        corrente.pannello = leggiColore(p, "pannello", corrente.pannello);
        corrente.pannelloAlto = leggiColore(p, "pannelloAlto", corrente.pannelloAlto);
        corrente.bordo = leggiColore(p, "bordo", corrente.bordo);
        corrente.bordoVivo = leggiColore(p, "bordoVivo", corrente.bordoVivo);
        corrente.accento = leggiColore(p, "accento", corrente.accento);
        corrente.accentoChiaro = leggiColore(p, "accentoChiaro", corrente.accentoChiaro);
        corrente.accentoScuro = leggiColore(p, "accentoScuro", corrente.accentoScuro);
        corrente.selezione = leggiColore(p, "selezione", corrente.selezione);
    }

    private static Color leggiColore(Properties p, String chiave, Color ripiego) {
        String v = p.getProperty(chiave);
        if (v == null || v.trim().isEmpty()) {
            return ripiego;
        }
        try {
            return colore(v.trim());
        } catch (RuntimeException e) {
            System.err.println("colore non valido per " + chiave + ": " + v);
            return ripiego;
        }
    }

    private static void salvaConfigurazione() {
        File f = fileConfigurazione();
        if (f.getParentFile() != null && !f.getParentFile().isDirectory()) {
            f.getParentFile().mkdirs();
        }
        Properties p = new Properties();
        p.setProperty("tavolozza", corrente.nome);
        p.setProperty("fondo", esadecimale(corrente.fondo));
        p.setProperty("fondoAlto", esadecimale(corrente.fondoAlto));
        p.setProperty("pannello", esadecimale(corrente.pannello));
        p.setProperty("pannelloAlto", esadecimale(corrente.pannelloAlto));
        p.setProperty("bordo", esadecimale(corrente.bordo));
        p.setProperty("bordoVivo", esadecimale(corrente.bordoVivo));
        p.setProperty("accento", esadecimale(corrente.accento));
        p.setProperty("accentoChiaro", esadecimale(corrente.accentoChiaro));
        p.setProperty("accentoScuro", esadecimale(corrente.accentoScuro));
        p.setProperty("selezione", esadecimale(corrente.selezione));
        OutputStream out = null;
        try {
            out = new FileOutputStream(f);
            p.store(out, "Tavolozza di Atlante. Modifica questi colori o usa Vista > Colori.");
        } catch (IOException e) {
            System.err.println("non riesco a salvare la tavolozza: " + e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // niente
                }
            }
        }
    }

    static String esadecimale(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    static Color colore(String esadecimale) {
        String v = esadecimale.trim();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }
        return new Color(Integer.parseInt(v, 16));
    }

    // ------------------------------------------------------------------
    // Tema
    // ------------------------------------------------------------------

    private static boolean provaFlatlaf() {
        try {
            Class<?> impostazioni = Class.forName("com.formdev.flatlaf.FlatLaf");
            Class<?> scuro = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            Map<String, String> extra = new HashMap<String, String>();
            extra.put("@accentColor", esadecimale(corrente.accento));
            extra.put("@accentBaseColor", esadecimale(corrente.accento));
            extra.put("@background", esadecimale(corrente.fondo));
            extra.put("@foreground", esadecimale(corrente.testo));
            extra.put("@componentBackground", esadecimale(corrente.pannello));
            extra.put("@componentBorder", esadecimale(corrente.bordo));
            extra.put("@componentFocusedBorder", esadecimale(corrente.accento));
            extra.put("@selectionBackground", esadecimale(corrente.selezione));
            extra.put("@selectionForeground", "#FFE9E9");
            extra.put("@selectionInactiveBackground", esadecimale(corrente.pannelloAlto));
            extra.put("@disabledBackground", esadecimale(corrente.fondoAlto));
            extra.put("@disabledText", esadecimale(corrente.testoDebole));
            extra.put("@textBorderColor", esadecimale(corrente.bordo));
            try {
                impostazioni.getMethod("setGlobalExtraDefaults", Map.class).invoke(null, extra);
            } catch (NoSuchMethodException e) {
                // versione piu' vecchia di FlatLaf: si prosegue senza
            }
            Object istanza = scuro.getDeclaredConstructor().newInstance();
            scuro.getMethod("setup").invoke(null);
            UIManager.setLookAndFeel((javax.swing.LookAndFeel) istanza);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static void provaNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    UIManager.put("nimbusBase", PANNELLO);
                    UIManager.put("control", FONDO);
                    UIManager.put("info", PANNELLO);
                    UIManager.put("nimbusLightBackground", FONDO_ALTO);
                    UIManager.put("text", TESTO);
                    UIManager.put("nimbusSelectionBackground", ACCENTO_SCURO);
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // si resta sul tema predefinito della JVM
        }
    }

    /** Ritocchi comuni a FlatLaf e agli altri temi. */
    private static void ritocchiComuni() {
        metti("Component.arc", 14);
        metti("Button.arc", 14);
        metti("TextComponent.arc", 10);
        metti("ProgressBar.arc", 10);
        metti("CheckBox.arc", 6);
        metti("Component.focusWidth", 1);
        metti("Component.innerFocusWidth", 1);
        metti("Component.focusColor", ACCENTO);
        metti("Component.accentColor", ACCENTO);
        metti("Component.borderColor", BORDO);
        metti("Component.disabledBorderColor", BORDO);
        metti("ScrollBar.thumbArc", 10);
        metti("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        metti("ScrollBar.width", 12);
        metti("ScrollBar.showButtons", Boolean.FALSE);
        metti("ScrollBar.track", FONDO);
        metti("ScrollBar.thumb", BORDO_VIVO);
        metti("ScrollBar.hoverThumbColor", ACCENTO_SCURO);
        metti("Tree.rowHeight", 28);
        metti("Tree.showDefaultIcons", Boolean.FALSE);
        metti("Tree.paintLines", Boolean.TRUE);
        metti("Tree.lineColor", BORDO);
        metti("Tree.selectionBackground", SELEZIONE);
        metti("Tree.selectionForeground", new Color(0xFFE9E9));
        metti("Tree.selectionInactiveBackground", PANNELLO_ALTO);
        metti("Tree.background", PANNELLO);
        metti("Tree.foreground", TESTO);
        metti("Tree.border", javax.swing.BorderFactory.createEmptyBorder());
        metti("Table.showHorizontalLines", Boolean.TRUE);
        metti("Table.showVerticalLines", Boolean.FALSE);
        metti("Table.gridColor", BORDO);
        metti("Table.rowHeight", 26);
        metti("Table.selectionBackground", SELEZIONE);
        metti("Table.selectionForeground", new Color(0xFFE9E9));
        metti("Table.background", PANNELLO);
        metti("Table.foreground", TESTO);
        metti("TabbedPane.tabHeight", 34);
        metti("TabbedPane.showTabSeparators", Boolean.TRUE);
        metti("TabbedPane.tabSeparatorColor", BORDO);
        metti("TabbedPane.underlineColor", ACCENTO);
        metti("TabbedPane.inactiveUnderlineColor", BORDO_VIVO);
        metti("TabbedPane.selectedBackground", PANNELLO_ALTO);
        metti("TabbedPane.background", FONDO_ALTO);
        metti("TabbedPane.foreground", TESTO);
        metti("TabbedPane.hoverColor", PANNELLO_ALTO);
        metti("SplitPane.background", FONDO);
        metti("SplitPane.dividerSize", 8);
        metti("SplitPane.border", javax.swing.BorderFactory.createEmptyBorder());
        metti("ToolTip.background", PANNELLO_ALTO);
        metti("ToolTip.foreground", TESTO);
        metti("ToolTip.border", javax.swing.BorderFactory.createLineBorder(BORDO_VIVO));
        metti("MenuItem.selectionBackground", SELEZIONE);
        metti("MenuItem.background", PANNELLO_ALTO);
        metti("MenuItem.foreground", TESTO);
        metti("MenuBar.background", FONDO_ALTO);
        metti("MenuBar.foreground", TESTO);
        metti("MenuBar.borderColor", BORDO);
        metti("MenuBar.itemMargins", new java.awt.Insets(6, 12, 6, 12));
        metti("PopupMenu.background", PANNELLO_ALTO);
        metti("PopupMenu.borderColor", BORDO_VIVO);
        metti("Panel.background", PANNELLO);
        metti("TextField.background", FONDO_ALTO);
        metti("TextField.foreground", TESTO);
        metti("TextField.placeholderForeground", TESTO_DEBOLE);
        metti("TextField.borderColor", BORDO);
        metti("ComboBox.background", FONDO_ALTO);
        metti("ComboBox.foreground", TESTO);
        metti("ComboBox.borderColor", BORDO);
        metti("Button.background", PANNELLO_ALTO);
        metti("Button.foreground", TESTO);
        metti("Button.borderColor", BORDO);
        metti("Button.hoverBackground", PANNELLO);
        metti("Button.default.background", ACCENTO);
        metti("Button.default.foreground", new Color(0xFFF2F2));
        metti("Button.default.hoverBackground", ACCENTO_CHIARO);
        metti("ToggleButton.background", PANNELLO_ALTO);
        metti("ToggleButton.foreground", TESTO);
        metti("ToggleButton.selectedBackground", ACCENTO);
        metti("ToggleButton.selectedForeground", new Color(0xFFF2F2));
        metti("ToggleButton.buttonType", "roundRect");
        metti("Separator.foreground", BORDO);
        metti("Label.foreground", TESTO);
        metti("Label.disabledForeground", TESTO_DEBOLE);
        metti("OptionPane.background", PANNELLO);
        metti("OptionPane.messageForeground", TESTO);
        metti("List.background", PANNELLO);
        metti("List.foreground", TESTO);
        metti("List.selectionBackground", SELEZIONE);
        metti("List.selectionForeground", new Color(0xFFE9E9));
        metti("List.selectionInactiveBackground", PANNELLO_ALTO);
        metti("ScrollPane.background", FONDO);
        metti("Viewport.background", FONDO);
        metti("CheckBox.background", PANNELLO);
        metti("CheckBox.foreground", TESTO);
        metti("RadioButton.background", PANNELLO);
        metti("RadioButton.foreground", TESTO);
        metti("TitledBorder.titleColor", TESTO_TENUE);
        metti("TitledBorder.border", javax.swing.BorderFactory.createLineBorder(BORDO));

        java.awt.Font base = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13);
        if (!"Segoe UI".equals(base.getFamily())) {
            base = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 13);
        }
        String[] chiavi = {"Label", "Button", "TextField", "ComboBox", "Tree", "Table",
                "List", "Menu", "MenuItem", "TabbedPane", "TitledBorder", "ToolTip", "RadioButton",
                "CheckBox", "ToggleButton", "OptionPane", "CheckBoxMenuItem", "RadioButtonMenuItem"};
        for (String c : chiavi) {
            UIManager.put(c + ".font", base);
        }
    }

    private static void metti(String chiave, Object valore) {
        UIManager.put(chiave, valore);
    }

    /** Carattere monospaziato, per identificatori e misure. */
    public static java.awt.Font monospaziato(float dimensione, int stile) {
        int punti = Math.round(dimensione);
        java.awt.Font f = new java.awt.Font("Consolas", stile, punti);
        if (!"Consolas".equals(f.getFamily())) {
            f = new java.awt.Font(java.awt.Font.MONOSPACED, stile, punti);
        }
        return f;
    }

    /** Colore con trasparenza, per i fondi delle pillole. */
    public static Color velato(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa);
    }

    private Aspetto() {
    }
}

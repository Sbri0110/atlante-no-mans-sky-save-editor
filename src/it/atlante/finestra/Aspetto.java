package it.atlante.finestra;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/**
 * L'aspetto dell'applicazione.
 *
 * Il tema e' costruito su FlatLaf, che viene cercato nel percorso delle classi
 * senza dichiararlo come dipendenza obbligatoria: se il JAR non c'e', il
 * programma usa il tema Nimbus incluso nella JVM e funziona lo stesso. Cosi'
 * l'interfaccia resta curata senza rendere il progetto impossibile da compilare
 * per chi non ha la libreria.
 *
 * La tavolozza riprende il marchio: rosso cremisi su fondo quasi nero.
 */
public final class Aspetto {

    // Tavolozza
    public static final Color FONDO = new Color(0x0D0B0D);
    public static final Color FONDO_ALTO = new Color(0x14100F);
    public static final Color PANNELLO = new Color(0x1A1416);
    public static final Color PANNELLO_ALTO = new Color(0x221A1C);
    public static final Color BORDO = new Color(0x33262A);
    public static final Color BORDO_VIVO = new Color(0x4A3238);

    public static final Color ACCENTO = new Color(0xE5484D);
    public static final Color ACCENTO_CHIARO = new Color(0xFF7A7A);
    public static final Color ACCENTO_SCURO = new Color(0x8C2226);

    public static final Color TESTO = new Color(0xEDE6E7);
    public static final Color TESTO_TENUE = new Color(0xA29194);
    public static final Color TESTO_DEBOLE = new Color(0x6E5F63);

    public static final Color OK = new Color(0x4ADE80);
    public static final Color ATTENZIONE = new Color(0xFEBC2E);
    public static final Color ERRORE = new Color(0xFF5F57);
    public static final Color INFO = new Color(0x38BDF8);

    private static boolean flatlaf;

    private Aspetto() {
    }

    /** Vero se il tema e' FlatLaf, falso se si e' ripiegati su Nimbus. */
    public static boolean conFlatlaf() {
        return flatlaf;
    }

    /** Applica il tema. Va chiamata prima di creare qualsiasi componente. */
    public static void installa() {
        flatlaf = provaFlatlaf();
        if (!flatlaf) {
            provaNimbus();
        }
        ritocchiComuni();
    }

    private static boolean provaFlatlaf() {
        try {
            Class<?> impostazioni = Class.forName("com.formdev.flatlaf.FlatLaf");
            Class<?> scuro = Class.forName("com.formdev.flatlaf.FlatDarkLaf");

            // Il colore d'accento e il raggio degli angoli si passano come
            // valori predefiniti globali, prima di installare il tema.
            Map<String, String> extra = new HashMap<String, String>();
            extra.put("@accentColor", "#E5484D");
            extra.put("@accentBaseColor", "#E5484D");
            extra.put("@background", "#0D0B0D");
            extra.put("@foreground", "#EDE6E7");
            extra.put("@componentBackground", "#1A1416");
            extra.put("@componentBorder", "#33262A");
            extra.put("@componentFocusedBorder", "#E5484D");
            extra.put("@selectionBackground", "#3A1B1E");
            extra.put("@selectionForeground", "#FFE9E9");
            extra.put("@selectionInactiveBackground", "#2A181A");
            extra.put("@disabledBackground", "#141011");
            extra.put("@disabledText", "#6E5F63");
            extra.put("@textBorderColor", "#33262A");
            try {
                java.lang.reflect.Method m = impostazioni.getMethod("setGlobalExtraDefaults", Map.class);
                m.invoke(null, extra);
            } catch (NoSuchMethodException e) {
                // Versione piu' vecchia di FlatLaf: si prosegue senza.
            }

            Object istanza = scuro.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method setup = scuro.getMethod("setup");
            setup.invoke(null);
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

    /** Ritocchi che valgono per qualunque tema, e proprieta' tipiche di FlatLaf. */
    private static void ritocchiComuni() {
        // Proprieta' di FlatLaf: ignorate in silenzio dagli altri temi.
        metti("Component.arc", 14);
        metti("Button.arc", 14);
        metti("TextComponent.arc", 10);
        metti("ProgressBar.arc", 10);
        metti("CheckBox.arc", 6);
        metti("Component.focusWidth", 1);
        metti("Component.innerFocusWidth", 1);
        metti("Component.focusColor", ACCENTO);
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
        metti("Tree.selectionBackground", ACCENTO_SCURO);
        metti("Tree.selectionForeground", new Color(0xFFE9E9));
        metti("Tree.selectionInactiveBackground", new Color(0x2A181A));
        metti("Tree.background", PANNELLO);
        metti("Tree.border", javax.swing.BorderFactory.createEmptyBorder());
        metti("Table.showHorizontalLines", Boolean.TRUE);
        metti("Table.showVerticalLines", Boolean.FALSE);
        metti("Table.gridColor", BORDO);
        metti("Table.rowHeight", 26);
        metti("Table.selectionBackground", ACCENTO_SCURO);
        metti("Table.selectionForeground", new Color(0xFFE9E9));
        metti("TabbedPane.tabHeight", 34);
        metti("TabbedPane.showTabSeparators", Boolean.TRUE);
        metti("TabbedPane.tabSeparatorColor", BORDO);
        metti("TabbedPane.underlineColor", ACCENTO);
        metti("TabbedPane.inactiveUnderlineColor", BORDO_VIVO);
        metti("TabbedPane.selectedBackground", PANNELLO_ALTO);
        metti("TabbedPane.hoverColor", new Color(0x261A1C));
        metti("SplitPane.background", FONDO);
        metti("SplitPane.dividerSize", 8);
        metti("SplitPane.border", javax.swing.BorderFactory.createEmptyBorder());
        metti("ToolTip.background", PANNELLO_ALTO);
        metti("ToolTip.foreground", TESTO);
        metti("ToolTip.border", javax.swing.BorderFactory.createLineBorder(BORDO_VIVO));
        metti("MenuItem.selectionBackground", ACCENTO_SCURO);
        metti("MenuBar.background", FONDO_ALTO);
        metti("MenuBar.borderColor", BORDO);
        metti("MenuBar.itemMargins", new java.awt.Insets(6, 12, 6, 12));
        metti("PopupMenu.background", PANNELLO_ALTO);
        metti("PopupMenu.borderColor", BORDO_VIVO);
        metti("Panel.background", PANNELLO);
        metti("TextField.background", FONDO_ALTO);
        metti("TextField.foreground", TESTO);
        metti("TextField.placeholderForeground", TESTO_DEBOLE);
        metti("ComboBox.background", FONDO_ALTO);
        metti("ComboBox.foreground", TESTO);
        metti("Button.background", PANNELLO_ALTO);
        metti("Button.foreground", TESTO);
        metti("Button.hoverBackground", new Color(0x2E2124));
        metti("Button.default.background", ACCENTO);
        metti("Button.default.foreground", new Color(0xFFF2F2));
        metti("Button.default.hoverBackground", ACCENTO_CHIARO);
        metti("ToggleButton.background", PANNELLO_ALTO);
        metti("ToggleButton.selectedBackground", ACCENTO_SCURO);
        metti("ToggleButton.selectedForeground", new Color(0xFFE9E9));
        metti("ToggleButton.buttonType", "roundRect");
        metti("Separator.foreground", BORDO);
        metti("Label.foreground", TESTO);
        metti("Label.disabledForeground", TESTO_DEBOLE);
        metti("OptionPane.background", PANNELLO);
        metti("OptionPane.messageForeground", TESTO);
        metti("List.background", PANNELLO);
        metti("List.selectionBackground", ACCENTO_SCURO);
        metti("List.selectionForeground", new Color(0xFFE9E9));
        metti("List.selectionInactiveBackground", new Color(0x2A181A));
        metti("ScrollPane.background", FONDO);
        metti("Viewport.background", FONDO);

        // Carattere: il sistema, con cifre monospaziate per i valori numerici.
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        if (!"Segoe UI".equals(base.getFamily())) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        }
        String[] chiavi = {"Label", "Button", "TextField", "ComboBox", "Tree", "Table",
                "List", "Menu", "MenuItem", "TabbedPane", "TitledBorder", "ToolTip", "RadioButton",
                "CheckBox", "ToggleButton", "OptionPane"};
        for (String c : chiavi) {
            UIManager.put(c + ".font", base);
        }
    }

    private static void metti(String chiave, Object valore) {
        UIManager.put(chiave, valore);
    }

    /** Carattere monospaziato, per identificatori e misure. */
    public static Font monospaziato(float dimensione, int stile) {
        int punti = Math.round(dimensione);
        Font f = new Font("Consolas", stile, punti);
        if (!"Consolas".equals(f.getFamily())) {
            f = new Font(Font.MONOSPACED, stile, punti);
        }
        return f;
    }

    /** Colore di sfondo con leggera variazione, per le fasce alternate. */
    public static Color alternato(int indice) {
        return indice % 2 == 0 ? PANNELLO : PANNELLO_ALTO;
    }
}

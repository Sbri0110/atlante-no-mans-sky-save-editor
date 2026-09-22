package it.atlante.finestra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le razze dei piloti e i tipi di astronave.
 *
 * Sono le due cose che nel salvataggio non si leggono a parole: il pilota dello
 * squadrone non dice "sono un Korvax", dice che il suo modello e' una certa
 * risorsa, e la sua nave dice che il modello sta in una certa cartella. Le
 * tabelle qui sotto sono la corrispondenza, presa dal vecchio editor
 * ({@code nomanssave.gy} per le razze, {@code nomanssave.gL} per le navi): sono
 * le stesse risorse, con i nomi che il gioco mostra.
 */
public final class Piloti {

    /** Una razza di pilota. */
    public static final class Razza {
        public final String nome;
        public final String risorsa;
        public final String icona;

        Razza(String nome, String risorsa, String icona) {
            this.nome = nome;
            this.risorsa = risorsa;
            this.icona = icona;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    /** Un tipo di astronave. */
    public static final class Nave {
        public final String nome;
        public final String risorsa;
        /**
         * L'icona della nave. Nel gioco le navi non hanno un'illustrazione per
         * tipo — si vedono in 3D — quindi resta l'icona dell'interfaccia: il
         * tipo lo dice il nome scritto accanto.
         */
        public final String icona;

        Nave(String nome, String risorsa) {
            this(nome, risorsa, "UI-SHIPICON.PNG");
        }

        Nave(String nome, String risorsa, String icona) {
            this.nome = nome;
            this.risorsa = risorsa;
            this.icona = icona;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    private static final String NPC = "MODELS/COMMON/PLAYER/PLAYERCHARACTER/";

    /** Le razze che il gioco usa per i piloti dello squadrone. */
    public static final List<Razza> RAZZE;

    /** I tipi di astronave, nell'ordine in cui li elenca il vecchio editor. */
    public static final List<Nave> NAVI;

    static {
        List<Razza> razze = new ArrayList<Razza>();
        razze.add(new Razza("Korvax", NPC + "NPCKORVAX.SCENE.MBIN", "UI-KORVAX.PNG"));
        razze.add(new Razza("Gek", NPC + "NPCGEK.SCENE.MBIN", "UI-GEK.PNG"));
        razze.add(new Razza("Vy'keen", NPC + "NPCVYKEEN.SCENE.MBIN", "UI-VYKEEN.PNG"));
        razze.add(new Razza("Fourth Race", NPC + "NPCFOURTH.SCENE.MBIN", "UI-SHIPICON.PNG"));
        RAZZE = Collections.unmodifiableList(razze);

        String sc = "MODELS/COMMON/SPACECRAFT/";
        List<Nave> navi = new ArrayList<Nave>();
        // Le icone sono quelle del gioco, estratte da icons/shiptypes: il gioco
        // le usa nel menu del tipo di nave. Per le navi-premio c'e' in piu'
        // l'immagine della nave esatta, che e' un oggetto del catalogo (Golden
        // Vector e' ^EXPD_SHIP01, Utopia Speeder ^EXPD_SHIP09 e cosi' via).
        navi.add(new Nave("Fighter", sc + "FIGHTERS/FIGHTER_PROC.SCENE.MBIN",
                "UI-SHIPTYPE-FIGHTER.PNG"));
        navi.add(new Nave("Shuttle", sc + "SHUTTLE/SHUTTLE_PROC.SCENE.MBIN",
                "UI-SHIPTYPE-SHUTTLE.PNG"));
        navi.add(new Nave("Hauler", sc + "DROPSHIPS/DROPSHIP_PROC.SCENE.MBIN",
                "UI-SHIPTYPE-HAULER.PNG"));
        navi.add(new Nave("Explorer", sc + "SCIENTIFIC/SCIENTIFIC_PROC.SCENE.MBIN",
                "UI-SHIPTYPE-EXPLORER.PNG"));
        navi.add(new Nave("Exotic", sc + "S-CLASS/S-CLASS_PROC.SCENE.MBIN",
                "UI-SHIPTYPE-EXOTIC.PNG"));
        navi.add(new Nave("Living", sc + "S-CLASS/BIOPARTS/BIOSHIP_PROC.SCENE.MBIN",
                "UI-SHIPTYPE-LIVING.PNG"));
        navi.add(new Nave("Solar", sc + "SAILSHIP/SAILSHIP_PROC.SCENE.MBIN",
                "UI-SHIPTYPE-SOLAR.PNG"));
        navi.add(new Nave("Robot", sc + "SENTINELSHIP/SENTINELSHIP_PROC.SCENE.MBIN",
                "UI-SHIPTYPE-ROBOT.PNG"));
        navi.add(new Nave("Corvette", sc + "BIGGS/BIGGS.SCENE.MBIN",
                "UI-SHIPTYPE-CORVETTE.PNG"));
        navi.add(new Nave("Utopia Speeder", sc + "FIGHTERS/VRSPEEDER.SCENE.MBIN",
                "PRODUCT-EXPD_SHIP09.PNG"));
        navi.add(new Nave("Golden Vector", sc + "FIGHTERS/FIGHTERCLASSICGOLD.SCENE.MBIN",
                "PRODUCT-EXPD_SHIP01.PNG"));
        navi.add(new Nave("Horizon Vector NX", sc + "FIGHTERS/FIGHTERSPECIALSWITCH.SCENE.MBIN",
                "PRODUCT-SWITCH_SHIP01.PNG"));
        navi.add(new Nave("Starborn Runner", sc + "FIGHTERS/WRACER.SCENE.MBIN",
                "PRODUCT-EXPD_SHIP12.PNG"));
        NAVI = Collections.unmodifiableList(navi);
    }

    /** La razza che corrisponde a una risorsa, o null. */
    public static Razza razzaDi(String risorsa) {
        if (risorsa == null || risorsa.isEmpty()) {
            return null;
        }
        for (Razza r : RAZZE) {
            if (r.risorsa.equalsIgnoreCase(risorsa)) {
                return r;
            }
        }
        // Il modello puo' arrivare senza percorso completo: si guarda il nome.
        String solo = risorsa.toUpperCase();
        if (solo.indexOf("NPCKORVAX") >= 0) {
            return RAZZE.get(0);
        }
        if (solo.indexOf("NPCGEK") >= 0) {
            return RAZZE.get(1);
        }
        if (solo.indexOf("NPCVYKEEN") >= 0) {
            return RAZZE.get(2);
        }
        if (solo.indexOf("NPCFOURTH") >= 0) {
            return RAZZE.get(3);
        }
        return null;
    }

    /** Il tipo di astronave che corrisponde a una risorsa, o null. */
    public static Nave naveDi(String risorsa) {
        if (risorsa == null || risorsa.isEmpty()) {
            return null;
        }
        for (Nave n : NAVI) {
            if (n.risorsa.equalsIgnoreCase(risorsa)) {
                return n;
            }
        }
        // Il salvataggio puo' avere il modello senza "_PROC" o con un altro
        // suffisso: si confronta la parte che conta, la cartella.
        String r = risorsa.toUpperCase();
        if (r.indexOf("FIGHTERCLASSICGOLD") >= 0) {
            return cerca("Golden Vector");
        }
        if (r.indexOf("FIGHTERSPECIALSWITCH") >= 0) {
            return cerca("Horizon Vector NX");
        }
        if (r.indexOf("WRACER") >= 0) {
            return cerca("Starborn Runner");
        }
        if (r.indexOf("VRSPEEDER") >= 0) {
            return cerca("Utopia Speeder");
        }
        if (r.indexOf("/FIGHTER") >= 0) {
            return cerca("Fighter");
        }
        if (r.indexOf("SHUTTLE") >= 0) {
            return cerca("Shuttle");
        }
        if (r.indexOf("DROPSHIP") >= 0) {
            return cerca("Hauler");
        }
        if (r.indexOf("SCIENTIFIC") >= 0) {
            return cerca("Explorer");
        }
        if (r.indexOf("BIOSHIP") >= 0) {
            return cerca("Living");
        }
        if (r.indexOf("SAILSHIP") >= 0) {
            return cerca("Solar");
        }
        if (r.indexOf("SENTINELSHIP") >= 0) {
            return cerca("Robot");
        }
        if (r.indexOf("BIGGS") >= 0) {
            return cerca("Corvette");
        }
        if (r.indexOf("S-CLASS") >= 0) {
            return cerca("Exotic");
        }
        return null;
    }

    private static Nave cerca(String nome) {
        for (Nave n : NAVI) {
            if (n.nome.equals(nome)) {
                return n;
            }
        }
        return null;
    }

    /**
     * Un seme nuovo, nel formato che il salvataggio usa: {@code 0x} e sedici
     * cifre esadecimali maiuscole.
     */
    public static String semeNuovo() {
        StringBuilder s = new StringBuilder("0x");
        java.util.Random r = new java.util.Random();
        String cifre = "0123456789ABCDEF";
        for (int i = 0; i < 16; i++) {
            s.append(cifre.charAt(r.nextInt(16)));
        }
        return s.toString();
    }

    private Piloti() {
    }
}

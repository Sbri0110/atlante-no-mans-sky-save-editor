package it.atlante.finestra;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * I tratti delle fregate e i nomi delle loro statistiche.
 *
 * Nel salvataggio una fregata ha una lista di tratti — {@code TraitIDs} — scritti
 * come identificativi: {@code ^EXPLORE_PRI}, {@code ^SPEED_TER_8}. Da soli non
 * dicono niente: servono il nome e l'effetto, che stanno in
 * {@code risorse/db/frigates.xml}, lo stesso elenco che usava il vecchio editor.
 *
 * Le statistiche sono undici numeri in fila ({@code Stats}) e l'ordine e' fisso:
 * Combat, Exploration, Industry, Trading, Cost Per Warp, Expedition Fuel Cost,
 * Expedition Duration, Loot, Ripara, Damage Reduction, Stealth. I nomi sono
 * quelli del vecchio editor, che a sua volta li prendeva dal gioco.
 */
public final class Tratti {

    /** Un tratto: identificativo, nome, quanto vale e su cosa. */
    public static final class Tratto {
        public final String id;
        public final String nome;
        public final String forza;
        public final String tipo;
        public final boolean benefico;

        Tratto(String id, String nome, String forza, String tipo, boolean benefico) {
            this.id = id;
            this.nome = nome;
            this.forza = forza;
            this.tipo = tipo;
            this.benefico = benefico;
        }

        /**
         * Come si scrive nel form: "Exploration Specialist (+15 Exploration)".
         *
         * Il tipo dice su cosa agisce e viene tradotto con il nome che il gioco
         * mostra; per velocita' e carburante il valore e' una percentuale, e il
         * segno di percentuale lo si mette li'.
         */
        public String etichetta() {
            String unita = percentuale() ? "%" : "";
            return nome + " (" + forza + unita + " " + nomeTipo(tipo) + ")";
        }

        private boolean percentuale() {
            return "SPEED".equals(tipo) || "FUELBURNRATE".equals(tipo)
                    || "FUELCAPACITY".equals(tipo);
        }

        @Override
        public String toString() {
            return etichetta();
        }
    }

    /** Le undici statistiche di una fregata, nell'ordine in cui stanno nel file. */
    public static final String[] STATS = {
            "Combat",
            "Exploration",
            "Industry",
            "Trading",
            "Costo per salto",
            "Carburante per spedizione",
            "Durata spedizione",
            "Bottino",
            "Riparazione",
            "Riduzione dei danni",
            "Furtività",
    };

    /** Il nome di un tipo di tratto, come lo mostra il gioco. */
    public static String nomeTipo(String tipo) {
        if (tipo == null) {
            return "";
        }
        if ("COMBAT".equals(tipo)) {
            return "Combat";
        }
        if ("EXPLORATION".equals(tipo)) {
            return "Exploration";
        }
        if ("MINING".equals(tipo)) {
            return "Industry";
        }
        if ("DIPLOMATIC".equals(tipo)) {
            return "Trading";
        }
        if ("FUELBURNRATE".equals(tipo)) {
            return "Costo per salto";
        }
        if ("FUELCAPACITY".equals(tipo)) {
            return "Carburante per spedizione";
        }
        if ("SPEED".equals(tipo)) {
            return "Durata spedizione";
        }
        if ("EXTRALOOT".equals(tipo)) {
            return "Bottino";
        }
        if ("REPAIR".equals(tipo)) {
            return "Riparazione";
        }
        if ("INVULNERABLE".equals(tipo)) {
            return "Riduzione dei danni";
        }
        if ("STEALTH".equals(tipo)) {
            return "Furtività";
        }
        return tipo;
    }

    private static List<Tratto> elenco;

    /** I tratti, letti una volta sola dal database. */
    public static List<Tratto> tutti() {
        if (elenco == null) {
            elenco = carica();
        }
        return elenco;
    }

    /** Il tratto con questo identificativo, o null. */
    public static Tratto perId(String id) {
        if (id == null) {
            return null;
        }
        for (Tratto t : tutti()) {
            if (t.id.equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Tratto> carica() {
        File f = new File("risorse" + File.separator + "db" + File.separator + "frigates.xml");
        if (!f.isFile()) {
            f = new File("risorse/db/frigates.xml");
        }
        if (!f.isFile()) {
            return Collections.<Tratto>emptyList();
        }
        final List<Tratto> letti = new ArrayList<Tratto>();
        try {
            SAXParser p = SAXParserFactory.newInstance().newSAXParser();
            p.parse(f, new DefaultHandler() {
                @Override
                public void startElement(String uri, String locale, String nome, Attributes a) {
                    if (!"trait".equals(nome)) {
                        return;
                    }
                    String id = a.getValue("id");
                    if (id == null) {
                        return;
                    }
                    letti.add(new Tratto(id, a.getValue("name"), a.getValue("strength"),
                            a.getValue("type"), "true".equals(a.getValue("beneficial"))));
                }
            });
        } catch (Exception e) {
            return Collections.<Tratto>emptyList();
        }
        return Collections.unmodifiableList(letti);
    }

    private Tratti() {
    }
}

package it.atlante.finestra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quali inventari ha ogni sezione.
 *
 * Il vecchio editor, nella scheda "Tuta", non mostrava i campi del salvataggio:
 * mostrava tre griglie di slot — inventario, stiva e tecnologie — con un selettore
 * per passare dall'una all'altra. Qui si dichiara quella corrispondenza: per ogni
 * sezione, l'elenco degli inventari con il nome mostrato e il percorso nel
 * salvataggio.
 *
 * I percorsi sono relativi alla radice e usano i nomi leggibili dei campi, gli
 * stessi che si vedono nell'editor.
 *
 * <b>Gruppi.</b> Quando i depositi di una sezione sono tanti, le voci hanno un
 * <i>gruppo</i> e la sezione mostra prima i gruppi e poi i loro depositi: le navi
 * (inventario e tecnologie di ognuna), i veicoli, il mercantile con i suoi dieci
 * contenitori, la base con i suoi e la cucina. Dove il gruppo non serve — la
 * tuta, il multi-tool — resta una fila di schede sola.
 */
public final class Inventari {

    /** Un inventario: dove sta e come si chiama. */
    public static final class Inventario {
        /**
         * Il gruppo a cui appartiene, o null.
         *
         * Per le navi e' il nome della nave: la stiva, le tecnologie e il cargo
         * di "Fenice nata tra le stelle" sono tre schede dello stesso gruppo.
         */
        public final String gruppo;

        public final String etichetta;
        public final String percorso;
        /**
         * Dove stanno le statistiche della cosa a cui appartiene questo
         * inventario, o null.
         *
         * Una nave ha il suo danno e il suo scudo, un multi-tool il suo danno e
         * la sua scansione: sono valori della cosa, non dell'inventario, e
         * vanno mostrati accanto ad esso — non in una sezione a parte.
         */
        public final String statistiche;

        Inventario(String etichetta, String percorso) {
            this(null, etichetta, percorso, null);
        }

        Inventario(String etichetta, String percorso, String statistiche) {
            this(null, etichetta, percorso, statistiche);
        }

        Inventario(String gruppo, String etichetta, String percorso, String statistiche) {
            this.gruppo = gruppo;
            this.etichetta = etichetta;
            this.percorso = percorso;
            this.statistiche = statistiche;
        }
    }

    /**
     * Una statistica scritta come un valore singolo, non come lista di
     * statistiche base.
     *
     * Salute, scudo, energia, unita', naniti e quicksilver stanno nel
     * salvataggio come numeri interi sciolti dentro PlayerStateData. Il vecchio
     * editor li chiamava "Statistiche principali" e li mostrava nella scheda
     * della tuta.
     */
    public static final class Statistica {
        public final String etichetta;
        public final String percorso;
        /**
         * Vero per le valute.
         *
         * Il salvataggio tiene unita', naniti e quicksilver in un intero con
         * segno, ma il gioco li mostra senza: un giocatore con due miliardi e
         * settecento milioni di unita' ha nel file {@code -1598048959}. Qui si
         * legge e si scrive il numero che si vede nel gioco, e la conversione
         * resta dentro questo editor.
         */
        public final boolean senzaSegno;

        Statistica(String etichetta, String percorso) {
            this(etichetta, percorso, false);
        }

        Statistica(String etichetta, String percorso, boolean senzaSegno) {
            this.etichetta = etichetta;
            this.percorso = percorso;
            this.senzaSegno = senzaSegno;
        }
    }

    private static final Map<String, List<Inventario>> PER_SEZIONE =
            new LinkedHashMap<String, List<Inventario>>();

    private static final Map<String, List<Statistica>> STATISTICHE_PRINCIPALI =
            new LinkedHashMap<String, List<Statistica>>();

    private static void sezione(String nome, Inventario... inventari) {
        PER_SEZIONE.put(nome, Collections.unmodifiableList(Arrays.asList(inventari)));
    }

    private static Inventario inv(String etichetta, String percorso) {
        return new Inventario(etichetta, percorso);
    }

    private static Inventario inv(String etichetta, String percorso, String statistiche) {
        return new Inventario(etichetta, percorso, statistiche);
    }

    static {
        // Il contesto va indicato perche' il salvataggio tiene due copie dello
        // stato del giocatore: quella della partita e quella della spedizione.
        String base = "BaseContext.PlayerStateData.";
        String sped = "ExpeditionContext.PlayerStateData.";

        // Il multi-tool non ha inventari fissi: se ne costruisce uno per ogni
        // multi-tool posseduto, leggendoli dal salvataggio. La voce qui serve
        // solo a far sapere alla sezione che si mostra a griglia e non come
        // albero di campi.
        sezione("Multitool");

        sezione("Tuta",
                inv("Inventario", base + "Inventory"),
                inv("Tecnologie", base + "Inventory_TechOnly"));

        // Le statistiche principali della tuta: gli stessi valori che il gioco
        // mostra in basso a sinistra nella schermata dell'inventario. I nomi
        // italiani vengono da Etichette, cosi' restano gli stessi del resto
        // dell'interfaccia.
        List<Statistica> tuta = new ArrayList<Statistica>();
        tuta.add(new Statistica(Etichette.leggi("Health"), base + "Health"));
        tuta.add(new Statistica(Etichette.leggi("Shield"), base + "Shield"));
        tuta.add(new Statistica(Etichette.leggi("Energy"), base + "Energy"));
        tuta.add(new Statistica(Etichette.leggi("Units"), base + "Units", true));
        tuta.add(new Statistica(Etichette.leggi("Nanites"), base + "Nanites", true));
        tuta.add(new Statistica(Etichette.leggi("Specials"), base + "Specials", true));
        STATISTICHE_PRINCIPALI.put("Tuta", Collections.unmodifiableList(tuta));

        // Le navi non stanno qui: ognuna ha i suoi inventari e si leggono dal
        // salvataggio, come i multi-tool. Vedi navi().
        sezione("Navi");

        // I veicoli non stanno qui: come le navi sono una lista, e ognuno ha i
        // suoi depositi. Vedi veicoli().
        sezione("Veicoli");

        // Il mercantile ha inventario e tecnologie, e nella stiva i dieci
        // contenitori di stoccaggio: nel gioco sono gli stessi della base, e si
        // aprono da tutti e due i posti. Chi cerca il deposito mentre e' a bordo
        // non deve andare a cercarlo in un'altra sezione.
        //
        // Tredici schede in fila non si leggono: sono quindi due gruppi, e la
        // prima riga dice solo "Mercantile" o "Depositi".
        List<Inventario> mercantile = new ArrayList<Inventario>();
        mercantile.add(new Inventario("Mercantile", "Inventario", base + "FreighterInventory", null));
        mercantile.add(new Inventario("Mercantile", "Cargo", base + "FreighterInventory_Cargo", null));
        mercantile.add(new Inventario("Mercantile", "Tecnologie",
                base + "FreighterInventory_TechOnly", null));
        for (int i = 1; i <= 10; i++) {
            mercantile.add(new Inventario("Depositi", "Deposito " + i,
                    base + "Chest" + i + "Inventory", null));
        }
        PER_SEZIONE.put("Mercantile", Collections.unmodifiableList(mercantile));

        // Sedici contenitori in fila non si leggono: sono due gruppi, e la prima
        // riga dice solo "Contenitori" o "Cucina e pesca". I nomi delle voci
        // dicono da soli a quale dei due appartengono.
        List<Inventario> basi = new ArrayList<Inventario>();
        for (int i = 1; i <= 10; i++) {
            basi.add(new Inventario("Contenitori", "Deposito " + i, base + "Chest" + i + "Inventory", null));
        }
        basi.add(new Inventario("Contenitori", "Deposito speciale", base + "ChestMagicInventory", null));
        basi.add(new Inventario("Contenitori", "Deposito del razzo", base + "RocketLockerInventory", null));
        basi.add(new Inventario("Cucina e pesca", "Ingredienti di cucina",
                base + "CookingIngredientsInventory", null));
        basi.add(new Inventario("Cucina e pesca", "Unita' di cibo", base + "FoodUnitInventory", null));
        basi.add(new Inventario("Cucina e pesca", "Esca da pesca", base + "FishBaitBoxInventory", null));
        basi.add(new Inventario("Cucina e pesca", "Piattaforma da pesca",
                base + "FishPlatformInventory", null));
        PER_SEZIONE.put("Basi e contenitori", Collections.unmodifiableList(basi));
    }

    /** Gli inventari di una sezione, o lista vuota se la sezione non ne ha. */
    public static List<Inventario> perSezione(String nomeSezione) {
        List<Inventario> l = PER_SEZIONE.get(nomeSezione);
        return l == null ? Collections.<Inventario>emptyList() : l;
    }

    /**
     * Gli inventari di una sezione, ricavati dal salvataggio aperto.
     *
     * Per la sezione Navi non basta un elenco fisso: le navi possedute sono una
     * lista, e ognuna ha tre inventari. Il nome della nave fa da gruppo, cosi'
     * si passa dalla nave ai suoi tre depositi.
     */
    @SuppressWarnings("unchecked")
    public static List<Inventario> perSezione(Object radice, String nomeSezione) {
        if ("Navi".equals(nomeSezione)) {
            return navi(radice);
        }
        if ("Veicoli".equals(nomeSezione)) {
            return veicoli(radice);
        }
        if ("Multitool".equals(nomeSezione)) {
            return armi(radice);
        }
        return perSezione(nomeSezione);
    }

    /**
     * I veicoli posseduti, ognuno con inventario e tecnologie.
     *
     * Il salvataggio non dice che veicolo e': il nome e' vuoto e la risorsa
     * pure. L'ordine della lista invece e' fisso, ed e' quello che il vecchio
     * editor usava per riconoscerli — 0 e' il Roamer, 1 il Nomad, 2 il Colossus
     * e cosi' via. Per gli indici che non conosce si ripiega su "Veicolo N",
     * che e' meglio di un'etichetta inventata.
     */
    private static List<Inventario> veicoli(Object radice) {
        List<Inventario> elenco = new ArrayList<Inventario>();
        Object posseduti = risolvi(radice, "BaseContext.PlayerStateData.VehicleOwnership");
        if (!(posseduti instanceof List)) {
            return elenco;
        }
        List<Object> lista = (List<Object>) posseduti;
        for (int i = 0; i < lista.size(); i++) {
            String r = "BaseContext.PlayerStateData.VehicleOwnership[" + i + "]";
            if (!esiste(radice, r + ".Inventory")) {
                continue;
            }
            String nome = tipoVeicolo(i);
            elenco.add(new Inventario(nome, "Inventario", r + ".Inventory", null));
            if (esiste(radice, r + ".Inventory_TechOnly")) {
                elenco.add(new Inventario(nome, "Tecnologie", r + ".Inventory_TechOnly", null));
            }
        }
        return elenco;
    }

    /** Il nome del veicolo che sta a questo indice nell'elenco del salvataggio. */
    private static String tipoVeicolo(int indice) {
        switch (indice) {
            case 0: return "Roamer";
            case 1: return "Nomad";
            case 2: return "Colossus";
            case 3: return "Pilgrim";
            case 5: return "Nautilon";
            case 6: return "Minotaur";
            case 1000: return "Skiff";
            default: return "Veicolo " + (indice + 1);
        }
    }

    /**
     * I multi-tool posseduti.
     *
     * Come le navi: sono una lista, e ognuno ha il suo inventario e le sue
     * statistiche. Cambiando multi-tool cambiano gli slot e i valori.
     */
    @SuppressWarnings("unchecked")
    private static List<Inventario> armi(Object radice) {
        List<Inventario> elenco = new ArrayList<Inventario>();
        Object posseduti = risolvi(radice, "BaseContext.PlayerStateData.Multitools");
        if (!(posseduti instanceof List)) {
            return elenco;
        }
        List<Object> lista = (List<Object>) posseduti;
        for (int i = 0; i < lista.size(); i++) {
            String nome = "Multi-tool " + (i + 1);
            Object arma = lista.get(i);
            if (arma instanceof Map) {
                Object n = ((Map<String, Object>) arma).get("Name");
                if (n != null && !String.valueOf(n).trim().isEmpty()) {
                    nome = String.valueOf(n).trim();
                }
            }
            elenco.add(new Inventario(nome,
                    "BaseContext.PlayerStateData.Multitools[" + i + "].Store",
                    "BaseContext.PlayerStateData.Multitools[" + i + "].Store.BaseStatValues"));
        }
        return elenco;
    }

    /**
     * Le navi possedute, ognuna con il suo inventario e le sue tecnologie.
     *
     * Nel gioco una nave ha due depositi e non tre: l'inventario e le
     * tecnologie. Il campo {@code Inventory_Cargo} esiste nel salvataggio ma
     * resta vuoto — zero caselle sbloccate, zero oggetti — e mostrarlo faceva
     * credere a un deposito che non c'e'.
     *
     * Le navi vuote — quelle che il salvataggio tiene come segnaposto, una
     * cella sola e nessun oggetto — non si mostrano: sono righe che non
     * portano a niente.
     */
    private static List<Inventario> navi(Object radice) {
        List<Inventario> elenco = new ArrayList<Inventario>();
        Object possedute = risolvi(radice, "BaseContext.PlayerStateData.ShipOwnership");
        if (!(possedute instanceof List)) {
            return elenco;
        }
        List<Object> lista = (List<Object>) possedute;
        for (int i = 0; i < lista.size(); i++) {
            String radice2 = "BaseContext.PlayerStateData.ShipOwnership[" + i + "]";
            String nome = "Nave " + (i + 1);
            Object nave = lista.get(i);
            if (nave instanceof Map) {
                Object n = ((Map<String, Object>) nave).get("Name");
                if (n != null && !String.valueOf(n).trim().isEmpty()) {
                    nome = String.valueOf(n).trim();
                }
            }
            if (!esiste(radice, radice2 + ".Inventory")) {
                continue;
            }
            elenco.add(new Inventario(nome, "Inventario", radice2 + ".Inventory",
                    radice2 + ".Inventory.BaseStatValues"));
            if (esiste(radice, radice2 + ".Inventory_TechOnly")) {
                elenco.add(new Inventario(nome, "Tecnologie",
                        radice2 + ".Inventory_TechOnly", null));
            }
        }
        return elenco;
    }

    /**
     * Vero se il gioco ha davvero messo qualcosa in questo inventario.
     *
     * E' il criterio che decide quali schede si vedono: un campo che esiste nel
     * salvataggio ma non ha ne' caselle sbloccate ne' oggetti non e' un
     * deposito, e' un campo vuoto. Cosi' la tuta non mostra una stiva che non
     * ha e le navi non mostrano un cargo che non hanno, mentre il mercantile
     * tiene il suo.
     */
    @SuppressWarnings("unchecked")
    public static boolean esiste(Object radice, String percorso) {
        Object inv = risolvi(radice, percorso);
        if (!(inv instanceof Map)) {
            return false;
        }
        Map<String, Object> mappa = (Map<String, Object>) inv;
        Object validi = mappa.get("ValidSlotIndices");
        if (validi instanceof List && !((List<Object>) validi).isEmpty()) {
            return true;
        }
        Object slots = mappa.get("Slots");
        return slots instanceof List && !((List<Object>) slots).isEmpty();
    }

    /** Quante colonne ha una griglia di slot, 0 se non e' dichiarato. */
    public static int larghezza(Map<String, Object> inventario) {
        return intero(inventario.get("Width"));
    }

    /** Quante righe ha una griglia di slot, 0 se non e' dichiarato. */
    public static int altezza(Map<String, Object> inventario) {
        return intero(inventario.get("Height"));
    }

    private static int intero(Object v) {
        if (v instanceof it.atlante.json.Json.Numero) {
            return (int) ((it.atlante.json.Json.Numero) v).comeLong();
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return 0;
    }

    /** Le statistiche principali di una sezione, o lista vuota. */
    public static List<Statistica> statistichePrincipali(String nomeSezione) {
        List<Statistica> l = STATISTICHE_PRINCIPALI.get(nomeSezione);
        return l == null ? Collections.<Statistica>emptyList() : l;
    }

    /** Vero se la sezione si mostra come griglie di slot invece che come campi. */
    public static boolean aGriglia(String nomeSezione) {
        return PER_SEZIONE.containsKey(nomeSezione);
    }

    // ------------------------------------------------------------------

    /**
     * Il contesto che si sta giocando.
     *
     * Il salvataggio tiene due copie dello stato del giocatore: quella della
     * partita e quella della spedizione. {@code ActiveContext} dice quale delle
     * due e' in corso — "Main" oppure "Expedition" — e leggerla e' quello che
     * permette all'editor di aprire un salvataggio di spedizione senza mostrare
     * una partita che non c'entra.
     */
    public static String contesto(Object radice) {
        Object attivo = risolvi(radice, "ActiveContext");
        String nome = attivo == null ? "" : String.valueOf(attivo).toLowerCase();
        return nome.indexOf("expedition") >= 0 ? "ExpeditionContext" : "BaseContext";
    }

    /**
     * Risolve un percorso come "BaseContext.PlayerStateData.Inventory".
     *
     * I percorsi usano i nomi leggibili, che sono le chiavi vere della mappa:
     * il salvataggio e' gia' tradotto in memoria. Le parentesi quadre indicano
     * un indice di lista.
     *
     * Il prefisso del contesto viene sostituito con quello attivo: i percorsi
     * sono scritti una volta sola e valgono sia per la partita sia per la
     * spedizione.
     *
     * @return il valore, o null se il percorso non esiste in questo salvataggio
     */
    @SuppressWarnings("unchecked")
    public static Object risolvi(Object radice, String percorso) {
        String p = percorso;
        if (p.startsWith("BaseContext.")) {
            p = contesto(radice) + p.substring("BaseContext".length());
        }
        Object corrente = radice;
        for (String pezzo : p.split("\\.")) {
            String nome = pezzo;
            int indice = -1;
            int parentesi = pezzo.indexOf('[');
            if (parentesi > 0 && pezzo.endsWith("]")) {
                nome = pezzo.substring(0, parentesi);
                try {
                    indice = Integer.parseInt(pezzo.substring(parentesi + 1, pezzo.length() - 1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (!(corrente instanceof Map)) {
                return null;
            }
            corrente = ((Map<String, Object>) corrente).get(nome);
            if (corrente == null) {
                return null;
            }
            if (indice >= 0) {
                if (!(corrente instanceof List)) {
                    return null;
                }
                List<Object> lista = (List<Object>) corrente;
                if (indice >= lista.size()) {
                    return null;
                }
                corrente = lista.get(indice);
            }
        }
        return corrente;
    }

    /** Toglie il prefisso del contesto, per mostrare un percorso piu' corto. */
    public static String accorcia(String percorso) {
        String p = percorso;
        if (p.startsWith("BaseContext.PlayerStateData.")) {
            p = p.substring("BaseContext.PlayerStateData.".length());
        } else if (p.startsWith("ExpeditionContext.PlayerStateData.")) {
            p = p.substring("ExpeditionContext.PlayerStateData.".length());
        }
        return p;
    }

    /** Solo gli inventari che esistono davvero in questo salvataggio. */
    public static List<Inventario> esistenti(Object radice, List<Inventario> tutti) {
        List<Inventario> trovati = new ArrayList<Inventario>();
        for (Inventario i : tutti) {
            if (esiste(radice, i.percorso)) {
                trovati.add(i);
            }
        }
        return trovati;
    }

    private Inventari() {
    }
}

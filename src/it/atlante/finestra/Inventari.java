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
 */
public final class Inventari {

    /** Un inventario: dove sta e come si chiama. */
    public static final class Inventario {
        public final String etichetta;
        public final String percorso;

        Inventario(String etichetta, String percorso) {
            this.etichetta = etichetta;
            this.percorso = percorso;
        }
    }

    private static final Map<String, List<Inventario>> PER_SEZIONE =
            new LinkedHashMap<String, List<Inventario>>();

    private static void sezione(String nome, Inventario... inventari) {
        PER_SEZIONE.put(nome, Collections.unmodifiableList(Arrays.asList(inventari)));
    }

    private static Inventario inv(String etichetta, String percorso) {
        return new Inventario(etichetta, percorso);
    }

    static {
        // Il contesto va indicato perche' il salvataggio tiene due copie dello
        // stato del giocatore: quella della partita e quella della spedizione.
        String base = "BaseContext.PlayerStateData.";
        String sped = "ExpeditionContext.PlayerStateData.";

        sezione("Tuta",
                inv("Inventario", base + "Inventory"),
                inv("Stiva", base + "Inventory_Cargo"),
                inv("Tecnologie", base + "Inventory_TechOnly"));

        sezione("Multitool",
                inv("Armi", base + "WeaponInventory"),
                inv("Tecnologie dell'arma", base + "WeaponInventory_TechOnly"));

        sezione("Navi",
                inv("Stiva della nave", base + "ShipInventory"),
                inv("Tecnologie della nave", base + "ShipInventory_TechOnly"),
                inv("Corvetta", base + "CorvetteStorageInventory"));

        sezione("Mercantile",
                inv("Stiva del mercantile", base + "FreighterInventory"),
                inv("Cargo del mercantile", base + "FreighterInventory_Cargo"),
                inv("Tecnologie del mercantile", base + "FreighterInventory_TechOnly"));

        sezione("Basi e contenitori",
                inv("Deposito 1", base + "Chest1Inventory"),
                inv("Deposito 2", base + "Chest2Inventory"),
                inv("Deposito 3", base + "Chest3Inventory"),
                inv("Deposito 4", base + "Chest4Inventory"),
                inv("Deposito 5", base + "Chest5Inventory"),
                inv("Deposito 6", base + "Chest6Inventory"),
                inv("Deposito 7", base + "Chest7Inventory"),
                inv("Deposito 8", base + "Chest8Inventory"),
                inv("Deposito 9", base + "Chest9Inventory"),
                inv("Deposito 10", base + "Chest10Inventory"),
                inv("Deposito speciale", base + "ChestMagicInventory"),
                inv("Deposito del razzo", base + "RocketLockerInventory"),
                inv("Ingredienti di cucina", base + "CookingIngredientsInventory"),
                inv("Unità di cibo", base + "FoodUnitInventory"),
                inv("Esca da pesca", base + "FishBaitBoxInventory"),
                inv("Piattaforma da pesca", base + "FishPlatformInventory"));
    }

    /** Gli inventari di una sezione, o lista vuota se la sezione non ne ha. */
    public static List<Inventario> perSezione(String nomeSezione) {
        List<Inventario> l = PER_SEZIONE.get(nomeSezione);
        return l == null ? Collections.<Inventario>emptyList() : l;
    }

    /** Vero se la sezione si mostra come griglie di slot invece che come campi. */
    public static boolean aGriglia(String nomeSezione) {
        return PER_SEZIONE.containsKey(nomeSezione);
    }

    // ------------------------------------------------------------------

    /**
     * Risolve un percorso come "BaseContext.PlayerStateData.Inventory".
     *
     * I percorsi usano i nomi leggibili, che sono le chiavi vere della mappa:
     * il salvataggio e' gia' tradotto in memoria. Le parentesi quadre indicano
     * un indice di lista.
     *
     * @return il valore, o null se il percorso non esiste in questo salvataggio
     */
    @SuppressWarnings("unchecked")
    public static Object risolvi(Object radice, String percorso) {
        Object corrente = radice;
        for (String pezzo : percorso.split("\\.")) {
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
            if (risolvi(radice, i.percorso) instanceof Map) {
                trovati.add(i);
            }
        }
        return trovati;
    }

    private Inventari() {
    }
}

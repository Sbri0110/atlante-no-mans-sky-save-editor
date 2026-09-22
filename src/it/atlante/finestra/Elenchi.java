package it.atlante.finestra;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Le sezioni che sono elenchi di elementi, non griglie di slot.
 *
 * Nel salvataggio ci sono tre modi di tenere le cose, e ognuno vuole un pannello
 * diverso:
 *
 * <ul>
 *   <li><b>inventari</b> — cento slot con dentro un oggetto: griglia con le icone
 *       ({@link GrigliaSlot});</li>
 *   <li><b>elenchi</b> — pochi elementi ricchi di campi, come i quattro piloti
 *       dello squadrone o le ventitre' fregate: si sceglie l'elemento e si
 *       modificano i suoi campi ({@link PannelloElenco});</li>
 *   <li><b>valori singoli</b> — un campo e basta.</li>
 * </ul>
 *
 * Qui sta la corrispondenza per il secondo caso. Il nome di ogni elemento non
 * esiste sempre: le fregate hanno un {@code CustomName} che il giocatore puo'
 * non aver mai dato, i piloti non hanno nome affatto. In quel caso si usa un
 * numero progressivo, che e' comunque meglio di una riga vuota.
 */
public final class Elenchi {

    /** Un elenco: dove sta, come si chiamano gli elementi, cosa mostrare. */
    public static final class Elenco {
        public final String percorso;
        public final String campoNome;
        public final String prefisso;
        public final String campoSottotitolo;
        /**
         * Il campo da cui ricavare l'icona: un oggetto con dentro un
         * {@code Filename}, come {@code NPCResource} per il pilota o
         * {@code ShipResource} per la sua navicella. Vedi
         * {@link Icone#iconaDiRisorsa}.
         */
        public final String campoIcona;
        /** Il secondo campo per l'icona, o null: la navicella del pilota. */
        public final String campoIcona2;
        /**
         * Un'icona uguale per tutti gli elementi, quando il salvataggio non ha
         * niente da cui ricavarla: i compagni sono tutti bestie, e l'icona della
         * zampa e' quella che il gioco usa per loro.
         */
        public final String iconaFissa;

        Elenco(String percorso, String campoNome, String prefisso, String campoSottotitolo) {
            this(percorso, campoNome, prefisso, campoSottotitolo, null, null, null);
        }

        Elenco(String percorso, String campoNome, String prefisso, String campoSottotitolo,
               String campoIcona, String campoIcona2) {
            this(percorso, campoNome, prefisso, campoSottotitolo, campoIcona, campoIcona2, null);
        }

        Elenco(String percorso, String campoNome, String prefisso, String campoSottotitolo,
               String campoIcona, String campoIcona2, String iconaFissa) {
            this.percorso = percorso;
            this.campoNome = campoNome;
            this.prefisso = prefisso;
            this.campoSottotitolo = campoSottotitolo;
            this.campoIcona = campoIcona;
            this.campoIcona2 = campoIcona2;
            this.iconaFissa = iconaFissa;
        }
    }

    private static final Map<String, Elenco> PER_SEZIONE = new LinkedHashMap<String, Elenco>();

    static {
        String base = "BaseContext.PlayerStateData.";

        // Il pilota: la razza sta nel modello dell'equipaggio, la navicella nel
        // modello della nave. Sono due risorse diverse dello stesso pilota.
        PER_SEZIONE.put("Squadrone", new Elenco(
                base + "SquadronPilots", null, "Pilota", "PilotRank",
                "NPCResource", "ShipResource"));

        // Le fregate: la razza dice chi le ha costruite, la classe dice che
        // fregata e' (Exploration, Mining, Diplomacy, Pirate, DeepSpace,
        // GhostShip, Normandy) e ogni classe ha la sua icona.
        PER_SEZIONE.put("Fregate", new Elenco(
                base + "FleetFrigates", "CustomName", "Fregata", "FrigateClass",
                "Race", "FrigateClass"));

        // Il nome di un compagno sta in CustomName, non in Name: cercando
        // "Name" l'elenco restava sempre su "Compagno 1, 2, 3...".
        PER_SEZIONE.put("Compagni", new Elenco(
                base + "Pets", "CustomName", "Compagno", "CreatureType",
                null, null, "UI-PET.PNG"));
    }

    /** L'elenco di una sezione, o null se la sezione non e' un elenco. */
    public static Elenco perSezione(String nomeSezione) {
        return PER_SEZIONE.get(nomeSezione);
    }

    /** Vero se la sezione si mostra come elenco di elementi. */
    public static boolean aElenco(String nomeSezione) {
        return PER_SEZIONE.containsKey(nomeSezione);
    }

    private Elenchi() {
    }
}

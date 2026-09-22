package it.atlante.finestra;

import it.atlante.json.Json;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Importa le parti di una Corvette da un file JSON.
 *
 * Il vecchio editor aveva questa funzione: si sceglie un file esportato dal
 * gioco (o da un altro salvataggio) e le sue parti finiscono nel deposito dei
 * pezzi della Corvette. Il file e' un oggetto JSON con un elenco
 * {@code Objects} (o {@code objects}), e ogni voce descrive un pezzo.
 *
 * Le parti che il deposito non conosce, o che ci sono gia', si saltano: il
 * deposito tiene quello che il gioco gli ha messo, e riempirlo di doppioni lo
 * renderebbe illeggibile.
 */
public final class ImportaCorvetta {

    /** Cosa e' successo: serve a dirlo all'utente. */
    public static final class Esito {
        public int importate;
        public int saltate;
        public String problema;

        public String messaggio() {
            if (problema != null) {
                return problema;
            }
            return importate + " parti importate"
                    + (saltate > 0 ? ", " + saltate + " saltate (gia' presenti)" : "");
        }
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private ImportaCorvetta() {
    }

    /**
     * Legge il file e riempie le caselle libere del deposito.
     *
     * @param inventario la mappa del deposito dei pezzi
     * @param file       il file JSON scelto
     */
    @SuppressWarnings("unchecked")
    public static Esito importa(Map<String, Object> inventario, File file) {
        Esito esito = new Esito();
        if (inventario == null) {
            esito.problema = "Nessun deposito di pezzi aperto.";
            return esito;
        }
        Object elencoGrezzo;
        try {
            String testo = new String(Files.readAllBytes(file.toPath()), UTF8);
            elencoGrezzo = Json.leggi(testo);
        } catch (Exception e) {
            esito.problema = "Il file non si legge: " + e.getMessage();
            return esito;
        }
        List<Object> oggetti = trovaOggetti(elencoGrezzo);
        if (oggetti == null) {
            esito.problema = "Il file non contiene un elenco valido 'Objects' o 'objects'.";
            return esito;
        }
        if (oggetti.isEmpty()) {
            esito.problema = "La lista degli oggetti della Corvette e' vuota.";
            return esito;
        }

        Object slotsGrezzo = inventario.get("Slots");
        List<Object> slots = slotsGrezzo instanceof List
                ? (List<Object>) slotsGrezzo : new ArrayList<Object>();
        if (!(slotsGrezzo instanceof List)) {
            inventario.put("Slots", slots);
        }
        Object validiGrezzo = inventario.get("ValidSlotIndices");
        List<Object> validi = validiGrezzo instanceof List
                ? (List<Object>) validiGrezzo : new ArrayList<Object>();

        // Le posizioni gia' occupate, per non sovrascriverle.
        List<String> presenti = new ArrayList<String>();
        for (Object o : slots) {
            if (o instanceof Map) {
                Object id = ((Map<String, Object>) o).get("Id");
                if (id != null && !String.valueOf(id).isEmpty()) {
                    presenti.add(String.valueOf(id));
                }
            }
        }

        for (Object o : oggetti) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> voce = (Map<String, Object>) o;
            String id = identificativo(voce);
            if (id == null || id.isEmpty() || "^".equals(id)) {
                esito.saltate++;
                continue;
            }
            if (presenti.contains(id)) {
                esito.saltate++;
                continue;
            }
            int[] posizione = posizioneLibera(validi, slots);
            if (posizione == null) {
                esito.saltate++;
                continue;
            }
            slots.add(pezzo(id, posizione[0], posizione[1]));
            presenti.add(id);
            esito.importate++;
        }
        return esito;
    }

    /** L'elenco degli oggetti, dentro la radice o sotto una chiave. */
    @SuppressWarnings("unchecked")
    private static List<Object> trovaOggetti(Object radice) {
        if (radice instanceof List) {
            return (List<Object>) radice;
        }
        if (!(radice instanceof Map)) {
            return null;
        }
        Map<String, Object> m = (Map<String, Object>) radice;
        for (String chiave : new String[]{"Objects", "objects"}) {
            Object v = m.get(chiave);
            if (v instanceof List) {
                return (List<Object>) v;
            }
        }
        return null;
    }

    /** L'identificativo di un pezzo, comunque sia scritto nel file. */
    @SuppressWarnings("unchecked")
    private static String identificativo(Map<String, Object> voce) {
        for (String chiave : new String[]{"ObjectID", "objectID", "Id", "id", "Product", "ProductID"}) {
            Object v = voce.get(chiave);
            if (v != null && !String.valueOf(v).isEmpty()) {
                return String.valueOf(v);
            }
        }
        Object risorsa = voce.get("Resource");
        if (risorsa instanceof Map) {
            Object f = ((Map<String, Object>) risorsa).get("Filename");
            if (f != null) {
                String nome = String.valueOf(f);
                int barra = nome.lastIndexOf('/');
                if (barra >= 0) {
                    nome = nome.substring(barra + 1);
                }
                int punto = nome.indexOf('.');
                if (punto > 0) {
                    nome = nome.substring(0, punto);
                }
                if (!nome.isEmpty()) {
                    return "^" + nome.toUpperCase();
                }
            }
        }
        return null;
    }

    /** Una posizione libera fra quelle sbloccate. */
    @SuppressWarnings("unchecked")
    private static int[] posizioneLibera(List<Object> validi, List<Object> slots) {
        List<String> occupate = new ArrayList<String>();
        for (Object o : slots) {
            if (o instanceof Map) {
                Object indice = ((Map<String, Object>) o).get("Index");
                if (indice instanceof Map) {
                    Map<String, Object> i = (Map<String, Object>) indice;
                    occupate.add(i.get("X") + "," + i.get("Y"));
                }
            }
        }
        for (Object o : validi) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> i = (Map<String, Object>) o;
            String chiave = i.get("X") + "," + i.get("Y");
            if (!occupate.contains(chiave)) {
                int x = (int) numero(i.get("X"));
                int y = (int) numero(i.get("Y"));
                return new int[]{x, y};
            }
        }
        return null;
    }

    private static long numero(Object v) {
        if (v instanceof Json.Numero) {
            return ((Json.Numero) v).comeLong();
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return 0;
    }

    /** Il pezzo, nella forma che il salvataggio usa. */
    private static Map<String, Object> pezzo(String id, int x, int y) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        Map<String, Object> tipo = new LinkedHashMap<String, Object>();
        tipo.put("InventoryType", "Product");
        p.put("Type", tipo);
        p.put("Id", id);
        p.put("Amount", new Json.Numero("1"));
        p.put("MaxAmount", new Json.Numero("500"));
        p.put("DamageFactor", new Json.Numero("0.0"));
        p.put("FullyInstalled", Boolean.TRUE);
        p.put("AddedAutomatically", Boolean.FALSE);
        Map<String, Object> indice = new LinkedHashMap<String, Object>();
        indice.put("X", new Json.Numero(String.valueOf(x)));
        indice.put("Y", new Json.Numero(String.valueOf(y)));
        p.put("Index", indice);
        return p;
    }
}

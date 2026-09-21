package it.atlante.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lettore e scrittore JSON senza dipendenze esterne.
 *
 * Perche' non usare una libreria: i salvataggi di No Man's Sky contengono
 * numeri che non sopravvivono a un passaggio in virgola mobile. Un campo come
 * {@code 0.6000000238418579} letto come double e riscritto diventa
 * {@code 0.6}, e il gioco riceve un valore diverso da quello che aveva
 * scritto. Un intero grande come un identificatore di sistema supera i 53 bit
 * e viene arrotondato in silenzio.
 *
 * Per questo ogni numero viene conservato come {@link Numero}, che tiene il
 * testo originale e lo restituisce identico. E' la differenza fra un editor
 * che riscrive un salvataggio fedelmente e uno che lo corrompe in modo
 * invisibile.
 *
 * L'ordine delle chiavi viene conservato (LinkedHashMap): un JSON riscritto
 * con le chiavi in ordine diverso resta valido, ma rende impossibile il
 * confronto byte per byte durante il collaudo.
 */
public final class Json {

    private Json() {
    }

    /** Un numero JSON, con il testo esattamente come appariva nel file. */
    public static final class Numero {
        private final String testo;

        public Numero(String testo) {
            this.testo = testo;
        }

        public String testo() {
            return testo;
        }

        public boolean intero() {
            return testo.indexOf('.') < 0 && testo.indexOf('e') < 0 && testo.indexOf('E') < 0;
        }

        public long comeLong() {
            return Long.parseLong(testo);
        }

        public double comeDouble() {
            return Double.parseDouble(testo);
        }

        @Override
        public String toString() {
            return testo;
        }

        @Override
        public boolean equals(Object altro) {
            return altro instanceof Numero && testo.equals(((Numero) altro).testo);
        }

        @Override
        public int hashCode() {
            return testo.hashCode();
        }
    }

    // ------------------------------------------------------------------
    // Lettura
    // ------------------------------------------------------------------

    public static Object leggi(String testo) {
        Lettore l = new Lettore(testo);
        l.saltaSpazi();
        Object v = l.valore();
        l.saltaSpazi();
        if (!l.finito()) {
            throw new IllegalArgumentException("contenuto inatteso dopo il valore, alla posizione " + l.pos);
        }
        return v;
    }

    private static final class Lettore {
        private final String s;
        private int pos;

        Lettore(String s) {
            this.s = s;
        }

        boolean finito() {
            return pos >= s.length();
        }

        void saltaSpazi() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        Object valore() {
            if (finito()) {
                throw new IllegalArgumentException("fine del testo mentre si attendeva un valore");
            }
            char c = s.charAt(pos);
            switch (c) {
                case '{':
                    return oggetto();
                case '[':
                    return lista();
                case '"':
                    return stringa();
                case 't':
                    parola("true");
                    return Boolean.TRUE;
                case 'f':
                    parola("false");
                    return Boolean.FALSE;
                case 'n':
                    parola("null");
                    return null;
                default:
                    return numero();
            }
        }

        private void parola(String attesa) {
            if (!s.startsWith(attesa, pos)) {
                throw new IllegalArgumentException("atteso '" + attesa + "' alla posizione " + pos);
            }
            pos += attesa.length();
        }

        private Map<String, Object> oggetto() {
            Map<String, Object> mappa = new LinkedHashMap<String, Object>();
            pos++; // {
            saltaSpazi();
            if (!finito() && s.charAt(pos) == '}') {
                pos++;
                return mappa;
            }
            while (true) {
                saltaSpazi();
                if (finito() || s.charAt(pos) != '"') {
                    throw new IllegalArgumentException("attesa una chiave alla posizione " + pos);
                }
                String chiave = stringa();
                saltaSpazi();
                if (finito() || s.charAt(pos) != ':') {
                    throw new IllegalArgumentException("atteso ':' alla posizione " + pos);
                }
                pos++;
                saltaSpazi();
                mappa.put(chiave, valore());
                saltaSpazi();
                if (finito()) {
                    throw new IllegalArgumentException("oggetto non chiuso");
                }
                char c = s.charAt(pos);
                if (c == ',') {
                    pos++;
                } else if (c == '}') {
                    pos++;
                    return mappa;
                } else {
                    throw new IllegalArgumentException("atteso ',' o '}' alla posizione " + pos);
                }
            }
        }

        private List<Object> lista() {
            List<Object> lista = new ArrayList<Object>();
            pos++; // [
            saltaSpazi();
            if (!finito() && s.charAt(pos) == ']') {
                pos++;
                return lista;
            }
            while (true) {
                saltaSpazi();
                lista.add(valore());
                saltaSpazi();
                if (finito()) {
                    throw new IllegalArgumentException("lista non chiusa");
                }
                char c = s.charAt(pos);
                if (c == ',') {
                    pos++;
                } else if (c == ']') {
                    pos++;
                    return lista;
                } else {
                    throw new IllegalArgumentException("atteso ',' o ']' alla posizione " + pos);
                }
            }
        }

        private String stringa() {
            pos++; // virgoletta aperta
            StringBuilder b = new StringBuilder();
            while (true) {
                if (finito()) {
                    throw new IllegalArgumentException("stringa non chiusa");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return b.toString();
                }
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                if (finito()) {
                    throw new IllegalArgumentException("sequenza di escape incompleta");
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"':  b.append('"');  break;
                    case '\\': b.append('\\'); break;
                    case '/':  b.append('/');  break;
                    case 'b':  b.append('\b'); break;
                    case 'f':  b.append('\f'); break;
                    case 'n':  b.append('\n'); break;
                    case 'r':  b.append('\r'); break;
                    case 't':  b.append('\t'); break;
                    case 'u':
                        if (pos + 4 > s.length()) {
                            throw new IllegalArgumentException("escape \\u incompleto");
                        }
                        b.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("escape sconosciuto: \\" + e);
                }
            }
        }

        private Numero numero() {
            int inizio = pos;
            if (!finito() && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) {
                pos++;
            }
            while (!finito()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    pos++;
                } else {
                    break;
                }
            }
            if (pos == inizio) {
                throw new IllegalArgumentException("numero non valido alla posizione " + inizio);
            }
            return new Numero(s.substring(inizio, pos));
        }
    }

    // ------------------------------------------------------------------
    // Scrittura
    // ------------------------------------------------------------------

    public static String scrivi(Object valore) {
        StringBuilder b = new StringBuilder(1 << 16);
        scriviIn(b, valore);
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    private static void scriviIn(StringBuilder b, Object v) {
        if (v == null) {
            b.append("null");
        } else if (v instanceof Numero) {
            b.append(((Numero) v).testo());
        } else if (v instanceof Boolean) {
            b.append(((Boolean) v).booleanValue() ? "true" : "false");
        } else if (v instanceof String) {
            scriviStringa(b, (String) v);
        } else if (v instanceof Map) {
            Map<String, Object> mappa = (Map<String, Object>) v;
            b.append('{');
            boolean primo = true;
            for (Map.Entry<String, Object> e : mappa.entrySet()) {
                if (!primo) {
                    b.append(',');
                }
                primo = false;
                scriviStringa(b, e.getKey());
                b.append(':');
                scriviIn(b, e.getValue());
            }
            b.append('}');
        } else if (v instanceof List) {
            List<Object> lista = (List<Object>) v;
            b.append('[');
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) {
                    b.append(',');
                }
                scriviIn(b, lista.get(i));
            }
            b.append(']');
        } else {
            throw new IllegalArgumentException("tipo non scrivibile: " + v.getClass());
        }
    }

    private static void scriviStringa(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n");  break;
                case '\r': b.append("\\r");  break;
                case '\t': b.append("\\t");  break;
                case '\b': b.append("\\b");  break;
                case '\f': b.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        // I caratteri non ASCII restano in chiaro: il file e'
                        // UTF-8 e il gioco legge UTF-8.
                        b.append(c);
                    }
            }
        }
        b.append('"');
    }

    // ------------------------------------------------------------------
    // Confronto strutturale
    // ------------------------------------------------------------------

    /**
     * Confronto profondo fra due strutture lette da JSON.
     *
     * Serve al collaudo del round-trip: leggere un salvataggio, riscriverlo,
     * rileggerlo e verificare che i due alberi coincidano campo per campo.
     * Il confronto e' sui valori, non sui byte: differenze di spaziatura o di
     * ordine nelle liste non contano, differenze di contenuto si'.
     *
     * @return null se i due alberi coincidono, altrimenti la descrizione del
     *         primo percorso in cui differiscono
     */
    @SuppressWarnings("unchecked")
    public static String confronta(Object a, Object b, String percorso) {
        if (a == null || b == null) {
            return a == b ? null : percorso + ": " + a + " contro " + b;
        }
        if (a instanceof Map && b instanceof Map) {
            Map<String, Object> ma = (Map<String, Object>) a;
            Map<String, Object> mb = (Map<String, Object>) b;
            if (ma.size() != mb.size()) {
                return percorso + ": numero di campi diverso (" + ma.size() + " contro " + mb.size() + ")";
            }
            for (Map.Entry<String, Object> e : ma.entrySet()) {
                if (!mb.containsKey(e.getKey())) {
                    return percorso + ": campo mancante in B: " + e.getKey();
                }
                String d = confronta(e.getValue(), mb.get(e.getKey()), percorso + "/" + e.getKey());
                if (d != null) {
                    return d;
                }
            }
            return null;
        }
        if (a instanceof List && b instanceof List) {
            List<Object> la = (List<Object>) a;
            List<Object> lb = (List<Object>) b;
            if (la.size() != lb.size()) {
                return percorso + ": liste di lunghezza diversa (" + la.size() + " contro " + lb.size() + ")";
            }
            for (int i = 0; i < la.size(); i++) {
                String d = confronta(la.get(i), lb.get(i), percorso + "[" + i + "]");
                if (d != null) {
                    return d;
                }
            }
            return null;
        }
        if (a instanceof Numero && b instanceof Numero) {
            Numero na = (Numero) a;
            Numero nb = (Numero) b;
            if (na.equals(nb)) {
                return null;
            }
            // Un confronto numerico, non testuale: 1 contro 1.0 resta una
            // differenza reale, ma va segnalata come numerica e non come testo.
            return percorso + ": numero diverso (" + na + " contro " + nb + ")";
        }
        return a.equals(b) ? null : percorso + ": " + riassunto(a) + " contro " + riassunto(b);
    }

    private static String riassunto(Object o) {
        String s = String.valueOf(o);
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }
}

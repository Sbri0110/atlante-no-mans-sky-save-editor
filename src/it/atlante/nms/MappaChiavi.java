package it.atlante.nms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mappa fra le chiavi cifrate del salvataggio e i nomi delle proprieta'.
 *
 * No Man's Sky non scrive i nomi dei campi: scrive terne di tre caratteri
 * prodotte da un hash del nome. Un salvataggio contiene quindi
 * {@code {"F2P":4737}} invece di {@code {"Version":4737}}.
 *
 * La corrispondenza fra terna e nome e' un dato, non un algoritmo: le terne
 * sono calcolabili, i nomi si scoprono leggendo i salvataggi. Il file
 * {@code jsonmap.txt} e' quell'elenco, una voce per riga nel formato
 * {@code terna&lt;TAB&gt;NomeProprieta}.
 *
 * La direzione nome &rarr; terna serve quando si scrive: un campo nuovo va
 * rimesso nel file con la sua terna. Finche' il nome e' noto basta la mappa
 * inversa; per un nome mai visto serve calcolare l'hash (vedi
 * {@code CifraturaNomi}, ancora da scrivere).
 */
public final class MappaChiavi {

    private final Map<String, String> perChiave = new LinkedHashMap<String, String>();
    private final Map<String, String> perNome = new LinkedHashMap<String, String>();

    private MappaChiavi() {
    }

    public static MappaChiavi carica(Path percorso) throws IOException {
        InputStream in = Files.newInputStream(percorso);
        try {
            return carica(in);
        } finally {
            in.close();
        }
    }

    public static MappaChiavi carica(InputStream in) throws IOException {
        MappaChiavi mappa = new MappaChiavi();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
        String riga;
        while ((riga = r.readLine()) != null) {
            int tab = riga.indexOf('\t');
            if (tab <= 0) {
                continue;
            }
            String chiave = riga.substring(0, tab).trim();
            String nome = riga.substring(tab + 1).trim();
            if (chiave.isEmpty() || nome.isEmpty()) {
                continue;
            }
            mappa.perChiave.put(chiave, nome);
            // Se un nome comparisse con due terne diverse vince la prima: e'
            // il comportamento del file di riferimento.
            if (!mappa.perNome.containsKey(nome)) {
                mappa.perNome.put(nome, chiave);
            }
        }
        return mappa;
    }

    public int dimensione() {
        return perChiave.size();
    }

    /** Nome della proprieta' a partire dalla terna, o null se sconosciuta. */
    public String nome(String chiave) {
        return perChiave.get(chiave);
    }

    /** Terna a partire dal nome, o null se il nome non e' nella mappa. */
    public String chiave(String nome) {
        return perNome.get(nome);
    }

    // ------------------------------------------------------------------
    // Traduzione dell'albero
    // ------------------------------------------------------------------

    /** Esito della traduzione di un albero, per il resoconto a video. */
    public static final class Esito {
        public int tradotte;
        public int ignote;
        public final List<String> esempiIgnote = new ArrayList<String>();

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(tradotte).append(" chiavi riconosciute, ").append(ignote).append(" ignote");
            if (!esempiIgnote.isEmpty()) {
                b.append(" (").append(esempiIgnote).append(')');
            }
            return b.toString();
        }
    }

    /**
     * Sostituisce le terne con i nomi leggibili, in profondita'.
     *
     * L'ordine dei campi viene conservato. Le chiavi sconosciute restano come
     * sono: sono le proprieta' introdotte da un aggiornamento del gioco e non
     * ancora mappate, e vanno mostrate invece che perse.
     */
    public Esito traduci(Object nodo) {
        Esito esito = new Esito();
        traduciIn(nodo, esito);
        return esito;
    }

    @SuppressWarnings("unchecked")
    private void traduciIn(Object nodo, Esito esito) {
        if (nodo instanceof Map) {
            Map<String, Object> mappa = (Map<String, Object>) nodo;
            Map<String, Object> nuovo = new LinkedHashMap<String, Object>(mappa.size() * 2);
            for (Map.Entry<String, Object> e : mappa.entrySet()) {
                String nome = perChiave.get(e.getKey());
                if (nome == null) {
                    esito.ignote++;
                    if (esito.esempiIgnote.size() < 12) {
                        esito.esempiIgnote.add(e.getKey());
                    }
                    nome = e.getKey();
                } else {
                    esito.tradotte++;
                }
                traduciIn(e.getValue(), esito);
                nuovo.put(nome, e.getValue());
            }
            mappa.clear();
            mappa.putAll(nuovo);
        } else if (nodo instanceof List) {
            List<Object> lista = (List<Object>) nodo;
            for (Object x : lista) {
                traduciIn(x, esito);
            }
        }
    }

    /**
     * Operazione inversa: rimette le terne al posto dei nomi, per riscrivere
     * il salvataggio nel formato che il gioco si aspetta.
     */
    public Esito cifra(Object nodo) {
        Esito esito = new Esito();
        cifraIn(nodo, esito);
        return esito;
    }

    @SuppressWarnings("unchecked")
    private void cifraIn(Object nodo, Esito esito) {
        if (nodo instanceof Map) {
            Map<String, Object> mappa = (Map<String, Object>) nodo;
            Map<String, Object> nuovo = new LinkedHashMap<String, Object>(mappa.size() * 2);
            for (Map.Entry<String, Object> e : mappa.entrySet()) {
                String chiave = perNome.get(e.getKey());
                if (chiave == null) {
                    esito.ignote++;
                    if (esito.esempiIgnote.size() < 12) {
                        esito.esempiIgnote.add(e.getKey());
                    }
                    chiave = e.getKey();
                } else {
                    esito.tradotte++;
                }
                cifraIn(e.getValue(), esito);
                nuovo.put(chiave, e.getValue());
            }
            mappa.clear();
            mappa.putAll(nuovo);
        } else if (nodo instanceof List) {
            List<Object> lista = (List<Object>) nodo;
            for (Object x : lista) {
                cifraIn(x, esito);
            }
        }
    }
}

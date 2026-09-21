package it.atlante.finestra;

import it.atlante.json.Json;

import java.util.List;
import java.util.Map;

/**
 * Le azioni rapide: quello che nel vecchio editor si faceva con un pulsante.
 *
 * Sono le funzioni che distinguono un editor da un visualizzatore. Non sono
 * scorciatoie che scrivono alla cieca: ognuna percorre il salvataggio, conta
 * quante cose ha trovato e riferisce il risultato, cosi' chi la usa sa cosa e'
 * cambiato prima di salvare.
 *
 * Ogni azione lavora sull'albero in memoria. La scrittura su disco avviene solo
 * con Salva, e passa comunque per la verifica del giro completo.
 */
public final class Azioni {

    /** Cosa ha prodotto un'azione, per il resoconto a video. */
    public static final class Esito {
        public int toccati;
        public String descrizione;

        Esito(int toccati, String descrizione) {
            this.toccati = toccati;
            this.descrizione = descrizione;
        }

        @Override
        public String toString() {
            return descrizione + " (" + toccati + ")";
        }
    }

    private Azioni() {
    }

    /**
     * Riporta a zero il danno di ogni slot di ogni inventario.
     *
     * Nel salvataggio ogni slot ha un campo {@code DamageFactor}: a zero lo
     * slot e' integro, sopra zero e' danneggiato e va riparato nel gioco
     * pagando. Azzerarli tutti equivale a riparare tutto.
     *
     * Si cercano i campi per nome invece di elencare gli inventari: cosi'
     * l'azione copre anche i contenitori aggiunti da un aggiornamento, che non
     * si conoscono in anticipo.
     */
    @SuppressWarnings("unchecked")
    public static Esito riparaTutto(Object albero) {
        int[] contatore = new int[1];
        percorri(albero, new Visitatore() {
            public void mappa(Map<String, Object> m) {
                Object danno = m.get("DamageFactor");
                if (danno instanceof Json.Numero) {
                    double valore = ((Json.Numero) danno).comeDouble();
                    if (valore > 0.0) {
                        m.put("DamageFactor", new Json.Numero("0.0"));
                        contatore[0]++;
                    }
                }
            }
        });
        return new Esito(contatore[0], contatore[0] == 0
                ? "Nessuno slot danneggiato"
                : "Slot riparati");
    }

    /**
     * Imposta un campo numerico ovunque compaia con quel nome.
     *
     * Serve per i valori del giocatore: {@code Units}, {@code Nanites},
     * {@code Specials}. Si agisce sia sul contesto principale sia su quello
     * della spedizione, altrimenti il valore tornerebbe indietro al primo
     * cambio di contesto.
     */
    public static Esito impostaNumero(Object albero, String campo, String valore) {
        final int[] contatore = new int[1];
        final String nome = campo;
        final String nuovo = valore;
        percorri(albero, new Visitatore() {
            public void mappa(Map<String, Object> m) {
                Object attuale = m.get(nome);
                if (attuale instanceof Json.Numero) {
                    m.put(nome, new Json.Numero(nuovo));
                    contatore[0]++;
                }
            }
        });
        return new Esito(contatore[0], contatore[0] == 0
                ? "Campo non trovato: " + campo
                : "Impostato " + campo + " a " + valore);
    }

    /**
     * Aggiunge un valore a un campo numerico invece di sostituirlo.
     *
     * Piu' sicuro per le risorse: chi ha gia' 800 milioni di unita' non vuole
     * scendere a un numero piu' basso perche' ha scritto 1000.
     */
    public static Esito aggiungiNumero(Object albero, String campo, String incremento) {
        final int[] contatore = new int[1];
        final String nome = campo;
        final double quanto = Double.parseDouble(incremento);
        percorri(albero, new Visitatore() {
            public void mappa(Map<String, Object> m) {
                Object attuale = m.get(nome);
                if (attuale instanceof Json.Numero) {
                    Json.Numero n = (Json.Numero) attuale;
                    if (n.intero()) {
                        long v = n.comeLong();
                        m.put(nome, new Json.Numero(String.valueOf(v + (long) quanto)));
                        contatore[0]++;
                    } else {
                        double v = n.comeDouble();
                        m.put(nome, new Json.Numero(String.valueOf(v + quanto)));
                        contatore[0]++;
                    }
                }
            }
        });
        return new Esito(contatore[0], contatore[0] == 0
                ? "Campo non trovato: " + campo
                : "Aggiunto " + incremento + " a " + campo);
    }

    /**
     * Ricarica completamente le tecnologie.
     *
     * Nel salvataggio ogni tecnologia installata ha un campo
     * {@code ChargeValue} (o {@code Amount} per gli oggetti impilabili). Si
     * portano tutti al massimo dichiarato dal campo {@code MaxAmount} della
     * stessa voce, quando c'e'.
     */
    @SuppressWarnings("unchecked")
    public static Esito ricaricaTutto(Object albero) {
        final int[] contatore = new int[1];
        percorri(albero, new Visitatore() {
            public void mappa(Map<String, Object> m) {
                Object carica = m.get("ChargeValue");
                if (carica instanceof Json.Numero) {
                    Object massimo = m.get("MaxAmount");
                    String nuovo = massimo instanceof Json.Numero
                            ? ((Json.Numero) massimo).testo() : "100.0";
                    if (!((Json.Numero) carica).testo().equals(nuovo)) {
                        m.put("ChargeValue", new Json.Numero(nuovo));
                        contatore[0]++;
                    }
                }
            }
        });
        return new Esito(contatore[0], contatore[0] == 0
                ? "Nessuna tecnologia da ricaricare"
                : "Tecnologie ricaricate");
    }

    // ------------------------------------------------------------------

    /** Riceve le mappe incontrate durante la visita. */
    private interface Visitatore {
        void mappa(Map<String, Object> m);
    }

    /**
     * Percorre tutto l'albero, mappe e liste.
     *
     * La profondita' e' limitata: un salvataggio puo' essere profondo e una
     * ricorsione senza limite rischia di esaurire lo stack.
     */
    @SuppressWarnings("unchecked")
    private static void percorri(Object nodo, Visitatore visitatore) {
        percorri(nodo, visitatore, 0);
    }

    @SuppressWarnings("unchecked")
    private static void percorri(Object nodo, Visitatore visitatore, int profondita) {
        if (profondita > 40 || nodo == null) {
            return;
        }
        if (nodo instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) nodo;
            visitatore.mappa(m);
            for (Object v : m.values()) {
                percorri(v, visitatore, profondita + 1);
            }
        } else if (nodo instanceof List) {
            for (Object v : (List<Object>) nodo) {
                percorri(v, visitatore, profondita + 1);
            }
        }
    }
}

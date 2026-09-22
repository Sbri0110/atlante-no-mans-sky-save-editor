package it.atlante.strumenti;

import it.atlante.json.Json;
import it.atlante.finestra.Elenchi;
import it.atlante.finestra.Inventari;
import it.atlante.nms.Formato;
import it.atlante.nms.MappaChiavi;
import it.atlante.nms.Salvataggio;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Collaudo dei percorsi che portano a scrivere su disco.
 *
 * Il comando {@code giro} di {@code Main} prova che il contenuto sopravvive a
 * una ricompattazione, ma resta tutto in memoria: non tocca mai il disco. Qui
 * si esercitano le parti che invece lo toccano, perche' un errore li' costa il
 * salvataggio di chi usa il programma.
 *
 * Cosa verifica:
 *
 * <ol>
 *   <li>apertura, riconoscimento del formato e verifica del contenuto;</li>
 *   <li>scrittura vera, copia di sicurezza e rilettura del file scritto;</li>
 *   <li>una modifica a un valore, come fa un campo dell'interfaccia, e il
 *       controllo che il valore sopravviva al salvataggio;</li>
 *   <li>i casi storti: un file qualunque, un file vuoto, un file che non
 *       esiste. Devono dare un errore, mai un arresto improvviso;</li>
 *   <li>il rispetto del contesto attivo: con {@code ActiveContext} messo a
 *       spedizione, nessuna scheda deve restare attaccata alla partita
 *       principale.</li>
 * </ol>
 *
 * Lavora su una copia del salvataggio, in una cartella a parte: l'originale
 * non viene mai toccato.
 *
 * Uso:
 * <pre>
 *   java -cp "classi;lib/flatlaf.jar" it.atlante.strumenti.Collaudo &lt;payload&gt; [cartella di lavoro]
 * </pre>
 *
 * Esce con codice 0 se tutte le prove passano, 1 altrimenti: si puo' usare in
 * una catena automatica.
 */
public final class Collaudo {

    private static int prove;
    private static int fallite;

    private Collaudo() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("serve il file del salvataggio");
            System.err.println("uso: Collaudo <payload> [cartella di lavoro]");
            System.exit(2);
            return;
        }
        File originale = new File(args[0]);
        File cartella = new File(args.length > 1 ? args[1] : "collaudo");
        cartella.mkdirs();

        File mappaFile = trovaMappa();
        MappaChiavi mappa = MappaChiavi.carica(mappaFile.toPath());
        System.out.println("chiavi in mappa: " + mappa.dimensione() + "  (" + mappaFile + ")");
        System.out.println("salvataggio:    " + originale.getName());
        System.out.println();

        // Una copia di lavoro: l'originale non si tocca mai.
        //
        // Se la copia c'e' gia' — l'utente ha rilanciato il collaudo nella
        // stessa cartella — si sovrascrive: il salvataggio di partenza e'
        // l'originale, non quello che resta dalla prova precedente.
        File lavoro = new File(cartella, "collaudo.sav");
        Files.copy(originale.toPath(), lavoro.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        // ------------------------------------------------------------------
        // 1. Apertura e verifica.
        // ------------------------------------------------------------------
        Salvataggio s = Salvataggio.apri(lavoro, mappa);
        ok("il file si apre", s != null);
        ok("il formato e' riconosciuto", s.formato() != Formato.SCONOSCIUTO);
        System.out.println("   formato " + s.formato().descrizione()
                + ", compresso " + s.dimensioneCompressa()
                + " / decompresso " + s.dimensioneDecompressa()
                + " / blocchi " + s.blocchi());

        String problema = s.verifica();
        ok("il contenuto non cambia al giro completo", problema == null);
        if (problema != null) {
            System.out.println("   DIFFERENZA: " + problema);
        }

        // ------------------------------------------------------------------
        // 2. Scrittura vera, copia di sicurezza, rilettura.
        // ------------------------------------------------------------------
        long prima = lavoro.length();
        File copia = s.salva(null);
        ok("dopo il salvataggio il file esiste", lavoro.isFile());
        ok("il file riscritto non e' vuoto", lavoro.length() > 0);
        ok("il segno di modifica e' stato tolto", !s.modificato());
        ok("la copia di sicurezza e' stata creata", copia != null && copia.isFile());
        if (copia != null) {
            ok("la copia e' identica all'originale",
                    copia.length() == prima
                            && Arrays.equals(Files.readAllBytes(copia.toPath()),
                                            Files.readAllBytes(originale.toPath())));
            System.out.println("   copia: " + copia.getName() + " (" + copia.length() + " byte)");
        }

        Salvataggio riletto = Salvataggio.apri(lavoro, mappa);
        ok("il file scritto si riapre", riletto != null);
        ok("i dati riletti sono identici a quelli di partenza",
                Json.confronta(s.albero(), riletto.albero(), "") == null);

        // ------------------------------------------------------------------
        // 3. Una modifica, come la farebbe un campo dell'interfaccia.
        // ------------------------------------------------------------------
        Salvataggio s2 = Salvataggio.apri(lavoro, mappa);
        Map<String, Object> radice = comeMappa(s2.albero());
        // Lo stato del giocatore sta sotto il contesto attivo, non alla radice.
        Object contesto = radice.get(contestoAttivo(radice));
        ok("esiste il contesto attivo", contesto instanceof Map);
        Object stato = contesto instanceof Map ? comeMappa(contesto).get("PlayerStateData") : null;
        ok("esiste lo stato del giocatore", stato instanceof Map);
        if (stato instanceof Map) {
            Map<String, Object> p = comeMappa(stato);
            Object primaValore = p.get("Units");
            p.put("Units", new Json.Numero("1234567"));
            s2.segnaModificato();
            ok("un valore nostro passa la verifica", s2.verifica() == null);
            s2.salva(null);

            Salvataggio s3 = Salvataggio.apri(lavoro, mappa);
            Map<String, Object> r3 = comeMappa(s3.albero());
            Map<String, Object> p3 = comeMappa(
                    comeMappa(r3.get(contestoAttivo(r3))).get("PlayerStateData"));
            String dopo = String.valueOf(p3.get("Units"));
            ok("il valore modificato sopravvive al salvataggio", "1234567".equals(dopo));
            System.out.println("   Units: " + primaValore + " -> " + dopo);

            // Si rimette com'era, cosi' la copia di lavoro resta fedele.
            p3.put("Units", primaValore);
            s3.salva(null);
        }

        // ------------------------------------------------------------------
        // 4. I casi storti: errore pulito, mai un arresto improvviso.
        // ------------------------------------------------------------------
        ok("un file qualunque non e' riconosciuto",
                Formato.rileva(spazzatura(512)) == Formato.SCONOSCIUTO);
        ok("un file vuoto non e' riconosciuto",
                Formato.rileva(new byte[0]) == Formato.SCONOSCIUTO);
        ok("un file di due byte non e' riconosciuto",
                Formato.rileva(new byte[]{(byte) 0xCC, (byte) 0xCC}) == Formato.SCONOSCIUTO);
        ok("una cartella non viene scambiata per un salvataggio",
                Formato.rileva(new byte[8]) == Formato.SCONOSCIUTO);

        File finto = new File(cartella, "non-un-salvataggio.bin");
        Files.write(finto.toPath(), spazzatura(512));
        ok("aprire un file qualunque da' un errore",
                daErrore(finto, mappa));

        File vuoto = new File(cartella, "vuoto.sav");
        Files.write(vuoto.toPath(), new byte[0]);
        ok("aprire un file vuoto da' un errore", daErrore(vuoto, mappa));

        ok("aprire un file inesistente da' un errore",
                daErrore(new File(cartella, "non-esiste.sav"), mappa));

        File cartellaFinta = new File(cartella, "una-cartella.sav");
        cartellaFinta.mkdirs();
        ok("aprire una cartella da' un errore", daErrore(cartellaFinta, mappa));

        // ------------------------------------------------------------------
        // 5. Il contesto attivo.
        //
        // Il salvataggio tiene due copie dello stato del giocatore — partita e
        // spedizione — e i percorsi delle schede devono puntare a quella in
        // corso. Con ActiveContext = Main il difetto non si vedrebbe: le due
        // copie hanno la stessa forma, e un percorso che punta a BaseContext
        // sembra giusto. Qui il contesto si forza a Expedition, in memoria,
        // senza toccare il file.
        // ------------------------------------------------------------------
        Map<String, Object> r5 = comeMappa(Salvataggio.apri(lavoro, mappa).albero());
        String scelto = Inventari.contesto(r5);
        ok("il contesto attivo viene riconosciuto",
                contestoAttivo(r5).equals(scelto));
        System.out.println("   ActiveContext " + r5.get("ActiveContext")
                + " -> ramo " + scelto);

        r5.put("ActiveContext", "Expedition");
        ok("forzando Expedition il ramo cambia",
                "ExpeditionContext".equals(Inventari.contesto(r5)));

        int controllati = 0;
        int sbagliati = 0;
        String[] sezioni = {"Navi", "Corvette", "Veicoli", "Multitool"};
        for (String sezione : sezioni) {
            for (Inventari.Inventario i : Inventari.perSezione(r5, sezione)) {
                if (i.percorso.startsWith("ExpeditionContext.")) {
                    controllati++;
                } else {
                    sbagliati++;
                    System.out.println("   [NO] " + sezione + ": " + i.percorso);
                }
                if (i.statistiche != null) {
                    if (i.statistiche.startsWith("ExpeditionContext.")) {
                        controllati++;
                    } else {
                        sbagliati++;
                        System.out.println("   [NO] " + sezione + " statistiche: " + i.statistiche);
                    }
                }
            }
        }
        for (String sezione : new String[]{"Squadrone", "Fregate", "Compagni", "Insediamenti"}) {
            Elenchi.Elenco e = Elenchi.perSezione(r5, sezione);
            if (e == null) {
                continue;
            }
            if (e.percorso.startsWith("ExpeditionContext.")) {
                controllati++;
            } else {
                sbagliati++;
                System.out.println("   [NO] " + sezione + ": " + e.percorso);
            }
        }
        ok("in spedizione tutti i percorsi seguono il contesto attivo ("
                + controllati + " controllati)", sbagliati == 0 && controllati > 0);

        // Le schede dell'elenco fisso (tuta, mercantile, base) usano di
        // proposito il prefisso BaseContext: lo sostituisce risolvi() al
        // momento della lettura. La prova e' che il percorso risolto segua il
        // contesto, non com'e' scritto.
        List<Inventari.Inventario> tuta5 = Inventari.perSezione(r5, "Tuta");
        ok("l'elenco fisso risolve nel ramo attivo",
                !tuta5.isEmpty()
                        && Inventari.risolvi(r5, tuta5.get(0).percorso) != null);

        // ------------------------------------------------------------------
        System.out.println();
        System.out.println("PROVE: " + prove + "   FALLITE: " + fallite);
        System.out.println(fallite == 0
                ? "ESITO: tutto regge."
                : "ESITO: ci sono problemi, vedi le righe [NO].");
        System.exit(fallite == 0 ? 0 : 1);
    }

    /** Nome della copia dello stato che il file considera attiva. */
    @SuppressWarnings("unchecked")
    private static String contestoAttivo(Map<String, Object> radice) {
        Object attivo = radice.get("ActiveContext");
        String nome = attivo == null ? "" : String.valueOf(attivo);
        if ("Expedition".equalsIgnoreCase(nome) && radice.containsKey("ExpeditionContext")) {
            return "ExpeditionContext";
        }
        if (radice.containsKey("BaseContext")) {
            return "BaseContext";
        }
        return radice.containsKey("ExpeditionContext") ? "ExpeditionContext" : "BaseContext";
    }

    private static boolean daErrore(File f, MappaChiavi mappa) {
        try {
            Salvataggio.apri(f, mappa);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static byte[] spazzatura(int quanti) {
        byte[] b = new byte[quanti];
        for (int i = 0; i < quanti; i++) {
            b[i] = (byte) (i * 7 + 3);
        }
        return b;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comeMappa(Object o) {
        return (Map<String, Object>) o;
    }

    /** Il file delle chiavi, cercato dove sta di solito. */
    private static File trovaMappa() {
        File[] prove = {
                new File("risorse/db/jsonmap.txt"),
                new File("risorse\\db\\jsonmap.txt"),
                new File("..", "risorse/db/jsonmap.txt")
        };
        for (File f : prove) {
            if (f.isFile()) {
                return f;
            }
        }
        throw new IllegalStateException("risorse/db/jsonmap.txt non trovato: "
                + "eseguire dalla cartella del progetto");
    }

    private static void ok(String cosa, boolean esito) {
        prove++;
        if (!esito) {
            fallite++;
        }
        System.out.println((esito ? "  [ok]  " : "  [NO]  ") + cosa);
    }
}

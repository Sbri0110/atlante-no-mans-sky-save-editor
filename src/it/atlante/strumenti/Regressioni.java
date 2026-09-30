package it.atlante.strumenti;

import it.atlante.io.FlussoBlocchi;
import it.atlante.io.Lz4;
import it.atlante.json.Json;
import it.atlante.nms.ContenitoreWgs;
import it.atlante.nms.Formato;
import it.atlante.nms.MappaChiavi;
import it.atlante.nms.Salvataggio;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/** Prove isolate: non accede ai salvataggi dell'utente. */
public final class Regressioni {
    private static int prove;

    private interface Azione { void esegui() throws Exception; }

    private static void ok(boolean condizione, String nome) {
        if (!condizione) throw new AssertionError(nome);
        prove++;
    }

    private static void rifiuta(Azione azione, Class<? extends Exception> tipo) throws Exception {
        try { azione.esegui(); } catch (Exception e) {
            ok(tipo.isInstance(e), "errore previsto " + tipo + ", ricevuto " + e);
            return;
        }
        throw new AssertionError("dato non valido accettato");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (final String n : new String[]{"+1", "01", "-", "1.", "1e", "1-2", "NaN", "Infinity"}) {
            rifiuta(new Azione() { public void esegui() { Json.leggi(n); } }, IllegalArgumentException.class);
            rifiuta(new Azione() { public void esegui() { new Json.Numero(n); } }, IllegalArgumentException.class);
        }
        for (final String n : new String[]{"0", "-0", "18446744073709551615", "0.6000000238418579", "1e+10"}) {
            ok(n.equals(Json.scrivi(Json.leggi(n))), "precisione numerica");
        }
        for (final String j : new String[]{"{\"a\":1,\"a\":2}", "\"a\nb\"", "\"\\u+001\""}) {
            rifiuta(new Azione() { public void esegui() { Json.leggi(j); } }, IllegalArgumentException.class);
        }
        ok(Formato.rileva("{}".getBytes(StandardCharsets.UTF_8)) == Formato.JSON_CHIARO, "JSON corto");
        for (final byte[] b : new byte[][]{{(byte) 0xf0}, {0x1f, 65, 1, 0}, {0x1f, 65, 1, 0, (byte) 255}}) {
            rifiuta(new Azione() { public void esegui() { Lz4.scompatta(b, 0); } }, IllegalArgumentException.class);
        }
        rifiuta(new Azione() { public void esegui() { Lz4.scompatta(new byte[]{0x20, 65, 66}, 1); } }, IllegalArgumentException.class);
        Random casuale = new Random(7);
        for (int n : new int[]{0, 1, 12, 13, 255, 524288, 524289}) {
            byte[] dati = new byte[n];
            casuale.nextBytes(dati);
            ok(Arrays.equals(dati, Lz4.scompatta(Lz4.compatta(dati), n)), "LZ4 casuale " + n);
            Arrays.fill(dati, (byte) 65);
            ok(Arrays.equals(dati, Lz4.scompatta(Lz4.compatta(dati), n)), "LZ4 ripetizioni " + n);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            FlussoBlocchi.scrivi(out, dati, null);
            ok(Arrays.equals(dati, FlussoBlocchi.leggi(new ByteArrayInputStream(out.toByteArray()), null)), "flusso " + n);
        }
        final byte[] header = new byte[17];
        System.arraycopy(FlussoBlocchi.MAGIC, 0, header, 0, 4);
        header[4] = 1;
        header[11] = 0x7f;
        rifiuta(new Azione() { public void esegui() throws Exception {
            FlussoBlocchi.leggi(new ByteArrayInputStream(header), null);
        } }, IOException.class);

        Path cartella = Files.createTempDirectory(new File("collaudo").toPath(), "regressioni-");
        final MappaChiavi mappa = MappaChiavi.carica(new File("risorse/db/jsonmap.txt").toPath());
        Path originale = cartella.resolve("collaudo.sav");
        byte[] iniziale = "{\"F2P\":4737,\"Units\":10}".getBytes(StandardCharsets.UTF_8);
        Files.write(originale, iniziale);
        final Salvataggio s = Salvataggio.apri(originale.toFile(), mappa);
        Map<String, Object> radice = (Map<String, Object>) s.albero();
        radice.put("Units", new Json.Numero("20"));
        s.segnaModificato();
        Path esportazione = cartella.resolve("copia.sav");
        s.salvaCome(esportazione.toFile());
        ok(s.modificato(), "esportazione conserva modifiche pendenti");
        ok(Arrays.equals(iniziale, Files.readAllBytes(originale)), "esportazione conserva originale");
        ok(Json.confronta(s.albero(), Salvataggio.apri(esportazione.toFile(), mappa).albero(), "") == null, "copia esportata");
        File backup = s.salva(null);
        ok(Arrays.equals(iniziale, Files.readAllBytes(backup.toPath())), "backup non scambiato per metadati");
        radice.put("Units", new Json.Numero("30"));
        s.segnaModificato();
        byte[] prima = Files.readAllBytes(originale);
        s.salvaCome(originale.toFile());
        ok(!s.modificato(), "salva con nome sullo stesso file");
        boolean copiaTrovata = false;
        for (File f : cartella.toFile().listFiles()) {
            if (f.getName().endsWith(".bak") && Arrays.equals(prima, Files.readAllBytes(f.toPath()))) copiaTrovata = true;
        }
        ok(copiaTrovata, "stesso file crea backup");
        radice.put("F2P", new Json.Numero("999"));
        String snapshot = Json.scrivi(radice);
        ok(s.verifica() != null, "collisione tra chiave leggibile e cifrata");
        ok(snapshot.equals(Json.scrivi(radice)), "verifica fallita non altera albero");
        radice.remove("F2P");
        final Path errato = cartella.resolve("errato.sav");
        Files.write(errato, "{\"a\":1e}".getBytes(StandardCharsets.UTF_8));
        rifiuta(new Azione() { public void esegui() throws Exception { Salvataggio.apri(errato.toFile(), mappa); } }, IOException.class);

        Path contenitore = Files.createDirectory(cartella.resolve("0123456789ABCDEF0123456789ABCDEF"));
        Path payload = contenitore.resolve("11111111111111111111111111111111");
        Path meta = contenitore.resolve("22222222222222222222222222222222");
        byte[] compresso = Formato.BLOCCHI.scrivi(iniziale, null);
        Files.write(payload, compresso);
        Files.write(meta, new byte[24]);
        Path vecchio = contenitore.resolve("11111111111111111111111111111111.bak");
        Files.write(vecchio, compresso);
        ok(ContenitoreWgs.elenca(cartella.toFile()).get(0).payload.equals(payload.toFile()), "rilevatore ignora backup");
        ContenitoreWgs.aggiornaContenitore(payload.toFile(), 123);
        ok(Files.readAllBytes(meta)[16] == 123, "metadati WGS aggiornati");
        ok(Arrays.equals(compresso, Files.readAllBytes(vecchio)), "backup WGS intatto");
        System.out.println("PROVE REGRESSIONE: " + prove + "   FALLITE: 0");
        System.out.println("File di prova: " + cartella);
    }
}

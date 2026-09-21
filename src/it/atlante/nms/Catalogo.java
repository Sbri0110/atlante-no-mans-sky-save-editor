package it.atlante.nms;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Il catalogo degli oggetti di gioco.
 *
 * Il database {@code items.xml} associa a ogni identificatore del salvataggio
 * il nome mostrato nel gioco, l'icona, la categoria e la descrizione:
 *
 * <pre>
 *   &lt;product category="COMPONENT" icon="PRODUCT-CASING.PNG" id="^CASING"
 *            multiplier="2" name="Metal Plating" subtitle="Crafted Technology Component"&gt;
 * </pre>
 *
 * Senza questo catalogo un editor puo' mostrare solo {@code ^CASING}; con il
 * catalogo mostra l'icona dell'oggetto e il suo nome vero. E' la differenza fra
 * un albero di sigle e un editor che si capisce.
 *
 * Misure sul database in dotazione: 5.240 voci fra prodotti, sostanze,
 * tecnologie e moduli procedurali.
 *
 * La lettura usa SAX, il lettore XML incluso nella JVM: il file e' di 2,2 MB e
 * un parser a eventi lo attraversa una volta sola senza tenerlo in memoria.
 */
public final class Catalogo {

    /** Una voce del catalogo. */
    public static final class Voce {
        public final String id;
        public final String nome;
        public final String sottotitolo;
        public final String categoria;
        public final String icona;
        public final String descrizione;
        public final String tipo;

        Voce(String id, String nome, String sottotitolo, String categoria,
             String icona, String descrizione, String tipo) {
            this.id = id;
            this.nome = nome;
            this.sottotitolo = sottotitolo;
            this.categoria = categoria;
            this.icona = icona;
            this.descrizione = descrizione;
            this.tipo = tipo;
        }

        /** Nome leggibile, con ripiego sull'identificatore. */
        public String etichetta() {
            return nome != null && !nome.isEmpty() ? nome : id;
        }

        @Override
        public String toString() {
            return etichetta();
        }
    }

    private final Map<String, Voce> perId = new LinkedHashMap<String, Voce>();
    private final List<String> categorie = new ArrayList<String>();

    private Catalogo() {
    }

    public static Catalogo carica(File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("catalogo non trovato: " + file);
        }
        Catalogo catalogo = new Catalogo();
        try {
            SAXParserFactory fabbrica = SAXParserFactory.newInstance();
            fabbrica.setNamespaceAware(false);
            fabbrica.setValidating(false);
            // Il file dichiara una codifica UTF-8 e non ha DTD esterni.
            fabbrica.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            SAXParser parser = fabbrica.newSAXParser();
            parser.parse(file, new Lettore(catalogo));
        } catch (SAXException e) {
            throw new IOException("catalogo illeggibile: " + e.getMessage(), e);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new IOException("parser XML non disponibile", e);
        }
        catalogo.raccogliCategorie();
        return catalogo;
    }

    /** Catalogo vuoto: l'editor funziona anche senza, mostrando gli identificatori. */
    public static Catalogo vuoto() {
        return new Catalogo();
    }

    public int dimensione() {
        return perId.size();
    }

    public List<String> categorie() {
        return Collections.unmodifiableList(categorie);
    }

    public Voce voce(String id) {
        return id == null ? null : perId.get(id);
    }

    /** Vero se l'identificatore e' noto al catalogo. */
    public boolean conosciuto(String id) {
        return id != null && perId.containsKey(id);
    }

    /** Cerca per nome o identificatore: serve alla ricerca dell'interfaccia. */
    public List<Voce> cerca(String testo, int massimo) {
        List<Voce> trovate = new ArrayList<Voce>();
        if (testo == null || testo.trim().isEmpty()) {
            return trovate;
        }
        String cercato = testo.trim().toLowerCase();
        for (Voce v : perId.values()) {
            String nome = v.nome == null ? "" : v.nome.toLowerCase();
            String id = v.id.toLowerCase();
            if (nome.contains(cercato) || id.contains(cercato)) {
                trovate.add(v);
                if (trovate.size() >= massimo) {
                    break;
                }
            }
        }
        return trovate;
    }

    private void raccogliCategorie() {
        List<String> viste = new ArrayList<String>();
        for (Voce v : perId.values()) {
            if (v.categoria != null && !v.categoria.isEmpty() && !viste.contains(v.categoria)) {
                viste.add(v.categoria);
            }
        }
        Collections.sort(viste);
        categorie.addAll(viste);
    }

    // ------------------------------------------------------------------

    private static final class Lettore extends DefaultHandler {

        /**
         * I soli elementi che <b>definiscono</b> un oggetto.
         *
         * Nel database l'attributo {@code id} compare anche su elementi che si
         * limitano a <em>citare</em> un oggetto: le 2.887 formule di crafting
         * ({@code <requirement id="^FUEL2" quantity="1">}) e i riferimenti
         * incrociati. Trattando ogni {@code id} come una definizione, l'ultimo
         * riferimento sovrascriveva la voce vera con una priva di nome, icona e
         * descrizione: un oggetto su cui si poteva ancora cliccare, ma che
         * appariva come una sigla senza immagine.
         *
         * {@code ^FUEL2} compare 51 volte, {@code ^CASING} 85: senza questo
         * elenco la maggior parte degli oggetti perdeva nome e icona.
         */
        private static final java.util.Set<String> DEFINIZIONI =
                new java.util.HashSet<String>(java.util.Arrays.asList(
                        "substance", "product", "technology", "techbox",
                        "procedural-product", "procedural-technology", "product-template"));

        private final Catalogo catalogo;
        private StringBuilder testoCorrente;
        private Voce voceCorrente;
        private boolean catturaDescrizione;

        Lettore(Catalogo catalogo) {
            this.catalogo = catalogo;
        }

        @Override
        public void startElement(String uri, String locale, String nome, Attributes attributi) {
            if (!DEFINIZIONI.contains(nome)) {
                // Elemento figlio. Si raccoglie il testo della descrizione, se
                // c'e', e non si tocca la voce in corso.
                if ("description".equals(nome) && voceCorrente != null) {
                    catturaDescrizione = true;
                    testoCorrente = new StringBuilder();
                }
                return;
            }
            String id = attributi.getValue("id");
            if (id == null || id.isEmpty()) {
                return;
            }
            voceCorrente = new Voce(
                    id,
                    attributi.getValue("name"),
                    attributi.getValue("subtitle"),
                    attributi.getValue("category"),
                    attributi.getValue("icon"),
                    null,
                    nome);
            catalogo.perId.put(id, voceCorrente);
            catturaDescrizione = false;
            testoCorrente = null;
        }

        @Override
        public void characters(char[] ch, int inizio, int quantita) {
            if (catturaDescrizione && testoCorrente != null) {
                testoCorrente.append(ch, inizio, quantita);
            }
        }

        @Override
        public void endElement(String uri, String locale, String nome) {
            if ("description".equals(nome) && catturaDescrizione) {
                catturaDescrizione = false;
                if (voceCorrente != null && testoCorrente != null) {
                    String descrizione = testoCorrente.toString().trim();
                    if (!descrizione.isEmpty()) {
                        Voce aggiornata = new Voce(voceCorrente.id, voceCorrente.nome,
                                voceCorrente.sottotitolo, voceCorrente.categoria,
                                voceCorrente.icona, descrizione, voceCorrente.tipo);
                        catalogo.perId.put(aggiornata.id, aggiornata);
                        voceCorrente = aggiornata;
                    }
                }
                testoCorrente = null;
                return;
            }
            // La chiusura dell'elemento di definizione chiude la voce.
            if (voceCorrente != null && nome.equals(voceCorrente.tipo)) {
                voceCorrente = null;
                testoCorrente = null;
                catturaDescrizione = false;
            }
        }
    }
}

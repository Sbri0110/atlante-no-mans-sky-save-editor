package it.atlante.finestra;

import it.atlante.json.Json;
import it.atlante.nms.Catalogo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Una sezione a inventario: sotto-schede, griglia degli slot e form del pezzo scelto.
 *
 * Struttura presa dal vecchio editor — si vede tutto l'inventario insieme e si
 * modifica il singolo slot accanto — aspetto e dettagli nostri:
 *
 * <ul>
 *   <li>le sotto-schede mostrano anche <b>quanti slot sono occupati</b>, cosi'
 *       si vede dove c'e' spazio senza aprirle tutte;</li>
 *   <li>quando una cosa ha piu' depositi — una nave ha stiva, tecnologie e
 *       cargo — le schede sono su due file: prima la cosa, poi i suoi depositi;</li>
 *   <li>il form mostra il pezzo scelto con la sua icona grande e la descrizione
 *       di gioco, non una fila di campi senza contesto;</li>
 *   <li>le azioni sono sul pezzo, non sull'inventario: <i>ripara</i>, <i>ricarica</i>
 *       e <i>svuota</i> agiscono su quello che hai davanti;</li>
 *   <li>il pulsante per marcare uno slot <b>super-caricato</b> sta nel form:
 *       nel gioco sono pochi e preziosi, e vanno scelti con intenzione.</li>
 * </ul>
 */
public final class PannelloInventario extends JPanel {

    private final Catalogo catalogo;
    private final Icone icone;
    private final Runnable suModifica;

    private final JPanel schedeGruppi = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
    private final JPanel schede = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
    private final JLabel contatore = new JLabel();
    private JPanel primaRiga;
    private final GrigliaSlot griglia;
    private final Modulo modulo;

    private Object radice;
    private String sezione = "";
    private List<Inventari.Inventario> inventari = new ArrayList<Inventari.Inventario>();
    private List<String> gruppi = new ArrayList<String>();
    private List<Inventari.Statistica> principali = new ArrayList<Inventari.Statistica>();
    private Inventari.Inventario corrente;
    private String gruppoCorrente;

    public PannelloInventario(Catalogo catalogo, Icone icone, Runnable suModifica) {
        this.catalogo = catalogo;
        this.icone = icone;
        this.suModifica = suModifica;

        setLayout(new BorderLayout());
        setBackground(Aspetto.PANNELLO);

        griglia = new GrigliaSlot(catalogo, icone, new GrigliaSlot.Ascoltatore() {
            public void scelto(int indice) {
                modulo.mostra(indice);
            }

            public void modificato() {
                aggiornaContatore();
                PannelloInventario.this.suModifica.run();
            }
        });

        modulo = new Modulo();

        primaRiga = new JPanel(new BorderLayout(12, 0));
        primaRiga.setOpaque(false);
        schedeGruppi.setOpaque(false);
        primaRiga.add(schedeGruppi, BorderLayout.WEST);

        // Il contatore sta sulla seconda riga: sulla prima le schede delle navi
        // occupano tutto lo spazio e il contatore finiva schiacciato a zero,
        // cioe' invisibile proprio dove serve sapere quanto spazio resta.
        JPanel secondaRiga = new JPanel(new BorderLayout(12, 0));
        secondaRiga.setOpaque(false);
        schede.setOpaque(false);
        secondaRiga.add(schede, BorderLayout.WEST);
        contatore.setFont(Aspetto.monospaziato(11, Font.PLAIN));
        contatore.setForeground(Aspetto.TESTO_DEBOLE);
        contatore.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        secondaRiga.add(contatore, BorderLayout.EAST);

        JPanel testa = new JPanel();
        testa.setLayout(new BoxLayout(testa, BoxLayout.Y_AXIS));
        testa.setBackground(Aspetto.FONDO_ALTO);
        testa.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Aspetto.BORDO),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        primaRiga.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondaRiga.setAlignmentX(Component.LEFT_ALIGNMENT);
        testa.add(primaRiga);
        testa.add(Box.createVerticalStrut(6));
        testa.add(secondaRiga);

        JPanel corpo = new JPanel(new BorderLayout());
        corpo.setBackground(Aspetto.PANNELLO);
        corpo.add(griglia, BorderLayout.CENTER);
        corpo.add(modulo, BorderLayout.WEST);

        add(testa, BorderLayout.NORTH);
        add(corpo, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------

    /** Mostra la sezione: sceglie le sotto-schede e apre la prima. */
    public void mostra(Object radice, String nomeSezione) {
        this.radice = radice;
        this.sezione = nomeSezione;
        inventari = Inventari.esistenti(radice, Inventari.perSezione(radice, nomeSezione));
        principali = Inventari.statistichePrincipali(nomeSezione);

        gruppi = new ArrayList<String>();
        for (Inventari.Inventario inv : inventari) {
            if (inv.gruppo != null && !gruppi.contains(inv.gruppo)) {
                gruppi.add(inv.gruppo);
            }
        }

        if (inventari.isEmpty()) {
            corrente = null;
            gruppoCorrente = null;
            contatore.setText("");
            schedeGruppi.removeAll();
            schede.removeAll();
            griglia.mostra(null, nomeSezione);
            modulo.svuota();
            testaAggiornata();
            return;
        }

        gruppoCorrente = gruppi.isEmpty() ? null : gruppi.get(0);
        corrente = primoDelGruppo(gruppoCorrente);
        disegnaSchede();
        apri(corrente);
    }

    private Inventari.Inventario primoDelGruppo(String gruppo) {
        for (Inventari.Inventario inv : inventari) {
            if (gruppo == null ? inv.gruppo == null : gruppo.equals(inv.gruppo)) {
                return inv;
            }
        }
        return inventari.get(0);
    }

    /** Ridisegna le due file di schede: le cose e i loro depositi. */
    private void disegnaSchede() {
        schedeGruppi.removeAll();
        if (!gruppi.isEmpty()) {
            javax.swing.ButtonGroup gruppo = new javax.swing.ButtonGroup();
            for (final String nome : gruppi) {
                JToggleButton b = new JToggleButton(nome);
                b.setFont(b.getFont().deriveFont(12f));
                b.setSelected(nome.equals(gruppoCorrente));
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        gruppoCorrente = nome;
                        corrente = primoDelGruppo(nome);
                        disegnaSchede();
                        apri(corrente);
                    }
                });
                gruppo.add(b);
                schedeGruppi.add(b);
            }
        }
        schedeGruppi.setVisible(!gruppi.isEmpty());
        if (primaRiga != null) {
            primaRiga.setVisible(!gruppi.isEmpty());
        }

        schede.removeAll();
        javax.swing.ButtonGroup gruppo2 = new javax.swing.ButtonGroup();
        for (final Inventari.Inventario inv : inventari) {
            boolean suo = gruppoCorrente == null
                    ? inv.gruppo == null : gruppoCorrente.equals(inv.gruppo);
            if (!suo) {
                continue;
            }
            JToggleButton b = new JToggleButton(inv.etichetta);
            b.setFont(b.getFont().deriveFont(12f));
            b.setSelected(inv == corrente);
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    apri(inv);
                }
            });
            gruppo2.add(b);
            schede.add(b);
        }
        // Con un solo deposito la seconda fila non serve: si vede gia' tutto.
        schede.setVisible(schede.getComponentCount() > 1);
        testaAggiornata();
    }

    private void testaAggiornata() {
        schedeGruppi.revalidate();
        schedeGruppi.repaint();
        schede.revalidate();
        schede.repaint();
        revalidate();
        repaint();
    }

    private void apri(Inventari.Inventario inv) {
        corrente = inv;
        griglia.mostra(Inventari.risolvi(radice, inv.percorso), inv.etichetta);
        // Si sceglie subito il primo slot occupato. Aprendo l'inventario con il
        // form vuoto la sezione sembra rotta: "nessuno slot scelto" non dice
        // niente, mentre il primo pezzo mostrato dice subito dove sei.
        int primo = griglia.primoOccupato();
        if (primo >= 0) {
            griglia.seleziona(primo);
            modulo.mostra(primo);
        } else {
            modulo.svuota();
        }
        // Le statistiche della cosa a cui appartiene questo inventario: il
        // danno e lo scudo di questa nave, il danno e la scansione di questo
        // multi-tool. Vanno mostrate dopo svuota(), che le azzera.
        modulo.mostraStatistiche(inv.statistiche == null ? null
                : Inventari.risolvi(radice, inv.statistiche));
        aggiornaContatore();
    }

    private void aggiornaContatore() {
        if (corrente == null) {
            return;
        }
        contatore.setText(griglia.occupati() + " / " + griglia.slotAttivi() + " slot occupati");
    }

    /**
     * Un valore che il salvataggio tiene con segno, letto come lo mostra il gioco.
     *
     * Le unita' di un giocatore che ne ha due miliardi e settecento milioni sono
     * scritte nel file come {@code -1598048959}: e' lo stesso numero, visto da
     * un intero a 32 bit. Nell'editor si mostra quello che si vede nel gioco.
     */
    private static String senzaSegno(String testo) {
        try {
            long v = Long.parseLong(testo.trim());
            if (v < 0) {
                v += 4294967296L;
            }
            return String.valueOf(v);
        } catch (NumberFormatException e) {
            return testo;
        }
    }

    /** Il contrario: il numero che si vede, scritto come lo tiene il salvataggio. */
    private static String conSegno(String testo) {
        try {
            long v = Long.parseLong(testo.trim());
            if (v > 2147483647L) {
                v -= 4294967296L;
            }
            return String.valueOf(v);
        } catch (NumberFormatException e) {
            return testo;
        }
    }

    /** Adatta la griglia allo spazio disponibile, dopo un ridimensionamento. */
    public void ridisponi() {
        griglia.ricalcolaColonne();
    }

    /** Collaudo: simula un clic sul primo slot occupato e riferisce l'esito. */
    public String provaSelezione() {
        int quante = griglia.quanteSchede();
        int indice = griglia.primoOccupato();
        if (indice < 0) {
            return "nessuno slot occupato (schede: " + quante + ")";
        }
        boolean inviato = griglia.provaClick(indice);
        return "schede " + quante + ", clic sullo slot " + indice
                + (inviato ? " inviato" : " NON inviato")
                + ", selezionato ora: " + griglia.selezionato();
    }

    /** Collaudo: sceglie direttamente uno slot, senza passare dal mouse. */
    public void selezionaDirettamente(int indice) {
        modulo.mostra(indice);
    }

    /** Quanti slot ha l'inventario aperto. */
    public int quantiSlot() {
        return griglia.quanteSchede();
    }

    /** Quale slot risulta selezionato, o -1. */
    public int slotSelezionato() {
        return griglia.selezionato();
    }

    /** Il primo slot occupato, o -1. */
    public int primoOccupato() {
        return griglia.primoOccupato();
    }

    // ------------------------------------------------------------------

    /** Il form compatto del pezzo scelto. */
    private final class Modulo extends JPanel {

        private final JLabel icona = new JLabel();
        private final JLabel nome = new JLabel();
        private final JLabel categoria = new JLabel();
        private final JLabel identificativo = new JLabel();
        private final JTextField quantita = new JTextField();
        private final JTextField massimo = new JTextField();
        private final JTextField danno = new JTextField();
        private final JToggleButton installato = new JToggleButton();
        private final JToggleButton superCaricato = new JToggleButton();
        private final JButton cambia = new JButton("Cambia oggetto...");
        private final JButton ripara = new JButton("Ripara");
        private final JButton ricarica = new JButton("Ricarica al massimo");
        private final JButton svuota = new JButton("Svuota slot");
        private final JLabel posizione = new JLabel();
        private final JPanel statistiche = new JPanel();
        private final JPanel principaliPanel = new JPanel();

        private int indice = -1;

        Modulo() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Aspetto.PANNELLO);
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Aspetto.BORDO));
            setPreferredSize(new Dimension(300, 500));

            // --- testa: icona e nome ---
            JPanel testa = new JPanel(new BorderLayout(12, 0));
            testa.setOpaque(false);
            testa.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
            testa.setAlignmentX(Component.LEFT_ALIGNMENT);
            testa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            icona.setPreferredSize(new Dimension(64, 64));
            testa.add(icona, BorderLayout.WEST);
            JPanel testi = new JPanel();
            testi.setOpaque(false);
            testi.setLayout(new BoxLayout(testi, BoxLayout.Y_AXIS));
            nome.setFont(nome.getFont().deriveFont(Font.BOLD, 14f));
            nome.setForeground(Aspetto.TESTO);
            nome.setAlignmentX(Component.LEFT_ALIGNMENT);
            categoria.setFont(categoria.getFont().deriveFont(11f));
            categoria.setForeground(Aspetto.TESTO_TENUE);
            categoria.setAlignmentX(Component.LEFT_ALIGNMENT);
            posizione.setFont(Aspetto.monospaziato(10, Font.PLAIN));
            posizione.setForeground(Aspetto.TESTO_DEBOLE);
            posizione.setAlignmentX(Component.LEFT_ALIGNMENT);
            testi.add(nome);
            testi.add(Box.createVerticalStrut(3));
            testi.add(categoria);
            testi.add(Box.createVerticalStrut(3));
            testi.add(posizione);
            testa.add(testi, BorderLayout.CENTER);
            add(testa);

            // --- campi ---
            JPanel campi = new JPanel();
            campi.setOpaque(false);
            campi.setLayout(new BoxLayout(campi, BoxLayout.Y_AXIS));
            campi.setBorder(BorderFactory.createEmptyBorder(0, 16, 10, 16));
            campi.setAlignmentX(Component.LEFT_ALIGNMENT);

            campi.add(riga("Identificativo", identificativo));
            campi.add(riga("Quantità", quantita));
            campi.add(riga("Massimo", massimo));
            campi.add(riga("Danno", danno));
            campi.add(riga("Installato", installato));
            campi.add(riga("Super-caricato", superCaricato));
            add(campi);

            // --- azioni ---
            // Due colonne e non un FlowLayout: il FlowLayout dichiara l'altezza
            // come se i pulsanti stessero tutti su una riga, e il secondo giro
            // finiva sotto il blocco delle statistiche.
            JPanel azioni = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
            azioni.setOpaque(false);
            azioni.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
            azioni.setAlignmentX(Component.LEFT_ALIGNMENT);
            azioni.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            cambia.setFont(cambia.getFont().deriveFont(11.5f));
            ripara.setFont(ripara.getFont().deriveFont(11.5f));
            ricarica.setFont(ricarica.getFont().deriveFont(11.5f));
            svuota.setFont(svuota.getFont().deriveFont(11.5f));
            azioni.add(cambia);
            azioni.add(ripara);
            azioni.add(ricarica);
            azioni.add(svuota);
            add(azioni);

            // Le statistiche della cosa a cui appartiene questo inventario:
            // il danno e lo scudo di questa nave, il danno e la scansione di
            // questo multi-tool. Stanno qui, accanto ai suoi slot, non in una
            // sezione a parte.
            statistiche.setLayout(new BoxLayout(statistiche, BoxLayout.Y_AXIS));
            statistiche.setOpaque(false);
            statistiche.setAlignmentX(Component.LEFT_ALIGNMENT);
            statistiche.setBorder(BorderFactory.createEmptyBorder(6, 16, 0, 16));
            add(statistiche);

            // Le statistiche principali della sezione: nella tuta sono salute,
            // scudo, energia, unita', naniti e quicksilver. Il vecchio editor le
            // chiamava cosi' e le teneva nella scheda della tuta.
            principaliPanel.setLayout(new BoxLayout(principaliPanel, BoxLayout.Y_AXIS));
            principaliPanel.setOpaque(false);
            principaliPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            principaliPanel.setBorder(BorderFactory.createEmptyBorder(6, 16, 0, 16));
            add(principaliPanel);

            add(Box.createVerticalGlue());

            collega();
            svuota();
        }

        private JPanel riga(String etichetta, Component editor) {
            JPanel r = new JPanel(new BorderLayout(10, 0));
            r.setOpaque(false);
            r.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel l = new JLabel(etichetta);
            l.setFont(l.getFont().deriveFont(11.5f));
            l.setForeground(Aspetto.TESTO_DEBOLE);
            l.setPreferredSize(new Dimension(104, 24));
            r.add(l, BorderLayout.WEST);
            if (editor instanceof JTextField) {
                JTextField t = (JTextField) editor;
                t.setFont(Aspetto.monospaziato(12, Font.PLAIN));
                t.setHorizontalAlignment(SwingConstants.RIGHT);
            }
            r.add(editor, BorderLayout.CENTER);
            return r;
        }

        private void collega() {
            quantita.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scrivi("Amount", quantita.getText());
                }
            });
            massimo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scrivi("MaxAmount", massimo.getText());
                }
            });
            danno.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scrivi("DamageFactor", danno.getText());
                }
            });
            installato.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    installato.setText(installato.isSelected() ? "sì" : "no");
                    scrivi("FullyInstalled", String.valueOf(installato.isSelected()));
                }
            });
            superCaricato.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    superCaricato.setText(superCaricato.isSelected() ? "sì" : "no");
                    if (indice >= 0) {
                        griglia.alternaSuperCaricato(indice);
                        griglia.scriviSpeciali();
                        suModifica.run();
                    }
                }
            });
            cambia.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (indice < 0) {
                        return;
                    }
                    PannelloDettagli.SelettoreOggetto s = new PannelloDettagli.SelettoreOggetto(
                            javax.swing.SwingUtilities.getWindowAncestor(Modulo.this), catalogo, icone);
                    Catalogo.Voce scelta = s.apri(valoreDi("Id"));
                    if (scelta != null) {
                        griglia.imposta(indice, scelta.id);
                        mostra(indice);
                        aggiornaContatore();
                        suModifica.run();
                    }
                }
            });
            ripara.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    scrivi("DamageFactor", "0.0");
                    danno.setText("0.0");
                    suModifica.run();
                }
            });
            ricarica.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String m = massimo.getText();
                    if (m == null || m.isEmpty()) {
                        return;
                    }
                    scrivi("Amount", m);
                    quantita.setText(m);
                    suModifica.run();
                }
            });
            svuota.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (indice < 0) {
                        return;
                    }
                    griglia.rimuovi(indice);
                    mostra(indice);
                    aggiornaContatore();
                    suModifica.run();
                }
            });
        }

        /** L'oggetto dello slot scelto, o null se la casella e' libera. */
        private Map<String, Object> slot() {
            return indice < 0 ? null : griglia.oggettoDi(indice);
        }

        private String valoreDi(String campo) {
            Map<String, Object> s = slot();
            if (s == null) {
                return "";
            }
            Object v = s.get(campo);
            if (v == null) {
                return "";
            }
            if (v instanceof Json.Numero) {
                return ((Json.Numero) v).testo();
            }
            return String.valueOf(v);
        }

        private void scrivi(String campo, String valore) {
            Map<String, Object> s = slot();
            if (s == null) {
                return;
            }
            Object attuale = s.get(campo);
            if (attuale instanceof Json.Numero) {
                s.put(campo, new Json.Numero(valore));
            } else if (attuale instanceof Boolean) {
                s.put(campo, Boolean.valueOf("true".equals(valore)));
            } else {
                s.put(campo, valore);
            }
            griglia.repaint();
            suModifica.run();
        }

        /**
         * Mostra le statistiche della cosa, se ce ne sono.
         *
         * Gli identificativi ({@code ^SHIP_DAMAGE}, {@code ^WEAPON_MINING})
         * sono quelli che il salvataggio usa davvero. I valori sono decimali:
         * scrivendo un intero il salvataggio cambierebbe tipo.
         */
        @SuppressWarnings("unchecked")
        void mostraStatistiche(Object valore) {
            statistiche.removeAll();
            if (!(valore instanceof List)) {
                statistiche.revalidate();
                statistiche.repaint();
                return;
            }
            List<Object> elenco = (List<Object>) valore;
            if (elenco.isEmpty()) {
                statistiche.revalidate();
                statistiche.repaint();
                return;
            }
            JLabel titolo = new JLabel("STATISTICHE");
            titolo.setFont(Aspetto.monospaziato(10f, Font.BOLD));
            titolo.setForeground(Aspetto.ACCENTO);
            titolo.setAlignmentX(Component.LEFT_ALIGNMENT);
            titolo.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
            statistiche.add(titolo);

            for (Object o : elenco) {
                if (!(o instanceof Map)) {
                    continue;
                }
                final Map<String, Object> voce = (Map<String, Object>) o;
                String id = String.valueOf(voce.get("BaseStatID"));
                statistiche.add(rigaStatistica(id, voce));
            }
            statistiche.revalidate();
            statistiche.repaint();
        }

        private JPanel rigaStatistica(String id, final Map<String, Object> voce) {
            JPanel r = new JPanel(new BorderLayout(10, 0));
            r.setOpaque(false);
            r.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            r.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel nome = new JLabel(Etichette.leggi(id));
            nome.setFont(nome.getFont().deriveFont(11.5f));
            nome.setForeground(Aspetto.TESTO_TENUE);
            nome.setPreferredSize(new Dimension(126, 24));
            nome.setToolTipText(id);
            r.add(nome, BorderLayout.WEST);

            Object valore = voce.get("Value");
            String testo = valore instanceof Json.Numero
                    ? ((Json.Numero) valore).testo() : String.valueOf(valore);
            if (testo.endsWith(".0")) {
                testo = testo.substring(0, testo.length() - 2);
            }
            final JTextField campo = new JTextField(testo);
            campo.setFont(Aspetto.monospaziato(12, Font.PLAIN));
            campo.setHorizontalAlignment(SwingConstants.RIGHT);
            campo.setToolTipText(id + "  —  modifica e premi Invio");
            campo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String v = campo.getText().trim().replace(',', '.');
                    // I valori sono decimali: senza il punto il salvataggio
                    // cambierebbe tipo e il gioco potrebbe rifiutarlo.
                    if (!v.contains(".")) {
                        v = v + ".0";
                    }
                    voce.put("Value", new Json.Numero(v));
                    suModifica.run();
                }
            });
            r.add(campo, BorderLayout.CENTER);
            return r;
        }

        /**
         * Le statistiche principali della sezione, quelle che nel salvataggio
         * sono numeri interi sciolti.
         *
         * Nella tuta sono salute, scudo, energia, unita', naniti e quicksilver:
         * gli stessi valori che il vecchio editor chiamava cosi' e che il gioco
         * mostra sotto l'inventario. Sono interi, quindi si scrivono senza
         * virgola: mettere "100.0" dove il gioco si aspetta "100" cambierebbe
         * il tipo del campo.
         */
        void mostraPrincipali() {
            principaliPanel.removeAll();
            if (principali.isEmpty() || radice == null) {
                principaliPanel.revalidate();
                principaliPanel.repaint();
                return;
            }
            boolean qualcuna = false;
            JLabel titolo = new JLabel("STATISTICHE PRINCIPALI");
            titolo.setFont(Aspetto.monospaziato(10f, Font.BOLD));
            titolo.setForeground(Aspetto.ACCENTO);
            titolo.setAlignmentX(Component.LEFT_ALIGNMENT);
            titolo.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
            principaliPanel.add(titolo);

            for (final Inventari.Statistica st : principali) {
                Object contenitore = Inventari.risolvi(radice, st.percorso);
                if (contenitore == null) {
                    continue;
                }
                final Map<String, Object> padre = padreDi(st.percorso);
                final String campo = campoDi(st.percorso);
                if (padre == null) {
                    continue;
                }
                qualcuna = true;

                JPanel r = new JPanel(new BorderLayout(10, 0));
                r.setOpaque(false);
                r.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
                r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                r.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel nome = new JLabel(st.etichetta);
                nome.setFont(nome.getFont().deriveFont(11.5f));
                nome.setForeground(Aspetto.TESTO_TENUE);
                nome.setPreferredSize(new Dimension(126, 24));
                nome.setToolTipText(Inventari.accorcia(st.percorso));
                r.add(nome, BorderLayout.WEST);

                Object valore = padre.get(campo);
                String testo = valore instanceof Json.Numero
                        ? ((Json.Numero) valore).testo() : String.valueOf(valore);
                if (testo.endsWith(".0")) {
                    testo = testo.substring(0, testo.length() - 2);
                }
                // Le valute si mostrano senza segno, come nel gioco.
                if (st.senzaSegno) {
                    testo = senzaSegno(testo);
                }
                final JTextField campoTesto = new JTextField(testo);
                campoTesto.setFont(Aspetto.monospaziato(12, Font.PLAIN));
                campoTesto.setHorizontalAlignment(SwingConstants.RIGHT);
                campoTesto.setToolTipText(Inventari.accorcia(st.percorso) + "  —  modifica e premi Invio");
                campoTesto.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String v = campoTesto.getText().trim().replace(".0", "");
                        if (v.isEmpty()) {
                            return;
                        }
                        if (st.senzaSegno) {
                            v = conSegno(v);
                        }
                        padre.put(campo, new Json.Numero(v));
                        suModifica.run();
                    }
                });
                r.add(campoTesto, BorderLayout.CENTER);
                principaliPanel.add(r);
            }
            if (!qualcuna) {
                principaliPanel.removeAll();
            }
            principaliPanel.revalidate();
            principaliPanel.repaint();
        }

        /** La mappa che contiene l'ultimo campo di un percorso. */
        @SuppressWarnings("unchecked")
        private Map<String, Object> padreDi(String percorso) {
            int punto = percorso.lastIndexOf('.');
            if (punto < 0) {
                return null;
            }
            Object p = Inventari.risolvi(radice, percorso.substring(0, punto));
            return p instanceof Map ? (Map<String, Object>) p : null;
        }

        private String campoDi(String percorso) {
            int punto = percorso.lastIndexOf('.');
            return punto < 0 ? percorso : percorso.substring(punto + 1);
        }

        void svuota() {
            indice = -1;
            icona.setIcon(Icone.segno("·", 64, Aspetto.TESTO_DEBOLE, true));
            nome.setText("Nessuno slot scelto");
            categoria.setText("Tocca uno slot della griglia");
            posizione.setText("");
            identificativo.setText("");
            quantita.setText("");
            massimo.setText("");
            danno.setText("");
            installato.setSelected(false);
            installato.setText("no");
            superCaricato.setSelected(false);
            superCaricato.setText("no");
            abilita(false);
            mostraStatistiche(null);
            mostraPrincipali();
        }

        private void abilita(boolean attivo) {
            quantita.setEnabled(attivo);
            massimo.setEnabled(attivo);
            danno.setEnabled(attivo);
            installato.setEnabled(attivo);
            superCaricato.setEnabled(attivo);
            ripara.setEnabled(attivo);
            ricarica.setEnabled(attivo);
            svuota.setEnabled(attivo);
        }

        void mostra(int i) {
            indice = i;
            boolean sbloccato = griglia.sbloccato(i);
            String id = valoreDi("Id");
            Catalogo.Voce voce = catalogo.voce(id);

            ImageIcon img = voce == null ? null : icone.perFile(voce.icona, 64);
            icona.setIcon(img != null ? img : Icone.segno("·", 64, Aspetto.TESTO_DEBOLE, true));
            if (!sbloccato) {
                nome.setText("Slot non sbloccato");
                categoria.setText("Questo inventario non ha tanti slot");
                posizione.setText("");
                identificativo.setText("");
                quantita.setText("");
                massimo.setText("");
                danno.setText("");
                installato.setSelected(false);
                installato.setText("no");
                superCaricato.setSelected(false);
                superCaricato.setText("no");
                abilita(false);
                cambia.setEnabled(false);
                mostraPrincipali();
                revalidate();
                repaint();
                return;
            }
            nome.setText(voce != null ? voce.etichetta() : (id.isEmpty() ? "Slot libero" : id));
            categoria.setText(voce == null ? (id.isEmpty() ? "nessun oggetto"
                    : "oggetto non riconosciuto")
                    : (voce.sottotitolo == null ? "" : voce.sottotitolo));
            posizione.setText("riga " + ((i / Math.max(1, griglia.colonne())) + 1)
                    + ", colonna " + ((i % Math.max(1, griglia.colonne())) + 1));
            identificativo.setText(id.isEmpty() ? "—" : id);

            String q = valoreDi("Amount");
            String m = valoreDi("MaxAmount");
            quantita.setText(togliZero(q));
            massimo.setText(togliZero(m));
            danno.setText(togliZero(valoreDi("DamageFactor")));

            boolean inst = "true".equals(valoreDi("FullyInstalled"));
            installato.setSelected(inst);
            installato.setText(inst ? "sì" : "no");

            boolean sup = griglia.superCaricati().contains(Integer.valueOf(i));
            superCaricato.setSelected(sup);
            superCaricato.setText(sup ? "sì" : "no");

            // Su uno slot libero si puo' fare una cosa sola, ed e' quella che
            // serve: metterci dentro qualcosa. Gli altri comandi non hanno un
            // oggetto su cui agire.
            cambia.setText(id.isEmpty() ? "Metti oggetto..." : "Cambia oggetto...");
            abilita(!id.isEmpty());
            cambia.setEnabled(true);
            mostraPrincipali();
            revalidate();
            repaint();
        }

        private String togliZero(String v) {
            if (v == null) {
                return "";
            }
            if (v.endsWith(".0")) {
                return v.substring(0, v.length() - 2);
            }
            return v;
        }
    }
}

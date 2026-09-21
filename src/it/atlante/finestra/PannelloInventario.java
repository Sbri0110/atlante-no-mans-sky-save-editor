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
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
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
 *   <li>il form mostra il pezzo scelto con la sua icona grande e la descrizione
 *       di gioco, non una fila di campi senza contesto;</li>
 *   <li>le azioni sono sul pezzo, non sull'inventario: <i>ripara</i> e
 *       <i>ricarica</i> agiscono su quello che hai davanti;</li>
 *   <li>il pulsante per marcare uno slot <b>super-caricato</b> sta nel form:
 *       nel gioco sono pochi e preziosi, e vanno scelti con intenzione.</li>
 * </ul>
 */
public final class PannelloInventario extends JPanel {

    private final Catalogo catalogo;
    private final Icone icone;
    private final Runnable suModifica;

    private final JPanel schede = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
    private final JLabel contatore = new JLabel();
    private final GrigliaSlot griglia;
    private final Modulo modulo;

    private Object radice;
    private List<Inventari.Inventario> inventari = new ArrayList<Inventari.Inventario>();
    private Inventari.Inventario corrente;

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

        JPanel testa = new JPanel(new BorderLayout(12, 0));
        testa.setBackground(Aspetto.FONDO_ALTO);
        testa.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Aspetto.BORDO),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        schede.setOpaque(false);
        testa.add(schede, BorderLayout.WEST);
        contatore.setFont(Aspetto.monospaziato(11, Font.PLAIN));
        contatore.setForeground(Aspetto.TESTO_DEBOLE);
        testa.add(contatore, BorderLayout.EAST);

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
        inventari = Inventari.esistenti(radice, Inventari.perSezione(nomeSezione));
        schede.removeAll();

        if (inventari.isEmpty()) {
            corrente = null;
            contatore.setText("");
            griglia.mostra(null, nomeSezione);
            modulo.svuota();
            schede.revalidate();
            schede.repaint();
            return;
        }

        javax.swing.ButtonGroup gruppo = new javax.swing.ButtonGroup();
        for (final Inventari.Inventario inv : inventari) {
            final JToggleButton b = new JToggleButton(inv.etichetta);
            b.setFont(b.getFont().deriveFont(12f));
            b.setSelected(inv == inventari.get(0));
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    apri(inv);
                }
            });
            gruppo.add(b);
            schede.add(b);
        }
        schede.revalidate();
        schede.repaint();
        apri(inventari.get(0));
    }

    private void apri(Inventari.Inventario inv) {
        corrente = inv;
        griglia.mostra(Inventari.risolvi(radice, inv.percorso), inv.etichetta);
        modulo.svuota();
        aggiornaContatore();
    }

    @SuppressWarnings("unchecked")
    private void aggiornaContatore() {
        if (corrente == null) {
            return;
        }
        Object inv = Inventari.risolvi(radice, corrente.percorso);
        if (!(inv instanceof Map)) {
            contatore.setText("");
            return;
        }
        Object elenco = ((Map<String, Object>) inv).get("Slots");
        int totali = elenco instanceof List ? ((List<Object>) elenco).size() : 0;
        int occupati = 0;
        if (elenco instanceof List) {
            for (Object s : (List<Object>) elenco) {
                if (s instanceof Map) {
                    Object id = ((Map<String, Object>) s).get("Id");
                    if (id != null && !String.valueOf(id).isEmpty() && !"^".equals(String.valueOf(id))) {
                        occupati++;
                    }
                }
            }
        }
        contatore.setText(occupati + " / " + totali + " slot occupati");
    }

    /** Adatta la griglia allo spazio disponibile, dopo un ridimensionamento. */
    public void ridisponi() {
        griglia.ricalcolaColonne();
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
        private final JLabel posizione = new JLabel();

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
            JPanel azioni = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 6));
            azioni.setOpaque(false);
            azioni.setBorder(BorderFactory.createEmptyBorder(0, 10, 12, 10));
            azioni.setAlignmentX(Component.LEFT_ALIGNMENT);
            azioni.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            cambia.setFont(cambia.getFont().deriveFont(11.5f));
            ripara.setFont(ripara.getFont().deriveFont(11.5f));
            ricarica.setFont(ricarica.getFont().deriveFont(11.5f));
            azioni.add(cambia);
            azioni.add(ripara);
            azioni.add(ricarica);
            add(azioni);
            add(Box.createVerticalGlue());

            collega();
            svuota();
        }

        private JPanel riga(String etichetta, java.awt.Component editor) {
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
                        scriviSuperCaricati();
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
                        griglia.cambiaOggetto(indice, scelta.id);
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
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> slot(int i) {
            Object inv = Inventari.risolvi(radice, corrente.percorso);
            if (!(inv instanceof Map)) {
                return null;
            }
            Object elenco = ((Map<String, Object>) inv).get("Slots");
            if (!(elenco instanceof List)) {
                return null;
            }
            List<Object> lista = (List<Object>) elenco;
            if (i < 0 || i >= lista.size()) {
                return null;
            }
            Object s = lista.get(i);
            return s instanceof Map ? (Map<String, Object>) s : null;
        }

        private String valoreDi(String campo) {
            Map<String, Object> s = slot(indice);
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
            Map<String, Object> s = slot(indice);
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

        /** Riscrive l'elenco degli slot super-caricati dentro l'inventario. */
        @SuppressWarnings("unchecked")
        private void scriviSuperCaricati() {
            Object inv = Inventari.risolvi(radice, corrente.percorso);
            if (!(inv instanceof Map)) {
                return;
            }
            List<Object> valori = new ArrayList<Object>();
            List<Integer> indici = new ArrayList<Integer>(griglia.superCaricati());
            java.util.Collections.sort(indici);
            for (Integer i : indici) {
                valori.add(new Json.Numero(String.valueOf(i)));
            }
            ((Map<String, Object>) inv).put("SpecialSlots", valori);
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
        }

        private void abilita(boolean attivo) {
            quantita.setEnabled(attivo);
            danno.setEnabled(attivo);
            installato.setEnabled(attivo);
            superCaricato.setEnabled(attivo);
            cambia.setEnabled(attivo);
            ripara.setEnabled(attivo);
            ricarica.setEnabled(attivo);
        }

        void mostra(int i) {
            indice = i;
            String id = valoreDi("Id");
            Catalogo.Voce voce = catalogo.voce(id);

            ImageIcon img = voce == null ? null : icone.perFile(voce.icona, 64);
            icona.setIcon(img != null ? img : Icone.segno("·", 64, Aspetto.TESTO_DEBOLE, true));
            nome.setText(voce != null ? voce.etichetta() : (id.isEmpty() ? "Slot libero" : id));
            categoria.setText(voce == null ? "oggetto non riconosciuto"
                    : (voce.sottotitolo == null ? "" : voce.sottotitolo));
            posizione.setText("slot " + (i + 1));
            identificativo.setText(id.isEmpty() ? "—" : id);

            String q = valoreDi("Amount");
            String m = valoreDi("MaxAmount");
            quantita.setText(togliZero(q));
            massimo.setText(togliZero(m));
            massimo.setEditable(false);
            danno.setText(togliZero(valoreDi("DamageFactor")));

            boolean inst = "true".equals(valoreDi("FullyInstalled"));
            installato.setSelected(inst);
            installato.setText(inst ? "sì" : "no");

            boolean sup = griglia.superCaricati().contains(Integer.valueOf(i));
            superCaricato.setSelected(sup);
            superCaricato.setText(sup ? "sì" : "no");

            abilita(!id.isEmpty());
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

package it.atlante.finestra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La navigazione per sezioni.
 *
 * Il primo tentativo mostrava il salvataggio come un unico albero: corretto e
 * completo, ma inutilizzabile. Duecentosessanta campi in fila non sono una
 * interfaccia — chi cerca "le navi" non sa che devono guardare dentro
 * {@code BaseContext > PlayerStateData > ShipOwnership}.
 *
 * Qui le sezioni sono quelle che servono davvero, ognuna con i campi che le
 * appartengono. I nomi sono quelli reali del salvataggio, ricavati leggendo un
 * salvataggio vero, non indovinati.
 *
 * Un asterisco in fondo a un nome indica un prefisso: {@code Chest*} prende
 * Chest1Inventory, Chest2Inventory e cosi' via, senza doverli elencare tutti.
 */
public final class Navigazione {

    /** Dove vivono i campi di una sezione. */
    public enum Contenitore {
        /** Schermata iniziale: informazioni del file e azioni rapide, non campi. */
        PRINCIPALE,
        /** BaseContext > PlayerStateData: la quasi totalita' dei campi di gioco. */
        GIOCATORE,
        /** CommonStateData: nome, durata, stagioni, ricompense, foto, musica. */
        COMUNE,
        /** DiscoveryManagerData: l'archivio delle scoperte. */
        SCOPERTE,
        /** Tutta la radice: usato solo dalla sezione "Tutto". */
        RADICE
    }

    /** Una sezione della navigazione. */
    public static final class Sezione {
        public final String nome;
        public final String descrizione;
        public final String segno;
        public final Contenitore contenitore;
        public final String[] campi;

        Sezione(String nome, String descrizione, String segno, Contenitore contenitore, String... campi) {
            this.nome = nome;
            this.descrizione = descrizione;
            this.segno = segno;
            this.contenitore = contenitore;
            this.campi = campi;
        }
    }

    private static final List<Sezione> SEZIONI = new ArrayList<Sezione>();

    static {
        SEZIONI.add(new Sezione("Principale", "Informazioni del file e azioni rapide",
                "UI-FILEICON", Contenitore.PRINCIPALE));

        SEZIONI.add(new Sezione("Tuta", "Inventario personale, stiva e tecnologie",
                "PRODUCT-SUIT_INV_TOKEN", Contenitore.GIOCATORE,
                "Inventory", "Inventory_Cargo", "Inventory_TechOnly", "RepairTechBuffer"));

        SEZIONI.add(new Sezione("Multitool", "Armi, moduli installati e munizioni",
                "UI-WEAPONICON", Contenitore.GIOCATORE,
                "Multitools", "ArchivedMultitools", "ActiveMultioolIndex", "CurrentWeapon",
                "WeaponInventory", "WeaponLayout", "StartingPrimaryWeapon",
                "StartingSecondaryWeapon", "BoltAmmo", "LaserAmmo", "PulseAmmo", "ScatterAmmo"));

        SEZIONI.add(new Sezione("Navi", "Flotta, inventari di bordo e statistiche",
                "UI-SHIPICON", Contenitore.GIOCATORE,
                "ShipOwnership", "CurrentShip", "PrimaryShip", "ArchivedShipOwnership",
                "MultiShipEnabled", "ShipInventory", "ShipLayout", "ShipHealth", "ShipShield",
                "ShipMoveableContents", "ShipUsesLegacyColours", "ShipNeedsTerrainPositioning",
                "CorvetteDraftShipSeed", "CorvetteEditShipName",
                "CorvetteEditAssociatedShipIndex", "CorvetteStorageInventory",
                "CorvetteStorageLayout"));

        SEZIONI.add(new Sezione("Squadrone", "Piloti reclutati e slot sbloccati",
                "UI-GEK", Contenitore.GIOCATORE,
                "SquadronPilots", "SquadronUnlockedPilotSlots"));

        SEZIONI.add(new Sezione("Mercantile", "Nave capitale, inventari e flotta",
                "UI-FREIGHTERICON", Contenitore.GIOCATORE,
                "CurrentFreighter", "CurrentFreighterNPC", "CurrentFreighterHomeSystemSeed",
                "PlayerFreighterName", "FreighterDismissed", "FreighterFleet", "FreighterLayout",
                "FreighterInventory", "FreighterInventory_Cargo", "FreighterInventory_TechOnly",
                "FreighterCargoLayout", "FreighterEngineEffect", "FreighterLastSpawnTime",
                "FreighterMatrixAt", "FreighterMatrixPos", "FreighterMatrixUp",
                "FreighterUniverseAddress"));

        SEZIONI.add(new Sezione("Fregate", "Navi da supporto e spedizioni",
                "UI-SHIPICON", Contenitore.GIOCATORE,
                "FleetFrigates", "FleetExpeditions", "FleetSeed"));

        SEZIONI.add(new Sezione("Corvette", "Inventario, tecnologie, parti di costruzione e depositi",
                "UI-SHIPTYPE-CORVETTE", Contenitore.GIOCATORE,
                "CorvetteStorageInventory", "CorvetteDraftShipSeed",
                "CorvetteEditAssociatedShipIndex", "CorvetteEditShipName"));

        SEZIONI.add(new Sezione("Veicoli", "Exocraft, piattaforma da pesca e mezzi",
                "PRODUCT-AM_EXOCRAFTTREE", Contenitore.GIOCATORE,
                "VehicleOwnership", "PrimaryVehicle", "VehicleAIControlEnabled", "SkiffData",
                "CustomTruckPresets", "CustomTruckPresetNames"));

        SEZIONI.add(new Sezione("Compagni", "Animali, uova e personalizzazioni",
                "UI-PET", Contenitore.GIOCATORE,
                "Pets", "Eggs", "UnlockedPetSlots", "PetAccessoryCustomisation", "PetBattleTeam"));

        SEZIONI.add(new Sezione("Basi e contenitori", "Basi costruite, container e depositi",
                "UI-BASEICON", Contenitore.GIOCATORE,
                "BaseBuildingObjects", "SeenBaseBuildingObjects", "PersistentPlayerBases",
                "Chest*", "RocketLocker*", "CookingIngredients*", "FoodUnit*", "FishBaitBox*",
                "FishPlatform*", "GraveInventory", "RefinerBufferData", "RefinerBufferKeys",
                "TerrainEditData"));

        SEZIONI.add(new Sezione("Insediamenti", "Colonie, produttivita' e storia",
                "PRODUCT-BLD_PLANET_HOLO", Contenitore.GIOCATORE,
                "SettlementStatesV2", "SettlementLocalSaveData", "SettlementHistory",
                "SettlementStateRingBufferIndexV2"));

        SEZIONI.add(new Sezione("Spedizioni", "La spedizione in corso, i suoi traguardi e le ricompense",
                "PRODUCT-EXPEDITION.S23.BANNER", Contenitore.COMUNE,
                "SeasonData", "SeasonState", "SeasonTransferInventoryData",
                "StartingSeasonNumber", "RestartAllInactiveSeasonalMissions"));

        SEZIONI.add(new Sezione("Conquista stazioni", "Cosa serve per rivendicare una stazione spaziale",
                "PRODUCT-STATIONCRATE", Contenitore.GIOCATORE,
                "Stats", "ProgressionLevel", "MissionProgress"));

                SEZIONI.add(new Sezione("Traguardi e fazioni", "Statistiche, missioni e reputazione",
                "UI-MILESTONES", Contenitore.GIOCATORE,
                "MissionProgress", "MissionRecurrences", "MissionVersion", "CurrentMissionID",
                "PreviousMissionID", "CurrentMissionSeed", "PreviousMissionSeed",
                "PostMissionIndex", "Stats", "ProgressionLevel", "InteractionProgressTable",
                "TelemetryStats", "ShopTier", "ShopNumber", "SavedInteractionDialogTable",
                "SavedInteractionIndicies"));

            }

    public static List<Sezione> elenco() {
        return Collections.unmodifiableList(SEZIONI);
    }

    private Navigazione() {
    }

    // ------------------------------------------------------------------
    // Filtro
    // ------------------------------------------------------------------

    /**
     * Vista in sola lettura sull'elenco dei campi, ma in scrittura sui valori.
     *
     * Il primo tentativo filtrava copiando le mappe scelte in una mappa nuova.
     * Sembrava funzionare, ma le modifiche finivano nella copia: il salvataggio
     * non cambiava e nessuno se ne accorgeva, perche' l'albero mostrava il
     * valore nuovo.
     *
     * Questa vista invece <b>mostra</b> solo i campi scelti e <b>scrive</b>
     * nell'originale. L'iterazione e' filtrata, la scrittura no.
     */
    static final class VistaFiltrata extends java.util.AbstractMap<String, Object> {

        private final Map<String, Object> originale;
        private final String[] modelli;

        VistaFiltrata(Map<String, Object> originale, String[] modelli) {
            this.originale = originale;
            this.modelli = modelli;
        }

        private boolean scelto(String chiave) {
            for (String modello : modelli) {
                if (modello.endsWith("*")) {
                    if (chiave.startsWith(modello.substring(0, modello.length() - 1))) {
                        return true;
                    }
                } else if (chiave.equals(modello)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public java.util.Set<Entry<String, Object>> entrySet() {
            java.util.Set<Entry<String, Object>> viste =
                    new java.util.LinkedHashSet<Entry<String, Object>>();
            for (Entry<String, Object> e : originale.entrySet()) {
                if (scelto(e.getKey())) {
                    viste.add(e);
                }
            }
            return viste;
        }

        @Override
        public Object get(Object chiave) {
            return originale.get(chiave);
        }

        @Override
        public boolean containsKey(Object chiave) {
            return chiave instanceof String && scelto((String) chiave)
                    && originale.containsKey(chiave);
        }

        @Override
        public Object put(String chiave, Object valore) {
            // La scrittura va nell'originale: e' il punto di tutta la faccenda.
            return originale.put(chiave, valore);
        }

        @Override
        public int size() {
            return entrySet().size();
        }
    }

    /**
     * Costruisce l'albero ridotto di una sezione.
     *
     * La catena dei rami viene conservata: se i campi stanno in
     * {@code BaseContext > PlayerStateData}, la sezione mostra quella catena,
     * cosi' si capisce sempre dove si sta lavorando. I valori non vengono
     * copiati: sono gli stessi oggetti del salvataggio, quindi le modifiche
     * finiscono nel file senza alcun passaggio di ritorno.
     */
    @SuppressWarnings("unchecked")
    public static Object filtro(Object radice, Sezione sezione) {
        if (sezione.contenitore == Contenitore.RADICE || !(radice instanceof Map)) {
            return radice;
        }
        Map<String, Object> r = (Map<String, Object>) radice;
        Map<String, Object> risultato = new LinkedHashMap<String, Object>();

        switch (sezione.contenitore) {
            case GIOCATORE: {
                // Il contesto principale e quello della spedizione in corso
                // hanno la stessa struttura: si mostrano entrambi, ma il
                // contesto in corso per primo, cosi' e' quello che si apre.
                String attivo = Inventari.contesto(radice);
                for (String contesto : new String[]{attivo,
                        "BaseContext".equals(attivo) ? "ExpeditionContext" : "BaseContext"}) {
                    Object c = r.get(contesto);
                    if (!(c instanceof Map)) {
                        continue;
                    }
                    Object psd = ((Map<String, Object>) c).get("PlayerStateData");
                    if (!(psd instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> dentro = new LinkedHashMap<String, Object>();
                    dentro.put("PlayerStateData",
                            new VistaFiltrata((Map<String, Object>) psd, sezione.campi));
                    risultato.put(contesto, dentro);
                }
                break;
            }
            case COMUNE: {
                Object comune = r.get("CommonStateData");
                if (comune instanceof Map) {
                    risultato.put("CommonStateData",
                            new VistaFiltrata((Map<String, Object>) comune, sezione.campi));
                }
                break;
            }
            case SCOPERTE: {
                Object scoperte = r.get("DiscoveryManagerData");
                if (scoperte != null) {
                    risultato.put("DiscoveryManagerData", scoperte);
                }
                break;
            }
            default:
                return radice;
        }
        // Se la sezione non trova nulla si restituisce comunque il risultato
        // vuoto, non la radice intera: mostrare tutto al posto di niente
        // faceva sembrare che ogni sezione avesse tutti i campi del salvataggio.
        return risultato;
    }

    /** Quanti campi di una sezione esistono davvero nel salvataggio. */
    @SuppressWarnings("unchecked")
    public static int conta(Object radice, Sezione sezione) {
        // La schermata iniziale non mostra campi: la sua riga nell'elenco deve
        // riportare la descrizione, non un conteggio.
        if (sezione.contenitore == Contenitore.PRINCIPALE) {
            return 0;
        }
        Object ridotto = filtro(radice, sezione);
        if (!(ridotto instanceof Map)) {
            return 0;
        }
        int totale = 0;
        for (Object v : ((Map<String, Object>) ridotto).values()) {
            totale += contaFoglie(v);
        }
        return totale;
    }

    @SuppressWarnings("unchecked")
    private static int contaFoglie(Object nodo) {
        if (nodo instanceof Map) {
            int n = 0;
            for (Object v : ((Map<String, Object>) nodo).values()) {
                n += contaFoglie(v);
            }
            return n;
        }
        if (nodo instanceof List) {
            int n = 0;
            for (Object v : (List<Object>) nodo) {
                n += contaFoglie(v);
            }
            return n;
        }
        return 1;
    }
}

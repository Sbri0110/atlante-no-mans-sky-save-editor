package it.atlante.finestra;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * I nomi leggibili dei campi del salvataggio.
 *
 * Il salvataggio contiene nomi di proprieta' in stile programmatore:
 * {@code Inventory_Cargo}, {@code DamageFactor}, {@code FullyInstalled}. Vanno
 * bene per il gioco, non per chi usa un editor: la differenza fra
 * {@code 0x013700C854F21221} e "Identificativo del pianeta" e' la differenza
 * fra un dump di dati e un programma.
 *
 * Il dizionario copre i nomi che ricorrono. Per quelli che non conosce,
 * {@link #leggi} spezza il nome in parole e le unisce con gli spazi, cosi'
 * {@code MaxAmount} diventa "Max amount" invece di restare attaccato.
 */
public final class Etichette {

    private static final Map<String, String> NOMI = new HashMap<String, String>();

    private static void n(String campo, String etichetta) {
        NOMI.put(campo, etichetta);
    }

    static {
        // --- valori del giocatore ---
        n("Units", "Unità");
        n("Nanites", "Naniti");
        n("Specials", "Quicksilver");
        n("Health", "Salute");
        n("Shield", "Scudo");
        n("Energy", "Energia");
        n("Hazard", "Rischio ambientale");
        n("HazardTimeAlive", "Tempo in ambienti ostili");
        n("TimeAlive", "Tempo di gioco");
        n("TotalPlayTime", "Durata della partita");
        n("ProgressionLevel", "Livello di progresso");
        n("DifficultyState", "Difficoltà");
        n("SaveName", "Nome del salvataggio");
        n("SaveSummary", "Riassunto");
        n("Version", "Versione del formato");
        n("Platform", "Piattaforma");
        n("ActiveContext", "Contesto attivo");
        n("SaveUniversalId", "Identificativo del salvataggio");
        n("GameMode", "Modalità di gioco");

        // --- inventari ---
        n("Inventory", "Inventario");
        n("Inventory_Cargo", "Inventario — stiva");
        n("Inventory_TechOnly", "Inventario — tecnologie");
        n("Slots", "Slot");
        n("ValidSlotIndices", "Slot utilizzabili");
        n("SpecialSlots", "Slot super-caricati");
        n("Width", "Larghezza");
        n("Height", "Altezza");
        n("Class", "Classe");
        n("StackSizeGroup", "Gruppo di impilamento");
        n("BaseStatValues", "Statistiche di base");
        n("NumSlotsFromTech", "Slot da tecnologia");
        n("IsCool", "È speciale");
        n("InventoryType", "Tipo di inventario");
        n("Amount", "Quantità");
        n("MaxAmount", "Quantità massima");
        n("DamageFactor", "Danno");
        n("FullyInstalled", "Installato");
        n("AddedAutomatically", "Aggiunto automaticamente");
        n("Index", "Posizione");
        n("X", "Colonna");
        n("Y", "Riga");

        // --- oggetti ---
        n("Id", "Identificativo");
        n("Type", "Tipo");
        n("Name", "Nome");
        n("Subtitle", "Sottotitolo");
        n("Description", "Descrizione");
        n("Category", "Categoria");
        n("Quantity", "Quantità");
        n("Value", "Valore");
        n("Icon", "Icona");
        n("Symbol", "Simbolo");
        n("Multiplier", "Moltiplicatore");

        // --- navi, armi, veicoli ---
        n("ShipOwnership", "Navi possedute");
        n("CurrentShip", "Nave attuale");
        n("PrimaryShip", "Nave principale");
        n("ArchivedShipOwnership", "Navi archiviate");
        n("MultiShipEnabled", "Più navi abilitate");
        n("ShipInventory", "Inventario della nave");
        n("ShipLayout", "Disposizione della nave");
        n("ShipHealth", "Integrità della nave");
        n("ShipShield", "Scudo della nave");
        n("ShipName", "Nome della nave");
        n("ShipSeed", "Seme della nave");
        n("Multitools", "Multitool");
        n("ArchivedMultitools", "Multitool archiviati");
        n("ActiveMultioolIndex", "Multitool attivo");
        n("CurrentWeapon", "Arma attuale");
        n("WeaponInventory", "Inventario dell'arma");
        n("WeaponLayout", "Disposizione dell'arma");
        n("VehicleOwnership", "Veicoli posseduti");
        n("PrimaryVehicle", "Veicolo principale");
        n("VehicleAIControlEnabled", "Guida automatica");

        // --- mercantile, fregate, squadrone ---
        n("CurrentFreighter", "Mercantile attuale");
        n("CurrentFreighterNPC", "Equipaggio del mercantile");
        n("PlayerFreighterName", "Nome del mercantile");
        n("FreighterInventory", "Inventario del mercantile");
        n("FreighterInventory_Cargo", "Mercantile — stiva");
        n("FreighterInventory_TechOnly", "Mercantile — tecnologie");
        n("FreighterFleet", "Flotta del mercantile");
        n("FleetFrigates", "Fregate");
        n("FleetExpeditions", "Spedizioni della flotta");
        n("SquadronPilots", "Piloti dello squadrone");
        n("SquadronUnlockedPilotSlots", "Slot squadrone sbloccati");
        n("PilotRank", "Grado del pilota");
        n("PilotName", "Nome del pilota");

        // --- compagni ---
        n("Pets", "Compagni");
        n("Eggs", "Uova");
        n("UnlockedPetSlots", "Slot compagni sbloccati");
        n("PetAccessoryCustomisation", "Accessori dei compagni");
        n("PetBattleTeam", "Squadra di combattimento");
        n("CreatureName", "Nome della creatura");
        n("Trust", "Fiducia");
        n("Biome", "Bioma");

        // --- basi e depositi ---
        n("BaseBuildingObjects", "Oggetti costruiti");
        n("SeenBaseBuildingObjects", "Oggetti conosciuti");
        n("PersistentPlayerBases", "Basi permanenti");
        n("RocketLockerInventory", "Deposito del razzo");
        n("CookingIngredientsInventory", "Ingredienti di cucina");
        n("FoodUnitInventory", "Unità di cibo");
        n("FishBaitBoxInventory", "Esca da pesca");
        n("FishPlatformInventory", "Piattaforma da pesca");
        n("GraveInventory", "Inventario della tomba");
        n("TeleportEndpoints", "Punti di teletrasporto");

        // --- insediamenti ---
        n("SettlementStatesV2", "Insediamenti");
        n("SettlementLocalSaveData", "Dati locali dell'insediamento");
        n("SettlementHistory", "Storia dell'insediamento");
        n("SettlementName", "Nome dell'insediamento");
        n("Population", "Popolazione");
        n("Happiness", "Felicità");
        n("Productivity", "Produttività");
        n("Debt", "Debito");

        // --- scoperte ---
        n("DiscoveryManagerData", "Registro delle scoperte");
        n("KnownWords", "Parole apprese");
        n("KnownWordGroups", "Gruppi di parole");
        n("KnownProducts", "Prodotti conosciuti");
        n("KnownTech", "Tecnologie conosciute");
        n("KnownSpecials", "Elementi speciali conosciuti");
        n("KnownRefinerRecipes", "Ricette del raffinatore");
        n("KnownPortalRunes", "Rune del portale");
        n("VisitedSystems", "Sistemi visitati");
        n("PlanetSeeds", "Semi dei pianeti");
        n("UniverseAddress", "Indirizzo nell'universo");
        n("GalacticAddress", "Indirizzo galattico");
        n("RealityIndex", "Indice di realtà");
        n("VoxelX", "Coordinata X");
        n("VoxelY", "Coordinata Y");
        n("VoxelZ", "Coordinata Z");
        n("SolarSystemIndex", "Indice del sistema");
        n("PlanetIndex", "Indice del pianeta");

        // --- traguardi, missioni, fazioni ---
        n("MissionProgress", "Progresso delle missioni");
        n("MissionRecurrences", "Missioni ricorrenti");
        n("MissionVersion", "Versione delle missioni");
        n("CurrentMissionID", "Missione in corso");
        n("CurrentMissionSeed", "Seme della missione");
        n("PostMissionIndex", "Indice dopo la missione");
        n("Stats", "Statistiche");
        n("TelemetryStats", "Statistiche di telemetria");
        n("InteractionProgressTable", "Tabella delle interazioni");
        n("Gek", "Gek");
        n("Korvax", "Korvax");
        n("Vykeen", "Vy'keen");
        n("Autophage", "Autofagi");
        n("Traders", "Mercanti");
        n("Warriors", "Guerrieri");
        n("Explorers", "Esploratori");

        // --- stagioni, ricompense, account ---
        n("CommonStateData", "Dati comuni");
        n("BaseContext", "Contesto principale");
        n("ExpeditionContext", "Contesto spedizione");
        n("PlayerStateData", "Stato del giocatore");
        n("SpawnStateData", "Dati di rigenerazione");
        n("SeasonData", "Dati della spedizione");
        n("SeasonState", "Stato della spedizione");
        n("SeasonId", "Identificativo della spedizione");
        n("SeasonTransferInventoryData", "Inventario di trasferimento");
        n("EarnedSeasonSpecialRewards", "Ricompense della spedizione ottenute");
        n("RedeemedSeasonRewards", "Ricompense della spedizione riscattate");
        n("RedeemedTwitchRewards", "Ricompense Twitch riscattate");
        n("RedeemedPlatformRewards", "Ricompense di piattaforma riscattate");
        n("UsedEntitlements", "Diritti utilizzati");
        n("StartingSeasonNumber", "Numero della spedizione iniziale");
        n("Title", "Titolo");
        n("Summary", "Riassunto");
        n("StartTimeUTC", "Inizio");
        n("EndTimeUTC", "Fine");
        n("Hash", "Impronta");
        n("MilestoneValues", "Valori dei traguardi");
        n("RewardCollected", "Ricompense raccolte");
        n("PhotoModeSettings", "Impostazioni della modalità fotografica");
        n("ByteBeatLibrary", "Libreria ByteBeat");
        n("Fog", "Nebbia");
        n("CloudAmount", "Nuvole");
        n("FoV", "Campo visivo");
        n("Vignette", "Vignettatura");
        n("MySongs", "Brani");
        n("Playlist", "Scala di riproduzione");
        n("Shuffle", "Riproduzione casuale");

        // Nomi che la regola dei prefissi tradurrebbe male: vanno elencati.
        n("TimeStamp", "Momento del salvataggio");
        n("HomeRealityIteration", "Realtà di origine");
        n("LastKnownDay", "Ultimo giorno conosciuto");
        n("SaveSummary", "Riassunto del salvataggio");
        n("TelemetryUploadVersion", "Versione dell'invio telemetrico");
        n("NextLoadSpawnsWithFreshStart", "Prossimo avvio da capo");
        n("UsesThirdPersonCharacterCam", "Telecamera in terza persona a piedi");
        n("UsesThirdPersonVehicleCam", "Telecamera in terza persona sul veicolo");
        n("UsesThirdPersonShipCam", "Telecamera in terza persona sulla nave");
        n("FullyInstalled", "Installato completamente");
        n("AddedAutomatically", "Aggiunto dal gioco");
        n("MultiplayerLobbyID", "Identificativo della lobby");
        n("MultiplayerPrivileges", "Permessi multigiocatore");
        n("BuildersKnown", "Costruzioni conosciute");
        n("InteractionProgressTable", "Tabella dei progressi di interazione");
    }

    /** Preposizioni e congiunzioni che restano minuscole nell'etichetta. */
    private static final java.util.Set<String> MINUSCOLE =
            new java.util.HashSet<String>(java.util.Arrays.asList("di", "del", "della", "dei", "delle", "da", "in", "a", "e"));

    private Etichette() {
    }

    /** L'etichetta leggibile di un campo. */
    public static String leggi(String campo) {
        if (campo == null || campo.isEmpty()) {
            return "";
        }
        String nota = NOMI.get(campo);
        if (nota != null) {
            return nota;
        }
        // I campi numerati delle liste (0, 1, 2...) non sono nomi.
        if (campo.length() <= 2 && Character.isDigit(campo.charAt(0))) {
            return "Elemento " + (Integer.parseInt(campo) + 1);
        }
        // I prefissi ricorrenti si tolgono: Chest1Inventory -> "Deposito 1 — inventario"
        for (Map.Entry<String, String> e : PREFISSI.entrySet()) {
            if (campo.startsWith(e.getKey())) {
                String resto = campo.substring(e.getKey().length());
                if (resto.isEmpty()) {
                    return e.getValue();
                }
                return e.getValue() + " " + leggi(resto);
            }
        }
        return separa(campo);
    }

    private static final Map<String, String> PREFISSI = new LinkedHashMap<String, String>();

    static {
        PREFISSI.put("Chest", "Deposito");
        PREFISSI.put("ChestMagic", "Deposito speciale");
        PREFISSI.put("Freighter", "Mercantile");
        PREFISSI.put("Ship", "Nave");
        PREFISSI.put("Settlement", "Insediamento");
        PREFISSI.put("Wonder", "Meraviglia");
        PREFISSI.put("Multiplayer", "Multigiocatore");
        PREFISSI.put("Space", "Spazio");
        PREFISSI.put("Portal", "Portale");
        PREFISSI.put("Atlas", "Atlas");
        PREFISSI.put("Nexus", "Nexus");
        PREFISSI.put("Grave", "Tomba");
        PREFISSI.put("First", "Primo");
        PREFISSI.put("Previous", "Precedente");
        PREFISSI.put("Current", "Attuale");
        PREFISSI.put("Next", "Prossimo");
        PREFISSI.put("Last", "Ultimo");
        PREFISSI.put("Has", "Ha");
        PREFISSI.put("Is", "È");
        PREFISSI.put("Use", "Usa");
        PREFISSI.put("Uses", "Usa");
        PREFISSI.put("Time", "Tempo");
        PREFISSI.put("Total", "Totale");
        PREFISSI.put("Max", "Massimo");
        PREFISSI.put("Min", "Minimo");
        PREFISSI.put("Num", "Numero di");
        PREFISSI.put("Pet", "Compagno");
        PREFISSI.put("Fleet", "Flotta");
        PREFISSI.put("Fish", "Pesca");
        PREFISSI.put("Food", "Cibo");
        PREFISSI.put("Bolt", "Dardi");
        PREFISSI.put("Laser", "Laser");
        PREFISSI.put("Pulse", "Impulso");
        PREFISSI.put("Scatter", "Mitraglia");
    }

    /**
     * Spezza un nome attaccato in parole.
     *
     * {@code MaxAmount} diventa "Max amount", {@code ShipHealth} diventa
     * "Nave salute" se il prefisso e' noto. Non e' una traduzione: e' il minimo
     * per rendere un nome leggibile senza doverlo elencare tutti.
     */
    static String separa(String campo) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < campo.length(); i++) {
            char c = campo.charAt(i);
            boolean maiuscola = Character.isUpperCase(c);
            if (i > 0 && maiuscola && !Character.isUpperCase(campo.charAt(i - 1))) {
                b.append(' ');
            }
            if (c == '_') {
                b.append(' ');
                continue;
            }
            b.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return b.toString().trim();
    }

    /** Vero se il campo ha un'etichetta tradotta, non solo spezzata. */
    public static boolean conosciuto(String campo) {
        return NOMI.containsKey(campo);
    }
}

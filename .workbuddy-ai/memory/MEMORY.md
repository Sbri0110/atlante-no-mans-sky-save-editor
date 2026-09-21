# MEMORY.md — Atlante (progetto)

Editor di salvataggi per No Man's Sky, in Java 8 + Swing + FlatLaf.
Cartella: `<cartella del progetto>`. Interfaccia e commenti in italiano.

## Come si compila e si prova

- Compilare: `./strumenti/jdk8/bin/javac.exe -encoding UTF-8 -cp "lib/flatlaf.jar" -d classi $(find src -name "*.java")`
  (oppure `compila.bat`, che rigenera anche `Atlante.jar`).
  **`-encoding UTF-8` e' obbligatorio**: senza, gli accenti si doppiano.
- Avviare: `java -cp "classi;lib/flatlaf.jar" it.atlante.Atlante` (separatore `;` su Windows).
- Rigenerare le schermate della documentazione:
  `java -cp "classi;lib/flatlaf.jar" it.atlante.strumenti.Schermate <cartella>`
  Apre il salvataggio vero e fotografa la finestra a 1500x940: le immagini restano vere.
- Collaudo del formato: `java -cp classi it.atlante.Main giro <payload>`
  Deve dire "il giro completo conserva i dati".
- `Atlante.jar` e' in `.gitignore`: nel repo solo sorgenti e risorse.

## Regole di lavoro (dette da Sbri)

- **Verificare sempre con il gioco, mai supporre.** Prima di aggiungere una
  scheda, un campo o un'etichetta: guardare il salvataggio vero (e il vecchio
  editor per i nomi) e portare i numeri misurati. Sbri lo ha chiesto due volte.
- Un campo di inventario **e' un deposito solo se il gioco ci ha messo qualcosa**:
  `ValidSlotIndices` non vuoto o `Slots` non vuoto. `Inventory_Cargo` esiste nel
  salvataggio di tuta, navi e mercantile ma e' sempre vuoto: non va mostrato.
- Le cose hanno **due** depositi: inventario e tecnologie. Il mercantile ha in
  piu' i dieci contenitori di stoccaggio (gli stessi `Chest1..10Inventory` della
  base), raggruppati a parte.
- Sbri non vuole elenchi di schede in fila quando sono tante: raggruppare.

## Dove stanno i dati nel salvataggio (verificato sul file, non supposto)

- Stato del giocatore: `BaseContext.PlayerStateData` (e `ExpeditionContext`).
- Inventari: mappa con `Slots` (solo gli slot **occupati**, ognuno con
  `Index{X,Y}`), `Width`, `Height`, `ValidSlotIndices` (le caselle sbloccate,
  posizione scritta **sciolta** `{X,Y}`), `SpecialSlots` (super-caricati, forma
  `{Type:{InventorySpecialSlotType:"TechBonus"}, Index:{X,Y}}`), `BaseStatValues`.
- Navi: `ShipOwnership[i]` con `Inventory`, `Inventory_TechOnly`,
  `Inventory_Cargo`, `Name`. Le navi segnaposto hanno `Width=Height=1` e nome vuoto.
- Multi-tool: `Multitools[i].Store` (un solo inventario), `Name`.
- Statistiche della cosa: `BaseStatValues` con `BaseStatID` tipo `^SHIP_DAMAGE`,
  `^WEAPON_MINING`, `^VEHICLE_AGILE`; valori **decimali** (`995.0`).
- Statistiche principali della tuta: `Health`, `Shield`, `Energy` (interi),
  `Units`, `Nanites`, `Specials` (valute: interi **con segno** nel file, senza
  segno nel gioco).
- Salvataggio di prova: Xbox Game Pass,
  `%LOCALAPPDATA%\Packages\HelloGames.NoMansSky_bs190hzg1sesy\SystemAppData\wgs\000901FF2F07BBDE_29070100B936489ABCE8B9AF3980429C\18C4F9F24DEB4BDF965C25D080DAF634\EEC95D8A4074480B94EC48FD53A6C503`
  (9,4 MB decompressi, 19 blocchi). Il gioco e' in `C:\XboxGames\No Man's Sky`.

## Il vecchio editor come riferimento

`<cartella del vecchio editor>\Atlante.jar` e' il fork precedente
(classi `nomanssave.*`, ofuscate, 948 classi). Per sapere cosa faceva una scheda:

    ./strumenti/jdk8/bin/javap.exe -p -c -cp "<jar>" nomanssave.<classe> | grep "// String"

Le schede sono classi ofuscate (`aJ` = Tuta, `gH` = Astronavi, `gv` = Multi-tool...).
L'estratto delle stringhe per scheda e' in `riferimenti/vecchio-editor/funzioni-grezze.txt`.

## Convenzioni di stile del codice

- Commenti e messaggi in italiano, apostrofi ASCII (`perche'`, `piu'`, `e'`).
- Niente lambda: classi anonime, generics espliciti, compatibilita' Java 8.
- I commenti spiegano il *perche'* e citano i dati misurati, non ripetono il codice.

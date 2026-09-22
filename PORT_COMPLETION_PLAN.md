# Complete OpenComputers-port — uitvoeringsplan

**Doel:** de bruikbare functionaliteit van OpenComputers `master-MC1.12` overbrengen naar Minecraft 1.21.1 / NeoForge, inclusief Lua/OpenOS, apparaten, interfaces, opslag, rendering, recepten, documentatie en geteste integraties.

**Aanpak:** bestaande Java-implementaties afmaken en vergelijken met upstream. Kleine wijzigingen per laag; bestaand gedrag beschermen met gerichte tests. Een groen testpakket bewijst geen visuele of volledige functionele gelijkwaardigheid.

**Werkplek:** `feature/finish-port`; startpunt `b05faaf8c`. Referentie: lokale upstream-checkout `.upstream-opencomputers-master-MC1.12`. Runtime: Java 21, NeoForge 21.1.234, Minecraft 1.21.1, Gradle/JUnit/GameTests en de specifieke Modrinth-testinstance.

**Uitvoering:** plan gevolgd door directe uitvoering is door de gebruiker gevraagd. Geen extra plan-goedkeuringsronde. Geen automatische merge/push/publicatie. GitHub Actions blijven uit. Hoogstens één gecombineerde reviewerpass voor een grote/risicovolle implementatie; geen reviewer voor dit plan of kleine fixes.

## Wanneer is de port compleet?

- [ ] Iedere upstream-feature staat in de pariteitsmatrix, met bronverwijzing, port-implementatie en bewijs. Geen ongemarkeerde ontbrekende blokken, callbacks of recepten.
- [ ] Alle kernfuncties zijn geïmplementeerd; geen stub of alleen een API-vormtest als bewijs van werking.
- [ ] Computers, servers, robots, drones, microcontrollers en tablets doorlopen hun volledige spelersflow.
- [ ] Lua-programma's, bestanden, componentadressen, inventarissen en netwerken blijven correct bij save/reload, chunk unload, serverherstart en verplaatsen/breken volgens upstream-semantiek.
- [ ] Alle GUI's, item-/blokmodellen, schermen, hologrammen en prints zijn visueel gecontroleerd, inclusief kleuren, rotaties, verlichting en resoluties.
- [ ] Geen bekende crash, duplicatie, dataverlies, vastloper of ernstige performancefout in de ondersteunde testmatrix.
- [ ] Ondersteunde modintegraties zijn expliciet gekozen en getest. Een verdwenen 1.12-mod wordt niet stilzwijgend als geport geteld: vervanger, niet-toepasselijk besluit of open beperking vastleggen.
- [ ] Documentatie en releasepakket beschrijven alleen bewezen gedrag. Geen voltooiingspercentage op basis van aantallen bestanden of tests.

Het importeren van volledige Minecraft-1.12-werelden is een apart migratieprobleem, geen impliciete belofte van deze modport. Wel controleren: compatibiliteit van Lua-programma's en gedocumenteerde OC-dataformaten. Een eventuele wereldconverter vereist eerst een expliciet formaat-/scopebesluit en kopieën van testwerelden.

## Vaste werkwijze per werkpakket

1. Upstream-contract en huidige implementatie lezen; concrete ontbrekende of afwijkende flow vastleggen.
2. Bestaande failure reproduceren met één gerichte test of een opgenomen live-reproductie voor rendering.
3. Kleinste volledige fix maken, zonder naastgelegen subsystemen onnodig te wijzigen.
4. Gerichte tests draaien; daarna de relevante integratie-/GameTests en volledige gates voor de afgeronde wijzigingsset.
5. Bij spelersgedrag: nieuwe JAR bouwen, client netjes sluiten, oude test-JAR bewaren, installeren, SHA256 vergelijken en opnieuw starten.
6. Ingame vóór/na controleren. Commandoacceptatie is geen resultaat: echte GUI-/blok-/inventaris-/chat-/beeldcontrole gebruiken.
7. Testobjecten opruimen en opruiming teruglezen. Bevindingen, bewijs en resterend werk bijwerken; alleen eigen bestanden committen.

Volledige gates:

```powershell
.\gradlew.bat test build --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
git diff --check
git ls-tree -r HEAD .github/workflows
git ls-tree -r origin/develop .github/workflows
Get-FileHash .\build\libs\neoopencomputers-0.1.0.jar -Algorithm SHA256
```

Geen parallelle Gradle-processen. Nieuwe tests gebruiken vaste waarden, een geïnjecteerde/bevroren klok en vaste random seeds waar relevant. Tijd/FPS zijn observaties met vergelijkbare omstandigheden, geen flakey unit-testasserties.

## Fase 0 — Volledige pariteitsinventaris

**Bronnen:** upstream `common/block`, `common/item`, `server/component`, `server/machine`, `integration`, `api`, resources/Lua en de port `common/ModBlocks.java`, `ModItems.java`, `ModContentIds.java`, registries en tests.

- [ ] Matrix opstellen per blok, item, tier, component/callback, driver, recipe, asset en configuratieoptie.
- [ ] Per rij: upstream-bron, huidige equivalent, ontbrekend/afwijkend/onbewezen, risico, automatische test en ingame scenario.
- [ ] Callbacknamen, argumenten/defaults, returnwaarden, foutmeldingen, events, energieverbruik en zijde-/richtingsemantiek vergelijken; niet alleen classnamen.
- [ ] Registraties en creative tab vergelijken met upstream; recipes/tags/loot en geïntegreerde documentatie valideren.
- [ ] Ontbrekende platformafhankelijke integraties apart uitsplitsen, beschikbaarheid verifiëren via officiële 1.21.1-bronnen.

**Gereed wanneer:** ieder verschil een taak of expliciet compatibiliteitsbesluit heeft. De bestaande audit is de startlijst, niet de volledige matrix.

## Fase 1 — Bevestigde gebruikersblokkades

### 1A. Terminal-GUI-performance

**Bestanden:** `src/main/java/li/cil/oc/client/TerminalFont.java`, gerichte tests onder `src/test/java/li/cil/oc/client`; alleen wanneer nodig `TerminalScreen.java`.

- [x] Baseline vastleggen: OpenOS en 160×50 terminal, GUI versus actieve wereldweergave. Audit: 4–7 FPS versus 60 FPS; concrete vóórmeting 6 FPS. Exacte captureverschillen staan in `TERMINAL_GUI_PERFORMANCE.md`.
- [x] Renderpad meten/profileren; per-pixel flush en herhaalde glyph-maplookups bevestigd met Minecraft-bron en JFR.
- [ ] Alleen de GUI-glyphrenderer aanpassen: pixelquads bundelen; Unicode/fallback en exacte 8×16/16×16 maten behouden. De bestaande atlas is een mogelijke latere optimalisatie als batching onvoldoende blijkt. Geen wijziging aan wereldgeometrie, projectie, netwerksync of multiblockberekening.
- [ ] Gerichte test voor bitmap-/glyphgelijkheid, kleur, transformatie en Unicode/fallback; geen test die slechts de nieuwe methode letterlijk nadoet.
- [ ] Gerichte en volledige gates; nieuwe client-JAR en vóór/na-screenshots, OpenOS-invoer, kleur, box-drawing en wide glyphs controleren.
- [ ] Ook volle 160×50 uitvoer, lagere tiers en GUI-schalen meten. Acceptatie: duidelijke verbetering, geen grote terugval tegenover de wereldbaseline, correcte glyphs en invoer. Resultaten apart voor het bestaande modprofiel en een minimale ondersteunde setup registreren.

**Screen-evidence gate:** `SCREEN_WORK_PROTOCOL.md`; architectuureigenaar is de terminal-GUI-glyphrenderer. Rollback naar `b05faaf8c` voor deze slice; baseline-JAR/hash en screenshots staan in `PORT_AUDIT_2026-09-22.md`.

### 1B. Vertalingen, fouten en slotweergave

**Bestanden:** `client/MicrocontrollerScreen.java`, `client/ComputerCaseScreen.java`, `common/blockentity/ComputerCaseBlockEntity.java`, `common/SimpleMachine.java`, `assets/neoopencomputers/lang/en_us.json`; bestaande taal-/boot-/GUI-tests.

- [x] Ontbrekende microcontroller-titel toevoegen. Ingame `Microcontroller` zichtbaar op 2026-09-22.
- [ ] Bekende runtimefouten via vertaalbare componenten aan de speler tonen; vrije Lua-fouttekst en eerste-regelgedrag behouden. Ook server/rack/robot/drone-foutpaden controleren.
- [ ] Slot-watermarks met lege én bezette slots vóór/na vergelijken. Blend-state alleen wijzigen als live bewijs de oorzaak bevestigt; kleur-/blend-state herstellen voor volgende widgets.
- [ ] Blok- én itemtint van alle computertiers, tooltips en taalwissel controleren.

### 1C. Robotterminal en schermloze layout

**Bestanden:** `common/blockentity/RobotBlockEntity.java`, `common/menu/RobotMenu.java`, `client/RobotScreen.java`, `common/network/TerminalNetworking.java`, bestaande terminal payloads/snapshots en robot GameTests.

- [ ] Via de assembler een complete bootbare robot maken; interne GPU/screen/keyboard-componenten en binding aantonen.
- [ ] Bestaande terminal-snapshot/delta- en invoermechanismen hergebruiken voor de robot, met container-/afstand-/eigenaarvalidatie op de server.
- [ ] OpenOS-uitvoer, typen, toetsen loslaten, plakken en muis ondersteunen volgens tier/upstream.
- [ ] Werkelijke schermloze texture/layout selecteren; slotcoördinaten, hitboxes, energie-/statusweergave en GUI-schalen passend maken.
- [ ] Ingame: assemblage, boot, Lua-commando, rijden/draaien, inventory/world action, scherm sluiten/heropenen en reload. Uitvoer en invoer moeten blijven werken bij robotbeweging.

## Fase 2 — Runtime en duurzame toestand

**Bestanden:** `common/machine/LuaArchitecture.java`, `common/SimpleMachine.java`, netwerk-/filesystemregistries en host-load/save-methoden; upstream `server/machine/luac/NativeLuaArchitecture.scala` en persistence API.

- [ ] Eerst onafhankelijke reproducerende tests: Lua-lokale variabele, coroutine/wachtend event, open bestand, timer en componentproxy vóór/na chunk unload en save/reload.
- [ ] LuaJ-beperkingen en upstream native Lua-architecturen inventariseren. Onderzoeken welke runtime volledige veilige heap-/coroutinepersistentie kan leveren op de ondersteunde OS/architecturen; licenties en binary packaging controleren.
- [ ] Een korte architectuurkeuze vastleggen vóór runtimevervanging. Geen onveilige algemene Java-deserialisatie van werelddata; geen replay van wereldacties om toestand te reconstrueren.
- [ ] Werkelijke hervatting implementeren, inclusief userdata/handles, deadlines, machine-energie, componentadressen, signalen, CPU-budget en migratie van bestaande port-saves.
- [ ] Stop/start/reboot/crash/sleep/pause, watchdog, geheugenlimieten, callback-threading en foutafhandeling vergelijken met upstream.
- [ ] Lua/OpenOS BIOS, require/package, Unicode, numeriek gedrag, native architectuurkeuze en sandboxing testen met echte upstream-programma's.

**Acceptatie:** hervatten zonder reboot, dubbel uitgevoerde acties of bestandsverlies. Documentatie aanpassen naar “reboot bij reload” telt niet als volledige pariteit; dit blijft een blocker totdat het opgelost is of de gebruiker expliciet een beperktere scope kiest.

## Fase 3 — Netwerk, energie en ontbrekende basisblokken

**Bestanden:** `common/ModBlocks.java`, `ModItems.java`, `ModBlockEntities.java`, `ModContentIds.java`, block/blockentity-implementaties, `NetworkRegistry.java`, resources en GameTests.

- [ ] Capacitor en carpeted capacitor porten: energieopslag, capaciteit/adjacencybonus, netwerkdeelname, save/load, breken/plaatsen, tapijtinteractie, kleur, model en recepten.
- [ ] Kabelverbindingen, splitsen/samenvoegen, componentzichtbaarheid, hotplug, adressen en events testen; ook chunkgrenzen en twee dimensies.
- [ ] Power converter/distributor/charger en buffers vergelijken: FE-input, schaalfactoren, throughput, distributie, redstonesnelheid en laadbare items.
- [ ] Serverracks/relays/net splitters testen met meerdere subnetten, side mappings en gelijktijdige clients.
- [ ] Netwerkduurtest: veel componenten, snelle connect/disconnect, unload/reload; geen duplicaten, energiewinst of node-leaks.

## Fase 4 — Apparaten en componenten afmaken

Elke rij krijgt upstream-vergelijking, gerichte unit-/GameTests en minstens één echte spelersflow:

| Onderdeel | Verplichte ingame acceptatie |
| --- | --- |
| Computer cases T1/T2/T3, servers en racks | Craft/inbouw, ontbrekende hardware, OpenOS-installatie op HDD, boot, shutdown, energieverlies, hotplug, rackterminal en reload |
| HDD/floppy/EEPROM/RAID/disk drive | Lezen/schrijven, labels, read-only, capaciteit, mount/unmount, EEPROM flash/checksum, RAID-wissels, restart, drops zonder duplicatie |
| GPU/screen/keyboard/terminal | Alle resoluties/kleurdieptes, palette, fill/copy, multiblock/orientaties, touch/precise, cursor, toetsen/muis/plakken, reload |
| Robot | Alle tiers, assembler, upgrades, inventaris/tank, bewegen/turn/swing/place/use, fake-playerrechten, energie, screen/input, break/drop/reload |
| Drone | Assembler, plaatsing/pickup, GUI, BIOS, status/light, beweging/collision/hover, inventory-upgrades, unload/reload |
| Microcontroller | Assembler/plaatsing, slots, EEPROM boot, side networks, redstone/energie, GUI, reload |
| Tablet | Assembler, gebruik, screen/input, charging, driver-upgrades, dimension/login-semantiek en verlies/terughalen |
| Modems/linked/wireless/internet | Broadcast/send/open/close, payloadtypes, afstand/tier, wake, tunnel, disconnect/reconnect; HTTP/TCP-foutpaden en hostbeperkingen |
| Redstone | Analoge input/output, events/wake, alle lokale zijden, bundled provider en wireless-integratie |
| Adapter/transposer/inventory/tank | Item/fluid transfers, side/slot mapping, volle/lege doelen, filters, energie, capability-invalidatie |
| Geolyzer/navigation/motion/waypoint | Scanlimieten, werelddata, posities/rotaties, events en energie |
| Hologram | Set/fill/clear, kleuren/palette, schaal/translatie/rotatie, netwerkupdates, rendering/reload |
| Printer/print/texture picker | Texture selecteren, vorm printen, plaatsen/roteren/activeren, collision, licht/opacity, redstone/button, drops en reload |
| Generator/solar/battery/chunkloader | Energieproductie/verbruik, fuelcontainer, dimension/time, laadstatus, tickets vrijgeven en serverrestart |
| Overige upgrades | Crafting, database, data, debug, experience, sign, piston, leash, tractor beam, angel, barcode, trading en MFU: callbacks én zichtbaar effect |
| Nanomachines | Consumptie, controller/energie, effecten, netwerkprotocol, dood/logout/dimensie, HUD en opslag |

## Fase 5 — Visuele en interactionele pariteit

**Bestanden:** `client/`, `assets/neoopencomputers/{models,blockstates,textures,sounds,lang,doc}`, menu-slotdefinities en clientregistraties.

- [ ] Alle blokken/items/tier-varianten in een testgalerij; inventory/hand/dropped/placed, zes zijden, licht/donker, normale en afwijkende rotaties.
- [ ] Alle GUI's op meerdere venstergroottes/GUI-schalen: text clipping, slot-watermarks, ontbrekende keys, tooltips, knoppen, hitboxes, JEI-focus en inventarisinteractie.
- [ ] Scherm-multiblocks horizontaal/verticaal, tierkleuren, Unicode/wide glyphs, scissor, glyphcontrast, wereldtekst en power-off toestand.
- [ ] Robot/drone-beweging en onderdelen, hologramalpha, printcustomtextures en sound/eventfeedback vergelijken met upstream.
- [ ] Screenshots per scenario bewaren. `VISUAL_SMOKE_RUNBOOK.md` aanvullen waar de complete port meer vereist dan de eerste alpha.

## Fase 6 — Integraties, API en verpakking

- [ ] `li.cil.oc.api` public contracts vergelijken, inclusief events, services, driver/providerregistratie, callbackregels en serialization. Klein voorbeeld-addon bouwen en in de echte client/server laden.
- [ ] Generieke NeoForge inventory/fluid/energy-capabilities end-to-end valideren; supported mods per versie pinnen.
- [ ] JEI/manual/providerintegratie controleren; bundled/wireless redstone en beschikbare tegenhangers van upstream-modintegraties onderzoeken en implementeren waar van toepassing.
- [ ] Dedicated server zonder clientklassen opstarten; twee clients, permissies/eigenaarschap, disconnect tijdens bewerkingen en container-/payloadvalidatie testen.
- [ ] Windows en Linux Java 21; native-runtime dependencies indien nodig; installable/API/javadoc JARs en bundled libraries/licenties controleren.
- [ ] Client met minimale modset én bestaande Modrinth-modset; compatibiliteit met Sodium/Iris en relevante companion mods vastleggen.

## Fase 7 — Regressie, duurtesten en afronding

- [ ] Vanaf nieuwe survivalwereld de crafting/assembler/installatieketen uitvoeren; command-spawned fixtures tellen niet als bewijs dat recepten/progressie werken.
- [ ] Afzonderlijk creative/admin/debugflows testen. Spelersrechten/gamemode kiezen passend bij scenario; geen OP-only succes als bewijs voor normale spelers.
- [ ] Actieve computers/robots/drones langdurig laten werken, save/reload/serverrestart, chunk unload en dimensies wisselen. Monitor geheugen, TPS/MSPT, FPS, netwerkverkeer en opslaggroei.
- [ ] Multiplayerscenario's: concurrent GUI-gebruik, ownership, offline player, reconnect, inventory movement en wereldacties zonder duplicatie/dataverlies.
- [ ] Buglijst leeg voor releaseblockers; overige beperkingen expliciet. Regressies uit iedere fix opnieuw in de relevante matrix draaien.
- [ ] README, manual, changelog, configuration comments, versie-/compatibiliteitslijst en installatiegids bijwerken. Verouderde claims/testaantallen verwijderen of actualiseren.
- [ ] Reproduceerbaar release-artifact, SHA256, schoon werkpad en testbewijs voorbereiden. Publiceren/mergen/pushen alleen wanneer daarvoor opdracht is gegeven.

## Bewijsregistratie en voortgang

Per scenario bewaren: commit/JAR-hash, profiel/modversies, wereld/seed/posities, hardware/tier, inputstappen, verwachte uitkomst, daadwerkelijke uitkomst, logs/screenshots en cleanup. Ingame resultaten gelden uitsluitend voor de geteste variant.

| Werkpakket | Status bij aanvang |
| --- | --- |
| Baseline-audit | Gereed: 2040 tests, 440 GameTests; zie `PORT_AUDIT_2026-09-22.md` |
| Volledige pariteitsmatrix | In uitvoering |
| Terminal-GUI-performance | OpenOS, volle 160×50 uitvoer en gekleurde achtergronden nu 60 FPS; lagere tiers/schalen/minimale modset nog open |
| UI-vertalingen/foutfeedback | Microcontroller-titel en case NoEnergy/NoCPU hersteld; overige hosts open |
| Robotterminal/layout | Beeld/input/snapshot-sync en compacte layout getest; beweging en volledige live OpenOS-flow open |
| Lua execution persistence | Open, architectuurwerk noodzakelijk |
| Capacitors/netwerk/energie | Open |
| Alle overige functionele/visuele/integratie-scenario's | Te verifiëren; bestaande tests niet als volledige pariteit tellen |

Resultaten hieronder tijdens uitvoering aanvullen met commit en bewijs. Alleen afvinken wat daadwerkelijk is aangetoond.

Startregister: `PORT_PARITY_MATRIX.md`. Aanvullende bronvergelijking vond onder meer robot-ROM, kabelkleuren/-geometrie, hover boots, debugger, presents en Lua-geheugenrapportage. Deze vallen onder de bovenstaande werkpakketten en blijven open totdat ze zijn uitgevoerd en getest.

### Uitvoering 2026-09-22

Eerste runtimewijziging: GUI-pixels bundelen en glyphlookup eenmaal per letter uitvoeren. Build + 2041 tests + 440 GameTests groen; OpenOS-boot, invoer, Unicode en blokweergave ingame gecontroleerd. Zie `TERMINAL_GUI_PERFORMANCE.md` voor reproduceerbare opstelling, hashes, profileresultaten, screenshots en beperkingen. Tijdelijke testopstelling verwijderd en lege ruimte teruggelezen; client netjes afgesloten.

De bovenstaande eerste ronde is inmiddels opgevolgd door de resultaten hieronder. Runtime-persistentie en brede pariteitsdekking blijven open. Lange invoerbursts apart onderzoeken; korte ingame commando's werkten.

### Doorlopende uitvoering — titel en foutfeedback

Microcontroller-titel hersteld. Computer-case NoEnergy/NoCPU gebruiken nu vertaalbare componenten met leesbare fallback; vrije Lua-fouttekst blijft letterlijk en alleen de eerste regel wordt getoond. Gerichte tests faalden eerst op ontbrekende titel en ruwe foutkey, daarna alle 26 groen; volledige build en 442 GameTests groen in deze ronde. Ingame titel screenshot: `build/finish-port-implementation/microcontroller-title-verified.png`. Clientlog bevestigt `Computer error: Not enough energy.` met geïnstalleerde JAR `731F3F1EF4E792BE9F4B88B18AEC9B3E78EE896791FAA949617D57CA94CA0372`. Foutfeedback van andere hosts en overige fase-1B-visuele controles blijven open.

Robot-ROM hersteld in `fedefc2f5`: vier originele upstream Lua-bestanden, filesystem-mount en behoud/opruiming van het ROM-adres. Echte OpenOS-boottest via assembler en HDD bewijst automatische autorun, `require("robot")` en `go left 1` met fysieke draaiing. Lifecycle-GameTest bewijst readonly, discovery en save/load/unload. De robot-GUI is daarmee nog niet klaar.

Terminalglyphs gebruiken nu de bestaande bitmapatlas (`a3b10fda`); rasterfallback voor andere Unicode blijft behouden. Aparte fix bundelt gekleurde achtergrondcellen. Live volle terminal en gekleurde achtergrond nu beide 60 FPS; Unicode/kleuren/input en wereldbeeld opnieuw gecontroleerd. Volledige gates: 2047 unit tests en 442 GameTests. Details en exacte hashes: `TERMINAL_GUI_PERFORMANCE.md`.

### Robotterminal en muisinvoer

RobotMenu deelt nu snapshot/delta-sync met TerminalMenu; interne screen/keyboard/GPU-wiring en toegangs-/afstandcontroles toegevoegd. RobotScreen rendert het scherm en stuurt invoer door. Ingame gevonden: AbstractContainerScreen slikte muisklikken in; terminalklikken nu eerst afhandelen en overige muisacties aan de inventory doorgeven. Live beeld, getypte tekst, touch-coordinaten en inventorytransfer bevestigd; gewone OpenOS-terminal/muis en volle 160x50 rode achtergrond opnieuw getest op 60 FPS. Build: 2052 unit-tests en 443 GameTests groen. Exacte hash, opstelling, screenshots, cleanup en resterende varianten: ROBOT_TERMINAL_PORT.md.

Volgende robotstappen: compacte schermloze layout, menu/uitvoering bij beweging, volledige live assembler/OpenOS-flow en Lua-persistentie. Overige fasen en open matrixrijen blijven staan; deze voortgang is geen volledige portering.
### Compacte robot en componentpersistentie

Robot zonder screen gebruikt nu de bestaande 108-hoge GUI; slotposities, powercontrole, tooltips en inventorytransfer blijven uitgelijnd. Client krijgt schermstatus voordat slots worden aangemaakt. Live compact boot/power en itemtransfer gecontroleerd, plus schermrobot met tekst en touch. Screen/keyboard-drivers bewaren nu adres en componentdata via het bestaande GPU-patroon; framebuffer/viewport/kleuren/palette/depth getest met echte driver-ItemStack-roundtrips. Samen: 2055 unit-tests en 446 GameTests groen. Zie ROBOT_TERMINAL_PORT.md en SCREEN_KEYBOARD_PERSISTENCE.md.

Nieuw concreet bewegingsprobleem: moveRobot maakt via NBT een nieuwe BE en Lua-runtime; iedere stap herstart de uitvoering, open menu raakt de oude BE kwijt. Bron wordt bovendien verwijderd voordat doelplaatsing is bevestigd. Volgende werkstap bewijst twee opeenvolgende stappen uit dezelfde Lua-uitvoering en behoudt de levende runtime, met gecontroleerde targetplaatsing/rollback. Deze regressie faalde eerst aantoonbaar op een reboot en is inmiddels hersteld; zie de volgende uitvoeringsstap.

Testprocedure vanaf gebruikerscorrectie: Minecraft op Windows-scherm 3, geen desktopinvoer tijdens parallel werk. Automatisch plaatsingsscript werd door antivirus geblokkeerd en verwijderd; plaatsing per start apart verifiëren, geen beveiliging omzeilen.
### Robotuitvoering tijdens beweging

Dezelfde BE/machine/componenten blijven nu bestaan tijdens een stap. Twee opeenvolgende stappen vanuit een echte EEPROM-coroutine, behoud van menu/scherm/inventaris/adressen, oude/nieuwe stroomverbinding en een door pre-event geblokkeerd doel zijn getest. Full build: 2055 unit-tests en 448 GameTests groen. Zie ROBOT_MOVEMENT_PERSISTENCE.md. Nieuwe JAR nog niet ingame getest/geinstalleerd; client-sync/animatie, chunkgrenzen en save/reload blijven open. Ook een testisolatiefout verholpen: een toetsenbord-afstandstest liet spelers achter in de naastgelegen debugkaarttest.

### Robotstatus in de client

Blockentity-sync voor running/tier/lampkleur/tool ingevoerd; renderer gebruikt de gesynchroniseerde status en het echte toolslot. RED op ontbrekende status; daarna 2055 unit-tests en 449 GameTests groen. Zie ROBOT_VISUAL_SYNCHRONIZATION.md. Nieuwe JAR nog niet ingame bekeken; beweging/draai/swing-animatie en upstream actie-energie/timing blijven open.

### Energie en timing van robotbeweging

Move/turn verbruiken nu de configureerbare upstream energie en vragen de bijbehorende wachttijd aan. Lege buffer verandert de wereld niet; mislukte beweging betaalt energie terug. setLightColor retourneert RGB en wacht 0,1 seconde. Full build: 2055 unit-tests en 450 GameTests groen. Zie ROBOT_ACTION_ENERGY_TIMING.md. Animatie, overige wereldacties en live verificatie blijven open.

### Robotbeweging en draaien animeren

Animatiestart, duur, oude positie en draaihoek worden nu meegestuurd; renderer interpoleert beweging/draaiing en omvat de vorige positie in zijn zichtbare gebied. Testdekking voor begin/midden/einde en herhaalde sync; 2055 unit-tests en 450 GameTests groen. Zie ROBOT_MOVEMENT_ANIMATION.md. Upstream afterimage/interactie, swing en ingame weergave blijven open.

### Tijdelijke robotpositie en interactie

Interne robot-afterimage toegevoegd: oude positie blijft tijdelijk klikbaar, botsingsvorm volgt de robot, doelbeveiliging blijft gelden en vervalt automatisch. Tests voor menu, beschermde afbraak en precies eenmaal cargo-drops; full build met 2055 unit-tests en 452 GameTests groen. Zie ROBOT_AFTERIMAGE_INTERACTION.md. Live weergave/interactie, overlappende acties, swing, chunkgrenzen en reload blijven open.

### Overlap, scheduler en richtingen

Te vroege tweede stap wordt zonder kosten geweigerd. Scheduler respecteert nu Context.pause binnen gesynchroniseerde callbacks; beweging wacht minimaal tot animatie-einde. Noord/zuid-actierichtingen en oost/west-renderrotatie gecorrigeerd, inclusief bijbehorend teken van draaianimatie. Vaste-kloktest, alle oriëntaties en echte tweestaps-EEPROM groen; full build met 2057 unit-tests en 453 GameTests. Zie ROBOT_MOVEMENT_SCHEDULING.md. Live verificatie en overige pariteit blijven open.

### Robot afbreken en terugplaatsen

Ontbrekende robotloot hersteld: hardware en componentgegevens blijven in het robotitem, losse inventaris valt apart en wordt niet gekopieerd. EEPROM-nodeadres wordt nu ook bewaard. Survival-afbraak, echte itemplaatsing, scherm/adressen en opnieuw booten van bewaarde EEPROM getest. Full build: 2057 unit-tests en 454 GameTests. Zie ROBOT_BREAK_REPLACE.md. Creative pick, verdere dropvarianten, HDD/reload en live controles blijven open.

### Robot-HDD na afbreken en plaatsen

Bestaande survival-roundtrip uitgebreid: HDD-adres/label, map en exacte binaire inhoud blijven bewaard; EEPROM voert na terugplaatsen init.lua vanaf dezelfde schijf uit. Adresvergelijking houdt nu alle filesystem-componenten bij. Geen extra productiepatch nodig. Full build: 2057 unit-tests en 454 GameTests groen; robot-hdd-final.log. Zie ROBOT_BREAK_REPLACE.md. Volledig geinstalleerd OpenOS, unmanaged-sectoren, chunk/reload en live controles blijven open.

### OpenOS-pickup en echte swing-harvest

Volledige OpenOS-image start opnieuw na robot-drop en echte itemplaatsing, leest het eerder geschreven bestand en mount/gebruikt robot-ROM opnieuw. HDD-fixture kopieert bestanden; interactief installeren blijft open. Swing gebruikt nu gereedschapsslot en normale speler-harvest met blokbeveiliging, loot en daadwerkelijke slijtage; cargo blijft intact. RED/GREEN en full build: 2057 unit-tests en 456 GameTests. Zie ROBOT_BLOCK_SWING.md. Volledige swing-timing/animatie/entities, overige wereldacties en live controles blijven open.

### Configureerbare robotslijtage

robot.itemDamageRate toegevoegd met upstream default 0.1 en bereik 0..1. Swing gebruikt deze basis voor de bestaande upgrade-/damage-events. Tests bewijzen geen slijtage bij nul, normale slijtage bij een en aanpasbare werkelijke schade via events. Full build: 2058 unit-tests en 457 GameTests; robot-wear-final.log. Zie ROBOT_BLOCK_SWING.md. Overige swing- en wereldactiepariteit en live verificatie blijven open.

### Graaftijd en Lua-wereldlezingen

Robotgraaftaken lopen nu over serverticks op basis van hardness, grounded gereedschapssnelheid, harvestRatio en Pre-event. Voortgang/annulering toegevoegd; doel, tool, positie en machinestatus worden bewaakt. Echte Lua-test vond daarnaast een deadlock in direct detect; detect/compare lezen de wereld nu op de serverthread. Vaste tickgrenzen en Lua compare/swing/detect groen. Full build: 2058 unit-tests en 461 GameTests; robot-dig-threading-final.log. Zie ROBOT_TIMED_DIGGING.md. Swing-animatie, overige wereldactiepariteit en live controle blijven open.

### Gereedschapsanimatie tijdens graven

Swingstatus opgenomen in bestaande client-sync; renderer roteert alleen het gereedschap volgens upstream herhaalde sinusboog, met minimaal vijf ticks. Annulering stopt de animatie; succesvolle korte actie mag de boog afmaken. Vaste-tijdcurve, packet-roundtrip en annulerings/minimumduurtests groen. Full build: 2058 unit-tests en 462 GameTests; robot-swing-animation-final.log. Zie ROBOT_TIMED_DIGGING.md. Visuele bevestiging op scherm 3 en overige wereldacties blijven open.

### Linkermuisklik en gerichte robotinteractie

Swing valideert upstream actie-/kalibratiezijden, gebruikt de echte ray-hit en respecteert LeftClickBlock-cancel/useItem/useBlock, wereldrechten en sneak-status. Blok-attack uitgevoerd waar toegestaan; toestand na attack bewaard voor vertraagd graven. Tests voor halve blokken, verboden zijden, ore-attack en delayed sneak. Full build: 2058 unit-tests en 466 GameTests; robot-left-click-final.log. Zie ROBOT_LEFT_CLICK.md. Entities/fire/drop/XP, verdere eventdetails, place/use en live verificatie blijven open.

### Drops naar robotinventaris

Nieuwe drops van block-attack en harvest gaan nu naar cargo; bestaande items blijven liggen en overschot blijft behouden. Pickup-beveiliging, eigenaar en pre/post-events worden gerespecteerd. Tests bevestigen exacte aantallen bij vrije ruimte, gedeeltelijk volle stacks en geweigerde pickup. Full build: 2058 unit-tests en 468 GameTests; robot-harvest-drops-verified.log. Zie ROBOT_HARVEST_DROPS.md. Ore-XP naar experience-upgrade, overige wereldacties en live tests blijven open.

### Ore-XP naar experience-upgrade

Exacte afbraak onderschept aangepaste BlockDropsEvent-XP bij aanwezige experience-upgrade, zonder dubbele wereldorbs. Zonder upgrade blijft normale XP-drop; annulering levert geen ore-XP op. Echte T3-hardware en drie vaste-XP-scenarios getest. Full build: 2058 unit-tests en 471 GameTests; robot-harvest-xp-final.log. Zie ROBOT_HARVEST_DROPS.md. Entities/fire/cobweb, verdere modinteracties, andere wereldacties en live tests blijven open.

### Vuur, spinnenwebben en fallback bij halve blokken

Robot swing dooft gewoon en soul fire met beveiligingscontrole, correcte fire-uitkomst, pauze en zonder slijtage. Upstream notAfraidOfSpiders en swing-delayconfig toegevoegd; cobwebs met mining pick verdwijnen na configureerbare tijd zonder onterechte loot. Gecorrigeerd: upstream probeert na gemiste kalibratiestralen alsnog het aanwezige blok, dus halve blokken blijven breekbaar. Full build: 2058 unit-tests en 474 GameTests; robot-fire-web-final.log. Zie ROBOT_LEFT_CLICK.md. Client gesloten; entities, overige wereldacties, persistence en live tests blijven open.

### Entityselectie en robotaanvallen

Robot swing selecteert nu nabije living entities, minecarts en drones tegenover de afstand tot het geraakte blok. Normale aanval gebruikt gereedschapsattributen, RobotAttackEntityEvent en NeoForge-beveiliging; geleende spelerstatus wordt hersteld. Speleraanvallen standaard uit via canAttackPlayers. Minecarts krijgen de upstream herhaalde pogingen en hun drop gaat naar cargo. Zes GameTests bewijzen schade, beide beveiligingslagen, attribuutherstel, spelerconfig-hookgating, blokkering door een nabijer blok en exact een minecartdrop. Full build: 2058 unit-tests en 480 GameTests; robot-entity-final.log. Zie ROBOT_ENTITY_ATTACKS.md. Exacte combat/cooldown/enchantmentpariteit, living-kill XP, echte PvP-regels en live tests blijven open.

### Echte equipment- en cargoinventaris voor upgrades

Gevonden en hersteld: Agent.equipmentInventory was losse opslag en mainInventory gebruikte menu-indexen in plaats van cargo-indexen. Twee live views koppelen upgrades nu aan de echte vier equipment-slots en zestien cargo-slots. Echte robot/controller-tests bewijzen equip in beide richtingen via cargo 16, behoud van andere items, removals, grenscontrole en gescheiden clear. Full build: 2058 unit-tests en 482 GameTests; robot-inventory-views-final.log. Zie ROBOT_INVENTORY_VIEWS.md. getComponentInSlot/hardware-indexmapping, attack-XP, overige pariteit en live tests blijven open.

### Actie-XP na entityverwijdering

Ontbrekende RobotAttackEntityEvent.Post-handler hersteld: experience-upgrades krijgen eenmaal robotActionXp wanneer een robotaanval de entity direct verwijdert. Echte hardwaretest bewijst nul voor geannuleerde aanvallen, exact een beloning bij minecartverwijdering en geen beloning voor gewone hits of de vertraagde sterfanimatie van een mob. Algemene mob-orbverzameling is geen upstreamvereiste; eerdere brede verwijzing naar ontbrekende living-kill-XP daarmee verduidelijkt. Full build: 2058 unit-tests en 483 GameTests; robot-attack-xp-verified.log. Zie ROBOT_ENTITY_ATTACKS.md. Hardware-indexmapping, verdere combat/wereldactiepariteit en live tests blijven open.

### Robothardware terugvinden en juiste slots koppelen

getComponentInSlot-stub vervangen door lookup van actuele aangesloten hardware. Echte hardwaretest vond bovendien foutieve dubbele slotkoppelingen door geneste enumeratie tijdens geheugencontrole. MachineHost krijgt een compatibele default-overload met exact bronitem; SimpleMachine geeft dat item door en Robot gebruikt itemidentiteit in plaats van gedeelde iteratorstatus. Test verifieert meerdere componentinstanties, ongeldige slots en volledige vernieuwing na herbouw. Full build: 2058 unit-tests en 484 GameTests; robot-component-lookup-mapping.log. Zie ROBOT_INVENTORY_VIEWS.md. Legacy gecombineerde slotnamespace, hover-hardwarecontrole, containercomponenten en mappings bij andere hosts blijven open.

### Geinstalleerde hover-upgrades en vlieghoogte

Hovercontrole scant nu echte internalComponents in plaats van ongeldige gecombineerde inventarisindexen. Passieve hoverhardware heeft geen componentenvironment en werd daardoor gemist. Vier echte bewegingsproeven bewijzen beide tiers, tierbegrenzing, geen effect van een upgrade alleen in cargo en exacte energie/positie bij toegestane of geblokkeerde beweging. Full build: 2058 unit-tests en 488 GameTests; robot-hover-hardware-final.log. Zie ROBOT_INVENTORY_VIEWS.md. Overige hostmappings, gecombineerde API-slotnamespace, runtime containers en verdere portering blijven open.

### Slotkoppeling voor alle overige machinehosts

De bij Robot gevonden geneste-iteratorfout ook gereproduceerd en hersteld in computerkast, microcontroller, drone en rackserver. Alle vier koppelen aangesloten environments nu via het exacte bronitem; rack-busregistratie behouden. Vier concrete hosttests installeren CPU/RAM/EEPROM en controleren unieke volledige slotmapping na twee herbouwcycli. Full build: 2058 unit-tests en 492 GameTests; host-component-mapping-final.log. Zie COMPONENT_SLOT_MAPPING.md. Runtime robotcontainers, gecombineerde API-slotsemantiek en verdere portpariteit blijven open.

### Toelatingsregels voor verwisselbare robotcomponenten

Runtime slots 1..3 volgen nu het type en de tier van hun ingebouwde container; ontbrekende containers sluiten het slot, scherm/toetsenbord blijven uitgesloten. Tool/cargo mogen hardwareitems als gewone spullen bevatten, zoals upstream. Echte robottoets voor type/tier/missing container en cargo; oude tegenstrijdige source-stringtest verwijderd. Full build: 2057 unit-tests en 493 GameTests; robot-container-rules-verified.log. Zie ROBOT_RUNTIME_CONTAINERS.md. Dit is alleen toelating: live component-aanmaak/verwijdering zonder Lua-reboot, state-persistence en clientslotweergave blijven open.

### Verwisselbare robotcomponenten verbinden en bewaren

Runtime slots krijgen een aparte lifecycle: laden/verbinden, tick-updates, data opslaan voor verwijderen, loskoppelen en opnieuw verbinden zonder machineherbouw. Robot-save/load en inventory-clear meegenomen; componentmenuslots beperkt tot een item, direct geinjecteerde grotere stacks blijven intact maar inactief. Echte modemtest bewijst poort/adresbehoud bij hot-swap en reload, ongewijzigde draaiende architecture en geen oude verbindingen. Full build: 2057 unit-tests en 494 GameTests; robot-hot-swap-verified.log. Zie ROBOT_RUNTIME_CONTAINERS.md. Echte Lua-continuiteit/signalen, andere upgrades, runtime tanks en clientslotweergave blijven open.

### Lua-continuiteit en verwijdermeldingen bij hot-swap

Echte EEPROM/Lua-test houdt lokale tabeltoestand vast, ontvangt add/remove/add voor hetzelfde modemadres, controleert component.list en geopende poort, en detecteert onverwachte reboot via EEPROM-data. Vond ontbrekende component_removed voor neighbor-only nodes: zichtbaarheid is na edge-verwijdering al weg. SimpleMachine bewaart eerdere zichtbaarheid voor die melding; twee units bewaken precies een zichtbare verwijdermelding en geen verborgen componentmelding. Full build: 2059 unit-tests en 495 GameTests; robot-hot-swap-lua-signals.log. Zie ROBOT_RUNTIME_CONTAINERS.md. Wereldreload van Lua-coroutines, cached EEPROM-bootbron, runtime tanks en overige pariteit blijven open.

### Gewijzigde EEPROM daadwerkelijk uitvoeren na herstart

LuaArchitecture leest bij initialisatie de actuele EEPROM opnieuw. Echte robotreproducties bevestigen dat geprogrammeerde nieuwe code zowel na stop/start als na save/load van de gestopte machine wordt uitgevoerd; voorheen bleef oude bootSource actief. 497 GameTests groen; aansluitend test/build groen met 2059 unit-tests na correctie van drie null-hardwarelijstfixtures. Zie EEPROM_COLD_BOOT.md en eeprom-reboot-final/unit-verified.log. Live flashflow, Lua-geinitieerde restart en volledige coroutinepersistence blijven open.

### Verwisselbare tanks bereikbaar en leeg betrouwbaar opgeslagen

Robot-tankview bevat nu runtime tanks in equipment-volgorde voor vaste hardware. Test bewijst stabiele volgorde/count, directe verdwijning na verwijderen, exact1000mB behoud na terugplaatsen en blijvend nul na leegmaken/terugplaatsen. Runtime save schrijft naar bestaande driverdata zodat velden gewist kunnen worden; lege tanks verwijderen oude fluid/amount-velden. Full build: 2059 unit-tests en 498 GameTests; robot-runtime-tank-final.log. Zie ROBOT_RUNTIME_CONTAINERS.md. Tankcontroller-Lua en overige upgradeflows, clientmetadata en bredere portpariteit blijven open.

### Herstart vanuit Lua met zelfgeschreven EEPROM

Echte robot herschrijft via Lua zijn EEPROM, bewaart een marker, voert computer.shutdown(true) uit en start de nieuwe code. Oude code mag niet verderlopen of opnieuw starten; vervangende code bevestigt marker en blijft draaien. Test groen zonder aanvullende productiefix bovenop EEPROM-refresh. Full build: 2059 unit-tests en 499 GameTests; eeprom-lua-reboot.log. Zie EEPROM_COLD_BOOT.md. Volledige coroutinepersistence, overige upgradeflows en live verificatie blijven open.


## 2026-09-22 robot client container metadata

Opening payload bevat type/tier van alle drie vaste containers, inclusief onbeperkte floppy-tier. Clientplaatsing en schermoverlays volgen dezelfde descriptor als de server; ontbrekende containers tonen unavailable. Echte opening-buffer/clientmenu-test met zeven itemtypes, cargo en stacklimiet. Full test/build groen: 2059 unit-tests, 500 GameTests; robot-client-containers-final.log. Zie ROBOT_RUNTIME_CONTAINERS.md. Live visuele toets blijft open. Nieuwe prioriteit uit logs: rack-laadfout bij ontbrekende registry provider in ServerRackMountableEnvironment.load; green tests dekken dit nog niet.


## 2026-09-22 rack detached loading and persistence

Vanilla loadStatic reproduceerde rackverlies: mountables laadden inventaris voordat een wereld/registry beschikbaar was. Rack stelt aanmaak nu uit tot onLoad, bewaart ruwe data tussentijds en bouwt bestaande mountables niet opnieuw. Server herstelt hardware voor machine-state; server en diskdrive flushen componentdata voor itemserialisatie. Nieuwe test bewaakt alle serveritems, EEPROM-component, floppydata, apparaatadressen en herhaald onLoad. Full build: 2059 unit-tests, 501 GameTests; rack-persistence-verified.log zonder ERROR/Exception. Zie RACK_DETACHED_LOADING.md. Draaiende Lua-continuiteit, live visuele controle en volledige portmatrix blijven open.


## 2026-09-22 real running-rack gap and native persistence proof

Echte draaiende rack-EEPROM hervat niet na detached reload: gerichte opt-in test faalt met program restarted instead of resuming. LuaJ save/load bevat geen heap/coroutine. Archief tools/native-lua-probe/RunningRackPersistenceProbe.java blijft expliciete open releasegate buiten reguliere suite; groen 501 betekent geen persistencepariteit. Standalone OC-JNLua/Eris-proef op Java21 Windows x64 hervat wel locals, geneste coroutine, cyclische tabel en opnieuw gekoppelde Java-callback tussen twee JVM-processen, voor Lua5.2/5.3/5.4. Reproduceerbare hashgepinde runner toegevoegd. Nog geen runtime in mod ingebouwd. Zie NATIVE_LUA_PERSISTENCE.md voor bewijs, beperkingen en concrete native loader/API/scheduler/persistence/test-integratiestappen. Prioriteit verschuift naar echte native architectuurintegratie; overige portscope blijft ongewijzigd.


## 2026-09-22 native Lua state loader integrated

Hashgepinde JNLua-klassen en native resources met licentienotices gebundeld. NativeLuaState beheert drie VM-versies met geheugenlimiet, platformkeuze, close en beperkte bibliotheken; debug/Eris alleen hosttoegang, geen hostbestand/Java-libraries of binaire load. Echte NeoForge-test vond en bewaakt JNI-classloaderprobleem: JNI-klassen moeten in mod-classdirectories, niet aparte librarylayer of alleen resources. Full build groen: 2066 unit-tests, 502 GameTests; native-state-classdirs.log. Zie NATIVE_LUA_PERSISTENCE.md. Dit registreert nog geen CPU-architectuur; machine-API, instructietimeouts, callbacks en echte coroutinepersistence blijven de eerstvolgende integratiestappen. Windowsuitvoering bewezen, andere OS-uitvoering en live cliëntcontrole blijven open.


## 2026-09-22 native object graph persistence

NativeLuaPersistence bewaart/herstelt Eris-objectgraphs in versiegebonden NBT. Deterministische permanente native-functiebindingen, inclusief verborgen wrapper-upvalues; mutable globals/Lua-closures blijven onderdeel van snapshot. Private metamethodkey voorkomt uitvoeren van gewone user __persist bij save; stack/fout/close-contract getest. Twaalf tests over Lua52/53/54 bewijzen verse VM, locals/globals/nested coroutines/cycles/binaire strings/rebound callbacks, herhaald save/restore en ontbrekende bindingen. Full build:2078 unit-tests,502 GameTests; native-persistence-integrated.log. Zie NATIVE_LUA_PERSISTENCE.md. Default-LuaJ/running-rack blijft expliciet onopgelost tot native machine/API/scheduler-inpassing; overige portscope blijft open.


## Voortgang 2026-09-22: native machine hervat na rack-herlaad

- Native Lua5.2-architectuur met upstream-kernel, signal/sleep/shutdown en beide gesynchroniseerde callbackfasen geintegreerd; nog niet geregistreerd of standaard gekozen. Basis computer/component-binding; Unicode/OS/Value/OpenOS-pariteit blijft open.
- Echte native rack-GameTest hervat lokale Lua-state na detached NBT-load zonder reboot. Save heeft tijdelijke begrensde werkruimte; gedeelde string-metatable wordt hersteld zodat kernelpatroonfuncties behouden blijven. Unit-tests bewijzen callback eenmaal uitvoeren bij opslaan voor/na uitvoering en stoppen oneindige Lua/pcall/xpcall/coroutine-lussen met deterministische klok.
- Volledige test/build/GameTest-run:2085 unit-tests,503 verplichte GameTests groen; native-architecture-integrated.log. Geen client gestart of Modrinth-installatie. Default LuaJ blijft zonder heap-persistence: dit bewijs geldt uitsluitend voor de expliciet geselecteerde native testarchitectuur.
- Volgende stap: volledige native API/Value- en OpenOS-integratie, memory-pressure/save-failure-afhandeling, timer/handle/case/robot-herlaadmatrix en architectuurselectie. Zie NATIVE_LUA_PERSISTENCE.md; overige plan- en releasegates blijven open.


## Voortgang 2026-09-22: native Unicode en gameklok

- Native Lua heeft nu de tien Unicode-functies en OS clock/time/date; datumtabellen, tijdzoneprefix, game-ticks, codepunten en bestaande tekenbreedtes gedekt. LuaJ/native delen de bestaande Unicode-berekeningen.
- Nieuwe kerneltests: Unicode/sub/wide/errorgevallen, datumformaten/normalisatie/defaults, vaste tr-TR/en-US locale. Opgeslagen Unicode/date-functies en supplementary Unicode werken na beide gesynchroniseerde callback-herlaadfasen.
- native-unicode-os-integrated.log:2095 unit-tests en503 verplichte GameTests groen; build geslaagd. Geen client geopend of Modrinth-installatie. Native nog niet geregistreerd/default.
- Volgende werkpakket blijft native userdata/OC Value- en handle-persistence, resterende API-contracten en werkelijk OpenOS-booten. Overige persistence-, platform- en portgates blijven open; zie NATIVE_LUA_PERSISTENCE.md.


## Voortgang 2026-09-22: native OC Value-objecten en open HDD-handles

- Native userdata ondersteunt OC Value properties/call/methods/doc/dispose en raw NBT save/load. Lua-kernel bewaart proxy-identiteit en gedeelde verwijzingen; beide callback-opslagfasen hervatten een nieuw Java-object zonder dubbele wereldactie. LuaJ/native delen dezelfde argumentvalidatie.
- Echte rack-GameTest: HDD met binair bestand openen, een byte lezen, rack detached opslaan/herladen, resterende bytes vanaf bewaarde positie lezen en handle sluiten. Dit bewijst HDD-handlecontinuiteit, niet automatisch tmp/case/robot.
- native-userdata-handles.log volledig groen:2097 unit-tests en504 verplichte GameTests; build geslaagd, geen ERROR-regels. Geen Minecraft-client gestart/installatie uitgevoerd; native nog niet standaard geregistreerd.
- Volgende stappen: daadwerkelijk native OpenOS booten, resterende API-contracten/architectuurkeuze, tmp-filesystem-persistence en overige reload/memory/platform-gates. Zie NATIVE_LUA_PERSISTENCE.md.


## Voortgang 2026-09-22: tmp-filesystem na herladen

- SimpleMachine bewaart nu tmp-node/adres, inhoud, eigenaars en open handles. Laden sluit de oude architectuur voor het vervangen/herstellen van tmp, verbindt het herstelde filesystem voor Lua-load, en maakt tmp leeg voor oude saves zonder tmp-tag. Reboot-wisinstelling blijft behouden.
- Nieuwe native rack-test reproduceerde 'tmp address changed'; na fix hervat hetzelfde programma de binaire read-handle vanaf de bewaarde positie met hetzelfde tmp-adres. Unit-tests dekken herhaald laden en legacy-save zonder tmp.
- native-tmp-first.log:505 GameTests groen; native-tmp-integrated.log: volledige2099 unit-tests en build groen. Geen client/installatie/push. Native nog niet geregistreerd/default.
- Volgende gate: daadwerkelijk native OpenOS booten via bestaande computer/robot-fixtures; resterende API/architectuur-, timer/uptime-, memory/error- en platformchecks blijven open.


## Voortgang 2026-09-22: native OpenOS-shell werkt op tier1

- Volledige native BIOS/OpenOS-floppyboot op tier1 CPU/GPU/192KiB RAM, shellprompt en getypt echo-commando nu bewezen in NeoForge GameTest. Geen hardwareverzwaring om de test te laten slagen.
- Eerste run stopte met geheugentekort: ontbrekende upstream64-bit RAM-factor1.8 hersteld, plus maxTotalRam64MiB-config en ongeschaalde Lua-geheugenrapportage. Snapshot bewaart schaal voor kernelbaseline-herstel. Allocatielimiet blijft getest.
- native-openos-ram-scale.log:506 GameTests groen; native-openos-integrated.log: volledige2100 unit-tests/build groen. Native nog niet geregistreerd/default; geen client/installatie/push.
- Er resteert substantieel portwerk: volledige OpenOS-session reload/timers, overige native API/architectuurkeuze, error/platformmatrix, ontbrekende features en volledige ingame/visuele/integratietests. Geen betrouwbaar compleet-percentage uit testaantallen afleiden.

## Native OpenOS shell reload: 2026-09-22

Native OpenOS now passes a detached case reload with a shell environment variable: set ocresume=731 before saving, echo $ocresume after restoring a fresh block entity, requiring standalone output 731. The extended fixture has a powered converter and asserts no network energy loss during synchronous reload. Initial NoEnergy was insufficient fixture power; export was also corrected to the bundled OpenOS set command.

SimpleMachine persists accumulated uptime across different clock epochs and repeated loads, resetting on restart. Fixed-clock regression reproduced 2.5 seconds becoming zero before the fix. Current wall-clock semantics still differ from upstream tick-based uptime; timer/deadline parity remains open. The case test does not reload the external floppy or the whole world. Native VM disposal on case removal also remains open.

Verification: native-openos-reload-powered.log all507 GameTests passed; native-openos-reload-integrated.log full2101 unit tests with zero failures/errors and successful build. Artifact SHA256 69B7C8A6B9D1A0E2A452B69A828375DA5BFB5675D3824411B4AC4A8EC9248BA7. No client/install/push; native still unregistered/default LuaJ. Next client launch awaits the requested coordination for brief focus use; independent development continues. Full port remains incomplete.

## Tick-based uptime and native deadline continuation: 2026-09-22

SimpleMachine now follows upstream Machine.scala uptime semantics: advance one tick per running update, including sleep/pause, report ticks/20. Stop/restart resets. Saves use a long uptime tick counter; previous uptimeSeconds snapshots migrate on load. Offline wall time does not count. This supersedes the earlier wall-clock uptime caveat.

uptime-ticks-red.log reproduced three focused failures against the old wall-clock implementation. Updated unit tests exercise fixed-clock jumps, sleeping/paused updates, stop/restart, repeated reloads and old seconds snapshots. nativeRackRetainsUptimeDeadlineAfterReload preserves a five-second deadline, checks elapsed ticks survived detached reload, then continues computer.pullSignal until the deadline. This proves basic native deadline continuation, not all OpenOS event timers or full-world restart.

uptime-ticks-integrated.log passed all2102 unit tests with zero failures/errors, all508 required GameTests and build. Artifact SHA256 0AB8E04BCF9DCDEAE27C1310988D293E4E1163C15161CC1EDDE6E8F8DDEE5788. No client/install/push; screen3 focus coordination is still pending. Next: native VM disposal on host removal, world restart, OpenOS timers, remaining architecture/API/platform/visual requirements. Full port incomplete.

## Tablet runtime gap - 2026-09-22

Inspection found only TabletItem assembly/charge data plus a temporary TabletAnalysisHost. There is no persistent tablet VM, inventory tick integration or terminal menu. Existing tablet tests prove data/driver contracts, not the player flow.

Implement in order: a Tablet machine host with stable decoded components, built-in 80x25/four-bit screen, tablet component, keyboard connections, energy and save/dispose behavior; a server cache and inventory/use integration; terminal input/output through existing TerminalMenu; tier-dependent component editing and delayed block analysis; lifecycle and full OpenOS/native regression tests; live client acceptance on display3. Reuse SimpleMachine and existing screen/networking rather than a second VM or UI transport.

Upstream common/item/Tablet.scala caches wrappers, saves components, retains execution for dimension changes, but clears running state on ordinary cache eviction. Verify these distinct semantics rather than promising universal resume after logout/dropping. Full-world/server lifecycle coverage remains required. No additional plan approval is needed under the user's instruction to execute the complete port.

## Tablet implementation checkpoint - 2026-09-22

The earlier tablet gap section is historical: runtime, item use, terminal, analysis, cache, save/drop/death/container/dimension handling, full BIOS/OpenOS shell continuation and the one-slot expansion editor are now implemented with server-side regression coverage. The editor follows upstream container slot type/tier limits and locks the held tablet. Final tablet-editor-final.log: build,2108units,all544GameTests pass. See [[../04 ai-context/Tablet runtime]] for exact scope and evidence.

Still required: writable OpenOS installation through the complete player flow, actual full process restart, external automation paths and real-client visual/input acceptance on display3. Existing wider parity, platform, multiplayer/survival and release gates remain in scope; this checkpoint is not full-port completion.

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
| Terminal-GUI-performance | Eerste fix getest: OpenOS 6 → 60 FPS; vol scherm 31 FPS, aanvullende varianten open |
| UI-vertalingen/foutfeedback | Gepland in eerste uitvoeringsronde |
| Robotterminal/layout | Open |
| Lua execution persistence | Open, architectuurwerk noodzakelijk |
| Capacitors/netwerk/energie | Open |
| Alle overige functionele/visuele/integratie-scenario's | Te verifiëren; bestaande tests niet als volledige pariteit tellen |

Resultaten hieronder tijdens uitvoering aanvullen met commit en bewijs. Alleen afvinken wat daadwerkelijk is aangetoond.

Startregister: `PORT_PARITY_MATRIX.md`. Aanvullende bronvergelijking vond onder meer robot-ROM, kabelkleuren/-geometrie, hover boots, debugger, presents en Lua-geheugenrapportage. Deze vallen onder de bovenstaande werkpakketten en blijven open totdat ze zijn uitgevoerd en getest.

### Uitvoering 2026-09-22

Eerste runtimewijziging: GUI-pixels bundelen en glyphlookup eenmaal per letter uitvoeren. Build + 2041 tests + 440 GameTests groen; OpenOS-boot, invoer, Unicode en blokweergave ingame gecontroleerd. Zie `TERMINAL_GUI_PERFORMANCE.md` voor reproduceerbare opstelling, hashes, profileresultaten, screenshots en beperkingen. Tijdelijke testopstelling verwijderd en lege ruimte teruggelezen; client netjes afgesloten.

Eerstvolgend: volle terminal/achtergrondbelasting verder profileren, vervolgens fase 1B (titel/foutfeedback) en fase 1C (robotterminal), naast het verder invullen van de volledige pariteitsmatrix. UI-vertalingen en robot/runtime-persistentie zijn nog niet geïmplementeerd in deze uitvoeringsronde. Lange invoerbursts apart onderzoeken; korte ingame commando's werkten.

### Doorlopende uitvoering — titel en foutfeedback

Microcontroller-titel hersteld. Computer-case NoEnergy/NoCPU gebruiken nu vertaalbare componenten met leesbare fallback; vrije Lua-fouttekst blijft letterlijk en alleen de eerste regel wordt getoond. Gerichte tests faalden eerst op ontbrekende titel en ruwe foutkey, daarna alle 26 groen; volledige build en 442 GameTests groen in deze ronde. Ingame titel screenshot: `build/finish-port-implementation/microcontroller-title-verified.png`. Clientlog bevestigt `Computer error: Not enough energy.` met geïnstalleerde JAR `731F3F1EF4E792BE9F4B88B18AEC9B3E78EE896791FAA949617D57CA94CA0372`. Foutfeedback van andere hosts en overige fase-1B-visuele controles blijven open.

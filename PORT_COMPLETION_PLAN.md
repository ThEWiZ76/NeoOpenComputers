# Complete OpenComputers-port — uitvoeringsplan

**Doel:** de bruikbare functionaliteit van OpenComputers `master-MC1.12` overbrengen naar Minecraft 1.21.1 / NeoForge, inclusief Lua/OpenOS, apparaten, interfaces, opslag, rendering, recepten, documentatie en geteste integraties.

**Aanpak:** bestaande Java-implementaties afmaken en vergelijken met upstream. Kleine wijzigingen per laag; bestaand gedrag beschermen met gerichte tests. Een groen testpakket bewijst geen visuele of volledige functionele gelijkwaardigheid.

## Uitvoeringsprioriteit — correctie van de gebruiker, 2026-10-20

Eerst volledige technische functionaliteit en stabiliteit; look-and-feel en polish pas als alles werkt. De handleiding-/JEI-presentatie kreeg te veel aandacht. Rond alleen de al ingezette functionele fout af waarbij server-only loot-disks niet via JEI opzoekbaar zijn; geen verdere tekst-, categorie- of layoutpolish in deze fase.

Vervolgorde: ontbrekende kernfuncties en ROM/OpenOS-gedrag, duurzame opslag/herladen en machine-/netwerkgedrag, noodzakelijke integraties, daarna brede functionele client/server- en herstartacceptatie. Cosmetische verschillen worden verzameld voor de eindfase. Niet-werkende invoer, ontbrekende toegang tot functies en onbruikbare terminalperformance blijven functionele problemen; het bestaan van een GUI is geen reden om die uit te stellen. Deze prioriteit gaat boven de historische volgorde van onderstaande checkpoints. Beta-inschatting blijft voorlopig3-6 geconcentreerde werkweken met lage zekerheid, herijken op resterende technische featuregroepen.

**Werkplek:** `feature/finish-port`; startpunt `b05faaf8c`. Referentie: lokale upstream-checkout `.upstream-opencomputers-master-MC1.12`. Runtime: Java 21, NeoForge 21.1.234, Minecraft 1.21.1, Gradle/JUnit/GameTests en de specifieke Modrinth-testinstance.

**Uitvoering:** plan gevolgd door directe uitvoering is door de gebruiker gevraagd. Geen extra plan-goedkeuringsronde. Tussentijdse commits pushen naar `origin/feature/finish-port` is op 2026-10-07 expliciet gevraagd; lokale en remote SHA na push vergelijken. Geen automatische merge/publicatie. GitHub Actions blijven uit. Hoogstens één gecombineerde reviewerpass voor een grote/risicovolle implementatie; geen reviewer voor dit plan of kleine fixes.

**Voortgangsupdates:** de gebruiker vraagt sinds 2026-10-07 ook een tussentijdse schatting van de afstand tot een beta met alle afgesproken features geimplementeerd. Rapporteer resterende featuregroepen en acceptatiegates; geen percentage op basis van testaantallen. Voorlopige orde van grootte op die datum: 3-6 weken geconcentreerd implementatie- en testwerk, met lage betrouwbaarheid zolang de volledige pariteitsinventaris en integratiescope open zijn. Dit is een werkschatting, geen leverdatum; bij nieuw bewijs herijken.

## Microcontroller-netwerken: implementatie en resterende acceptatie

Checkpoint 2026-10-20: c125614e99c802ab724818be2548433b2bef25ed is gepusht. Build, 2129 units en 701 gewone GameTests slagen; echte herstart in oc-restart-remap-20261020-01 slaagt met 702 tests in zowel prepare PID34520 als verify PID48236. Geen client-/Computer Use-actie. Beta-inschatting blijft voorlopig 3-6 geconcentreerde werkweken, lage zekerheid; dit sluit een concrete netwerk-/reloadfout, geen volledige microcontroller- of clientacceptatie.

Bron: upstream Microcontroller.scala, traits/Hub.scala en util/ExtendedArguments.scala; vergeleken op 2026-10-20. Runtime be5ceb8c3babae9d39d4959388f11ea446e43961 implementeert de netwerkscheiding en zes callbacks. Build, 2131 units en alle 707 gewone GameTests slagen, inclusief twee echte native Lua-machines voor externe start/configuratie/ping-pong/stop en de drie bestaande microcontroller-hervatvarianten met bewaarde interne proxy/uitvoerflag. Zie [[../03 manuals/Microcontroller networking]] voor contracten, logs en compatibiliteitsgrenzen. Eerdere verbonden-node-adresherstel in c125614e9 blijft een prerequisite; be5ceb8c3 herstelt ook de gereproduceerde volgorde van split/removal-callbacks.

- [x] Vijf externe aansluitingen, geen aansluiting op de voorkant. Elke zijde houdt een eigen netwerk; node() geeft upstream-conform geen gemeenschappelijke node terug. Interne hardware blijft op een afzonderlijk machinenetwerk met een microcontroller-component/energiebuffer.
- [x] Externe microcontroller-componenten bieden start, stop, isRunning, lastError, isSideOpen en setSideOpen. Start hervat geen gepauzeerde machine. Zijdeargumenten zijn wereldrichtingen: buiten 0..5 geeft invalid side, de voorkant unsupported side; setSideOpen geeft de vorige waarde terug.
- [x] Eén primaire ontvangst-/controlenode per extern netwerk, ook na samenvoegen/splitsen. Inkomende modem-pakketten bereiken intern de machine; interne pakketten gaan uitsluitend via geopende uitgangen. Een gesloten uitgang blijft fysiek aangesloten en ontvangt nog steeds input/control. Geen passieve doorsturing tussen externe netwerken: de microcontroller-override roept Hub.onPlugMessage niet aan, dus de gewone hubqueue/TTL-verwerking is hier niet van toepassing.
- [x] Voeding scheiden: eigen instellingen/buffer/FE, externe transfer en frontblokkade geimplementeerd. Checkpoint7cd2fa09d toetst native lopend/gepauzeerd/gestopt verbruik, nulverbruik, ignorePower, atomair NoEnergy en custom-capacity64/legacy317. ProjectRed-only en ProjectRed+CC beide709 tests; build/2131 units groen. Waarden boven de nieuwe geconfigureerde capaciteit worden expliciet begrensd zoals andere connectorloads; willekeurige live-configwijzigingen/historische referentiemigratie blijven aparte acceptatie.
- [x] Opslag, detached-load, unload-before-save en hergebruik van dezelfde instantie bewaren machinecontinuiteit, nodeadressen, uitvoerflags en energie. Front-analyse toont de interne machine; andere zijden tonen hun controlenode. Hardwareherbouw mag de interne snooperverbinding niet verliezen.
- [ ] Echte serverregressies voor isolatie, callbacks, argumentfouten, primair gedrag bij merge/split, modemroutering en energie; native Lua-programma met geinstalleerd modem en externe computer die de microcontroller bestuurt. Bestaande native microcontroller- en redstone-tests behouden. Daarna echte client-/spelersflow op scherm3 en bredere herstartacceptatie.

Aanvullend checkpoint 156db2b6b093fea856337e6dd69b340bf196452c: nieuwe voorkant koppelt bij rotatie correct los, vrijgekomen zijde verbindt opnieuw; vier draaistanden met zes echte kabels, wereldgeometrie en FE-zijden getest. Build/2131 units/708 gewone GameTests groen. Werkelijke JVM-herstart met deze microcontroller-topologie en ComputerCraft slaagt in oc-restart-microcontroller-20261020-01: prepare PID58500 en verify PID56588 beide711 tests, normale afsluiting. Behouden: machine/plug/controladressen, intern Lua-proxy/uitvoerflag, modempoort en open tmp-handle; ruwe disk-energie/flags voor ticking gecontroleerd. Bestaande native computer/tablet- en vier-host-redstoneprobes slagen ook.

Checkpoint d7a8cb73490e035718b5e95c1514817212e94d1c: assembler-item daadwerkelijk geplaatst, Survival afgebroken en opnieuw gestart; tier-twee pick/Creative break en creative-tier niet-speler-break/terugplaatsen getest. Eén samengesteld item behoudt hardware, binary EEPROM-data, hardwareadressen en gehele energie. Geen VM/tmp/externe netwerknodes in het item; terugplaatsen begint gestopt. Ook de gereproduceerde lege-snooper-merge bij BlockItem-plaatsing hersteld. Build/2131 units en alle712 gewone GameTests met ProjectRed+CC groen; zie [[../03 manuals/Microcontroller networking]]. Templatefixture gebruikt immediate assembly, dus geen claim over assembler-GUI/energietiming.

Nog open: GUI/analyzer/kabelbeeld en volledige survival/multiplayer/assemblage. ProjectRed/gemengde providers en de gerichte energie/configmatrix hebben bewijs op7cd2fa09d. Het nieuwere checkpoint hieronder sluit immutable hardware en gewone klik/sneak-EEPROM-callbacks; volledige protection/clientacceptatie blijft open. Geen client- of Computer Use-actie in dit checkpoint. Beta blijft voorlopig 3-6 geconcentreerde werkweken met lage zekerheid.

Checkpoint bc17524aeb24e2c1bebcbab3f3579661754a671d: immutable hardware en gewone klik-power/sneak-EEPROM-callbacks hersteld. EEPROM-hot-swap behoudt de native VM en modem; reboot gebruikt nieuwe chipcode. Offhand/volle inventaris geeft precies een oude chip terug. Bestaande GUI is alleen-lezen inspectie via sneak lege hand, met achtblok-afstandscontrole. Vijf interactietests en een echte chunkgrenstest toegevoegd; de laatste reproduceerde onbedoeld laden door vanilla input sampling en is met een eerste-hop-guard hersteld. Build/2131 units/718 ProjectRed+CC-tests en beide721-test herstartprocessen groen in oc-restart-microcontroller-immutable-20261020-01 (PIDs16144/44436). Vastgelopen rode runs/logs/dumps bewaard; oorzaak van elke historische unload-lus niet als bewezen voorgesteld. Geen client-/Computer Use-actie. Volledige assemblage, GUI/visueel, protection en multiplayer blijven open. Beta-inschatting blijft 3-6 geconcentreerde werkweken, lage zekerheid; herijken na iedere mijlpaal en relevante scopewijziging. Deze schatting bedoelt volledige implementatie plus beta-acceptatie; geen featurepercentage uit testaantallen.

Checkpoint c27ef60642e42ff32e430d9c3a9973bcfcfec5da: chunkloader-dimensiebeleid met moderne IDs, whitelist/blacklist, exacte handmatige fout en stille automatische weigering geimplementeerd. Gereproduceerd en hersteld: tickets blijven in de oude wereld na dimensiewissel; disabled-save behoudt oude active=true; opgeslagen verboden tickets worden zonder startupvalidatie toch geladen. Build/2132 units en beide725-test processen in oc-restart-chunkloader-policy-20261020-01 groen (PIDs59012/43124), ProjectRed+CC. Drie gewone regressies, een unit en een echte beleidswijziging tussen JVMs beschermen dit. Oude numerieke configuratie expliciet omzetten naar namespaced IDs; volledige live portal-/client/multiplayer/platformacceptatie blijft open. Zie [[../03 manuals/Chunkloader dimension policy]]. Beta blijft 3-6 geconcentreerde werkweken, lage zekerheid; geen client-/Computer Use-actie of productieconfigwijziging.

Checkpoint 9b53261c36574ddfe75115222062f3d01e6ea4ea: vijf ontbrekende robot-control/naamcallbacks hersteld; native programma en exacte stopped/paused/resultaatsemantiek getest. Gedeelde SimpleMachine.start retourneert false zonder slaaptiming te resetten wanneer al actief; vaste-kloktests bewaken direct/component gedrag en behouden directe API-pauseresume. Niet-lege namen/Unicode/witruimte blijven in world/item-NBT; client-tagvorming/toepassing getest, geen live leverings-/renderclaim. Build/2135 units en beide727-test processen in oc-restart-robot-control-20261020-02 groen (PIDs59064/59708). Volgende bevestigde gap: upstream random standaardnamen via robot.names, lege-naam-regeneratie en initiele assembler/drone/itemnamen. Forestry getDrone en CoFH access-kandidaten correct als integraties geclassificeerd. Zie [[../03 manuals/Robot control callbacks]]. Beta blijft 3-6 geconcentreerde werkweken met lage zekerheid; geen client-/Computer Use-actie.

Checkpoint ada35429256ae8b1695415fce21b9c5b48fa7ab9: originele135-namenresource, defaultnaamgeneratie, assembler-display/stored namen, robotplaatsing met aambeeld-voorrang en naambehoud bij robotdrop/drone-opname hersteld. Missing/empty werelddata genereert een naam die de volgende save/load behoudt; niet-lege witruimte blijft. Drie nieuwe servertests, bestaande drone-eigendomstest uitgebreid en drie gerichte units met vaste seed. Build/2138 units en alle726 gewone ProjectRed+CC-tests groen. Geen nieuwe twee-JVM-run of client-/Computer Use-actie; live naamweergave/anvil/multiplayer, named pick-block, bredere migratie en agentnaam-diskacceptatie blijven open. Zie [[../03 manuals/Agent names]]. Beta blijft3-6 geconcentreerde werkweken, lage zekerheid. Volgende brede werkpakket: moderne integratiescope en concrete inherited callback-contracten verder inventariseren.

Checkpoint 820fee37bb656273720d460aaede4eeaa80ae4e8: EnderStorage1.21.1-2.13.0.191/CCL4.6.1.529 optioneel gekoppeld aan adapter met de vijf upstream frequentie/kleur/eigenaarcallbacks. Echte chest/tank, alle16 kleuren, private UUID/naam en public-owner, save/load en native Lua via fysieke adapter bewezen. Build/2138 units;728 gemengde tests,728 EnderStorage-only tests en726 tests zonder optionele mods groen. Geen bundeling/harde POM-afhankelijkheid; geen clientactie. MODERN_INTEGRATION_SCOPE.md splitst alle171 integratiebronbestanden in21 groepen plus metadata uit met bekende/open contracten; geen legacygroep stilzwijgend geschrapt. AE2 en JEI hebben geverifieerde moderne doelen maar zijn nog open. Volgend grote pakket: AE2-netwerk/storage/crafting. Beta blijft3-6 geconcentreerde werkweken, lage zekerheid.

Checkpoint 5b60fee0b7577cd2a95c831969903a9237e695eb: eerste AE2-controllerkoppeling hersteld met11 netwerk/storage/CPU/energiecallbacks, lang-aantallen/filtersemantiek en veilige databasebeschrijvingen. Echte drive/cellen/CPU, energie en native Lua via fysieke adapter getest; build/2140 units,730 gemengde/728 AE2-only/726 zonder optionele mods groen. Geen verplichte gepubliceerde/bundelde AE2-afhankelijkheid. Volledige crafting-userdata/persistence, interface/bus/converter/hulpintegraties en clientacceptatie blijven open. Zie [[../03 manuals/AE2 controller integration]] en [[AE2 integration]]. Beta blijft3-6 geconcentreerde werkweken, lage zekerheid; geen client-/Computer Use-actie.

Checkpoint f36740a717c5b9ee121f51f2ab91aca8ef3bf189: AE2 getCraftables plus Craftable/Status userdata geimplementeerd: echte asynchrone berekening, serverthread-indiening, named CPU, werkelijk assemblerresultaat, fouten en processing-annulering. Gereproduceerde craftable-only catalogue-hoeveelheid hersteld naar receptaantal; alias-herladen deelt een aanvraag zonder dubbele indiening. Build/2140 units,731 gemengde/729 AE2-only/726 absent tests groen. Echte JVM-herstart, completion-terwijl-host-unloaded en bredere failure/cleanupmatrix blijven eerstvolgende gates; NBT-/aliasbewijs is geen volledig herstartbewijs. Zie [[../03 manuals/AE2 crafting integration]]. Interfaces/bussen/converters/hulpintegraties en client blijven open. Beta3-6 geconcentreerde werkweken, lage zekerheid; geen client-/Computer Use-actie.

Checkpoint 2b4e1b752ddeda019c877841b135e82325022f8a: AE2 crafting-wereldjournal herstelt actieve aanvragen onafhankelijk van de OC-heap en bewaart compacte uitkomsten voor oudere snapshots. Echte herstart reproduceerde verloren offline-completion; trace reproduceerde te late startup-bind, opgelost op level-end na AE2-node-init. Drie opstellingen bewijzen actieve link, vooraf afgeronde offline-aanvraag en100ticks zonder OC-host voordat de aanvraag afrondt en oude heap terugkomt. Exact6logs/4planken/1charcoal; build/2141 units; twee736-test JVMs in oc-restart-ae2-crafting-20261020-06, PIDs53528/54476;726 absent tests groen. Zie [[../03 manuals/AE2 crafting restart]]. Dit is BE-unload/hostafwezigheid, geen volledige chunk-ticket/multiplayer/clientmatrix. Save-during-computing, failurematrix en overige integraties blijven open. Beta3-6 geconcentreerde werkweken, lage zekerheid; geen client-/Computer Use-actie.

Checkpoint b40e4be50d388502ca9c2dddc34141f8ec777998: blok-/paneelinterfaces delen nu12netwerkcallbacks met echte native crafting; import/exportfilters per wereldzijde, providers, databasekopie zonder fysieke items en0.5pauze hersteld. Null-face/internal-node fout en gemengde driver-NBT-keybotsing gereproduceerd/hersteld. CPU_BUSY/CPU_TOO_SMALL/NO_CPU_FOUND getest op3hostroutes met exacte ingredientbehoud. Build/2141 units;741 tests per JVM in oc-restart-ae2-interface-20261020-04, PIDs56872/33420;736 mixed/726 absent groen. Een hergebruikte-wereld unload-loop bleef in PID8348 hangen; twee dumps/log bewaard, eigen proces gestopt, oorzaak niet als opgelost voorgesteld. exportIntoSlot, converter/hulpintegraties, resterende failure/unload/clientmatrix blijven open. Zie [[../03 manuals/AE2 interfaces and bus configuration]]. Beta3-6 geconcentreerde werkweken, lage zekerheid; geen client-/Computer Use-actie.

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

## Tablet writable installation checkpoint - 2026-09-22

The writable OpenOS installation gate now has an actual installer regression: insert bundled media through the editor, boot BIOS/OpenOS, run install --noreboot and confirm, remove media through the editor, boot installed HDD, then write/read a home file through shell commands. No preinstalled filesystem fixture. Final tablet-install-final.log: build/2108units/all545GameTests pass. This supersedes the writable-installation item above for this native tablet setup. Real assembler GUI/survival flow, actual process restart, wider variants and client visual acceptance remain required.

## Actual process restart checkpoint - 2026-09-22

Opt-in ProcessRestartGameTests now spans two GameTestServer JVMs and normal shutdown/world/player saves. A native computer retains Lua locals and an external floppy open-file offset; the dedicated player's tablet reloads newest component data in stopped state after orderly logout. Verified separate PIDs67064/64320, both546 tests, build/2108units green in verify. See [[../03 manuals/Process restart test]] for repeatable commands and preserved logs/world. First attempt exposed only insufficient fixture power before the verification batch; power now starts on the first world tick. Broader host restart, crash recovery, world settings, migration, survival and client/visual gates remain in scope.

Export checkpoint 37fb64458a819bd3e45377174a1cabdf69e7bf41: actual exportIntoSlot with sided inventory/slot handling, speed/fuzzy filters, powered machine extraction, rejected-remainder refund/drop and exact item conservation. Ten new server/native tests; build/2141 units/746 mixed/726 absent pass. See [[../03 manuals/AE2 manual export]]. Converter/auxiliary, remaining crafting/stability/client gates stay open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.

Auxiliary AE2 checkpoint cf37b92c85bf5cbfb0979427e9b811959a569b97: real item/fluid cell descriptions, basic-cell capacity/content metrics, live/wrapper support and AE2 tagged-wrench OC use restored. Default spatial strategy already preserves OC print NBT/orientation; no redundant mover added. Four new tests; build/2141 units/750 mixed/726 absent green. See [[../03 manuals/AE2 cell descriptions and auxiliary hooks]]. Full spatial/client and remaining crafting/stability/global integration acceptance stay open. Beta3-6 concentrated workweeks, low confidence; no client/Computer Use action.

AE2 failure checkpoint ece63b5f4470c66d2135391ad051533d5c40dfb3: reproduced/fixed late named-CPU lookup that accepted a same-name replacement. Capture actual CPU before calculation; disappearance returns cpu_offline. Fifteen real controller/block/panel tests prove CPU removal/replacement, missing/same-kind-replaced hosts and submitted-host failure with exact 16-log conservation/alias cleanup. Build/2141 units/765 mixed/726 absent; actual770-test prepare/verify JVMs in oc-restart-ae2-failures-20261020-01, PIDs45520/38640. See [[../03 manuals/AE2 CPU and target failures]]. Computing-save/multi-grid/future/load/client and global integration gates remain open. Beta3-6 concentrated workweeks, low confidence; no client/Computer Use action.

Mekanism checkpoint 8e852cb544df38e7bba0becd166a4e6ab64d0465: optional chemical descriptions/native configurator ability mapping, exact long/native53 proof, generic FE item charging and original OC wrench eligibility/notification restored. Adapter/charger honor usability; ProjectRed charger usage consumes once; drone keeps upstream type-only pickup. Seven new Mekanism tests plus one core test and artifact/POM guard; build/2142 units/773 mixed/734 Mekanism-only/727 absent pass in isolated worlds with clean exit. Native cube/cable transfers1000FE to OC. Reused-world shutdown unload loop and discarded premature world-lock run documented, not claimed fixed. See [[../03 manuals/Mekanism integration and FE item charging]]. Client/multiplayer/load and other integration/semantic gates remain open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.


CC relay checkpoint 68650919030cd096146fcada74ea159726017ffa: original14 modem/remote methods, optional public capability, per-attachment channels/filtering, existing queue and real component context restored. Leading reply-channel and CC/native UTF-8/binary/table conversion defects fixed with actual-runtime evidence. Five new tests; build/2142 units/778 mixed/732 CC-only/727 absent pass, with native53 ping/pong and actual CC Lua. See [[../03 manuals/ComputerCraft relay integration]]. Peripheral/media/Lua-object and real-client/survival/multiplayer/platform/load acceptance remain open; beta3-6 concentrated workweeks, low confidence. The first732-test CC-only shutdown hung in a NEW world (verifiedPID56496, two matching dumps, then owned stop); retry exits cleanly with loopback JDWP enabled. The unload loop is therefore not restricted to reused worlds and remains unproven/unfixed. Prior reused-world734/773 clean exits are not fix evidence. No client/Computer Use action.


Robot query checkpoint ddc7515421376e939a2984387e76e082ff82cafa: shutdown trace proves36 generation tasks requested through robot animation-cancel collision lookup; shared non-loading robot/afterimage geometry and redstone lookup fixes3 deterministic regressions. Build/2142 units/781 mixed/735 historical CC-world/730 absent pass. Both traced final shutdowns request0 generation tasks and exit normally. See [[../03 manuals/Robot non-loading queries]]. This fixes the reproduced robot reload route; every historical freeze/full unload stress is not claimed closed. Remaining integration/semantic/client/survival/multiplayer/platform gates stay open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.

CC peripheral checkpoint be937fd62efab97248ecfe65a1916bb7ab60dc51: optional sided/native/generic adapter bridge with original blacklist/native precedence, per-context lifecycle/events/tasks and binary-safe Java arguments. Actual native53 printer and chest transfer pass; transient mounts prove readonly/binary/seek/append/non-truncating read/write handle restore and detach-failure cleanup. Six new tests; build/2143 units/787 mixed/741 CC-only/730 absent pass with normal exit. See [[../03 manuals/ComputerCraft peripheral integration]]. Media/Lua-object, process persistence of external mounts and broader client/survival/multiplayer/platform/load acceptance remain open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.

CC media block-drive checkpoint a6f0c3905487863bcbaad501506c8af137914766: public media-capability driver with native labels/storage/ID and stable OC identity. Fixed block-drive save-before-eject/split/take/replacement/clear and deferred world-assignment initialization. Five new regressions; build/2143 units/792 mixed/731 absent and actual750-test CC-only prepare/verify JVMs (PIDs4208/50868) pass with normal exit, native Lua local/external open-file offset and CC-native byte/label readback. See [[../03 manuals/ComputerCraft media and diskdrive persistence]]. Rack/internal-host media removal/load/restart is next and the media checklist remains open; then Lua-object and broader client/survival/multiplayer/platform/load gates. Beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.

Media host checkpoint c1094c68a1a419a178c50e3c43e88d24e142cbb4: restored upstream rack main-node identity, targeted pre-removal component persistence, rack-drive/case/server split/take/replacement/clear state and deferred world-bound case initialization. Five regular regressions and two native CC-media host restart probes added;2143 units/797 mixed/735 absent and757 CC-only tests in each actual JVM (PIDs6864/47060) pass with normal exit. Native case/rack locals, identities and open offsets plus native CC bytes/labels verified. See [[../03 manuals/Media host persistence and rack main bus]]. Tablet/robot/full-module-detach, Lua-object and broader client/survival/multiplayer/platform/load gates remain open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.

Portable-media checkpoint aeae0a776273805c475b11cae42883fc5b5d7938: seven reproduced storage/menu/detach regressions fixed; targeted component snapshots before menu copies, tablet expansion persistence and rack child-filesystem cleanup. Native CC tablet/robot continuation and whole-module channel cleanup verified. Build/2143 units/807 mixed/742 absent/767 tests in both normal restart JVMs pass. See [[../03 manuals/Portable media and menu persistence]]. Other menus/automation, portable two-JVM proof, migration and full client/survival/multiplayer/platform gates remain open. No client/Computer Use action. Beta3-6 concentrated workweeks, low confidence.

Adapter upgrade checkpoint 1c688f06d2768623e553d31dd13ffed049b7a2c6: actual item ownership replaces driver-only reuse; component snapshots precede menu/container copies and world inventory encoding; legacy host data is consumed once; onLoad restores world-bound upgrades and neighbor network. Seven baseline regressions plus two reload failures reproduced;10 ordinary tests and a native Lua5.3 two-JVM probe added. Build/2143 units/817 mixed/756 tests in both no-optional-mod JVMs (PIDs3732/10524) pass normally. See [[../03 manuals/Adapter upgrade ownership and persistence]]. Other inventory/caller/addon and client/survival/multiplayer/platform gates remain open. RAID destructive upstream contract needs separate audit. No client/Computer Use action. Beta3-6 concentrated workweeks, low confidence.

RAID checkpoint 2b54dcf2a00e276abd69fe95630e68eb4915b984: upstream destructive assembly/member-removal semantics restored; recursive member formatting and raw/lock normalization, old-array erasure/handle closure, identical-stack no-op, comparator0/15, replacement-node cleanup and deferred unload-safe snapshot restoration. Seven baseline regressions, seven ordinary tests and a native Lua5.3 two-JVM probe. Build/2143 units/824 mixed/764 tests in both no-optional-mod JVMs (PIDs49964/32436) pass normally. See [[../03 manuals/RAID formatting and lifecycle persistence]]. Rotation, wiring, addon HDDs, access/visual/client/survival/multiplayer/platform and broader caller/API gates remain open. No client/Computer Use action. Beta3-6 concentrated workweeks, low confidence.

RAID facing checkpoint c8c9bfd95800c97fad20839ecdc75daf3a4907ca: horizontal player-facing placement, rotation/mirror and four model variants; overlays follow the selected front with corrected outward winding; actual loaded-host/eight-block menu checks. Two red geometry/resource units and four red server regressions reproduced. Build/2146 units/828 mixed/768 tests in both no-optional-mod JVMs (PIDs44468/60156) pass; native RAID retains WEST front plus machine/local/label/open binary state. See [[../03 manuals/RAID facing and overlay geometry]]. Real GPU/client, wiring, protection/multiplayer/addon/migration/platform and broader menu audits remain open. No client/Computer Use action. Beta3-6 concentrated workweeks, low confidence.


Block-menu checkpoint 84351b9281e9e29ffb46dd51ba95aad5421acbc1: shared same-level/current-loaded-host/eight-block access for13 block menus; rack modules inherit the host check. Twelve failures reproduced in776 baseline tests. Sixteen final tests cover bounds, dimension, missing/stale hosts, installed modules and non-loading lookup; build/2146 units/844 mixed/779 absent pass with normal process exit. Three fixture defects corrected without relaxing functional assertions. See [[../03 manuals/Block inventory menu access]]. Client/GPU, protection/survival/multiplayer, broader API/integration/migration/platform and full-port gates remain open. Named Modrinth client is open on user-confirmed screen3; MCP works through IPv6, but unfocused pausing awaits manual F3+P. Beta3-6 concentrated workweeks, low confidence.


Screen-input checkpoint cdff547e46b0c925912799ee2b3a7f6ca97a5900: restored neighbor-only keyboard dispatch across all members of the actual merged rectangle; group-local keyboard presence/listing and physical terminal current-host/same-dimension checks. A red dispatcher unit, five initial server regressions and three controlled routing failures reproduced after repairing an unjoined item fixture. Six actual-network tests added; build/2146 units/850 mixed/785 absent pass with normal exit. See [[../03 manuals/Screen keyboard routing and terminal hosts]]. Physical GUI range remains distinct from inventory range; existing keyboard eight-block acceptance is unchanged. Real-client/GPU/protection/addon/survival/multiplayer/unload/API/platform/full-port acceptance remains open. Client MCP works; manual F3+P remains pending. Beta3-6 concentrated workweeks, low confidence.


CC Lua-object checkpoint f7a457dcfa7c3b1bfd96d7fa5c7471a4b6a401bc: optional IDynamicLuaObject/@LuaFunction converter, legacy value wrapper, binary-safe codec, server-thread calls, owned task completion/cancellation and shared IDs. CC-free persisted shell and explicit native expiration semantics restored. Seven missing-feature failures reproduced in811 baseline tests; nine ordinary tests include actual adapter/CC ReadHandle programs on native53 and LuaJ. Build/2146 units/859 mixed/786 absent and820 tests in each real CC-only restart JVM (PIDs796/54056) pass with normal exit. Native local731/computer identity survive; foreign methods expire without serializing Java context. See [[../03 manuals/ComputerCraft Lua object conversion]]. Actual client/survival/multiplayer/protection/addon/load/portable-media/migration/API/platform/full-port gates remain open. Client MCP works; manual F3+P remains pending. Beta3-6 concentrated workweeks, low confidence.


CC collection/FIFO checkpoint 3ba4dd5f979b9069ce5d636e097ad3fc40b8dd05: recurse through collection-shaped binary results, resolve own callback tasks FIFO and preserve peripheral task return payloads through a shared completion builder. Four baseline failures in815 tests; two new cases plus actual native53/LuaJ program extensions verify nested binary lists, buffer cursor, callback result data and delayed exactly-once tail work. Build/2146 units/861 mixed/815 CC-only/786 absent pass normally. See [[../03 manuals/ComputerCraft Lua object conversion]]. Upstream OC signal payload policy and persisted format are unchanged; earlier process proof stays scoped to f7a457dcf. Full client/addon/load/protection/survival/multiplayer/API/migration/platform and port gates remain open. Beta3-6 concentrated workweeks, low confidence.


Native outgoing-value checkpoint ddfad17850e45d96395713d3571ca7c03598423a: restored primitive arrays, Character strings and shared/cyclic table identity in the native component/signal boundary; byte arrays remain binary strings. Temporary registry roots are released and failed conversions restore the stack. Twelve of15 native unit cases and six of793 baseline GameTests reproduced failures; LuaJ live control passed. Build/2161 units/868 mixed/793 absent pass normally, including three native unload/load continuations with retained cyclic graph/local731 and queued NBT arrays. Legacy signal normalization/map serialization remain unchanged. See [[../03 manuals/Native Lua outgoing values]]. Incoming argument graphs and wider client/survival/multiplayer/addon/load/migration/platform/full-port gates remain open. Beta3-6 concentrated workweeks, low confidence.


Native argument checkpoint 2fa780a7e0b44771ec3130aa26e97814fbf78333: function/thread arguments normalize to null, shared/cyclic table values retain identity in detached Java maps, and invalid-UTF8 binary keys no longer collide. Nine of15 initial unit cases, three separate binary-key cases and six of793 baseline server tests reproduced failures. Build/2179 units/868 mixed/793 absent pass normally; seven existing real-computer scenarios now inspect actual callback arguments alongside the outgoing/reload checks. Text keys, numeric/binary values, depth/stack safeguards and persisted schema remain. See [[../03 manuals/Native Lua argument conversion]]. Broader callback/table-key/foreign-userdata, client/survival/multiplayer/load/addon/platform and full-port acceptance remain open. No new process/client proof. Beta3-6 concentrated workweeks, low confidence.


JEI documentation checkpoint 19b84ae4d5e828b005a02d636c09884b580a9467: optional19.21.0.246 plugin provides manual/callback categories and ItemSearch focus/hover lifecycle. Eight focused unit cases; build/2187 units/868 mixed including JEI/793 absent pass with complete server summaries and normal exit. Newer19.64 was rejected by loader21.1.234 despite a misleading Gradle success and is not counted. No required published dependency/bundling or profile/loader/client change. See [[../03 manuals/JEI documentation integration]] and [[../02 plans/JEI integration]]. Loot cycling/subtypes/runtime addition/hidden ingredients/GUI/icon and actual client/full-integration/full-port gates remain open. Beta3-6 concentrated workweeks, low confidence.


Loot cycling checkpoint b9ab176319f56fc6c8a882502a6006d01eef2206: actual crafting now uses copied, ordered API-registered cycling templates; opt-out controls outputs rather than valid loot inputs. Legacy string keys and unknown/empty identity fallback are restored, ordinary disks remain rejected. Three baseline failures in796 tests; four final cases cover registered/bundled sequence/wrap, wrench retention and template isolation. Build/2187 units/872 mixed/797 absent pass normally. See [[../03 manuals/Loot disk cycling]]. This is a core prerequisite, not completion of the JEI recipe display. Public addon factory restart identity, client registry sync/external loot sources and full JEI/client/port gates remain open. Beta3-6 concentrated workweeks, low confidence.

## Addon-opslag — wereldkoppeling en handles

Technische checkpoints126020134/cb34eac02: fromSaveDirectory gebruikt nu actieve-wereld/opencomputers/root en herstelt open binaire lees-/schrijfhandles zonder truncatie. Twee echte padregressies en een handle-regressie reproduceerden de fouten. Eindbuild2195 units/903 gemengde GameTests slagen;818 absent-tests slagen op het eerdere wereldpadcheckpoint. [[../03 manuals/Addon save-directory filesystems]] bevat bewijs en grenzen. Volgende stap: buffered=true daadwerkelijk implementeren met save-gebonden schijfwrites, correcte entrykosten en herstartacceptatie. Geen automatische bestandsmigratie; geen verdere look-and-feel-polish. Client/JEI-acceptatie blijft gebundeld voor later functioneel testwerk.

## Addon-opslag — buffering en procesherstart

Runtimecbda79eab6be645406338f058e0fbb4f2c92b587 implementeert buffered=true met private save-snapshots, geserialiseerde IO, disk-authoritatieve inhoud en handlemetadata in NBT. Entrykosten/quota gelden voor beide modi; gedeelde RAM/buffered writer- en directory-renamefouten zijn gereproduceerd en gecorrigeerd. Build2202 units/903 gemengde GameTests slagen. Echte native Lua5.3-continuation in een nieuwe JVM slaagt: prepare PID33748 en verify PID19984 ieder824 tests, beide offsets en binaire bytes gecontroleerd, normale afsluitingen. [[../03 manuals/Addon save-directory filesystems]] bevat de exacte grenzen en logs. Dit vervangt de eerdere open buffering/herstarttaak voor de addon-API. Volgende technische stap: externe/wereldgebonden lootdefinities en gewogen sampling. Volledige native-backendconfiguratie, zware snapshotbelasting, crash-midden-in-save, bredere platforms en clientacceptatie blijven apart open. Geen look-and-feel-polish.

## Wereld-lootdefinities en selectie —b4d462200

[[World loot disks]] vervangt de eerdere ontbrekende-wereldloaderstatus: wereldgebonden readonly factories, cycling/loginmetadata, gewogen selectie zonder gewichtexpansie en legacy wereldpad-fallback zijn geimplementeerd en gepusht. Build2203 units/908 gemengde/823 absent GameTests slagen; echte recipe-update-codec behoudt wereldoutputs na unload. Dit is nog GEEN chest-generatie: moderne loot-tablekoppeling en misc.lootProbability als gewicht zijn de volgende functionele stap. De upstream chesthook staat uitcommentarieerd ondanks de README/config; houd die functionele eis open. Geen client-/CU-actie; volledige portering en genoemde acceptatiegates blijven open.


## Gewogen loot-disks in echte kistinventarissen — e4bf5dceb9466e71b58591baebd180d8d5af8ac0

De eerder ontbrekende chestkoppeling is geimplementeerd: vier bedoelde vanilla tabellen krijgen een gewogen disk-entry, met behoud van bestaande pools/voorwaarden/functies en ComputerCraft-loot. Default5 blijft een gewicht; nul schakelt injectie bij resource-reload uit. Build2203 units/911 mixed/826 absent slagen, normale afsluitingen;512 vaste-seed lazy ChestBlockEntity-generaties toetsen ook wereldspecifieke disks. [[World loot disks]] bevat de precieze grenzen en bewijslogboeken. Dit sluit server-side chestgeneratie; daadwerkelijke client-/structuurontdekking en wereld-descriptor/native-JVM-herstartacceptatie blijven open. Geen cosmetische polish of Computer Use. Volgende functionele acceptatie: wereldgebonden diskcontent en actieve native handles over een echte serverherstart. Volledige portering blijft actief; beta voorlopig3-6 geconcentreerde werkweken, lage zekerheid.


## Wereld/legacy loot-disk: echte native herstart — 3bc5314ab06b1408e03a0a36defe07042be01f69

De gerichte twee-JVM-acceptatie is gesloten: oc-restart-world-loot-20261020-02, prepare PID58848 en verify PID72588, beide827 tests en normale afsluiting; build2203 units groen. Een werkelijk door dungeon-loot gegenereerde wereld-disk en een legacy-only disk blijven in fysieke disk drives naast native Lua5.3 werken met behouden locals, componentadressen, twee open read-handles/offsets en binaire bytes. Write-open geweigerd, bronbestanden onafhankelijk ongewijzigd; geen runtimefix nodig. [[World loot disks]] bevat tooling, reproduceercommando's en grenzen. Volgende bronkandidaat voor reproductie: managed HDD negeert lock bij filesystem/labelconstructie; floppy mist de FileSystemLike drive-mode/lock-route. Eerst gerichte failure tegen het upstream-contract, daarna fix. Native fromSaveDirectory/bufferChanges voor eigen HDD/floppy/RAID blijft een afzonderlijk migratiegevoelig punt; geen bestanden verplaatsen of bewaarde inhoud vervangen zonder expliciete compatibiliteitsaanpak. Client en bredere host/platformacceptatie blijven open. Geen Computer Use of polish; volledig portdoel actief, beta voorlopig3-6 geconcentreerde werkweken met lage zekerheid.


## Managed HDD-lock en herstelde handles — 57b2250819f0a59b8a1ea2639c5fdb6c34c8db02

Reproductie:1 unitfailure en3 verplichte serverfailures; managed HDD bleef schrijfbaar, label wijzigbaar en owner verdween bij save. Runtime herstelt readonly filesystem/label, behoudt lock in echte snapshots/itemdata en blokkeert writes via door de readonly wrapper verstrekte oude outputhandles. Build2204 units/825 gewone/915 gemengde tests groen. Native herstart met wereld-/legacy disks plus interne gelockte HDD: oc-restart-world-loot-managed-lock-20261020-01, PIDs59468/64684, beide831 tests en normale afsluitingen. Alle3 read-offsets, binaire bytes, Lua-local en componentidentiteiten behouden; owner na herladen/host-save en write-openweigering gecontroleerd. [[Managed disk locking]] bevat bewijs en grenzen. Geen opslagbackendmigratie of client-/CU-actie. Vervolg: floppy FileSystemLike mode/lock/GUI en formattering/adresreset bij modewisseling gericht reproduceren; daarna migratiegevoelige native HDD/floppy/RAID save-directory-backend apart aanpakken. Volledig portdoel actief; polish uitgesteld, beta voorlopig3-6 geconcentreerde werkweken met lage zekerheid.


## Floppy-modes, locks en echte modewisseling — ce9d912a2196a1ffa30966b0316a480ee0eb358f

Gewone floppy's openen de bestaande DriveMenu, ondersteunen raw/managed met eigen capaciteit/een platter en behouden locks/data/labels. Loot-disks zijn uitgesloten; held-hand/sneak/stale-menu en legacy-data getest. DiskItemEnvironment vervangt dubbele wrappers/constructie. Een echte modewisseling wist actieve oude inhoud/handles/adres en lock, behoudt label/onverwante metadata; dezelfde mode blijft intact. Itemwijzigingen vervangen gekopieerde CustomData zodat inventory-snapshots niet meeveranderen. Baseline6 echte serverfailures plus1 unitfailure; build2206/832 gewoon/922 gemengd groen. Native5.3-procesherstart met4diskdrives + HDD in oc-restart-world-loot-floppy-mode-20261020-01: PIDs69044/58640, beide838 tests, normale afsluitingen, vier bewaarde read-offsets plus signed rawbyte, mode/lock/labels/adressen. [[Disk modes and floppy control]] bevat bewijs, fixturecorrecties en grenzen. Volgende technische taak: compatibiliteitsplan en reproduceerbare migratie voor native managed HDD/floppy/RAID save-directory/bufferChanges en externe raw-drive-opslag. Geen bestaande inhoud/identiteiten verliezen of oude bronbestanden overschrijven; client/brede acceptatie blijft open. Geen Computer Use of polish. Volledig doel actief; beta voorlopig3-6 geconcentreerde werkweken, lage zekerheid.


## Native storage migration in uitvoering

[[Native storage migration]] beschrijft de resterende native managed HDD/floppy/RAID en raw-drive backend, importveiligheid en acceptatie. Eerst guarded import/journal, daarna daadwerkelijke runtimebinding/configuratie, RAID en compressed raw opslag. Bestaande data/identiteiten/handles en bezette doelpaden behouden; geen completionclaim voor een losse importhelper. Volledig portdoel en functionele prioriteit blijven gelden.


## Guarded volume-importfundament — 25129d5118e22ec3cdbae0687f9c5755179e90b7

[[Native storage migration]] stap1 is geimplementeerd en gevalideerd: NativeVolumeStore importeert echte oude RAM-bomen met bronbehoud, pending/complete journal, conflictweigering, stale-snapshotbeveiliging, relatieve binding los van zichtbaar nodeadres en bestaande direct/buffered backends.12 gerichte units, build2218 units/922 gemengde GameTests slagen, normale afsluitingen. [[Native volume import]] splitst bewijs en beperkingen; een onvoorbereide loottestwereld was alleen een fixturefout en telt niet als groen bewijs. Nog geen native driver/RAID/raw backend omgezet. Eerst buffered casebeleid/forceCaseInsensitiveFS, daarna daadwerkelijke native binding en bufferChanges=true, RAID/raw opslag en bredere proces/clientacceptatie. Bestaande werelden onaangeraakt; doel blijft volledige portering, polish uitgesteld; beta voorlopig3-6 geconcentreerde werkweken, lage zekerheid.

## Native managed storage and RAID world binding — 2026-10-20

Native HDD/floppy runtimeed36a3f96 and RAID runtime9cdb3b5947b81096b5ffde47865d9be6e9121078 now use guarded world-backed storage with defaulttrue bufferChanges and independent fs.oc:storage bindings. Old inline trees preserve source/identities/labels/locks/handles during import; new snapshots omit file trees. RAID member mutation durably erases the old array and reassembly gets a new binding; ordinary reload/pickup preserves it. Build2224 units/840 ordinary tests green; optional-provider/native pair oc-restart-world-loot-raid-storage-mixed-20261020-02 prepare PID40500/verify PID66772 each936 required tests/normal exits. Actual bound RAID/HDD/floppy bytes and native continuations checked. [[Native storage migration]], [[RAID formatting and lifecycle persistence]], [[Native volume import]] contain details/limits. Next compressed external raw drive import/storage; broad IO/platform/client/Survival acceptance remains open. Technical priority unchanged; no polish/client/CU/production action; full goal active, beta3-6 concentrated workweeks lowconfidence.

## Native raw storage completion checkpoint — 6cf4c2890fe2b2869fa5d84d70abe9cdbb2b804a

HDD/floppy raw bytes now live in upstream-compatible GZIP .bin world files with guarded legacy import/independent bindings, metadata-only native snapshots and explicit conflict/corruption/capacity failures. Managed HDD/floppy and RAID already switched. Alias/head/tail defects fixed; late unknown inline data cannot be ignored. Final2237 units, mixed native prepare PID71224/verify PID59096 each938 required tests/normal exits; actual compressed raw bytes and continued Lua/managed/RAID/AE2 states verified. [[Native storage migration]] step4 checked; [[Native volume import]] has evidence/limits. Remaining feature/ROM/callback/host/integration audit and broad IO/platform/client/Survival acceptance remain; no cosmetic polish/client/CU/production action. Full goal active, beta3-6 concentrated workweeks lowconfidence.

## Native storage failure isolation — 411c02283c26175a1e17c05ae5c2d24fa94a5099

Actual missing/corrupt media previously broke computer/RAID onLoad and made Minecraft discard a diskdrive (initial fixture also crashed the test server). Expected native mount failures now retain host/item/pending snapshot, with unavailable media and safe ejection/recovery; failed RAID member cannot form an empty replacement or lose lock metadata. Build2237 units/847 absent, mixed native pair prepare PID70872/verify PID69572 each943 required/normal exits. [[Native volume import]] contains exact scope/red/green/archival evidence. Continue technical parity and active IO/heap/platform/client acceptance; no polish/client/CU/production action. Full goal active, beta3-6 concentrated workweeks lowconfidence.


## Buffered write-failure checkpoint - 0e3362c5e6b6d80fc7a403f4c4a2e1bd2c0f37d7

Queued saves can retry after repaired IO; checkpoints track partial durable progress rather than planned bytes. Native item metadata is published only after a successful flush without closing streams.2 recovery unit reds and1 actual-host failed-eject red reproduced; final2239 units/848 absent required tests and mixed native prepare/verify each944 required tests pass with normal exits. See [[../03 manuals/Native volume import]] for scope/evidence/limits. Running-heap IO, close/unload/crash/load/platform/custom-addon and actual-client acceptance plus full feature/callback/ROM parity remain open. Technical functionality first; cosmetic polish deferred. Full goal active, beta3-6 concentrated workweeks lowconfidence.


## Live Lua IO-failure checkpoint - ec91e1ca708f5a2e6129d422cb9cb903d1418775

Native Lua survives failed diskdrive.eject: same media/local731/open reader offset+binary bytes/writer remain; repaired retry creates exactly1 physical disk and persists both dirty bytes. No new runtime fix required beyond0e3362c5e. Build2239 units/849 absent required tests and934 mixed required tests, normal exits. See [[../03 manuals/Native volume import]] for fixture/evidence/limits. Autosave/chunk-unload/heap-restore faults and broader technical feature/callback/ROM/platform/client acceptance stay open; no new restart or client claim. Technical first, beta3-6 concentrated workweeks lowconfidence.


## Computer IO-failure checkpoint - 770e92d21fe1db4d35ba4e29cc03dd10475f66cb

Two native HDD/heap tests cover explicit failed save and failed onChunkUnloaded, live reader/writer/local retention, repaired unload/old-VM stop and new BlockEntity.loadStatic/onLoad continuation. Final bytes include all3 writes. No further runtime fix required. Build2239 units/851 absent/936 mixed required tests, normal exits. [[../03 manuals/Native volume import]] records exact scope: direct lifecycle callbacks, no actual chunk eviction/automatic retry or restart claim. Broad storage/platform/client acceptance remains open; next return to functional feature parity. Technical first, beta3-6 concentrated workweeks lowconfidence.


## TIS-3D serial integration work package - 2026-10-20

Pinned target: TIS-3D MC1.21.1-neoforge-2.0.0 (release sourcefff58fd3dbca7ba08a5ed0262274555c873641c9), MarkdownManual1.2.8 and Architectury13.0.11. Both published TIS1.7.7/2.0 require NeoForge>=21.1.248; keep core default21.1.234 and explicitly test optional profile with -PwithTIS3D -Pneo_version=21.1.248. Do not downgrade/patch provider metadata. Publisher Modrinth metadata/artifact SHA512 checks, source API/tree and both mod descriptors retained under build/finish-port-implementation/integration-discovery/tis3d. TIS2 uses a custom serial-interface-provider registry, no old SerialAPI.addProvider.

1. Reproduce missing provider against a real adapter/installed TIS2. Pin optional compile/runtime dependencies; no hard/bundled provider requirement.
2. Register through NeoForge's deferred registry, behind installed-mod classloading boundary. Adapter identity/loaded-chunk guards; serial_port with setReading/read/write, signed-short FIFO128 each way, explicit receive enable, buffer-full result, peek/skip, reset/detach and legacy-compatible node/queue/isReading NBT.
3. Register the existing localized protocol page through MarkdownManual's client-only document-provider registry, with a discoverable TIS protocol reference. This is functional access, no visual polish.
4. Verify provider/queue/NBT/removal contracts, native Lua through real powered TIS casing/serial module and pipes, default provider absence and all-provider mixed profile. Appropriate full build/gates; no reviewer subagents or client focus.
5. Commit/push implementation and canonical evidence -> root mirrors/inventory, verify exact remote equality/clean tree. Keep client, real process/unload/multiplayer/platform/load and other provider families explicitly open.

Beta remains3-6 concentrated workweeks, low confidence; this is one missing legacy integration, not full-port completion.


## TIS-3D server integration checkpoint - 748f5e993819e9d097fe512f2d9bbd8efee3116c

Missing TIS2 provider reproduced, then optional adapter serial_port restored:3 callbacks, signed FIFO128 both directions, enable/full/peek/skip/reset, exact flat-node/buffer NBT, loaded-position/adapter identity and client-only existing protocol resources. Actual powered TIS controller/casing/serial pipes exchange with native Lua (65535 ->-1, -32768 back). Build2239 units/853 TIS-only required tests on21.1.248, core absent851 on unchanged21.1.234, final all-provider mixed938 on21.1.248; all normal exits. [[../03 manuals/TIS-3D serial integration]] records provider releases/loader requirement, original red, compile fixture correction, proof/limits and commands. No bundled/hard dependency or visual polish. The separately tested core234 JAR was installed in the closed backed-up Modrinth profile; manual screen3 start/client acceptance pending. Actual client/TIS assembly/JVM/chunk/platform/load/protection and other provider families remain open; full goal active, beta3-6 concentrated workweeks lowconfidence.

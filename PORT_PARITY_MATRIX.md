# Upstream-pariteit: werkregister

Referentie: `.upstream-opencomputers-master-MC1.12/src/main/`. Port: `src/main/`.
Dit is een startregister van onderzochte verschillen, geen claim dat iedere callback al is vergeleken. Het volledige uitzoekwerk staat in fase 0 van `PORT_COMPLETION_PLAN.md`.

| Onderdeel | Bron / huidige toestand | Werk en bewijs voor acceptatie |
| --- | --- | --- |
| Terminal GUI | Glyph- en achtergrondbatches ingevoerd; volle 160x50 uitvoer en kleuren 60 FPS | ASCII, box drawing, Unicode, kleuren, input en wereldbeeld getest; lagere tiers/schalen/minimale modset nog open. Zie `TERMINAL_GUI_PERFORMANCE.md`. |
| Microcontroller-titel | Titel hersteld in b3791190 | Unit-test en live GUI bevestigd. |
| Computerfouten | Case NoEnergy/NoCPU vertaald; vrije Lua-fouttekst behouden | Unit-tests groen; NoEnergy live bevestigd. Overige hosts en bootfouten blijven open. |
| Slots en tiers | Case-watermarks visueel afwijkend; bloktints deels live gecontroleerd | Oorzaak eerst bewijzen; lege/volle slots, alle item-/bloktints, rotaties, licht en GUI-schalen controleren. |
| Robotterminal | Gedeelde snapshot/delta/input ingevoerd; live beeld, typen, touch en inventory transfer getest | Zie ROBOT_TERMINAL_PORT.md. Compacte schermloze layout en power/inventorytransfer getest. Beweging behoudt runtime/menu in GameTests; live beweging, animatie, clipboard/drag/scroll en volledige live assemblage/OpenOS/reload blijven open. Zie ROBOT_MOVEMENT_PERSISTENCE.md. |
| Robot OpenOS-ROM | Originele robot-ROM hersteld in fedefc2f5 | Echte assembler/OpenOS GameTest bevestigt automatische mount, require(robot) en go left 1; readonly/adres/save/load/unload getest. Overige wereldacties en live persistence blijven open. |
| Lua hervatten | `common/machine/LuaArchitecture.java`: geen heap/coroutine-save | Runtimekeuze en werkelijke persistence; sentinel, coroutine, timer, open bestand en proxy na chunk unload/herstart. |
| Lua geheugen | `LuaArchitecture` rapporteert `freeMemory` als helft van geïnstalleerd geheugen | Meting en limieten vergelijken; RAM-uitbreiding, allocatie/GC en OOM afvangen. Afwezigheid van alle limieten is nog niet bewezen. |
| Lua architecturen | Upstream `server/machine/LuaStateFactory.scala`; port registreert LuaJ | Versie-/architectuurkeuze, semantiek en packaging uitwerken met persistence; bestaande Lua-programma's testen. |
| Capacitorblokken | Beide varianten geimplementeerd op 2026-10-07: opslag, adjacency, comparator, NBT, carpetrecept, diergroepen en schokcooldown | Serverbewijs in CapacitorGameTests, inclusief gemengd cluster en natuurlijke ticker/netwerkafname; chunkgrens-varianten en echte visuele/player-flowacceptatie blijven open. |
| Kabelkleuren | Kleurfilter/netwerkherbouw, RGB-NBT/legacy metadata, dye-tags/interactie, itemkleur, recepten, aansluitgeometrie en client-tints/redraw geimplementeerd; RGB normalisatie en lichtgrijs wildcard getest | Real-client visuele/survival acceptatie, chunkgrenzen en echte mod/protection-integratie blijven open. |
| Kabelgeometrie | Port kabelmodel is `cube_all` | Dunne kabel/neighborarmen, selectie/collision, itemmodel en connect/disconnect vanuit alle zijden live controleren. |
| Bundled redstone | Port standaardinput retourneert nul | Beschikbare moderne integratie kiezen en daadwerkelijke zestien kanalen in beide richtingen testen. |
| Hover boots | Upstream `common/init/Items.scala`, `common/item/HoverBoots.scala`; item ontbreekt | Player-item/energie/landing/dye porten; robot-hoverupgrade telt niet als deze feature. |
| Debugger | Upstream `common/item/Debugger.scala`; debuggeritem ontbreekt | Admin/debugtool, permissies en functies porten; debug card is apart. |
| Presents | Upstream `common/item/Present.scala`, `common/EventHandler.scala`; seizoensflow ontbreekt | Beslissen/porten; loot en kalendergedrag met vaste klok/random seed testen. |
| Dynamische recepten | Upstream `common/recipe/Recipes.scala`: kleuren/ontkleuren | Recepten/tags/remaining items en data preservation; complete survivalcraftingketen. |
| Alle andere apparaten | Bestaande implementaties en tests zijn geen volledige pariteitsverklaring | Per tier callbacks/events/energie/errors vergelijken; robot, drone, tablet, rack, disk, printer, hologram, transposer, internet, wireless, upgrades en overige componenten volgens fase 4. |
| Manual en loot | 767 upstream manualbestanden aanwezig; 337/338 lootbestanden, ontbrekende README niet functioneel | Iedere spelersclaim tegen implementatie testen, links/afbeeldingen controleren; bestandstelling bewijst geen werking. Fake endstone is al geregistreerd. |
| Public API | Lokale upstreamsnapshot mist volledige Java API-bron | Exacte volledige upstream API pinnen; signatures, events, drivers en voorbeeld-addon compileren/laden. |
| Modintegraties | Generieke capabilities vervangen niet alle specifieke upstreamdrivers | AE2, CC, JEI, Mekanism, WAILA, ProjectRed, EnderStorage, IC2 en overige upstreamintegraties afzonderlijk inventariseren; beschikbare 1.21.1-vervanger of expliciet besluit vastleggen. |
| Verpakking/platform | Bestaande Windows-build en GameTests groen | Java 21 op Windows/Linux, dedicated server, minimale modset en huidige modset, twee clients en release-artifacts/licenties. |
| Duur-/migratietests | Nog geen volledige end-to-end pariteitsrun | Survival, multiplayer, save/reload, chunkgrenzen, dimensies, crash recovery en lange belasting; oude werelden alleen binnen expliciet gekozen converter-scope. |

Per afgeronde rij bewijs en resterende varianten koppelen aan het uitvoeringsplan. Openstaande beslissingen tellen als open, niet als geïmplementeerd.


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

## Drone cargo storage - 2026-09-22

DroneEntity no longer exposes an always-empty main inventory. Capacity follows upstream Drone.computeInventorySize: sum max(1, inventoryCapacity / 4) for installed inventory drivers, capped at eight. Standard inventory upgrades therefore provide four slots each. Cargo is separate from hardware, stored with slot indices under oc:cargo; selectedSlot clamps to cargo capacity. Hardware changes recalculate capacity, preserve overlapping slots and drop only overflow when shrinking. removeItemNoUpdate now also refreshes cargo and hardware environments.

DroneInventoryGameTests covers zero/one/two/three upgrades, sparse cargo in slots 2 and 8, NBT entity reload with exact item counts and selection, and removal/reinstallation with eleven overflow iron ingots dropped exactly once and three diamonds retained. Initial tests failed against zero-slot inventory (drone-cargo-red.log). Final build and all 578 GameTests pass (drone-cargo-final.log), with 2109 unit tests unchanged.

This is cargo storage groundwork, not completed drone parity. DroneMenu still exposes hardware only; cargo GUI/synchronization, InventoryControl Lua callbacks, world inventory interactions, destruction/pickup semantics, native running-drone cargo continuation and real-client visuals remain open. No client launch/install, push or merge.

## Drone and robot inventory callbacks - 2026-09-22

Drone now exposes inventorySize, select, count, space, compareTo and transferTo. AgentInventoryControl implements upstream InventoryControl behavior and is shared with RobotBlockEntity: one-based slots, empty stacks compare equal, compareTo defaults to ignoring item data with optional strict comparison, bounded partial merges, successful self/zero transfers, full incompatible-stack swaps, and 0..64 requested counts. This fixes corresponding robot parity discrepancies as well as missing drone APIs.

InventoryControlGameTests invokes registered callbacks on actual robot/drone components. It checks slot numbering, empty source rejection, merge conservation at a full stack, full destination rejection, self/zero transfer, partial incompatible rejection, full swap, default/strict custom-name comparison, count/space and invalid index. Red run failed robot empty equality and missing drone inventorySize. Three old RobotBlockEntity source-string tests were removed in favor of these behavioral checks; one had falsely passed because unrelated methods contained matching strings. Final build, 2106 remaining unit tests and all 580 GameTests pass (inventory-api-final.log).

Native Lua continuation with drone cargo, cargo menu/client synchronization, world interactions, pickup/destruction and visual acceptance remain open. No client launch/install, push or merge.

## Native drone cargo continuation - 2026-09-22

DroneCargoPersistenceGameTests runs native Lua on a tier-2 drone with inventory, inventory controller and tractor upgrades. Lua splits eleven diamonds into eight/three, selects slot two and waits. The test saves/removes/recreates the entity, resumes the same program and proxies, checks local marker731 and selection, reads retained cargo through the controller, picks up five newly spawned iron ingots using the tractor, and merges diamonds back. Java independently verifies final cargo types/counts. Final build and all581 GameTests pass (drone-cargo-native-final.log); 2106 unit tests remain green.

Initial fixture attempts omitted upgrades because loadFromItemStack assigns components to the first compatible empty slot. Correct fixture order is tractor tier2, controller tier1, inventory tier0. The generic loader's order sensitivity remains a follow-up; this run did not alter tier restrictions or production behavior. This is native entity-NBT continuation, not full JVM restart or visual acceptance.

Upstream common/container/Drone.scala exposes eight cargo slots (four columns, two rows) with unavailable slots disabled, not editable hardware slots. Current DroneMenu/DroneScreen still expose hardware; correct cargo GUI and synchronization remain the next parity task. No client launch/install, push or merge.

## Drone cargo menu - 2026-09-22

DroneMenu now exposes eight cargo slots in four columns/two rows rather than editable internal hardware. The hardware host reference remains available for machine status and power controls. CargoInventory resolves the current mainInventory on each operation, avoiding stale references when capacity changes. Unavailable slots reject insertion/pickup and are inactive; shift transfers operate only on cargo. Opening data includes cargo capacity, and an added ContainerData field synchronizes later changes. DroneScreen renders ordinary cargo slots and unavailable-slot overlays instead of hardware tier/type hints. Layout uses the existing panel with cargo at x98..152, y18/36; real-client visual acceptance is still pending.

DroneMenuGameTests failed against the old hardware menu, then passed: eight cargo plus36 player slots, four-slot gating, shift-in/out conservation of11 diamonds without changing the inventory upgrade, live expansion to8 and writing slot8 after the underlying container changes. It also roundtrips writeClientSideData into the client constructor and applies capacity updates to4 and0, checking slot activation/insertion restrictions. Final build,2106 unit tests and all582 GameTests pass (drone-menu-final.log).

Not a full rendered-client or network-transport acceptance test. Screen3 client testing, drone status text/energy display, world actions, pickup/destruction and component-order-sensitive loading remain open. No client launch/install, push or merge.

## Drone component placement order - 2026-09-22

DroneEntity.loadFromItemStack and DroneAssemblerTemplate now allocate components in descending driver tier order (stable among equal tiers), preventing low-tier upgrades from occupying scarce high-tier slots first. The assembler driver-validation helper uses the same ordering. Existing slot types, tier limits and capacity restrictions remain intact.

DroneInventoryGameTests now checks all six permutations of inventory(tier0), inventory controller(tier1), tractor(tier2). Direct loading must retain exactly one of each; assembler tests supply CPU/RAM/EEPROM plus each permutation, assemble instantly through the existing test API, and check all six output components. Before the fix, order012 lost the tractor on loading (drone-order-red.log). Final build,2106 units and all584 GameTests pass (drone-order-verified.log). Earlier native cargo test fixture ordering workaround is no longer required for valid combinations.

This covers valid mixed-tier component lists. Malformed or over-capacity externally fabricated items, drone destruction/pickup, world actions, status/energy UI and real-client visual acceptance remain separate work. No client launch/install, push or merge.

## Drone sneak start and wrench pickup - 2026-09-22

DroneEntity.interact now follows upstream: ordinary interaction opens cargo GUI; sneaking starts a stopped drone; sneaking with a registered wrench in the main hand packs the drone. Removed entities reject further interaction. Packing stops the machine, detaches hardware environments before encoding their final item state, emits the tier-correct drone item, drops cargo once and discards the entity. DroneItem stores the machine connector energy; loading restores it before hardware is attached, with old/assembled items retaining their existing initial-energy behavior.

DronePickupGameTests checks sneak start, packing a running drone, repeated interaction without duplicate drops, preservation of five hardware components/tier, seven diamond cargo drops, three queued generator coal drops, no retained generator fuel after replacement, empty replacement cargo and exact231 machine energy. Initial energy fixture incorrectly requested731 in a500-capacity connector; diagnostics showed packed/restored500, so the fixture was corrected and initialization explicitly asserted. Final build,2106 unit tests and all585 GameTests pass (drone-pickup-final.log).

Direct test uses the actual interaction and item-loading APIs, not rendered client clicks. Void removal, damage/hit signals, dimension target offsets, full survival placement and visual acceptance remain open. No client launch/install, push or merge.

## Drone targeting and hit signals - 2026-09-22

DroneEntity previously inherited Entity.isPickable=false, so normal entity ray selection could not reach its interaction even though direct interaction tests passed. It now reports pickable/pushable while present. skipAttackInteraction mirrors upstream hitByEntity for a running drone: normalized attacker-eye direction, hit signal arguments x,z,y and optional configured username, plus immediate velocity impulse away from the attacker. Stopped and removed drones do not emit hit signals.

DroneInteractionGameTests uses ProjectileUtil with the normal pickable/non-spectator predicate, then Player.attack against an actual running drone. It checks selected entity, hit name/direction/argument count, immediate velocity and stopped/removed behavior. Initial run failed normal targeting (drone-hit-red.log). Final build,2106 unit tests and all586 GameTests pass (drone-hit-verified.log).

This proves targeting and attack entry points, not rendered crosshair/client networking or displacement over subsequent ticks. Current DroneEntity.tick still uses simplified target-step movement which overwrites velocity; upstream inertia/gravity/drag and sustained knockback need a separate movement parity pass. Void behavior, dimension targets and visual acceptance also remain open. No client launch/install, push or merge.

## Drone server movement physics - 2026-09-22

DroneEntity.tick now uses the upstream acceleration/inertia model instead of overwriting velocity with a constant target step: add target acceleration to existing velocity, clamp per axis to0.4, move through vanilla collision handling and apply0.8 drag. Close-to-target settling follows upstream thresholds. Stopped drones apply0.05 gravity (honoring modern NoGravity), ground friction and vertical damping/bounce. Physics runs on the server; client position tracking remains the normal entity mechanism.

DroneMovementGameTests manually ticks real drones in fixed conditions: first0.1 movement/0.08 velocity, cumulative0.28 after the second tick with0.144 velocity, retained lateral0.2 motion, stopped0.05 fall/0.04 falling velocity and NoGravity suspension. Before the fix tests failed absent falling and absent drag (drone-movement-red.log). Final build,2106 unit tests and all588 GameTests pass (drone-movement-verified.log), including existing native drone reload, cargo, leash and pickup tests.

This is focused server physics coverage. Water/lava shutdown, void handling, dimension target offsets, long-path/collision convergence, client interpolation/animations and rendered flight acceptance remain open. Upstream also cancels motion at an exactly equal target; no broader knockback claim is made for that case. No client launch/install, push or merge.

## Drone water/lava shutdown - 2026-09-22

DroneEntity now stops the machine before its update when the drone eye position is inside water or lava, matching upstream isInsideOfMaterial behavior. DroneMovementGameTests covers both real fluids: confirmed eye submersion, stopped machine, falling motion, removal from fluid and successful dry restart. Both red cases kept running before the fix (drone-fluid-red.log). Final build,2106 unit tests and all590 GameTests pass (drone-fluid-verified.log).

Coverage is synchronous server ticks and actual fluid blocks, not rendered submerged-flight acceptance. Other-mod fluids, flowing-fluid boundaries, void behavior, dimension offsets and client visuals remain separate gates. TankWorldControl callbacks compareFluid/drain/fill are also still absent from robot and drone. No client launch/install, push or merge.

## World fluid comparison - 2026-09-22

Added compareFluid(side[, tank]) to robot and drone through AgentTankWorldControl. Robot action sides are restricted to local down/up/front and mapped through rotation; drone accepts global0..5. Comparison checks the selected internal tank against a loaded neighbor's sided FluidHandler capability, or an in-world fluid source if no capability exists. Optional external tank indices are one based and validated. Comparing does not mutate fluid.

TankWorldControlGameTests invokes each registered component callback and checks empty internal tank, water equality, explicit source index1, rejection of index2, lava mismatch, solid-block mismatch and preserved1000 internal water. Robot rejects backward action side. Before implementation both lacked compareFluid; final build,2106 units/all592 GameTests pass (tank-world-compare-verified.log).

Direct proof currently covers source blocks, not external modded multi-tank capabilities. drain/fill and conservation across world sources/capability handlers remain the next TankWorldControl work. FluidUtils upstream reference includes source-block wrappers and whole-bucket world transfers; NeoForge FluidUtil provides BucketPickupHandlerWrapper and tryPlaceFluid for adaptation. No client launch/install, push or merge.

## World fluid drain and fill - 2026-09-22

Robot and drone expose drain/fill via AgentTankWorldControl, retaining the compareFluid action-side restrictions. Drain simulates extraction and internal acceptance before executing a matching fluid extraction; it uses sided block capabilities or NeoForge BucketPickupHandlerWrapper for source blocks. Fill uses a neighboring capability when present; otherwise a temporary1000mB FluidTank is passed to FluidUtil.tryPlaceFluid and internal fluid is consumed only on successful placement. Existing source blocks and partial bucket placements are rejected. Missing/full/empty tank cases return upstream-style errors.

TankWorldControlGameTests now also runs a source-water roundtrip through both host APIs: solid destination rejection,999mB placement/pickup rejection without mutation,1000mB placement and pickup conservation, and lava rejection by a water-filled tank. Red run lacked fill; final build,2106 units and all592 GameTests pass (tank-transfer-verified.log). The existing two host tests were expanded, so the GameTest count did not increase.

External modded capability handlers, full/zero/empty return-value edge cases, flowing/waterlogged blocks, protected locations, Nether evaporation and native Lua continuation remain additional gates. This completes the basic callbacks but does not certify the full fluid interoperability matrix. No client launch/install, push or merge.

## Sided multi-tank capability integration - 2026-09-22

Added opt-in TestFluidCapabilities, enabled only by runGameTestServer via neoopencomputers.testFluidCapabilities. ModCapabilities registers its provider only when this property is true. The provider exposes fixture-specific handlers on one exact side of a lodestone at a dimension/position key; AutoCloseable cleanup removes state, restores the block and invalidates capability caches. Normal client/server launches do not enable it.

The existing robot/drone TankWorldControl tests now exercise actual world capability lookup against two external FluidTanks (lava1000, water1200/2000): any/explicit comparison,800mB partial insertion, full-destination rejection without loss,600mB typed extraction from the second reservoir, preservation of the first lava reservoir, and full-internal-tank rejection. Build,2106 unit tests and all592 GameTests pass (tank-capability-verified.log). No production fluid algorithm change was needed for these cases.

This verifies NeoForge capability interoperability with a controlled test provider, not a separately installed third-party tank mod. Remaining fluid gates include data-component persistence, zero/empty edge cases, protection events, flowing/waterlogged blocks, Nether evaporation and native continuation. No client launch/install, push or merge.

## Tank fluid data components and empty-state loading - 2026-09-22

TankUpgradeEnvironment now stores a complete registry-aware FluidStack under fluidStack instead of only fluid id/amount. Loads support the legacy fluid/amount fields and clamp to capacity; saves remove legacy fields and write an explicit empty compound for drained tanks. Loading always clears old contents first. Registry access comes from the host world, or the current server during detached block-entity loading, with built-in registries as a no-server fallback.

TankDataPersistenceGameTests proves731 water with custom batch/concentration data survives a real robot block-entity replacement, and exercises legacy231 water migration, loading empty data into a previously filled environment, migration reload and reuse of a snapshot after fully draining. Red failures: lost data components and stale contents on empty load (tank-data-red.log). Final build,2106 units/all594 GameTests pass (tank-data-verified.log).

Direct data-component proof uses vanilla custom_data; arbitrary third-party component codecs and full process restart remain additional gates. No client launch/install, push or merge.

## Native fluid continuation and robot washout fix - 2026-09-22

New nativeRobotResumesWorldFluidTransfers test fills a source above the robot, saves/replaces the robot with its internal tank empty, resumes the same Lua proxy/local variable, drains the source back into the tank and rejects a999mB placement without losing water. This initially timed out without a Lua error. Expanded shared helper diagnostics proved the restored robot was removed and its block replaced by flowing water(level8), rather than a VM continuation failure.

robotProperties now uses forceSolidOn alongside its existing dynamicShape/noOcclusion, preserving the partial collision/render shape while preventing FlowingFluid.canHoldFluid from washing the robot away through the blocksMotion check. Final build,2106 units/all595 GameTests pass (native-fluid-final.log). Red evidence: native-fluid-continuation.log and native-fluid-diagnostic.log.

The test proves ordinary robot NBT replacement/native continuation while water flows around it; full JVM restart, moving robots/afterimages in fluids and rendered client acceptance remain additional gates. No client launch/install, push or merge.

## Drone below-world lifecycle - 2026-09-22

DroneEntity.onBelowWorld now reuses the stopped/disconnected item-packing and cargo-drop path on the server, matching upstream outOfWorld instead of inheriting Entity.discard with no drops. Client removal remains vanilla. tick returns for already removed entities and after baseTick removes one, preventing further machine/physics work after removal.

DronePickupGameTests moves an assembled inventory drone below minBuildHeight-64, ticks twice and verifies exactly one packed drone and seven cargo diamonds. Red run lost the packed drone (drone-void-red.log). Final build,2106 units/all596 GameTests pass (drone-void-verified.log).

This verifies the immediate upstream drop lifecycle; items spawned below the world may themselves be removed by normal Minecraft void handling on later ticks. It does not provide a new recovery/teleport mechanic. Dimension target offsets and visual/client acceptance remain open. No client launch/install, push or merge.

## Drone dimension transfer - 2026-09-22

DroneEntity.changeDimension now preserves target minus position across a successful transfer and closes/disconnects the old machine after the new entity has copied its state. Failed transfers leave the original target intact; same-dimension relocation does not close its own machine.

DroneDimensionGameTests performs a real Overworld/Nether round trip with a native Lua machine and seven cargo diamonds. It verifies relative flight target, cargo, component address and disposal of the old VM. Red runs reproduced both the absolute-target bug (drone-dimension-red.log) and the retained old VM (drone-dimension-verified.log). Final build, 2106 unit tests and all 597 GameTests pass (drone-dimension-final.log).

This test starts a native machine but does not demonstrate resumed Lua heap execution after transfer. Full process restart, portal gameplay and rendered client acceptance remain open. No client launch/install, push or merge.

## Native drone continuation across dimensions - 2026-09-22

nativeDroneContinuesAfterDimensionRoundtrip now runs an actual native Lua EEPROM until it waits for a signal, transfers the drone to the Nether and back, and resumes that same program. A local marker731, selected slot3, original component proxy and seven diamonds survive; the resumed proxy transfers two diamonds into slot1 and leaves five in slot3. Both replaced VMs are closed.

Build and all598 GameTests pass (drone-dimension-continuation.log); the existing unit suite remains green. The intermediate Nether entity is transferred back immediately, so sustained execution in the destination dimension, portal input, rendering and full JVM restart are separate acceptance gates. No client launch/install, push or merge.

## Shared robot/drone detection - 2026-09-22

Added the previously missing drone detect callback and shared AgentWorldControl.detect with robots. Side handling follows robot local down/up/front versus all six drone global directions. Classification follows upstream WorldAware: nearest living entity/minecart, air, fluid block, replaceable, non-colliding passable block, solid. Fluid/replaceable detection posts a BlockEvent.BreakEvent and reports cancellation as the blocked boolean. Waterlogged solid blocks are not misclassified as liquid.

AgentDetectionGameTests exercises both component callbacks against air, stone, waterlogged slab, water, short grass, torch and a sheep; a position-scoped event listener checks protected water/grass, unregisters in finally, and verifies unprotected grass afterward. Invalid side6 is rejected. Build and all600 GameTests pass (agent-detection.log).

This restores detection only; drone item world interactions such as suck/drop and actual third-party protection integration remain open. The tests use a controlled NeoForge listener, not an installed protection mod. No client launch/install, push or merge.

## Robot/drone block comparison - 2026-09-22

Added drone compare(side[, fuzzy]) and shared its implementation with robots in AgentWorldControl. Side validation now precedes empty-slot handling, and block identity is resolved from the selected BlockItem rather than reverse block.asItem lookup. This supports modern ItemNameBlockItem mappings such as redstone wire. Legacy subtype metadata is represented by distinct modern blocks; fuzzy remains accepted for API compatibility without comparing placement orientation or arbitrary item data.

AgentBlockComparisonGameTests covers both hosts: selected versus other slot, empty/matching/different block, rotated oak log, fuzzy argument, redstone wire with a supporting block, non-block diamond, inventory/selection conservation and invalid side on an empty slot. Initial failures proved missing drone compare and robot invalid-side acceptance (agent-compare-red.log). A later fixture needed a support block for redstone; final build/all602 GameTests pass (agent-compare-final.log).

World item insertion/extraction, actual protection-mod interoperability, native continuation of these world callbacks and client acceptance remain open. No client launch/install, push or merge.

## Shared robot/drone drop - 2026-09-22

Added drone drop and replaced the robot's unconditional world-item spawn with shared AgentInventoryWorldControl.drop. It validates local/global sides before mutation, clamps requested count0..64, rejects unloaded targets, inserts through the opposing sided block item capability or an adjacent entity automation capability, and retains partial/full remainders. Inventory permission checks post RightClickBlock/EntityInteract; denied access falls back to world tossing as upstream does. ItemTossEvent cancellation or rejected entity insertion retains cargo. World toss position/motion/pickup delay follow upstream. Config robot.delays.drop defaults0.5 with the0.06 scheduling adjustment.

AgentDropGameTests verifies robot and drone with actual chests: exact requested insertion, partial one-item capacity, full rejection, zero world drop, canceled toss conservation, one successful two-diamond world drop and unchanged selected slot. Build/all604 GameTests pass (agent-drop.log).

Entity inventory insertion and inventory interaction denial are implemented but not exercised by these two cases; actual protection mods and sided custom inventories remain integration gates. Suck remains to be ported for drones/corrected for robots. No client launch/install, push or merge.

## Drop integration and beta planning - 2026-10-07

Expanded both AgentDropGameTests fixtures with actual chest minecart insertion, denied block inventory access with the upstream world-toss fallback, and simultaneous denied entity access/canceled toss. Cargo and destination counts are conserved, and temporary listeners/entities are cleaned up. build/finish-port-implementation/agent-drop-integration.log reports BUILD SUCCESSFUL and all604 required GameTests passed. The RTK wrapper stayed alive after its child PowerShell exited; only that identified stale wrapper was stopped, without rerunning Gradle.

The user now explicitly requests intermediate pushes and beta-distance estimates. The existing feature/finish-port branch was pushed at b664266145d2c6227864f05dee2be494eb276db3 and the remote SHA was freshly verified. The canonical plan now records push authorization and the provisional3-6-week estimate, with low confidence until the parity inventory and integration scope are complete. This estimate does not certify feature completion.

## Shared robot/drone suck - 2026-10-07

Added drone suck and corrected the robot callback to return an extracted count rather than true, matching upstream InventoryWorldControl. Adjacent block/entity automation handlers use the same sided discovery and permission events as drop. Extraction simulates selected-slot-first cargo insertion, accepts only available capacity and preserves source remainder. World pickup searches the neighboring block; drones additionally search their own block first. Pickup delay is respected, robot pickup uses NeoForge pre/post events and target ownership, and successful calls apply robot.delays.suck(default0.5 minus0.06).

AgentSuckGameTests covers both hosts with actual chests: requested3 from11, selected-slot priority, denied extraction without mutation, partial one-item capacity, full cargo and zero inventory extraction. World items preserve delay and follow the upstream distinction: requested1 still collects five from a ground stack. Initial run reproduced missing drone callback and unsupported robot chest extraction (agent-suck-red.log). One older robot test expected a boolean and now asserts the correct count2. Final build/all606 GameTests pass; server log preserved as agent-suck-verified.log. Existing drop tests also pass after sharing source/permission helpers.

Entity extraction, custom sided handlers, pickup-event/ownership variants, native continuation and real-client acceptance remain additional gates. Broad beta estimate remains provisional3-6 weeks; this completes another world callback without proving the complete port.

## Pickup lifecycle and minecart extraction - 2026-10-07

A new regression reproduced robot cargo duplication when an ItemEntityPickupEvent.Pre handler removes the item before collection. AgentInventoryWorldControl now rechecks removed/empty state after the handler before inserting any cargo. Initial failure is retained in agent-suck-events-red.log.

The existing robot/drone fixtures also cover actual chest minecart extraction, denied EntityInteract access with unchanged source/cargo, and pickup priority: robots collect the neighboring item while drones collect their own block first. Temporary event listeners and entities are cleaned up. Final build/all607 GameTests pass (server log agent-suck-events-verified.log).

Custom sided inventories, further pickup-event/ownership variants, native world-callback continuation and client acceptance remain gates. No broad port completion claim; beta estimate unchanged pending the full inventory and visual/integration work.

## Base capacitor block - 2026-10-07

The existing capacitor recipe already matched the upstream block recipe, but its result was registered as an ordinary item. It is now a BlockItem under the same ID, preserving recipe references and existing item identity. Added CapacitorBlock/CapacitorBlockEntity, OC network storage, comparator updates, block/item models using existing upstream textures, loot, pickaxe tag, translation and API block registration. Config power.buffer.capacitor defaults1600 and capacitorAdjacencyBonus800.

Capacity follows upstream: base plus full bonus per axis neighbor at distance1, half bonus at distance2. Placement/load/removal refresh both near and second-degree capacitors. Detached load first accepts maximum theoretical capacity(base+9 bonuses), then validates actual neighbors. Reduced capacity clamps stored energy as upstream does; this is not an energy-preserving adjacency-removal redesign.

CapacitorGameTests first reproduced the missing block item (capacitor-red.log). New behavioral coverage checks isolated/direct/second-degree capacities, removal on both sides, charge, rounded comparator14 at90%, detached reload with charge above base and stable node address, then clamping after removing the distant neighbor. Final build/all609 GameTests pass; server log capacitor-base-verified.log. Existing recipes/material catalog tests also pass.

Carpeted generation/animal shock behavior, mixed clusters/chunk-boundary load order, full JVM restart and real-client rendering/player placement remain open. No client launch/install in this checkpoint. The base implementation is a completed slice of the capacitor feature group, not complete port acceptance.

## Carpeted capacitor - 2026-10-07

Registered carpeted_capacitor with a block item, upstream API alias carpetedCapacitor, existing texture/model assets, loot, pickaxe tag and shapeless capacitor+any wool carpet recipe. Both variants share the capacitor block/entity infrastructure. The server ticker generates once per20 game ticks, staggering by position. At least two sheep contribute configured sheepPower(default3); at least two cats/ocelots contribute ocelotPower(default6). Modern domestic cats and wild ocelots form the old ocelot group. Group size above two does not multiply output.

power.carpetedCapacitors.damageChance defaults0.001. A successful trial deals one generic damage, sets the animal panic target and waits1200 game ticks before another possible shock. The cooldown stays transient, matching upstream. Generation uses an explicit game-time/random source helper so behavioral tests use fixed times and seeded RNG; normal ticking passes the actual world clock/random source.

Three GameTests cover single-versus-multiple animal groups, fixed group output, mixed cats/ocelots, full-buffer limit, ordinary/carpeted adjacency, carpeted charge/variant reload, exactly one damage at chance1, no second shock during cooldown, resumed shock after cooldown and real crafting with red carpet. Config overrides and animal fixtures are restored/removed. Final build/all612 GameTests pass; server log carpeted-capacitor-verified.log.

These tests exercise generation with explicit fixed times; natural ticker cadence, physical animal movement, chunk-boundary cluster loading, protection interactions and real-client visual/survival acceptance remain further gates. No complete-port or fully accepted capacitor-flow claim.

## Natural capacitor ticker and network draw - 2026-10-07

carpetedCapacitorTickerSuppliesNetworkPower lets the actual ServerLevel tick the placed block for exactly20 game ticks. Two sheep are held in place with AI/physics disabled and invulnerability, isolating ticker/network behavior from animal movement. A neighboring ordinary capacitor is confirmed on the same network. Its observed global energy increases by exactly one configured sheep-group output, which it then consumes through tryChangeBuffer, restoring the baseline.

Final build/all613 GameTests pass; server log capacitor-ticker-verified.log. This supersedes the open natural ticker cadence item for this setup. Physical animal behavior, chunk boundaries/load ordering, full process restart, visual/survival and protection-mod acceptance remain open. Broader beta estimate is unchanged by this focused acceptance test.

## Cable color connectivity - 2026-10-07

CableBlockEntity now implements Colored and SidedEnvironment. Matching RGB colors connect, different colors isolate, and the default light gray is an upstream-compatible wildcard. Recoloring removes existing graph edges before joining compatible neighbors, retaining the node address. An isolated cable still receives its own network. On load, the stored color is applied before reconnecting. Color persists under oc:renderColorRGB; legacy oc:renderColor dye metadata migrates and RGB takes precedence. Update tags/packets synchronize only color, without server node data.

CableGameTests first reproduced the missing color interface (cable-colors-red.log). Coverage verifies red/blue separation in both joining directions, wildcard bridging/reconnection, stable address and NBT roundtrip, all six sides incompatible, legacy migration and client update tags. Final build/all615 GameTests pass; server log cable-colors-verified.log.

This completes the network/persistence slice only. Dye interaction/consumption, colored item placement/drops, color recipes, cable geometry/tints and real-client acceptance remain open. No client launch/install in this checkpoint. The provisional beta estimate remains3-6 weeks of concentrated work with low confidence, rather than a feature-completion percentage inferred from tests.

## Cable dye interaction and item lifecycle - 2026-10-07

CableBlock now handles DyeItem interactions in either hand. Server recoloring consumes exactly one dye outside Creative, including repeated same-color interactions as upstream does. Non-dye items pass through. Item color uses the modern DYED_COLOR component; BlockItem placement restores it, pick-block and normal loot drops preserve it. Default light gray drops omit the component so they stack with plain cable items. Existing loot rules remain responsible for drop quantity and conditions.

Two new GameTests first reproduced missing dye handling and lost placement color (cable-items-red.log). Final coverage includes survival/offhand and repeated consumption, creative conservation, non-dye rejection, actual BlockItem placement/count, arbitrary RGB, pick-block, exact colored loot and default-color stacking. GameTest makeMockPlayer(CREATIVE) only overrides isCreative; the fixture must also initialize abilities through GameType.CREATIVE.updatePlayerAbilities. Final build/all617 GameTests pass; cable-items-verified.log retains the server log.

Color mixing/washing recipes, cable model/tints and real-client acceptance remain open. No client launch/install in this checkpoint; beta estimate unchanged pending broader integration and visual acceptance.

## Cable color mixing and washing recipes - 2026-10-07

Added registered special recipes colorize_cable and decolorize_cable. Mixing requires one cable ingredient slot and one or more dyes. It uses DyedItemColor.applyDyes for the upstream-equivalent brightness-preserving blend, including a previous color and preserving unrelated components. Cable is added to minecraft:dyeable so the vanilla helper supports it. Washing requires one cable slot and one water bucket, removes only DYED_COLOR, yields one cable and uses normal crafting remainders to return the empty bucket. Inputs are copied rather than mutated.

The new GameTest first reproduced the missing registered recipe (cable-recipes-red.log). It verifies loaded recipe lookup, explicit red+blue RGB AD5398, inclusion of prior color, exact output count, custom-name preservation, unchanged inputs, invalid/multiple cable/non-dye/non-cable rejection and washing/bucket conservation. Final build/all618 GameTests pass; cable-recipes-verified.log preserves the server log. The recipe manager loads1445 recipes.

Mixing resolves modern dye tags through NeoForge DyeColor.getColor. Native DyeItems of every color are supported; real modded dyes remain an integration gate. NeoForge21.1.234's helper excludes BLACK from its tag-only lookup loop, so non-DyeItem black dyes require a follow-up compatibility test/fix. Block dye interaction currently accepts DyeItem only. Model/geometry/tints and real-client survival/visual acceptance remain open. No client launch/install in this checkpoint; broad beta estimate remains provisional3-6 weeks with low confidence.

## Cable geometry, connection states and tints - 2026-10-07

Replaced the full cube with the upstream quarter-block center and thin arms. Each direction stores none/cable/device in an EnumProperty, synchronized as ordinary blockstates. The server computes compatible neighbors from color and opposing SidedEnvironment ports, avoiding queries into unloaded chunks. Placement, load, neighbor changes and recoloring refresh both ends. Shapes cache the64 connected-arm masks for collision and raytrace. Cable uses noOcclusion.

Multipart models preserve upstream geometry/textures: long colored cable-to-cable arms, short colored device arms with untinted plugs, isolated end caps and an inventory model with two plugs. Block/item color handlers apply tint index0 only to cable bodies. Cable color packets explicitly mark the client block mesh dirty when RGB changes: ClientPacketListener otherwise only delivers onDataPacket, which does not invalidate baked tinted geometry. This client path was checked against local Minecraft/NeoForge source; real rendering remains unverified.

The first GameTest reproduced the cube collision/selection shape (cable-shape-red.log). New coverage verifies thin isolated bounds, raytrace misses in empty corners, all six axes, reciprocal cable arms, device ports, solid non-network blocks, splitter redstone open/close, incompatible colors, wildcard reconnection and removal. Final build/all620 GameTests pass; cable-shape-verified.log retains the server log. A separate read-only JSON check enumerated all729 connection combinations and confirmed existing model references and expected center/arm/end-cap counts.

No real-client launch/install in this checkpoint. Visual tint, immediate repaint, model lighting, player placement/collision, chunk-boundary reload and modded dye/protection acceptance remain gates. The full-port goal remains active; provisional beta estimate unchanged at3-6 weeks with low confidence.

## Tagged dyes and canonical cable RGB - 2026-10-07

DyeColors.colorOf now provides one resolver for cable interactions and recipes: native DyeItems take precedence, followed by all sixteen modern color tags, including black. This restores upstream ore-dictionary-style compatibility and avoids NeoForge21.1.234's tag-only black omission. CableBlock no longer limits interaction to DyeItem subclasses.

The new tag fixture exposed another actual network defect: Minecraft DyeColor.getTextureDiffuseColor returns opaque ARGB, while DyedItemColor.applyDyes stores24-bit RGB. Identical-looking dyed and crafted cables therefore had different connectivity values. Cable colors now normalize to24-bit RGB in defaults, setter, item reads, NBT/legacy loads, opposing Colored comparisons and client updates. Previously saved opaque RGB data migrates through the same normalization.

cableAcceptsTaggedDyesIncludingBlack temporarily binds a non-DyeItem fixture into red/black/light-gray tags, checks interaction and exact one-item consumption, loaded recipe matching/output, unchanged inputs, same-color network joining between interaction and recipe results, and light-gray wildcard connectivity. It restores the complete original registry tag map in finally before yielding a game tick. Initial failures are retained in cable-dye-tags-red.log and cable-rgb-red.log. Final build/all621 GameTests pass; cable-dye-tags-verified.log preserves the final server log. Existing legacy/client color assertions now check the correct upstream24-bit RGB contract.

These controlled tags prove the integration mechanism, not interoperability with an installed dye/protection mod. Real-client and chunk-boundary gates remain open. No client launch/install in this checkpoint. Next missing feature group: player hover boots (distinct from the already existing robot hover upgrade). Broader beta estimate remains provisional3-6 weeks with low confidence.

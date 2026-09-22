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
| Capacitorblokken | Upstream capacitor/carpeted capacitor; port heeft alleen capacitor-materiaalitem/recept | Blokken, opslag, adjacency, carpet/kleur, models en recipes porten; energieconservering en save/load. |
| Kabelkleuren | Upstream `common/tileentity/Cable.scala`, `common/block/Cable.scala`; port zonder kleurfilter | Verven/ontkleuren, NBT en netwerkherbouw; gelijke kleuren verbinden, verschillende isoleren, standaardkleurgedrag vergelijken. |
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

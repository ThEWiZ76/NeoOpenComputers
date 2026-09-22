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

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

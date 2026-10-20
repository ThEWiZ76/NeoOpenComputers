# JEI integration

Related: [[Complete portering]], [[../04 ai-context/Modern integration scope]], [[../03 manuals/JEI documentation integration]].

Target MC1.21.1/NeoForge21.1.234. Pinned JEI19.21.0.246; newer19.64 needs loader21.1.238 and is not the current target. Runtime checkpoint 19b84ae4d5e828b005a02d636c09884b580a9467 implements the documentation/input stage; the integration remains open.

- [x] Optional API/runtime dependency and absence/packaging checks.
- [x] Manual usage category and existing manual navigation APIs wired.
- [x] Class-only callback discovery, aliases/inheritance, font wrapping and multipage display wired.
- [x] ItemSearch hover/focus lifecycle and simulated-input safety covered by focused unit cases.
- [x] Subtype registration/interpreter implementation: assembled robot/microcontroller/drone/tablet components and floppy factory identity; ordinary disks ignore mutable labels/storage IDs. Registry contracts tested; actual client UID/overlay acceptance remains open. See [[../03 manuals/JEI item subtypes]].
- [ ] Complete loot-disk cycling parity: display extension bfbddb03 uses the actual recipe and linked input/output pairs (including addon/opt-out inputs), tested with884 mixed/803 absent. Config fe19f13 restores upstream misc.lootRecrafting and verifies authoritative recipe-set/packet state, including a cold-disabled startup. Still verify live client data-pack reload and lookup/transfer. See [[../03 manuals/Loot recrafting config]]. See [[../03 manuals/JEI loot disk cycling]].
- [ ] Runtime disk addition: batch on the client thread, avoid duplicates/foreign stack mutation, detach/clear correctly across JEI runtime replacement.
- [ ] Hidden ingredients: preserve upstream policy using actual current content and integration availability.
- [ ] GUI extra areas: map the old relay tab to actual visible modern screen geometry; do not invent obsolete panels.
- [ ] Compare animated callback icon versus current item rendering and record visual parity.
- [ ] Actual client acceptance on screen3: categories/order/links/wrapping/search focus/hover/reconnect, minimal and mixed profiles.
- [ ] End-to-end installed artifact/version/hash and release documentation evidence.

The current server runs prove compatibility with optional jars and without them. They do not execute the JEI client registration/render path. Preserve rejected-launch evidence: the newer JEI loader error returned Gradle success without GameTests, so every gate must require its real test summary. Full port and beta acceptance remain open.


Loot cycling checkpoint b9ab176319f56fc6c8a882502a6006d01eef2206: actual crafting now uses copied, ordered API-registered cycling templates; opt-out controls outputs rather than valid loot inputs. Legacy string keys and unknown/empty identity fallback are restored, ordinary disks remain rejected. Three baseline failures in796 tests; four final cases cover registered/bundled sequence/wrap, wrench retention and template isolation. Build/2187 units/872 mixed/797 absent pass normally. See [[../03 manuals/Loot disk cycling]]. This is a core prerequisite, not completion of the JEI recipe display. Public addon factory restart identity, client registry sync/external loot sources and full JEI/client/port gates remain open. Beta3-6 concentrated workweeks, low confidence.

Addon factory checkpoint 80f2679c62cee8ebab07a288fff860f502e5e797: public registration now persists active mod namespace plus stable name. Old UUID implementation fails fresh-JVM factory resolution; fixed prepare/verify both805 tests, PIDs46984/42364. Build/2188 units and874 mixed pass. Equal-name cross-mod serialization/cycling isolation covered. See [[../03 manuals/Addon floppy factory persistence]]. Old UUID migration, client metadata sync and JEI runtime display remain open; installed client remains9bc57db. Beta3-6 concentrated workweeks, low confidence.

Subtype checkpoint 202ae8d15b6ac5c8975a4673b5fd03aeac238a7b: build/2188 units/880 mixed/805 final JEI-only/802 absent pass. Prepared newest jar/hash and the source-audit recipe-vs-driver precedence discrepancy to reproduce before cycling display are in [[../03 manuals/JEI item subtypes]]. Current client still9bc57db; pending shutdown coordination has no reply yet.

Checkpoint bfbddb03: actual-recipe JEI crafting extension and factory precedence4c7f161 implemented; build/2188 units/884 mixed/803 absent pass. The upstream true-default misc.lootRecrafting option is confirmed missing from the COMMON config. Next investigate conditional server recipe loading to restore the setting with correct synchronized client visibility, then runtime disk metadata/additions and remaining GUI/client gates. No profile update yet.

Checkpoint fe19f13: missing lootRecrafting option restored with a server-evaluated recipe condition; no extra sync packet or client-local predicate. Unit/path default, real conditional reload, fresh false/true startup and actual recipe packet codec/receiver tests pass. Build/2189 units/886 final mixed/805 cold-disabled absent; entire test config restored. Client/JEI live acceptance remains open. Next: runtime disk metadata/additions and remaining visibility/GUI/icon gates.

# JEI integration

Related: [[Complete portering]], [[../04 ai-context/Modern integration scope]], [[../03 manuals/JEI documentation integration]].

Target MC1.21.1/NeoForge21.1.234. Pinned JEI19.21.0.246; newer19.64 needs loader21.1.238 and is not the current target. Runtime checkpoint 19b84ae4d5e828b005a02d636c09884b580a9467 implements the documentation/input stage; the integration remains open.

- [x] Optional API/runtime dependency and absence/packaging checks.
- [x] Manual usage category and existing manual navigation APIs wired.
- [x] Class-only callback discovery, aliases/inheritance, font wrapping and multipage display wired.
- [x] ItemSearch hover/focus lifecycle and simulated-input safety covered by focused unit cases.
- [ ] Subtypes: assembled robot/microcontroller/drone/tablet data and floppy factory identity; ordinary disks must not split on mutable labels/storage IDs.
- [ ] Loot-disk cycling display must reflect the real enabled recipe and actual input-to-next-output relation, including registered disks.
- [ ] Runtime disk addition: batch on the client thread, avoid duplicates/foreign stack mutation, detach/clear correctly across JEI runtime replacement.
- [ ] Hidden ingredients: preserve upstream policy using actual current content and integration availability.
- [ ] GUI extra areas: map the old relay tab to actual visible modern screen geometry; do not invent obsolete panels.
- [ ] Compare animated callback icon versus current item rendering and record visual parity.
- [ ] Actual client acceptance on screen3: categories/order/links/wrapping/search focus/hover/reconnect, minimal and mixed profiles.
- [ ] End-to-end installed artifact/version/hash and release documentation evidence.

The current server runs prove compatibility with optional jars and without them. They do not execute the JEI client registration/render path. Preserve rejected-launch evidence: the newer JEI loader error returned Gradle success without GameTests, so every gate must require its real test summary. Full port and beta acceptance remain open.


Loot cycling checkpoint b9ab176319f56fc6c8a882502a6006d01eef2206: actual crafting now uses copied, ordered API-registered cycling templates; opt-out controls outputs rather than valid loot inputs. Legacy string keys and unknown/empty identity fallback are restored, ordinary disks remain rejected. Three baseline failures in796 tests; four final cases cover registered/bundled sequence/wrap, wrench retention and template isolation. Build/2187 units/872 mixed/797 absent pass normally. See [[../03 manuals/Loot disk cycling]]. This is a core prerequisite, not completion of the JEI recipe display. Public addon factory restart identity, client registry sync/external loot sources and full JEI/client/port gates remain open. Beta3-6 concentrated workweeks, low confidence.

Addon factory checkpoint 80f2679c62cee8ebab07a288fff860f502e5e797: public registration now persists active mod namespace plus stable name. Old UUID implementation fails fresh-JVM factory resolution; fixed prepare/verify both805 tests, PIDs46984/42364. Build/2188 units and874 mixed pass. Equal-name cross-mod serialization/cycling isolation covered. See [[../03 manuals/Addon floppy factory persistence]]. Old UUID migration, client metadata sync and JEI runtime display remain open; installed client remains9bc57db. Beta3-6 concentrated workweeks, low confidence.

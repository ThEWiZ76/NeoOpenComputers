# AE2 Integration Implementation Plan

> Execute with superpowers:executing-plans. User authorized implementation and intermediate pushes. No plan-review agents. Canonical Obsidian plan; repository mirror AE2_INTEGRATION_PLAN.md.

**Goal:** Restore the complete upstream AE2 integration on Minecraft 1.21.1, including network/storage descriptions, crafting userdata and persistence, interfaces/buses, converters and auxiliary integration behavior.

**Architecture:** Optional AE2 classes remain behind an installed-mod guard. Resolve live grids through AE2's public in-world node capability without loading chunks. Server-thread callbacks operate on the real grid; asynchronous crafting calculations must never block the server thread. Database entries are descriptions, not owned items.

**Tech Stack:** Java21, NeoForge21.1.234, AE2 19.2.18 public API, native Lua53/54, JUnit and real server GameTests.

Baseline: upstream master-MC1.12 7d9eac5584e238a890a388a0f697c27c7391caa4, integration/appeng (12 files,27 annotated callbacks plus inherited and userdata contracts). Runtime baseline820fee37b. See [[../04 ai-context/Modern integration scope]] and [[Complete portering]].

## 1. Controller, energy and storage descriptions

Files: build.gradle; src/main/resources/META-INF/neoforge.mods.toml; src/main/java/li/cil/oc/common/ModDriverCatalog.java; new common/integration/AppliedEnergisticsIntegration.java and common/gametest/AppliedEnergisticsGameTests.java.

- [ ] Pin optional API compile dependency and opt-in `-PwithAE2` runtime/test profile; no external classes bundled.
- [ ] Write a real controller/drive/cell fixture and native Lua physical-adapter discovery test. Run before implementation, save missing-driver evidence.
- [ ] Register controller driver, component me_controller, NamedBlock priority5. Guard removed/unloaded/replaced tile; obtain current node/grid each call.
- [ ] Implement getCpus and seven energy callbacks. Consume1.5 budget for getEnergyDemand. Compare moving quantities only in a single server-thread snapshot.
- [ ] Implement getItemsInNetwork with stored/craftable-only union, long quantities, accurate converted descriptors and recursive subset filters. Implement getFluidsInNetwork with AE2 fluid-unit conversion verified from API source.
- [ ] Implement store optional filter/address/start/count, reachable-database lookup, one-based bounded slots and skipping occupied slots; never extract or give physical ownership. Test full database, invalid bounds/address, occupied entries and unchanged network contents.
- [ ] Verify build/units and real powered-grid item/fluid/CPU/energy contracts, native Lua; standalone, mixed-mod and no-AE2 profiles. Save logs; commit/push completed step.

## 2. Crafting userdata and lifecycle

Files: integration/ae2/CraftableValue.java, CraftingStatusValue.java and supporting request state only if needed; integration driver; AE2 GameTests and focused unit tests.

- [ ] Read modern ICraftingSimulationRequester, calculation, submit-result/link and pattern APIs. Reproduce missing getCraftables against real molecular assembler/pattern provider/storage/CPU setup.
- [ ] Implement getCraftables(filter), getItemStack, requesting and request(amount=1,prioritizePower=true,cpuName=""). Preserve upstream error/result contracts and recipe output count.
- [ ] Calculate asynchronously; consume Future off-thread, submit exactly once on server thread. Handle failed calculation, insufficient storage, unavailable/busy/named CPU and removed grid.
- [ ] Implement status computing/isDone/isCanceled/cancel; prove actual completed output and cancellation/resource conservation, including Lua calls.
- [ ] Persist position/dimension/key/request link/status; restore without loading chunks, bounded retry delay, deduplicate restored links and release lifecycle state. Prove actual two-process save/restart/resume and terminal status.
- [ ] Run focused and full relevant checks, document evidence, commit/push.

## 3. Interfaces and import/export buses

Files: focused integration/ae2 drivers and shared network control; AE2 GameTests.

- [ ] Verify modern block/part interface and bus capabilities; reproduce missing me_interface/me_importbus/me_exportbus discovery and EnvironmentProviders.
- [ ] Share complete network-control callbacks across upstream hosts. Restore interface get/set configuration, optional slot overload, database-copy semantics, quantity clamping and0.5-second pause.
- [ ] Restore side-specific bus configuration, errors and exportIntoSlot with real inventories, speed/fuzzy upgrades and0.25-second successful pause. Test simulated versus actual extraction, rejected inserts, rollback and item/fluid conservation.
- [ ] Test all sides, stale/unloaded hosts and missing parts without chunk loads, Lua argument/results and persistence. Commit/push verified step.

## 4. Cell converter and auxiliary contracts

Files: integration/ae2 converter, tool/spatial integration registration, EnvironmentProviders and tests.

- [ ] Map upstream cell descriptor keys to modern public cell APIs: quantities/types/bytes/free capacity/fuzzy/name/content; test real item and fluid cells. Do not fabricate zero-valued legacy metrics.
- [ ] Restore AE2 wrench behavior and upstream spatial movable whitelist for OC prints where supported; verify current public contracts and real interactions.
- [ ] Register corresponding item EnvironmentProviders and test API environment lookup.
- [ ] Verify absence loading, artifact/POM isolation, API linkage and mixed-mod behavior; commit/push.

## 5. Full acceptance

- [ ] Install verified candidate in the specific Modrinth profile when coordinated with user's parallel desktop work. Minecraft must be on screen3; no obfuscated PowerShell or bypassing antivirus.
- [ ] In real client verify controller/drive/buses/interface models, menus, placement/rotation, cable connection, native OpenOS Lua programs, real crafting result/cancel, save/reload and reconnect. Capture actual results and cleanup temporary fixtures.
- [ ] Dedicated/multiplayer acceptance, invalid inputs and no-AE2 launch; full regression and two-process persistence evidence as applicable.
- [ ] Update complete port plan, parity/gap/scope register and manual canonically first; mirror repository docs; commit/push and verify remote SHA equals local.

Beta estimate remains3-6 concentrated workweeks, low confidence; this is the whole port estimate, not AE2 alone. Re-estimate after crafting/persistence and remaining integration discovery. No step above is complete merely because callback names exist or unrelated tests pass.

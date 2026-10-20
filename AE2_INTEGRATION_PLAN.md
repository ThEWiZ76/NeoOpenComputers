# AE2 Integration Implementation Plan

> Execute with superpowers:executing-plans. User authorized implementation and intermediate pushes. No plan-review agents. Canonical Obsidian plan; repository mirror AE2_INTEGRATION_PLAN.md.

**Goal:** Restore the complete upstream AE2 integration on Minecraft 1.21.1, including network/storage descriptions, crafting userdata and persistence, interfaces/buses, converters and auxiliary integration behavior.

**Architecture:** Optional AE2 classes remain behind an installed-mod guard. Resolve live grids through AE2's public in-world node capability without loading chunks. Server-thread callbacks operate on the real grid; asynchronous crafting calculations must never block the server thread. Database entries are descriptions, not owned items.

**Tech Stack:** Java21, NeoForge21.1.234, AE2 19.2.18 public API, native Lua53/54, JUnit and real server GameTests.

Baseline: upstream master-MC1.12 7d9eac5584e238a890a388a0f697c27c7391caa4, integration/appeng (12 files,27 annotated callbacks plus inherited and userdata contracts). Runtime baseline820fee37b. See [[../04 ai-context/Modern integration scope]] and [[Complete portering]].

## 1. Controller, energy and storage descriptions

Files: build.gradle; src/main/resources/META-INF/neoforge.mods.toml; src/main/java/li/cil/oc/common/ModDriverCatalog.java; new common/integration/AppliedEnergisticsIntegration.java and common/gametest/AppliedEnergisticsGameTests.java.

- [x] Pin optional API compile dependency and opt-in `-PwithAE2` runtime/test profile; no external classes bundled.
- [x] Write a real controller/drive/cell fixture and native Lua physical-adapter discovery test. Run before implementation, save missing-driver evidence.
- [x] Register controller driver, component me_controller, NamedBlock priority5. Guard removed/unloaded/replaced tile; obtain current node/grid each call.
- [x] Implement getCpus and seven energy callbacks. Consume1.5 budget for getEnergyDemand. Compare moving quantities only in a single server-thread snapshot.
- [x] Implement getItemsInNetwork with stored/craftable-only union, long quantities, accurate converted descriptors and recursive subset filters. Implement getFluidsInNetwork with AE2 fluid-unit conversion verified from API source.
- [x] Implement store optional filter/address/start/count, reachable-database lookup, one-based bounded slots and skipping occupied slots; never extract or give physical ownership. Test full database, invalid bounds/address, occupied entries and unchanged network contents.
- [x] Verify build/units and real powered-grid item/fluid/CPU/energy contracts, native Lua; standalone, mixed-mod and no-AE2 profiles. Save logs; commit/push completed step.

## 2. Crafting userdata and lifecycle

Files: integration/ae2/CraftableValue.java, CraftingStatusValue.java and supporting request state only if needed; integration driver; AE2 GameTests and focused unit tests.

- [x] Read modern ICraftingSimulationRequester, calculation, submit-result/link and pattern APIs. Reproduce missing getCraftables against real molecular assembler/pattern provider/storage/CPU setup.
- [x] Implement getCraftables(filter), getItemStack, requesting and request(amount=1,prioritizePower=true,cpuName=""). Preserve upstream error/result contracts and recipe output count.
- [ ] Complete calculation/submission failure matrix. Implemented: AE2 asynchronous calculation, server-thread Future.isDone polling and get only after completion (nonblocking), then exactly-once submission; this replaces an unnecessary extra waiting worker. Named/missing/busy/too-small/no-CPU and missing-ingredient outcomes now tested via controller/block/panel. CPU_OFFLINE/failed calculation and removed-grid cases remain open.
- [x] Implement status computing/isDone/isCanceled/cancel; prove actual completed output and cancellation/resource conservation, including Lua calls.
- [x] Persist position/dimension/key/request link/status, canonical alias identity and world-owned progress/outcomes; three actual two-process cases now prove active/terminal/error and late old-heap recovery. Loaded-target level-end rebind fixes startup ordering; no forced chunk loads.
- [ ] Finish restart/failure acceptance: save while computing, busy/offline/too-small CPU, missing/replaced target, true chunk eviction/all orderings, history/cleanup/load and abrupt-stop consistency.
- [ ] Run focused and full relevant checks, document evidence, commit/push.

## 3. Interfaces and import/export buses

Files: focused integration/ae2 drivers and shared network control; AE2 GameTests.

- [x] Verify block/part/bus ghost-config contract in pinned AE2 implementation; reproduce missing drivers/native discovery and add EnvironmentProviders.
- [x] Share complete network control across block/panel interfaces, including real native crafting, and restore get/set config/default-slot overload/database-copy/clamp/0.5-second pause.
- [x] Restore side-specific import/export filter configuration and errors; all6faces, wrong-kind guards, providers, node identity and cold ghost NBT tested.
- [x] Restore item exportIntoSlot with real sided inventories, all five speed budgets, fuzzy upgrades and0.25-second successful pause. Actual-versus-simulated extraction, rejected/partial insertion, ME refund/full-cell drop and exact item conservation tested. Fluid filters are not item-slot exports; see [[../03 manuals/AE2 manual export]].
- [ ] Test all sides, stale/unloaded hosts and missing parts without chunk loads, Lua argument/results and persistence. Commit/push verified step.

## 4. Cell converter and auxiliary contracts

Files: integration/ae2 converter, tool/spatial integration registration, EnvironmentProviders and tests.

- [x] Map original cell descriptor keys through real StorageCells/BasicCellInventory APIs, preserving quantities/types/bytes/free capacity/fuzzy/name/content; actual item/fluid empty/two-type/full tests and live/wrapper input proof. Omit unavailable metrics on special cells.
- [x] Register modern AE2 tagged-wrench OC behavior; survival adapter proof with three real tools. Modern default spatial strategy already moves OC prints; real strategy reconstruction preserves NBT/rotation. Full powered spatial cycle remains an acceptance gate.
- [x] Existing five device EnvironmentProviders cover upstream component lookup; cell/wrench/print auxiliary hooks do not add a component type.
- [x] Verify absence loading, artifact/POM isolation and full mixed-mod server regression; commit/push auxiliary checkpoint.

## 5. Full acceptance

- [ ] Install verified candidate in the specific Modrinth profile when coordinated with user's parallel desktop work. Minecraft must be on screen3; no obfuscated PowerShell or bypassing antivirus.
- [ ] In real client verify controller/drive/buses/interface models, menus, placement/rotation, cable connection, native OpenOS Lua programs, real crafting result/cancel, save/reload and reconnect. Capture actual results and cleanup temporary fixtures.
- [ ] Dedicated/multiplayer acceptance, invalid inputs and no-AE2 launch; full regression and two-process persistence evidence as applicable.
- [ ] Update complete port plan, parity/gap/scope register and manual canonically first; mirror repository docs; commit/push and verify remote SHA equals local.

Beta estimate remains3-6 concentrated workweeks, low confidence; this is the whole port estimate, not AE2 alone. Re-estimate after crafting/persistence and remaining integration discovery. No step above is complete merely because callback names exist or unrelated tests pass.

Controller checkpoint 5b60fee0b7577cd2a95c831969903a9237e695eb: first work package verified with2140 units,730 mixed/728 standalone/726 absent server tests. Craftable-key union exists; positive recipe output and userdata acceptance stays in task2. See [[../03 manuals/AE2 controller integration]].

Crafting checkpoint f36740a717c5b9ee121f51f2ab91aca8ef3bf189: actual native assembler output, processing cancellation, named/missing CPU, missing ingredients and same-process saved aliases verified;2140 units/731 mixed/729 standalone/726 absent tests. Full two-process/host-offline persistence and wider submission-failure matrix remain task2 gates before claiming complete AE2. See [[../03 manuals/AE2 crafting integration]].

Restart checkpoint 2b4e1b752ddeda019c877841b135e82325022f8a: journal-backed active/terminal recovery after reproduced older-heap completion loss and startup late-bind regression. Final world oc-restart-ae2-crafting-20261020-06: PIDs53528/54476,736 tests each;2141 units;726 absent profile. See [[../03 manuals/AE2 crafting restart]]. Remaining explicit acceptance above and interfaces/buses/converters/auxiliary/client work stay open; beta3-6 workweeks, low confidence.

Interface/config checkpoint b40e4be50d388502ca9c2dddc34141f8ec777998: actual controller/block/panel crafting and CPU_BUSY/CPU_TOO_SMALL/NO_CPU_FOUND resource-conservation matrix;5added tests,2141 units,741-test prepare/verify JVMs,736 mixed/726 absent. Manual exportIntoSlot remains open. Reused-world ChunkMap unload hang preserved and not claimed fixed; fresh full suites pass. See [[../03 manuals/AE2 interfaces and bus configuration]].

Export checkpoint 37fb64458a819bd3e45377174a1cabdf69e7bf41: actual exportIntoSlot with sided inventory/slot handling, speed/fuzzy filters, powered machine extraction, rejected-remainder refund/drop and exact item conservation. Ten new server/native tests; build/2141 units/746 mixed/726 absent pass. See [[../03 manuals/AE2 manual export]]. Converter/auxiliary, remaining crafting/stability/client gates stay open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.

Auxiliary AE2 checkpoint cf37b92c85bf5cbfb0979427e9b811959a569b97: real item/fluid cell descriptions, basic-cell capacity/content metrics, live/wrapper support and AE2 tagged-wrench OC use restored. Default spatial strategy already preserves OC print NBT/orientation; no redundant mover added. Four new tests; build/2141 units/750 mixed/726 absent green. See [[../03 manuals/AE2 cell descriptions and auxiliary hooks]]. Full spatial/client and remaining crafting/stability/global integration acceptance stay open. Beta3-6 concentrated workweeks, low confidence; no client/Computer Use action.

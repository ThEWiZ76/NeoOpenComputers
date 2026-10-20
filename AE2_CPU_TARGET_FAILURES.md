# AE2 CPU and target failures

Runtime checkpoint: ece63b5f4470c66d2135391ad051533d5c40dfb3. Minecraft 1.21.1 / NeoForge 21.1.234 / AE2 19.2.18. Related: [[AE2 crafting integration]], [[AE2 crafting restart]], [[../02 plans/AE2 integration]].

## Reproduced product defect

Upstream chooses a named CPU in the request callback before the asynchronous calculation returns. The port instead looked up that name at submission time. Removing the selected CPU produced no-cpu-named, while replacing it with another CPU of the same name silently submitted the request to the replacement. A corrected real-grid baseline reproduces six CPU-case failures across controller, block interface and panel (ae2-failures-verified-red-build.log): removed CPUs report no-cpu-named and same-name replacements accept a job. The nine host-removal/replacement scenarios already pass that baseline.

Named CPU selection now captures the actual ICraftingCPU at request time, and submission uses that object. If it disappears, AE2 reports cpu_offline instead of selecting a replacement or backup. Missing initial names still fail explicitly and automatic CPU selection remains automatic. Releasing/retrying the request clears the captured reference; a restored calculation without a live selection resolves its name before recalculating. The selected runtime object itself is not serialized. Computing-save/restart identity policy and its full acceptance remain open; this checkpoint does not claim to prove it.

## Real server matrix

Fifteen added GameTests use actual powered grids, storage drives/cells, a processing provider and two separate named crafting CPUs. Each of controller/block/panel covers:

- Selected CPU removed after the requester/calculation starts but before submission; expect request failed (cpu_offline).
- Selected CPU replaced by a new actual CPU of the same name in that window; expect the same failure and no fallback.
- Host removed before submission; expect request failed (no controller).
- Host replaced with a new instance of the same block/part kind; expect the same failure rather than rebinding the stale value.
- Host removed after an actual processing link is submitted; expect failure, cancellation/CPU cleanup and retained ingredients.

CPU-window tests use the deliberate one-tick calculation/submission boundary and assert a saved requester node exists, no submitted link exists, and status is computing before mutation. They do not depend on wall-clock calculation speed. Original/restored aliases agree on isDone/isCanceled/cancel failure results. Each case checks exactly 16 logs across real ME storage and the external chest, no gold output, no CPU still busy, and no pre-submission input consumption. Final checks wait for the actual stable cleanup state, not a guessed delay. The source keeps its own power/connection path after host or selected CPU removal.

Early setup failures were preserved separately: ae2-failures-fixture-red.log, ae2-failures-wired-red.log and ae2-failures-name-fixture-red.log. A drive's front is not a grid connection, and loading customName NBT does not update the name of an already formed CPU cluster. The final fixture uses an independent cable path, public CraftingBlockEntity.setName, and explicit CPU-name/storage-mount readiness checks. Only the corrected baseline is used as product-regression evidence.

## Verification

Full build/2141 units/765 mixed AE2+EnderStorage+ProjectRed+CC GameTests pass. No-optional-mod profile passes 726 tests. Actual process pair oc-restart-ae2-failures-20261020-01: prepare PID45520 and verify PID38640, each770 tests with AE2+ProjectRed+CC. This rechecks the existing three active/offline/delayed-host crafting restart fixtures and four other restart probes alongside the new failure matrix. It is not a save-during-computing, abrupt-crash or panel-specific link-recovery proof.

Logs under build/finish-port-implementation: ae2-failures-verified-mixed-build.log; ae2-failures-absent-build.log; ae2-failures-restart-prepare-build.log; ae2-failures-restart-verify-build.log; matching saved latest/debug logs and process metadata.

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithAE2 -PwithEnderStorage -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
exit $LASTEXITCODE
```

For process restart use the same prepare/verify property pattern in [[AE2 crafting restart]], a fresh named oc-restart-* world, one Gradle process at a time and preserve latest.log between runs.

## Remaining beta gates

Failed calculation/future, missing-ingredient-at-submission races, unavailable/auto-excluded CPU outcomes, computing-save, multi-grid topology changes, true chunk eviction, history/cleanup/load and abrupt-stop consistency remain to be covered. The intermittent ChunkMap unload loop is still unresolved. Full spatial/client/multiplayer/protection/platform acceptance and the other integration families/full semantic audit remain open. No client/Computer Use/Modrinth install/window movement occurred. Real Minecraft acceptance remains required on screen 3 with desktop coordination.

Whole-port beta estimate remains **3-6 concentrated workweeks, low confidence**, including full implementation, bugfixes and real client acceptance. Test counts are not a completion percentage.

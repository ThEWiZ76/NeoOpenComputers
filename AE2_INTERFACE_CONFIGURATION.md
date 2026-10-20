# AE2 interfaces and bus configuration

Follow-up: exportIntoSlot is now implemented and verified in [[AE2 manual export]]. Remaining-export statements below describe this earlier checkpoint.

Runtime checkpoint: b40e4be50d388502ca9c2dddc34141f8ec777998. Related: [[AE2 controller integration]], [[AE2 crafting integration]], [[AE2 crafting restart]], [[../02 plans/AE2 integration]], [[../04 ai-context/Modern integration scope]]. MC1.21.1/NeoForge21.1.234/AE2 19.2.18.

## Implemented

- Block ae2:interface exposes me_interface with priority5. Panel ae2:cable_interface exposes me_interface with priority0. Both inherit the complete12 network-control callbacks, including actual crafting requests and their persistent value/target machinery.
- Import/export multipart drivers expose me_importbus priority1 / me_exportbus priority2 with their respective get/set configuration callbacks. A single bus does not advertise the other kind's callbacks or controller network callbacks. On a mixed host, compound composition combines the applicable APIs and uses the highest preferred-name priority, so an import/export/interface combination may be named me_exportbus.
- Item EnvironmentProviders registered for controller, block interface, panel, import bus and export bus. Multipart matching uses the actual part item ID; named driver types keep compound-environment persistence keys distinct.
- Block interface getInterfaceConfiguration(slot=1); setInterfaceConfiguration([slot][,database,entry,size=1]), including the address-first default-slot overload. Panel uses side first, then the same optional slot/address/entry/size arguments. Bus get/set similarly require a world side0..5 and select only the requested kind on that face.
- Setters copy database descriptors into AE2's ghost-config inventory, never physical inventory. Clamp positive item quantity to the item's max stack size; empty entry, nonpositive size or omitted database clears. One-based config/database bounds validated before mutation. Invalid side/slot and missing/non-database errors are explicit. Missing/wrong-kind parts return nil,no matching part. Successful writes pause0.5seconds and return true.
- Getters return converted item/fluid descriptions, nil for an empty slot. Modern type-only bus filters store a key with zero quantity; their item description reports size1, since the filter is identity rather than owned stock. Block/panel stocking configurations retain their actual configured quantity.
- Interface network resolution uses IActionHost.getActionableNode for the internal block node. AE2's sided getGridNode(null) does not mean internal and returns no exposed node; the first native fixture reproduced that error. Part recipes save the selected panel ID and side, resolve its actual node and guard stale same-process part identity. Panel network selection prefers the adapter-facing panel, otherwise the first matching face; explicit config calls always use their supplied face.
- Pinned compileOnly dependency now uses the full AE2 artifact rather than the API classifier: the ghost-config host API IConfigInvHost/GenericStackInv is in AE2's implementation. No reflection is used to modify private ghost inventories, and no foreign classes/dependency are bundled/published. Foreign references remain behind the installed-mod guard. Other AE2 versions require verification against that pinned contract.

## Real server/native proof

Missing block/panel drivers and native me_interface discovery reproduced before production edits (ae2-interface-red.log). The first generic ConfigDriver implementation then reproduced a mixed import/export NBT-key collision; distinct driver classes fixed it before this checkpoint was pushed (ae2-interface-identity-red.log).

Five added GameTests and expanded crafting fixtures cover:

- Real block interface, custom-named stone descriptor, default/explicit slots, size128 clamped64, zero/slot-only clearing, invalid bounds/address without mutation, exact0.5 pause sequence and unchanged physical database count12. A positive ghost configuration survives cold block-entity NBT reconstruction.
- Real fluix cable hosts with panels on all6faces and alternating import/export parts on all6faces. Each side selects the right actual ghost inventory; wrong-kind access fails. Single-kind hosts expose only their own API. Mixed child OC node addresses survive save/remove/load exactly, and bus ghost keys survive cold multipart NBT reconstruction. Removed panels no longer expose config. Database descriptions remain nonphysical.
- Native Lua through a physical adapter reads an actual size5 stocking entry, queries interface network/crafting/CPU/power APIs, clears the entry and observes nil.
- The same real assembler/provider/drive/CPU fixture now runs via controller, block interface and panel separately. Each discovers real recipes, requests actual output, handles named/missing CPU and missing ingredients, processing cancellation and saved aliases. All3 cover CPU_BUSY, CPU_TOO_SMALL with a16000-plank plan backed by4000actual stored logs, and NO_CPU_FOUND after removing the real CPU. Rejected jobs preserve the exact total of16064plank-equivalent units, including logs already sent into the external chest.

Build/2141 unit tests pass. Actual process pair oc-restart-ae2-interface-20261020-04, prepare PID56872 / verify PID33420, each741 tests with AE2+ProjectRed+CC; original3 controller restart/offline/delayed-host probes and the4 other restart probes remain green. This does not newly prove panel-specific JVM link recovery. Ordinary AE2+EnderStorage+ProjectRed+CC profile passes736 tests; no optional mods passes726. Artifact/POM verifier confirms isolation after the compile dependency change.

Logs under build/finish-port-implementation: ae2-interface-verified-prepare.log, ae2-interface-verified-verify.log, ae2-interface-mixed-verified.log, ae2-interface-absent-verified.log, matching build/debug logs, and the preserved missing-driver/null-side/identity red logs.

## Unresolved unload-loop observation

One reused-default-world run stalled before the ae2 batch, at debugCardPlayerState. Two thread dumps of verified NeoForge PID8348 showed the server continuously processing ChunkMap.processUnloads / ConcurrentLinkedQueue / CompletableFuture completion work. Logs and dumps were saved as ae2-interface-hang.log, ae2-interface-hang-threads.txt and ae2-interface-hang-threads-second.txt. Only that confirmed owned test-server process was stopped; its Gradle handle then returned terminal exit-1. No second Gradle run was started while it was live.

Fresh isolated worlds ran the entire suite, including the same debug-card batch, successfully. This is not proof that the intermittent unload-loop cause is fixed or that the new interfaces caused it. Keep it as an unresolved stability/long-load gate before beta; earlier unload-hang observations remain relevant.

## Run

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithAE2 -PwithEnderStorage -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
exit $LASTEXITCODE
```

For a real restart pair use the same properties/world pattern as [[AE2 crafting restart]], with a fresh oc-restart-* world, one Gradle process at a time, and preserve latest.log before verify.

## Remaining

exportIntoSlot is still missing; this checkpoint ports bus configuration, not the complete manual export operation. Next: real sided item transfer, exact slot/speed/fuzzy behavior, simulate/commit/rollback, power/protection and conservation tests. Cell converters, tool/spatial hooks, broader CPU_OFFLINE/computing-save/failure matrix, panel-specific fresh-process recovery, true chunk eviction/multi-grid panels, long-load/cleanup and multiplayer/protection remain open. Complete JEI/manual/other provider families remain in the global plan. No client/Computer Use/Modrinth install/window movement occurred; actual ingame visual acceptance must use screen3 with desktop coordination.

Whole-port beta estimate remains3-6 concentrated workweeks, low confidence, including full implementation, bugfixes and real client acceptance. Green test counts are not a completion percentage.

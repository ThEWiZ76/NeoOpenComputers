# API addon runtime acceptance

Related: [[Complete portering]], [[../03 manuals/Process restart test]], [[../04 ai-context/Port parity matrix]].

2026-10-20: close the explicitly open standalone-addon loading gate, beyond the existing API-JAR compiler smoke. Compile a small example.ocapi addon against the published API JAR and platform dependencies, excluding OC implementation output. Package it as a separate optional NeoForge mod; never bundle/publish it inside OC. The example registers its item driver and converter from enqueued common setup, not through test-only registry unlocking. A flagged vanilla paper fixture avoids creating user-facing test content.

Run actual LuaJ/native53 programs against its discovered component, callbacks, binary converter and AbstractValue methods. Native53 prepare/verify in distinct JVMs must preserve the addon environment count/node and aliased userdata state through natural chunk saves. Requested fixture absence and late runtime registration must fail explicitly. Preserve ordinary profiles and artifacts without the addon.

Investigate the suspected early registration lock: OC currently locks inside its own common-setup queue before dependent mods enqueue registration. Upstream locks in postInit. Reproduce before adjusting lifecycle; if confirmed, keep the registry open through setup/IMC and close at load completion. No permanently unlocked registry or reflection workaround. Validate addon loading, callbacks/invalid state, true restart and default build/tests; commit/push with exact evidence. Real client loading remains pending user start on screen3; no focus/mouse action. This is not every public API contract or universal old-binary compatibility.

Completed this scoped server/restart fixture; see [[../03 manuals/API addon runtime]] for confirmed defects, exact proof and still-open full API contracts.

## Follow-up: nullable driver data - 2026-10-20

Upstream Java DriverItem explicitly permits dataTag returning null to select the default oc:data child; common/inventory/ComponentInventory.scala enforces that fallback. Audit every generic DriverItem dataTag caller, not only the newly fixed case path. Add one shared resolver preserving a non-null custom child unchanged, apply it to item writeback and relevant machine/adapter/robot/rack/RAID/charger/boot routes, and document the public nullable contract. Keep unrelated concrete item metadata lookups unchanged.

Extend the standalone addon with an explicitly flagged nullable-tag card. Reproduce the current exception before implementation, then exercise actual native component reload and a distinct-JVM nullable-tag continuation; retain the normal driver and LuaJ checks. Verify fallback writes survive real ItemStack serialization and preserve sibling custom data. Run build/units and relevant optional/default server profiles, archive failure/green logs, commit/push. Cleared save tags, Lifecycle/error isolation/node-less and other host contracts remain the next separate audit; do not close full API parity from this fix.

Nullable resolution/writeback and the scoped case/item/two-process fixture are complete; see [[../03 manuals/API addon runtime#Nullable driver data checkpoint - 2026-10-20]]. Remaining cleared-tag/Lifecycle/error/node-less and specific-host semantics stay open.

## Follow-up: managed component lifetime - 2026-10-20

Upstream ComponentInventory and Java Lifecycle specify Constructing before load, Initializing before node connection, Initialized before updates, Disposing before disconnect and Disposed before releasing the instance. Node-less ManagedEnvironments must still load/save/update. The port has the interface but no runtime lifecycle calls and rejects node-less machine/adapter components. Reproduce with the separately compiled addon, recording real factory-created instance histories; verify case native reload/removal, stopped node-less update/item roundtrip and rack bus-remap versus real module removal.

Use one small internal state-notification helper. Keep component lifetime separate from network membership: do not dispose on arbitrary machine-node disconnects, since rack bus changes preserve execution. Explicit host-removal notifications must occur after the existing continuation snapshot and before releasing hardware; repeated unload/setRemoved must not duplicate child disposal. Preserve pre-disposal snapshots and existing native handle semantics. Apply creation/removal phases to the shared machine core and generic adapter/robot-runtime/rack-mountable routes, including node-less updating items where supported. Only claim tested host scenarios; client-side addon/render lifetimes, provider exception isolation and cleared-tag semantics remain separate gates.

Validate the actual addon regressions, existing ordinary/core/native suites, optional mixed/absence loading and a true-JVM host matrix before committing/pushing. No client or OS focus work is needed until the next combined test-JAR update.

Scoped server lifetime and node-less addon acceptance completed; see [[../03 manuals/API addon runtime#Managed component lifetime checkpoint - 2026-10-21]]. Other-owner/client/exception/cleared-tag and mutable-slot unload audits remain open.

## Follow-up: mutable robot-slot unload - 2026-10-21

Audit the related runtime-slot generator path after the hardware lifecycle fix. Existing robot teardown calls item-removal detachRuntimeComponent even for chunk unload, after snapshotting the still-installed item. Reproduce with real generator fuel in an expansion-container slot: unload/setRemoved must not drop fuel, a natural host snapshot must reload exactly that queue, and actually removing/reinserting the restored generator must drop fuel once without stale serialized fuel. Keep actual item removal's disconnect-before-save semantics and exact component lifecycle notifications. Add a focused regression before runtime changes; preserve native machine continuation snapshots. Test default server/build and the separate addon runtime-slot lifetime where relevant. Broader cleared-tag/exception/charger/client contracts remain separate.

Scoped mutable-slot unload/removal fuel and node-less addon acceptance completed; see [[../03 manuals/API addon runtime#Mutable robot-slot fuel checkpoint - 2026-10-21]]. General cleared-tag and other remaining API/full-port gates stay open.

## Follow-up: cleared item save tags and built-in ownership - 2026-10-21

Source audit confirms upstream ComponentInventory clears the driver-owned tag before environment.save and isolates component load/save exceptions. The port still copies old machine driver data, while robot runtime slots save into the old tag; adapter item save already starts fresh. Before removing the explicit shortcut, reproduce obsolete-field retention with the separately compiled addon (normal/null/custom child, sibling preservation) and test fresh-output serialization for built-ins. Specific source hazards: EepromEnvironment.save currently returns only node data while captured EEPROM contents are written via a side effect; CpuItem's passive environment does not serialize its oc:archClass/oc:archName selection; ApuEnvironment/GraphicsCardEnvironment GPU saves and eager GPU persistence omit those processor keys. The APU eager-save behavior may already lose its selected architecture; prove it before changing it.

Make each affected built-in serialize its owned state completely; do not retain arbitrary old addon fields to hide missing built-in serializers. Preserve CPU/APU architecture, EEPROM code/data/label/readonly/node, native disk binding/lock/raw mode, GPU state and sibling item metadata through a fresh-tag write. Audit every generic item owner (shared machine, mutable robot, adapter and charger), distinguish block/rack data contracts, and use the existing detached writer for publication. Add red regressions before runtime edits, then relevant unit/core/addon/native restart and optional/absence gates. Treat load/save exception isolation as the next explicit contract with a throwing addon fixture, preserving other component/world saves; do not broaden catches blindly around unrelated lifecycle callbacks or fatal VM errors. Client-only lifetimes, robot fuel UI/restart acceptance and broader full-port gates remain open. Real client is now usable autonomously under the temporary overnight authorization.

Fresh-output EEPROM/CPU/APU prerequisites and the independently reproduced eager APU GPU-disconnect architecture loss are repaired and default-tested; see [[../03 manuals/API addon runtime#Built-in fresh item-save checkpoint - 2026-10-21]]. Next remove the generic merge shortcut only after actual normal/null/custom-child addon regressions, then audit all generic item owners and isolate load/save errors.

## Strict addon item-save checkpoint - 2026-10-21

Actual addon default/null/custom-child saves across case/robot/adapter/charger reproduce9 failures; shared fresh-tag publication and charger load fix them. Fixed879 addon tests, native896 tests in each distinct-JVM phase, mixed974 and absent859;2240 units, all terminal exit0. Native Lua5.3/5.4 on five hosts preserve addon state/cycles/open binary handles. Full details and remaining gates: [[../03 manuals/API addon runtime]]. Client still782fddfd7; newest repair server-tested only. Beta3-6 concentrated workweeks, low confidence.

## Addon item persistence error isolation - 2026-10-21

Actual addon ordinary load/save exceptions and missing-class linkage failures isolated across four item owners (16 cases); last good save state retained. Preserve fatal JVM/thread and durable/async failures: overbroad catch broke native save/unload recovery, now repaired. Default895 server tests,2241 units in separate green build after fixing registry-dependent unit fixture; mixed990/absent859 with2241 units and terminal exit0. See [[../03 manuals/API addon runtime]] for precise red/intermediate evidence and boundaries. Installed/client-tested runtime still7f3e8b89b; latest repair server-only. Next charger lifecycle/update/transfer and wider functional gates. Beta3-6 concentrated workweeks, low confidence.

## Charger addon lifecycle and persistence - 2026-10-21

Charger actual addon node/no-node lifetime, update-without-power, loaded-server-world factory timing, transfer versus live unload snapshot, stable node identity, idempotent teardown and actual block-break/drop state verified. Initial lifecycle red2; first897 green; stronger actual break red2; final2241 units/918 tests per distinct-JVM phase, mixed994/absent859 all terminal exit0. Native Lua5.3/5.4 cases call restored external charger addon through the retained proxy. Detailed boundaries/evidence: [[../03 manuals/API addon runtime]]. Latest source server-only; installed/client-tested runtime still7f3e8b89b. Real built-in charger filesystem/open-handle JVM acceptance and wider port gates remain open. Beta3-6 concentrated workweeks, low confidence.

## Charger GUI shift-click persistence - 2026-10-21

Actual charger GUI quick-move node/no-node stale-copy bug reproduced (902 tests/2 failures; full-inventory positive control). Menu removes through the owned container before delivery and restores failed/partial insertion. Fixed902 required default tests/2241 units terminal exit0. Previous918/918 native, mixed994/absent859 gates belong to charger lifetime sourceeadbd22f9; no repeated matrix claimed for this menu-only change. See [[../03 manuals/API addon runtime]]. Combined real-client update/GUI acceptance next; wider port gates open. Beta3-6 concentrated workweeks, low confidence.

## Charger GUI and native continuation actual client acceptance - 2026-10-21

Installed/client-tested runtimea9166393b, core9ad818a58c5366a69925eab2a9b9a31282e4cb6efa3a900598b66559e0f1635e. Actual ChargerScreen QUICK_MOVE returns one post-disposal tablet732, reinsertion preserves node/Initialized732; native Lua5.4 sets733, graceful GUI restart40140->34444 retains733/identity/local cycle, normal shutdown/no lastError. Fresh saved item733, cleanup five blocks0/entitiesempty/no owned inventory item. See [[../03 manuals/API addon runtime]]. Live clientPID34444/no screen; overnight temporary authority active until user return, then prior coordinated screen3 rules. Wider port gates remain; beta3-6 concentrated workweeks, low confidence.

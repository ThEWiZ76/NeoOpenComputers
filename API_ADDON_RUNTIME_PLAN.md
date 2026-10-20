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

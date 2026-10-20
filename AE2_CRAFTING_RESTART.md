# AE2 crafting restart and offline completion

Runtime checkpoint: 2b4e1b752ddeda019c877841b135e82325022f8a. Related: [[AE2 crafting integration]], [[AE2 controller integration]], [[../02 plans/AE2 integration]], [[../02 plans/Complete portering]]. Pinned AE2 19.2.18/MC1.21.1/NeoForge21.1.234.

## Reproduced faults and correction

The first active-request restart passed unchanged production code: two actual JVMs retained the original AE2 CPU craftId, native Lua locals, craftable and status objects, completed/failed statuses, and exact final storage counts.

The stronger offline-completion pair reproduced a missing durable outcome: save a running OC computer, execute its BE unload callback and remove its world block, let the original AE2 job complete, stop the process normally, then restore the older pre-completion computer snapshot in a fresh JVM. AE2 no longer retains the completed CPU job/link, while that old Lua heap only knows a pending requester link. The restored status never reports successful completion. Red pair: oc-restart-ae2-crafting-20261020-02, prepare PID51708 and verify PID53736; only offline_verify failed. Logs ae2-offline-red-prepare.log and ae2-offline-red-verify.log.

CraftingJournal now stores world-owned request state in overworld data/neoopencomputers_ae2_requests.dat. Active entries retain target/key/settings/node/link state; terminal entries retain only UUID and done/canceled/error outcome. Entries are copied on record/read. No time-based expiry silently discards outcomes: an arbitrarily old unloaded heap may still reference one. Small terminal history therefore grows with completed request IDs; no automatic pruning policy is claimed.

Server startup restores active requesters independently of the OC host's heap. Value load reuses the live canonical request or overlays the world's newer progress/outcome onto an older heap's locator/settings. World outcomes also cover failed/canceled/completed state without resurrecting terminal jobs. Normal per-tick changes and cancellation/completion callbacks update SavedData.

The first journal implementation fixed old completed snapshots but reproduced a startup timing regression for an active job. The saved request's retry backoff could leave it unbound when the initialized CPU received its physical result. Debug lifecycle trace showed the late bind and later orphan-link cancellation. Source inspection confirms AE2 initializes nodes on level-end and processes crafting on server-end. Restored links now get a loaded-target rebind opportunity at lowest-priority LevelTickEvent.Post, after AE2 node initialization and before the next world tick can deliver output. The existing delayed retry path remains for unavailable targets. No target chunk is force-loaded by these lookups.

Diagnostic logs ae2-journal-trace-prepare-debug.log/verify-debug.log preserve request/CPU link IDs and bind/change events. Subsequent two-case pair oc-restart-ae2-crafting-20261020-05 passed735 tests in each process. Final verification below adds a host that stays absent across startup.

## Final actual-process acceptance

World oc-restart-ae2-crafting-20261020-06; prepare PID53528, verify PID54476; normal process exit/save both times. AE2+ProjectRed+CC,736 required GameTests in each process, zero failures. Build and2141 unit tests pass; a focused SavedData unit checks copy isolation, serialized terminal outcomes and compact history. The ordinary native crafting fixture continues to exercise real assembler output, named/missing CPU, missing ingredients, cancellation and same-process aliasing.

Three separate real controller/drive/cell/creative-energy/named1k-CPU/pattern-provider/assembler/furnace setups:

1. Active: native Lua first requests4planks, retains its completed status and a missing-CPU failure, then submits a1charcoal processing request. Save while that job waits for an actual furnace output. After restart, assert original CPU craftId unchanged, restore Lua local731 and userdata alias identity, move the actual physical charcoal from furnace output into ME storage, and observe the same job complete.
2. Completed with host absent: save a pre-completion computer snapshot into the test record, run BE unload, remove the host, and complete the furnace/CPU job in prepare. In a different JVM restore that older computer snapshot and observe the journal's true completed state, without another extraction/submission.
3. Still pending with host absent: save/remove the computer but leave the CPU pending at prepare shutdown. In verify keep the world position empty for100ticks, longer than AE2's orphan-link allowance, and assert the original CPU craftId still exists uncanceled. Deliver the physical furnace output and let the job finish with no OC computer present. Only then restore the old heap; it sees successful completion.

Every setup ends with exactly6oak logs,4oak planks and1charcoal in ME storage; furnace input/output empty. This accounts for8original logs converted by real assembler/furnace operations. Furnace fuel/burn time is not asserted against a changing value. Output transfer simulates capacity first, removes one real furnace item, then inserts exactly that item into the real ME inventory.

Important scope: the offline variants execute the actual computer BE unload/stop and remove the host while preserving the AE2 network. They do not prove chunk-ticket eviction or every true chunk-unload ordering. Older VM snapshots are test-fixture records, not a new gameplay item/export format.

Logs under build/finish-port-implementation: ae2-journal-final-prepare.log, ae2-journal-final-verify.log, corresponding debug/build logs; prepared journal copy ae2-journal-final-prepared.dat. Minimal profile with no optional mods passes726 tests, proving the new AE2-specific requester/journal classes are not eagerly loaded. Artifact/POM verifier still finds no bundled appeng classes or hard published AE2 dependency.

## Run

Use a fresh, isolated world name for each prepare/verify pair, and wait for each process to exit before starting another:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithAE2 -PwithProjectRed -PwithComputerCraft "-Pneoopencomputers.restartPhase=prepare" "-Pneoopencomputers.restartWorld=oc-restart-ae2-example" --no-daemon --console=plain
exit $LASTEXITCODE
```

After exit0, preserve latest.log, then run the same command with phase=verify and the same world name. The existing four non-AE2 restart probes also run in this profile. Do not reuse a partially verified world as a new prepare fixture.

## Remaining acceptance

Save/restart during an unfinished calculation (rather than an already submitted link), busy/offline/too-small CPU failure matrix, missing/replaced target and all real chunk-unload/side/dimension orders, long-load cleanup/history growth, crash/power-loss atomicity and multiplayer/protection/client acceptance remain open. The journal proves the documented normal-shutdown cases; it is not a distributed transaction across arbitrarily interrupted CPU/chunk/data writes. Completions that were already lost before a journal existed cannot be reconstructed by guessing from item counts.

Interfaces/import-export buses, cell converters, item EnvironmentProviders and tool/spatial hooks remain implementation work. No client launch, Computer Use, Modrinth install or screen movement occurred. Real ingame/visual acceptance must use screen3 and user desktop coordination. Whole-port beta estimate remains3-6 concentrated workweeks, low confidence, including full implementation, bugfixes and real client acceptance.

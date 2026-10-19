# Loaded-chunk discovery

Related: [[../Project]], [[../02 plans/Complete portering]], [[Network addresses]], [[Process restart test]], [[../04 ai-context/Port gap triage]].

## Investigation

The failed verify process in oc-restart-address-20261020-01 spun in ChunkMap.processUnloads while scheduleUnload kept requeuing work. That original hang is retained in the address/restart manuals. Three deterministic NetworkRegistry tests then reproduced independent unsafe accesses: querying an unloaded origin, querying an unloaded adjacent chunk, and calling a sided host's canConnect before checking that its neighbor was loaded. The guarded test world rejects those accesses while requiring a loaded neighbor to connect. Red evidence: build/finish-port-implementation/network-unloaded-lookups-red.xml.

Real-server boundary tests exposed more than the central lookup. The cable/screen fixture loads one isolated chunk, places actual blocks at its east edge, invokes their lifecycle/discovery paths and requires the adjacent chunk to remain unloaded. It initially failed; finer checkpoints located the first load when a second cable changed the first cable's connection properties. Those derived state updates used UPDATE_CLIENTS, which still lets vanilla propagate shape updates across the chunk boundary. The adapter fixture separately reproduced adjacent-chunk loading during discovery. Logs: network-boundary-screen-red.log and network-boundary-cable-adapter-red.log. The failed prepare world oc-restart-unloaded-20261020-01 is diagnostic only, even though its native checkpoint prepared successfully.

## Changes

- NetworkRegistry checks LevelReader chunk availability before reading origin/neighbor block entities and before calling sided connectivity callbacks. A plain BlockGetter retains its existing read semantics. Each position is read once per lookup.
- Cable side/color checks and screen side/keyboard checks skip unloaded neighbors.
- Cable connection properties are a derived geometry cache. Their update uses UPDATE_CLIENTS plus UPDATE_KNOWN_SHAPE; surrounding cable states are already explicitly refreshed. This avoids an unnecessary vanilla shape-propagation scan while preserving client updates and explicit connection geometry refresh.
- Screen layout traversal, connected-screen client notifications, keyboard scans and placement inheritance skip unloaded positions. Redstone input samples loaded first-hop neighbors rather than calling an unguarded whole-neighbor scan.
- Adapter discovery removes/defers environments on unloaded sides. Derived waiting flags are retried by the ordinary adapter tick once the neighbor is loaded; active environments are detached before updating against an unloaded target. Closed sides are not automatically reopened. The boundary test explicitly loads the target chunk, places a chest and requires automatic discovery on the next adapter tick.

The real boundary tests clean up their isolated block fixtures through the loaded chunks. They preserve the original test scope: normal network connectivity must still work, and the later-loaded adapter neighbor must be usable. This is not an exception that simply disables discovery at chunk edges.

## Acceptance

Runtime checkpoint 9a1df219b10f0a58dd60e29988dd57ca8bea3d1a passes the integrated build and 2125 unit tests. A fresh two-process pair in oc-restart-unloaded-20261020-02 passed all 702 required GameTests in both prepare PID9200 and verify PID56044, including the real boundary tests, a native running computer/open binary handle continuation and player/tablet persistence. Logs: network-unloaded-restart-prepare.log and network-unloaded-restart-verify.log under build/finish-port-implementation/. This proves the documented pair passed after the changes; it is not a claim about every unload order.

The adapter recovery fixture initially expected an inventory component with the inventory driver disabled by default. It now explicitly enables that driver only for its synchronous test and restores the prior setting in finally. Evidence of the fixture failure is retained in network-boundary-adapter-driver-fixture.log; no production default changed.

The earlier unit/ordinary-server success alone did not establish that the original unload hang was fixed. The original failed logs/dumps are retained. The original oc-restart-address-20261020-01 world was also re-verified using its still-prepared checkpoint from PID37536, without overwriting it with another prepare. Verify PID15444 passed all 702 tests and shut down normally (network-unloaded-original-world-verified.log). That exact previously hanging world now passes the retained Lua/open-file/tablet probe, rather than only a fresh replacement scenario. No client or Computer Use action occurred.

Remaining scope includes visual/client behavior, third-party drivers whose internal queries may load chunks, arbitrary unload order/load pressure and existing legacy-address/data migration. First-hop guards do not prove every third-party block's redstone implementation is non-loading.

## How to run/use this

From M:/development/OpenComputers:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer --no-daemon --console=plain
exit $LASTEXITCODE
```

Then run the fresh-world prepare/verify pair in [[Process restart test]], waiting for the first Gradle process to exit before starting the second. Keep dotted Gradle property arguments quoted in PowerShell.

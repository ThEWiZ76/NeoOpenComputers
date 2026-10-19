# Process restart regression

Latest interaction checkpoint: Runtime bc17524aeb24e2c1bebcbab3f3579661754a671d: build and 2131 unit tests pass; all 718 ordinary GameTests pass with ProjectRed and ComputerCraft together. Fresh world oc-restart-microcontroller-immutable-20261020-01 passes all 721 tests in prepare PID16144 and verify PID44436, both with normal shutdown. Logs: microcontroller-immutable-verified.log and microcontroller-immutable-restart-{prepare,verify}.log under build/finish-port-implementation/. This is tested server behavior, not complete port or client acceptance.

## Latest acceptance - 2026-10-20

Microcontroller rotation/restart checkpoint 156db2b6b093fea856337e6dd69b340bf196452c passes build, 2131 units and 708 ordinary GameTests. With -PwithComputerCraft, fresh world oc-restart-microcontroller-20261020-01 passes all 711 required GameTests in prepare PID58500 and verify PID56588, both normal shutdowns. The pair includes the new native microcontroller/private-network/control/modem/tmp continuation probe, the original computer/external-floppy/tablet probe and the four-host CC redstone disk-history probe. Logs: microcontroller-process-restart-prepare.log and microcontroller-process-restart-verify.log under build/finish-port-implementation/. Verify explicitly records pre-tick disk energy/flags and redstone input arrays. See [[Microcontroller networking]] for the separate physical rotation proof and remaining scope.

Connected-node address restoration checkpoint c125614e99c802ab724818be2548433b2bef25ed passes build, 2129 units and 701 ordinary GameTests. Fresh world oc-restart-remap-20261020-01 passes all 702 required GameTests in prepare PID34520 and verify PID48236, both normal shutdowns. Logs: network-connected-remap-restart-prepare.log and network-connected-remap-restart-verify.log under build/finish-port-implementation/. The native continuation/open-floppy/tablet probe remains intact. See [[Network addresses]] for the reproduced routing-map defect and chunkloader lifecycle interaction. This pair is not evidence for all host/addon remap, crash-recovery or client scenarios.

Non-loading discovery checkpoint 9a1df219b10f0a58dd60e29988dd57ca8bea3d1a passes build and 2125 units. Fresh world oc-restart-unloaded-20261020-02 passes all 702 required GameTests in prepare PID9200 and verify PID56044, both normal shutdowns. Logs: network-unloaded-restart-prepare.log and network-unloaded-restart-verify.log under build/finish-port-implementation/. The original oc-restart-address-20261020-01 checkpoint, prepared by PID37536, also verifies after the fix in PID15444 with all 702 tests passing (network-unloaded-original-world-verified.log). It was not overwritten/re-prepared to avoid the failure. See [[Loaded chunk discovery]] for reproduced causes, fixture details and limits.

Historical failure: address-generation checkpoint df9151c8ca0b95365262220aeeba0abb2ec0d4da passed build, 2122 units and 699 ordinary GameTests. In oc-restart-address-20261020-01, prepare PID37536 passed 700 required tests; verify PID59180 hung in ChunkMap.processUnloads before the probe and was terminated after preserving two dumps/logs. Those diagnostics remain valid failure evidence for that revision. The newer successful verification does not rewrite that failed outcome. Failed prepare world oc-restart-unloaded-20261020-01 is diagnostic only (a boundary regression was still failing). Older successful pairs below remain evidence for their documented revisions.

Related: [[../04 ai-context/Native Lua persistence]], [[../04 ai-context/Tablet runtime]], [[../02 plans/Complete portering]].

Run from M:\development\OpenComputers in PowerShell. Use a fresh world name beginning with oc-restart- for each pair. Keep the same name for prepare and verify. Quote each Gradle property argument because PowerShell otherwise splits the dotted property name.

```powershell
rtk proxy cmd /d /c gradlew.bat runGameTestServer '-Pneoopencomputers.restartPhase=prepare' '-Pneoopencomputers.restartWorld=oc-restart-example-01' --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat runGameTestServer '-Pneoopencomputers.restartPhase=verify' '-Pneoopencomputers.restartWorld=oc-restart-example-01' --no-daemon --console=plain
```

Wait for the first command to exit successfully before running the second. Each launches a distinct GameTestServer JVM and exits through the normal server shutdown/save path. The world is retained at run/gameTestServer/oc-restart-example-01 for inspection; the probe refuses to prepare over its existing checkpoint and refuses to verify a checkpoint already marked verified. No recursive deletion or world overwrite is part of the commands.

The ordinary build/run invocation does not register restart probes. Enabling the phase adds the native computer/tablet test and the microcontroller test below; when CC is installed, it also adds the redstone restart test in the same processRestart batch. The native computer probe uses a forced chunk at absolute coordinates 6,80,6, an external writable floppy drive, a chest record with the preparation PID, and a dedicated fixed-UUID test player. The world name is isolated through the --world server argument; the usual development world is not selected.

Prepare boots native Lua code that keeps local value731 and an open file positioned after the first byte of a binary payload on the external floppy. It also runs a carried tablet that changes its EEPROM data. The player remains connected until normal shutdown saves/disconnects players. The probe supplies controlled test power from the first world tick, including before its verification batch, because forced chunks already execute while other tests run.

Verify requires a prepared record from another process, loads the existing player file through PlayerList.placeNewPlayer, checks the tablet's newest EEPROM data and stopped state, and signals the restored computer. Lua verifies retained locals, external filesystem identity and the remaining binary bytes from the same open handle. No manual NBT reconstruction or in-memory snapshot transfer substitutes for chunk/player loading.

Verified 2026-09-22 with world oc-restart-20260922-1858: prepare PID 67064, verify PID 64320, both 546 required GameTests passed. Logs: build/finish-port-implementation/process-restart-powered-prepare.log and process-restart-powered-verify.log; the verify build and 2108 unit tests also passed. The ordinary invocation then passed build and all 545 GameTests in process-restart-default-suite.log, confirming the probe is absent without its options. The earlier oc-restart-20260922-1852 attempt failed with NoEnergy because test power was registered only when the verification batch began; it is retained as diagnostic evidence.

Scope: actual JVM termination, chunk persistence and player-file persistence for this native computer/external floppy/tablet setup. GameTestServer recreates its test level settings at startup, so this is not proof of arbitrary production-world settings, all host types, crash recovery, cross-version migration or visual client behavior. Those remain separate acceptance work.

## Microcontroller restart acceptance

Since runtime 156db2b6b093fea856337e6dd69b340bf196452c, MicrocontrollerProcessRestartGameTests adds its own opt-in prepare/verify test in the same processRestart batch. It places a native tier-one microcontroller at 4,80,12, real cables east/west and a separate chest record at 4,80,14 in the forced chunk. These coordinates do not overlap the existing computer/tablet or redstone fixtures.

Prepare boots an installed native CPU/RAM/EEPROM/modem program, closes the west output through its internal microcontroller proxy, opens modem port731, retains a Lua table value731 and pauses after reading one byte from a binary tmp file. The record stores the preparing PID and machine/five external plug/five control IDs. Normal shutdown writes the actual chunk state.

Verify requires a prepared record from another process and uses the naturally loaded world entities. A ChunkDataEvent.Load listener copies the microcontroller's raw saved NBT before ticking, without querying/loading any world from the asynchronous callback. It confirms positive stored energy on oc:snooper, zero capacity usage on the machine node and closed-west/open-east flags on disk. Power is provided from the first world tick so other batches cannot exhaust the fixture before its verification runs; pre-tick disk evidence prevents this from masking missing saved buffer data. Restored external/private networks must stay isolated and all recorded IDs must match.

The same running Lua continuation then verifies the retained local, internal microcontroller proxy/flags, modem proxy/open port and remaining binary bytes from its open tmp handle. It marks the EEPROM verified without rebooting. A world already marked prepared or verified must not be reused for a fresh pair. See [[Microcontroller networking]] for rotation/network evidence and remaining client, platform, custom-config and migration limits.

## Redstone restart acceptance

Add -PwithComputerCraft to both commands to include the optional CC redstone fixture. Use a new world name for every prepare/verify pair:

~~~powershell
rtk proxy cmd /d /c gradlew.bat runGameTestServer -PwithComputerCraft "-Pneoopencomputers.restartPhase=prepare" "-Pneoopencomputers.restartWorld=oc-restart-redstone-example-01" --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat runGameTestServer -PwithComputerCraft "-Pneoopencomputers.restartPhase=verify" "-Pneoopencomputers.restartWorld=oc-restart-redstone-example-01" --no-daemon --console=plain
~~~

The redstone fixture shares chunk 0,0 and the native probe batch. It places a case, microcontroller, robot and redstone IO at 10/14,80,10/14, with a separate chest record at 10,80,6. Four real CC computers emit analogue strength 15 and bundled mask 32773 downward. The fixture uses CC setAnalogOutput for numeric strengths; CC setOutput takes a boolean (official API: https://tweaked.cc/module/redstone.html). The shared CC boot helper retains these source computers for the restart instead of scheduling its ordinary temporary-source cleanup.

Prepare waits for real vanilla and bundled inputs, configures west output 9, east bundled color 7 strength 42 and wake threshold 8, then saves host-node addresses and the preparing PID in its record. The world and CC startup files remain for inspection. A 4000-tick bound allows four asynchronous CC boots on GameTestServer, whose waitUntilNextTick has no ordinary real-time tick pause.

Verify requires a prepared record from another PID. A ChunkDataEvent.Load listener copies matching saved block-entity NBT while the chunk is decoded, before ticking. It performs no world lookup or chunk load on that asynchronous callback. All four saved input arrays must already contain the expected samples; fresh measurements cannot hide missing disk state. The normal loaded hosts must retain their node addresses, outputs and threshold. Once the actual CC sources are ready, the restored IO network must produce no duplicate event for unchanged input, then exactly three bundled falling events plus one vanilla falling event when its source is removed, with no repeated events on another sample.

Verified 2026-10-19 with oc-restart-redstone-20261019-04: prepare PID 46908, verify PID 21220; both passed all 664 required tests, including the existing native-Lua/open-floppy/tablet continuation test. Logs are build/finish-port-implementation/redstone-process-restart-prepare.log and redstone-process-restart-verify.log. The verify log explicitly records inspection of disk input arrays for all four hosts before ticking.

Worlds ending 01-03 and their failed-prepare logs are diagnostic artifacts, not valid restart evidence. Their CC source program passed a numeric value to the boolean setOutput call. Chunk-ticking and timing hypotheses did not fix that program; the corrected analogue call produced the accepted pair. No diagnostic world was deleted or reused.

Scope: orderly process termination and actual chunk loading for redstone history/output/threshold/node identity on these four hosts, plus real network event delivery from IO. This does not prove crash recovery, all running-host Lua continuations, cross-version migration, multiplayer or visual client acceptance.

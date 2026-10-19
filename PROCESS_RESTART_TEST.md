# Process restart regression

Related: [[../04 ai-context/Native Lua persistence]], [[../04 ai-context/Tablet runtime]], [[../02 plans/Complete portering]].

Run from M:\development\OpenComputers in PowerShell. Use a fresh world name beginning with oc-restart- for each pair. Keep the same name for prepare and verify. Quote each Gradle property argument because PowerShell otherwise splits the dotted property name.

```powershell
rtk proxy cmd /d /c gradlew.bat runGameTestServer '-Pneoopencomputers.restartPhase=prepare' '-Pneoopencomputers.restartWorld=oc-restart-example-01' --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat runGameTestServer '-Pneoopencomputers.restartPhase=verify' '-Pneoopencomputers.restartWorld=oc-restart-example-01' --no-daemon --console=plain
```

Wait for the first command to exit successfully before running the second. Each launches a distinct GameTestServer JVM and exits through the normal server shutdown/save path. The world is retained at run/gameTestServer/oc-restart-example-01 for inspection; the probe refuses to prepare over its existing checkpoint and refuses to verify a checkpoint already marked verified. No recursive deletion or world overwrite is part of the commands.

The ordinary build/run invocation does not register restart probes. Enabling the phase adds the native restart test; when CC is installed, it also adds the redstone restart test in the same processRestart batch. The native probe uses a forced chunk at absolute coordinates 6,80,6, an external writable floppy drive, a chest record with the preparation PID, and a dedicated fixed-UUID test player. The world name is isolated through the --world server argument; the usual development world is not selected.

Prepare boots native Lua code that keeps local value731 and an open file positioned after the first byte of a binary payload on the external floppy. It also runs a carried tablet that changes its EEPROM data. The player remains connected until normal shutdown saves/disconnects players. The probe supplies controlled test power from the first world tick, including before its verification batch, because forced chunks already execute while other tests run.

Verify requires a prepared record from another process, loads the existing player file through PlayerList.placeNewPlayer, checks the tablet's newest EEPROM data and stopped state, and signals the restored computer. Lua verifies retained locals, external filesystem identity and the remaining binary bytes from the same open handle. No manual NBT reconstruction or in-memory snapshot transfer substitutes for chunk/player loading.

Verified 2026-09-22 with world oc-restart-20260922-1858: prepare PID 67064, verify PID 64320, both 546 required GameTests passed. Logs: build/finish-port-implementation/process-restart-powered-prepare.log and process-restart-powered-verify.log; the verify build and 2108 unit tests also passed. The ordinary invocation then passed build and all 545 GameTests in process-restart-default-suite.log, confirming the probe is absent without its options. The earlier oc-restart-20260922-1852 attempt failed with NoEnergy because test power was registered only when the verification batch began; it is retained as diagnostic evidence.

Scope: actual JVM termination, chunk persistence and player-file persistence for this native computer/external floppy/tablet setup. GameTestServer recreates its test level settings at startup, so this is not proof of arbitrary production-world settings, all host types, crash recovery, cross-version migration or visual client behavior. Those remain separate acceptance work.

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

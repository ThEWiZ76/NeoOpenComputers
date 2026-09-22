# Process restart regression

Related: [[../04 ai-context/Native Lua persistence]], [[../04 ai-context/Tablet runtime]], [[../02 plans/Complete portering]].

Run from M:\development\OpenComputers in PowerShell. Use a fresh world name beginning with oc-restart- for each pair. Keep the same name for prepare and verify. Quote each Gradle property argument because PowerShell otherwise splits the dotted property name.

```powershell
.\gradlew.bat runGameTestServer '-Pneoopencomputers.restartPhase=prepare' '-Pneoopencomputers.restartWorld=oc-restart-example-01'
.\gradlew.bat runGameTestServer '-Pneoopencomputers.restartPhase=verify' '-Pneoopencomputers.restartWorld=oc-restart-example-01'
```

Wait for the first command to exit successfully before running the second. Each launches a distinct GameTestServer JVM and exits through the normal server shutdown/save path. The world is retained at run/gameTestServer/oc-restart-example-01 for inspection; the probe refuses to prepare over its existing checkpoint and refuses to verify a checkpoint already marked verified. No recursive deletion or world overwrite is part of the commands.

The ordinary `./gradlew.bat build runGameTestServer` invocation does not register the restart probe. Enabling the probe adds one required test to the existing suite. The probe uses a forced chunk at absolute coordinates 6,80,6, an external writable floppy drive, a chest record with the preparation PID, and a dedicated fixed-UUID test player. The world name is isolated through the --world server argument; the usual development world is not selected.

Prepare boots native Lua code that keeps local value731 and an open file positioned after the first byte of a binary payload on the external floppy. It also runs a carried tablet that changes its EEPROM data. The player remains connected until normal shutdown saves/disconnects players. The probe supplies controlled test power from the first world tick, including before its verification batch, because forced chunks already execute while other tests run.

Verify requires a prepared record from another process, loads the existing player file through PlayerList.placeNewPlayer, checks the tablet's newest EEPROM data and stopped state, and signals the restored computer. Lua verifies retained locals, external filesystem identity and the remaining binary bytes from the same open handle. No manual NBT reconstruction or in-memory snapshot transfer substitutes for chunk/player loading.

Verified 2026-09-22 with world oc-restart-20260922-1858: prepare PID 67064, verify PID 64320, both 546 required GameTests passed. Logs: build/finish-port-implementation/process-restart-powered-prepare.log and process-restart-powered-verify.log; the verify build and 2108 unit tests also passed. The ordinary invocation then passed build and all 545 GameTests in process-restart-default-suite.log, confirming the probe is absent without its options. The earlier oc-restart-20260922-1852 attempt failed with NoEnergy because test power was registered only when the verification batch began; it is retained as diagnostic evidence.

Scope: actual JVM termination, chunk persistence and player-file persistence for this native computer/external floppy/tablet setup. GameTestServer recreates its test level settings at startup, so this is not proof of arbitrary production-world settings, all host types, crash recovery, cross-version migration or visual client behavior. Those remain separate acceptance work.

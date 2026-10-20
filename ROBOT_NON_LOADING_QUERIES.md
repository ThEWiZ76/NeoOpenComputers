# Robot non-loading queries

Runtime checkpoint `ddc7515421376e939a2984387e76e082ff82cafa`. Related: [[../Project]], [[../02 plans/Complete portering]], [[Loaded chunk discovery]], [[ComputerCraft relay integration]], [[../04 ai-context/Port gap triage]].

## Cause observed

The earlier CC-only fresh-world shutdown completed732 tests and spun in ChunkMap.scheduleUnload/processUnloads (ownedPID56496; two preserved dumps). A successful retry was not a fix. The earlier reused-world freezes likewise remained historical evidence.

A read-only, loopback JDI trace on the preceding bd2cda902 checkpoint observes36 new generation tasks DURING stopServer. Its stacks identify RobotBlockEntity.onChunkUnloaded -> clearDig(true) -> ServerLevel.sendBlockUpdated -> collision-shape lookup -> RobotBlock.getShape -> Level.getBlockEntity -> ServerChunkCache.getChunk(requireChunk=true) -> ChunkMap.scheduleGenerationTask. Canceling a swing sends a block update; a robot's shape lookup could therefore revive its own unloading chunk. This is distinct from the previously fixed network/cable/screen/adapter neighbor-discovery route.

That normal-exit trace proves the robot reload route, not the exact holder/refcount inside a stuck historical loop. ChunkMap checks readiness and may reschedule unload callbacks; the broader relationship to every historical hang is not claimed proven solely from these normal exits.

## Focused correction

RobotBlock.robotIfLoaded checks LevelReader.hasChunkAt before fetching a block entity. Selection/collision geometry, weak/strong redstone and connection queries share this lookup. An unavailable origin returns the ordinary fixed robot outline/collision, zero signal and no redstone connection. Loaded robots retain dynamic movement geometry and their real redstone hardware/output.

RobotAfterimageBlock uses the same lookup for each candidate neighbor, extending its old Level-only guard to all LevelReader views. Its loaded target still supplies translated robot geometry. Cache-only BlockGetter views keep their existing access semantics.

No Minecraft scheduler mixin, world ticket change, animation suppression or lifecycle/persistence change was introduced. The real bug was a loading query during collision calculation, so normal animation cancellation and block notifications remain intact.

## Deterministic regression and runtime evidence

Three new RobotChunkQueryGameTests first fail against the old code in735-test CC profile: robot outline query loads a missing origin; robot redstone query loads a missing origin; afterimage reads an unloaded neighbor through a LevelReader view. The first two use actual server chunks at separate fixed positions, assert the precondition, invoke the real BlockState methods and independently inspect loaded state. The reader fixture rejects unavailable lookups and also proves the loaded target's translated shape. No clock/random assertions are added.

After the correction, build/2142 unit tests and781 mixed-mod GameTests pass. A second traced run of the previously hanging oc-tests-cc-relay-alone-20261020-01 world passes735 tests with only CC installed. BOTH final traces enter stopServer, arm the same generation breakpoint and observe ZERO new generation tasks; both test and debugger JVM handles terminate normally. The no-optional-mod profile passes730 tests with ordinary non-debug launch and normal exit. Existing full-suite tests also cover loaded robot/afterimage collision alignment and real redstone directions/turn/move/reload.

Evidence under build/finish-port-implementation:

- unload-causal-trace-01-build.log / -probe.log / -latest.log / -debug.log: old code732 tests, normal exit but36 generation requests during shutdown.
- robot-query-red-build.log / -latest.log / -debug.log:735 tests, exactly3 expected failures and terminal failure.
- robot-query-mixed-traced-build.log / -probe.log / -latest.log / -debug.log:781 tests and build, world oc-tests-robot-query-mixed-20261020-01, normal exit, zero shutdown generation.
- robot-query-historical-world-build.log / -probe.log / -latest.log / -debug.log:735 tests in the preserved formerly hanging world, normal exit, zero shutdown generation.
- robot-query-absent-build.log / -latest.log / -debug.log:730 tests in oc-tests-robot-query-absent-20261020-01, normal exit.

The ignored TraceShutdownGeneration.java helper records task creation only after stopServer entry and reads fields/stacks without modifying the server. It does not capture arbitrary pre-stop generation or prove an exhaustive unload stress matrix. Tests were serialized; every Gradle/debugger handle reached terminal state and latest/debug logs were saved before another run. No stopped process or missing test summary was counted as success.

## How to run

```powershell
rtk proxy cmd /d /c "gradlew.bat build runGameTestServer -PwithComputerCraft -PwithMekanism -PwithAE2 -PwithEnderStorage -PwithProjectRed -Pneoopencomputers.gameTestWorld=oc-tests-robot-query-review-01 --no-daemon --console=plain"
exit $LASTEXITCODE
```

Use a fresh oc-tests-* world for independent runs and one Gradle at a time. The historical-world diagnostic intentionally reused the named retained world. Optional local JDWP setup is not part of the shipped mod or required test invocation.

## Remaining full-port gates

The deterministically reproduced robot query/reload defect is fixed with the evidence above. Broad unload/reload stress, other host/driver callbacks, real client/survival/multiplayer/platform acceptance and the remaining integration/semantic/resource/API work stay open. Do not declare every historical freeze fixed from a few successful shutdowns. No Minecraft client, Computer Use, Modrinth installation or window-moving helper was used. Whole-port beta estimate remains3-6 concentrated workweeks, low confidence.

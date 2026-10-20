# API addon runtime acceptance

Related: [[../02 plans/API addon runtime]], [[Process restart test]], [[../04 ai-context/Port parity matrix]].

## Confirmed fixes - 2026-10-20

A separately packaged NeoForge addon registering a driver from enqueued common setup reproduced a real startup failure: OC locked its driver registry in its own earlier setup queue. api-addon-red.log contains IllegalStateException / Mod loading has failed and no completed GameTests, despite the launcher returning BUILD SUCCESSFUL/exit0. This is failure evidence, never a passing baseline. Upstream common/Proxy.scala locks in postInit. OC now closes registration at FMLLoadCompleteEvent after setup and IMC; runtime Driver.add is still rejected by an actual addon-profile assertion.

After that fix, 857 ordinary tests passed, but the first real restart pair failed the addon continuation with no such component. SimpleMachine never loaded the generic environment from its driver's data and saved into a discarded CompoundTag; built-in stack-capturing environments masked this omission. The shared host now loads before node connection, writes environment data back through the actual driver/item and saves the old EEPROM environment before releasing its ownership map. Both ordinary native reload and real process restart exercise the external environment count/address and aliased AbstractValue state. LuaJ also runs the actual external callbacks, binary converter and userdata methods. No registry reflection/unlock or OC implementation import is used by the addon.

The fixture is tools/api-smoke-addon, compiled against the generated API JAR plus dependency classpath without OC implementation output. Its own @Mod metadata declares OC as a dependency and schedules it AFTER OC. It is a separate optional JAR, never part of main/API artifacts or a published required dependency; the installable artifact guard checks separation. Jar SHA256 1ca7ccf27629debdd8bb42d690d7afef6f2f5c577be8c31c0bda7e936183c132; default234 installable core SHA256 a65f733f68e3ee413328cc26dc5b6e2a2196901c0f649a50cb2c62ef2a936131.

## Verified evidence

Fresh oc-restart-api-addon-20261020-02: prepare PID9536 and verify PID67168, all863 required tests in each process, BUILD SUCCESSFUL and terminal exit0 after normal shutdown. Verify loads actual saved chunks in a second JVM; Lua retains the shared userdata counter42 then increments to43, and the item component retains count731/node before incrementing to732. The original failed -01 world/logs remain diagnostic evidence, backed up with every-file hashes before archival. Initial broad unit run hit eight null-placeholder fixture saves; real stacks passed857 server tests. The shared writeback now ignores absent/empty target stacks while preserving the existing unbound-environment save behavior; final units pass without weakening assertions.

Final api-addon-final-mixed-build.log passes2239 unit tests (zero failures/errors/skips) and952 required tests on loader248 with the addon plus Forestry/Butterflies/Patchouli, Railcraft/JEI322, EnderIO, TIS, CC, AE2, Mekanism, EnderStorage and ProjectRed. api-addon-final-absent-build.log passes2239 units and855 required tests on default234 without the addon/optional providers. Both BUILD SUCCESSFUL/terminal exit0, latest/debug logs archived. No simultaneous Gradle processes. Client still runs74bf7efb8; this checkpoint is not client proof of the new API fixes.

## How to run

```powershell
rtk proxy cmd /d /c gradlew.bat --no-daemon apiSmokeAddonJar
rtk proxy cmd /d /c gradlew.bat --no-daemon runGameTestServer '-PwithApiSmokeAddon' '-Pneoopencomputers.gameTestWorld=oc-tests-api-addon-example-01'
rtk proxy cmd /d /c gradlew.bat --no-daemon runGameTestServer '-PwithApiSmokeAddon' '-Pneoopencomputers.restartPhase=prepare' '-Pneoopencomputers.restartWorld=oc-restart-api-addon-example-01'
rtk proxy cmd /d /c gradlew.bat --no-daemon runGameTestServer '-PwithApiSmokeAddon' '-Pneoopencomputers.restartPhase=verify' '-Pneoopencomputers.restartWorld=oc-restart-api-addon-example-01'
```

Use fresh isolated world names, wait for normal exit0 between phases, inspect the required-test summary as well as Gradle's status. Normal developer client/server runs accept -PwithApiSmokeAddon too; it is not installed in the user's profile yet. The paper driver only accepts a paper stack with minecraft:custom_data containing oc_api_smoke:true.

## Open scope

This closes this standalone-driver/converter/userdata loading and orderly native53 case restart route, not all public APIs, host types, old binaries, crash recovery, migration or client/multiplayer/platform/load acceptance. Shared save currently starts from existing driver data to preserve built-in stack configuration. Upstream cleared-tag behavior, null dataTag fallback, lifecycle/error isolation, node-less components and external rack/adapter/portable host contracts require the remaining semantic API audit; do not call those implemented from this fixture. Beta remains3-6 concentrated workweeks with low confidence. No OS focus/mouse action or user-client update for these fixes.

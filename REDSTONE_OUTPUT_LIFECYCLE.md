# Redstone output lifecycle

## Upstream contract

The 1.12 Computer trait enables output only while its machine runs and a redstone card is installed. Robot overrides card detection so its installed-card output survives stopping. RedstoneAware and BundledRedstoneAware clear their cached outputs when enabled changes to false. Dust connectivity and ProjectRed bundled connectivity use that enabled flag. Physical weak/strong queries still read the output cache; forcing arbitrary Java output into an off host is not an upstream disabled-output test.

Sources in the local master-MC1.12 checkout: common/tileentity/traits/Computer.scala (hasRedstoneCard, markDirty), RedstoneAware.scala (setOutputEnabled), BundledRedstoneAware.scala (setOutputEnabled, canConnectBundled), common/block/RedstoneAware.scala and common/tileentity/Robot.scala.

## Port change

SimpleMachine notifies its host when starting, stopping, crashing or tearing down running hardware. Case and microcontroller refresh their output-enabled state through those dirty notifications and their server ticks. A disable transition clears all six vanilla outputs and all 96 bundled cells, marks persistent state dirty and notifies neighboring blocks. Pause retains running-card output; robot keeps its upstream installed-card rule and IO stays enabled.

NBT loading suppresses temporary hardware teardown notifications until the saved machine is restored, then derives the enabled flag without erasing loaded outputs. Disposed case/microcontroller snapshots remain protected. Rack removal saves and detaches its mountable before stopping its VM: lifecycle dirty notifications cannot serialize the stopped VM over the retained running snapshot.

Case and microcontroller update packets carry oc:redstoneOutputEnabled for client dust/ProjectRed connection checks. Detached packet-only data retains this field even before a level is attached. This is protocol implementation and server-side tag evidence, not live visual acceptance.

ProjectRed interaction connectivity now follows the host flag. Its real cable fixtures install valid hardware and start native machines on case/microcontroller endpoints rather than using inactive bare hosts. Ordinary raw output getters retain upstream cache semantics.

## Acceptance

Eight RedstoneEnableGameTests cover case/microcontroller stop, crash, card removal and absent-card dust connectivity. Six lifecycle fixtures boot actual native Lua EEPROMs with installed CPU, RAM and advanced cards. The Lua program drives physical vanilla output and bundled output when a provider is available. Tests check enabled server/client tags and connectivity, retain outputs across real save/load reinitialization, preserve paused output, immediately clear cache on disable, and verify every world side and bundled cell. Without a provider, retained legacy bundled state is explicitly populated and must also clear.

Initial logs in build/finish-port-implementation/:

- redstone-enable-first-probe.log: six valid lifecycle failures plus two overstrict forced-Java-output probes; the latter were replaced with actual dust-connectivity checks after reading upstream.
- redstone-enable-red.log: all eight corrected regressions reproduced.
- redstone-enable-rack-regression.log: lifecycle fixes exposed stopped state replacing a rack unload snapshot; teardown ordering was then corrected.

Run verification separately, waiting for each process to finish:

~~~powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat build runGameTestServer --no-daemon --console=plain
~~~

Real client wire/dust appearance, multiplayer and remaining integration acceptance stay open. No Modrinth client was installed or launched for this checkpoint.

## Two-JVM regression verification - 2026-10-19

The existing native/open-file/tablet and redstone restart probes were rerun with CC and ProjectRed after lifecycle changes. Fresh world oc-restart-output-lifecycle-20261019-01: prepare PID 39148, verify PID 55544; both passed all 678 required tests. Verify inspected the saved input arrays before ticking. Logs: build/finish-port-implementation/redstone-enable-restart-prepare.log and redstone-enable-restart-verify.log.

The retained restart fixture still uses real CC sources and the native probe; ProjectRed cable fixtures run afresh during each suite. This is not evidence that ProjectRed multipart wire state itself resumed across JVMs. All-host running Lua continuation, crash recovery, wire chunk boundaries and real client/multiplayer remain separate gates.

Final verification: build and all 2112 unit tests passed. The combined PR+CC suite passed all 676 required GameTests (redstone-enable-integrated-first.log). The later prepare/verify runs with the strengthened ProjectRed source-stop fixtures each passed all 678 required tests. The final ordinary build/run passed all 676 registered GameTests, including additional adjacent-lamp shutoff checks, with no optional mods installed (redstone-enable-absent-verified.log). Six PR and eight CC bodies skip only in that ordinary run; skipped bodies are not provider evidence. Lamp state is server-world evidence, not rendered client acceptance.

No further source changes followed these checks. Full port work remains active; this checkpoint does not certify full feature parity or beta readiness.

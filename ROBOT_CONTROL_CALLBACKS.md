# Robot control and name callbacks

Related: [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Port gap triage]], [[Process restart test]].

Runtime 9b53261c36574ddfe75115222062f3d01e6ea4ea, 2026-10-20: build and all 2135 unit tests pass. Fresh world oc-restart-robot-control-20261020-02 passes all727 required GameTests in prepare PID59064 and verify PID59708, with ProjectRed and ComputerCraft and normal shutdowns. This includes all723 ordinary tests and the four existing opt-in restart probes. No client or Computer Use action occurred.

Newer name-lifecycle checkpoint ada354292 restores the original name list, tested default generation and item/world naming paths; see [[Agent names]] for current proof and limits. The historical default-name gap below is superseded for those tested server paths.

## Implemented contract

Upstream common/tileentity/RobotProxy.scala at 7d9eac5584e238a890a388a0f697c27c7391caa4 exposes these on the physical robot component. A Java Agent.setName(String) setter and a name() alias were not equivalent Lua implementations.

| Callback | Behavior |
| --- | --- |
| start() | Returns whether starting changed state. Does not resume a paused robot. |
| stop() | Stops its machine; returns whether the state changed. |
| isRunning() | Direct callback returning machine.isRunning(). |
| getName() | Returns the robot name, synchronized like upstream. |
| setName(name) | Validates its string argument, returns the previous name and installs the replacement only when not running. Running/paused returns nil, is running. |

The existing direct name() alias remains compatible. Nonempty names, including Unicode and whitespace-only strings, survive the world NBT roundtrip. The assembled robot item carries the name. A live rename includes oc:name in the existing block-entity update; client application reads it, and older update tags without the field leave the client's current name intact. A Java Agent setter remains an internal unrestricted operation, matching upstream; the running restriction belongs to the Lua callback.

An empty string is accepted during use. The newer ada354292 checkpoint restores robot.names, missing/empty-name regeneration, default assembly names and tested item/display-name preservation; see [[Agent names]]. The earlier generic Robot reload fallback gap is closed on those tested server paths. Wider display-name migration and client acceptance remain separate.

## Shared machine prerequisite

The first native robot regression also reproduced repeated start incorrectly returning true. SimpleMachine.start reset sleep/deadline fields on an already-running machine, so it could bypass scheduled sleep. It now returns false immediately for a running, nonpaused machine. Direct Machine.start still resumes an explicit pause as upstream does; computer/robot component start callbacks deliberately reject pause resume. This fixes the shared machine behavior rather than adding a robot-only return-value workaround.

Two parameterized unit cases use a fixed clock and a deterministic sleeping architecture to check both direct API and computer-component start without bypassing the sleep deadline. A third unit protects direct pause resume versus component refusal. Two real server GameTests exercise robot-name callbacks, native robot Lua getName/setName/isRunning, start/stop results, paused behavior, world NBT and assembled-item payloads, and client-tag formation/application. They do not prove live packet delivery, name rendering or another computer's complete external-control flow.

## How to run/use this

From another connected OC computer, discover the robot component and call its proxy while the robot is stopped. For example, r = component.proxy(component.list('robot')()) followed by r.setName('Surveyor') and r.start(). A program on that running robot can read r.getName(), but cannot rename itself until stopped.

Run server regression checks from M:/development/OpenComputers:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
```

For a fresh-world two-process regression, use the sequential commands in [[Process restart test]], retaining the quoted dotted Gradle properties. The actual pair above rechecks shared-machine continuation on computer/tablet/microcontroller, redstone disk history and forbidden saved chunkloader tickets. It does not add a robot-name-specific disk/JVM probe.

Evidence under build/finish-port-implementation/: robot-control-callbacks-red.log (two missing-callback failures); robot-repeated-start-red.log; machine-repeated-start-red.xml (both direct/component repeated-start failures); robot-control-restart-prepare.log and robot-control-restart-verify.log (final pair). The earlier preliminary prepare world01 is not the final-revision pair.

Still open: actual external computer control/player naming, client packet/rendering and multiplayer acceptance, wider config/platform/load behavior and other robot callback semantics. [[Agent names]] records the newer default-name/item lifecycle checkpoint and its remaining variants. Beta estimate remains 3-6 concentrated workweeks with low confidence for full implementation plus beta acceptance, not a callback-count percentage.

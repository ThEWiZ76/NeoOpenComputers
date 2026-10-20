# Robot and drone names

Related: [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Port gap triage]], [[Robot control callbacks]].

Runtime ada35429256ae8b1695415fce21b9c5b48fa7ab9, 2026-10-20: build, all2138 unit tests and all726 ordinary GameTests pass with ProjectRed and ComputerCraft together. Evidence: agent-name-lifecycle-verified.log under build/finish-port-implementation/. No client, Computer Use or new two-JVM probe was run for this checkpoint.

## Implemented behavior

Upstream common/item/data/RobotData.scala and DroneData.scala, common/template/RobotTemplate.scala and DroneTemplate.scala at 7d9eac5584e238a890a388a0f697c27c7391caa4 use a shared list for default names. The original robot.names is now bundled unchanged at assets/neoopencomputers/robot.names. It contains135 parsed names; SHA256 483ee72ebb2dbe6368afcae532f01cf1b04a248a4a2e007d7e904e6acdfa88f4 matches the reference resource byte-for-byte.

AgentNames strips trailing # comments, trims list entries and ignores blank lines as upstream does. Selection uses the loaded list; an absent/unreadable/empty list falls back to Robot for either agent, matching upstream. Missing/empty names get a new default; nonempty strings, including whitespace, are retained. Selection tests use a fixed random seed. Once assigned, a name is persisted and does not change on subsequent ordinary save/load.

Robot assembler output now has both oc:name in BLOCK_ENTITY_DATA and a modern CUSTOM_NAME display component. Drone assembly stores its generated name in the oc:drone payload and CUSTOM_NAME. An anvil-style CUSTOM_NAME on a finished robot item overrides its stored name when placed; unnamed older robot items receive a default. Named robot drops retain both display and stored names. Drone item loading prefers CUSTOM_NAME, then its stored name, then a generated default; wrench/void packing stores the current agent name on the returned item before replacement. Existing cargo/hardware/energy ownership regressions remain green.

World robot/drone loads regenerate only missing/empty names. Immediate Lua robot setName('') still accepts the empty name as described in [[Robot control callbacks]]; a later world/item load regenerates it as upstream does. Nonempty whitespace survives without regeneration. This closes the prior generic Robot/Drone fallback gap on the tested assembly/placement/load paths, not every possible addon or client naming scenario.

## Evidence

Three new server tests cover actual assembler output and robot placement/drone item loading, a conflicting robot stored/anvil name plus drop payload and whitespace reload, and missing/empty robot/drone world data followed by another save/load. Generated choices are captured once and compared across lifecycle transitions; tests do not expect two random names to differ. The existing real wrench-pickup test now also checks display-name preservation and replacement while retaining its hardware, cargo, generator and energy assertions.

The first server run reproduced three required failures: missing assembler name, ignored anvil robot name and lost packed-drone display name (agent-name-lifecycle-red.log). Three focused unit tests cover comments/Unicode parsing, fixed-seed selection/empty-list fallback, and actual packaged-resource selection (seed731 selects LordFokas). The full final build/server run passes with normal shutdown.

This is actual server assembly/placement and in-memory world-NBT restoration proof. It is not new disk/JVM name persistence proof, live anvil GUI usage or client-render evidence. The earlier two-JVM pair at9b53261c3 remains evidence for its own robot/shared-machine checkpoint, not a new run of this revision.

## How to run/use this

Assemble a robot or drone normally: the finished item has its generated name before placement. Rename the finished item with an anvil, or use the stopped robot setName callback from [[Robot control callbacks]]. A named robot drop or packed drone item keeps that name when placed again.

From M:/development/OpenComputers:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
```

Remaining acceptance includes live item/anvil/tooltip/client display, multiplayer, broader formatted/custom data and addon migration, named pick-block semantics, and a dedicated disk/JVM lifecycle for named agents. This is not a 1.12-world/config converter. Beta estimate remains3-6 concentrated workweeks with low confidence for full implementation and beta acceptance; integration scope and the wider semantic/client matrix remain open.

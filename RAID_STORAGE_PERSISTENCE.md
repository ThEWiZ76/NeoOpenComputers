# RAID formatting and lifecycle persistence

Runtime 2b54dcf2a00e276abd69fe95630e68eb4915b984, 2026-10-20. Related: [[Portable media and menu persistence]], [[Adapter upgrade ownership and persistence]], [[Process restart test]].

## Upstream contract

Upstream common/tileentity/Raid.scala formats the individual HDDs when a full array is formed, clears member raw/lock metadata, and erases the array on disk removal. Whole-block pickup and world/chunk serialization instead preserve array identity/data. common/inventory/Inventory.scala treats setting the identical installed stack as a no-op. common/block/Raid.scala exposes comparator15 for a complete three-disk set and0 otherwise. These are distinct contracts: preserving old loose-disk data on assembly or preserving a disassembled array is not parity.

## Implementation

Completed arrays now format member filesystems recursively, including nonempty directories, and serialize the empty managed environment back through the actual driver tag. Saved raw bytes/head state are replaced by the managed snapshot, so toggling a removed member back to raw mode cannot resurrect old bytes. Labels/node metadata supported by the environment and unrelated item root components remain intact. The internal FileSystemEnvironment and its filesystem accessor are accessible to the RAID implementation so formatting can use the underlying filesystem directly, without Lua call/energy budgets. No public li.cil.oc.api interface was changed.

Disk mutation erases the previous array's files and closes channels before disposal; reassembly creates a new identity and an empty volume. Reassigning the exact installed ItemStack or empty-to-empty is a no-op. This does not treat a different copied ItemStack as an unchanged slot; upstream also treats that as replacement.

Lifecycle disposal saves the active filesystem snapshot before disconnecting it. Later saves retain that snapshot even after unload/setRemoved, including handle owners and positions. Loading disposes the previous live filesystem, defers initialization without a world, restores filesystem data/identity before graph connection and rejoins the real neighbor network in onLoad. Restoring a whole RAID item uses this same load path. Array label and ordinary visual-presence data remain serialized. RaidBlock now exposes the upstream comparator values.

## Evidence

Seven regressions fail against the preceding implementation (759 tests, exactly7 failures): retained old member files; retained raw/lock state; unload-before-save losing volume identity; repeated load leaving the old node connected; disk removal disconnecting without erasing the old volume; identical-stack assignment destroying the array; and missing comparator output. The first mixed implementation passes six and reaches a fixture error in the seventh: after a real split, the fixture reinserted the now-empty source reference rather than the returned disk. That fixture is corrected; no production change was made to hide it.

Final tests cover nested preexisting files and no formatting before the third disk, all four destructive removal paths, stale handle rejection, fresh volume identity, same-stack preservation, reload cleanup, strict binary bytes0/128/255/42 and an open read offset after explicit unload-before-save. The raw/lock test seeds a byte through the real raw-drive callback and proves it reads zero after formatting, removal and toggling back to raw mode. Comparator values are verified through the actual block state API; this is not a visual/live redstone-circuit claim. Existing block-item and broken-RAID item persistence tests also pass.

Build/2143 units and all824 mixed-provider tests pass with CC/AE2/Mekanism/EnderStorage/ProjectRed. Both no-optional-mod restart processes pass764 tests in world oc-restart-raid-storage-20261020-01 (prepare PID49964, verify PID32436), including759 ordinary tests and five process probes. The new native Lua5.3 probe preserves local731, computer/RAID/filesystem identities, label, binary bytes and an open read offset across normal JVM shutdown/startup. It uses the actual adjacent computer/RAID graph; restored hosts are not manually refreshed or reconnected by the test. Existing native computer/tablet, adapter and other core process probes also pass. All Gradle/server handles exit normally. Logs: build/finish-port-implementation/raid-storage-*-build/latest/debug.log, including the original red and first-green diagnostics.

## Run the process probe

Use a fresh restart world for prepare and exactly that same world for verify. Run one Gradle at a time, wait for terminal exit and retain run/gameTestServer/logs/latest.log and debug.log before starting the next phase.

~~~powershell
rtk proxy cmd /d /c "gradlew.bat build runGameTestServer -Pneoopencomputers.restartPhase=prepare -Pneoopencomputers.restartWorld=oc-restart-raid-storage-NEW --no-daemon --console=plain"
rtk proxy cmd /d /c "gradlew.bat build runGameTestServer -Pneoopencomputers.restartPhase=verify -Pneoopencomputers.restartWorld=oc-restart-raid-storage-NEW --no-daemon --console=plain"
~~~

## Remaining gates

Full RAID parity remains open for rotation/facing and associated geometry/resources, real comparator wiring/neighbor updates, menu access/range, GUI/activity visuals, arbitrary addon HDD drivers/capacities, broad historical/addon migration, crash/load/protection and client/survival/multiplayer/platform acceptance. The process pair here uses builtin memory-backed member drives/array serialization; no claim is made about every external directory-backed addon driver. Wider inventory/caller/API/integration audits remain required. No client, Modrinth, Computer Use or window movement occurred. Actual client acceptance remains required on screen3 with focus coordination.

Whole-port beta remains3-6 concentrated workweeks, low confidence, for full implementation plus acceptance. Test totals are not a completion percentage.

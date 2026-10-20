# Managed disk locking

Runtime checkpoint 57b2250819f0a59b8a1ea2639c5fdb6c34c8db02. See [[World loot disks]], [[Portable media and menu persistence]] and [[../02 plans/Complete portering]]. Functional parity work; no visual changes.

## Contract and fix

Upstream integration/opencomputers/DriverFileSystem.scala wraps both the managed filesystem and its label when DriveData is locked. The port previously always constructed a writable RAM filesystem and ItemDiskLabel, and its environment save replaced the item driver data without carrying oc:lock. A managed HDD could therefore be written/renamed/relabelled despite its lock, and saving erased the lock owner.

HardDiskDriveItem now selects an asReadOnly filesystem and the existing immutable string-label overload for locked managed disks. Existing file bytes, label and node/stream metadata still load normally. Its StackBackedEnvironment preserves the captured nonempty lock owner in the actual environment snapshot before copying that snapshot to item data. Raw-drive locking remains handled by DriveEnvironment; its existing save contract is preserved. Unlocked HDDs retain the writable path.

FileSystemRegistry's read-only facade now also wraps every non-null handle returned by getHandle. Position, length, read, seek and close delegate; write throws IOException even for a saved output handle opened before locking. Invalid handle lookup still returns null. This closes the restored-handle bypass for managed HDDs and every caller using the shared read-only wrapper, without changing the backing files or stored handle IDs/positions.

No disk backend migration occurs: native managed HDD content remains NBT-backed RAM. Existing already-erased lock metadata cannot be inferred/restored automatically. The fixture's saved item legitimately contains node/handle data in addition to user configuration; lock checks do not discard that data.

## Verified evidence

- managed-lock-red-build.log: full2204 unit run has1 failure for a writable handle obtained through the read-only wrapper; all824 ordinary GameTests complete with3 required failures for writable managed HDD, mutable label and lost lock at save. Build exits1 normally, with archived latest/debug logs. Native host acceptance was added after this baseline; it is not counted as an observed fourth baseline failure.
- managed-lock-green logs: build/all2204 units/all825 ordinary GameTests pass. The four new server tests cover all3 HDD tiers' mutation denial and binary content, readonly label, component/item encode/decode lock persistence, and a real native Lua5.3 case with installed locked HDD. The native program tests write/append/rename/remove/mkdir/label denial and binary reads; actual host save retains the owner.
- managed-lock-final-mixed logs: build/all2204 units/all915 required GameTests pass, including JEI/CC/AE2/Mekanism/EnderStorage/ProjectRed and the preseeded world loot suite. Complete summary, BUILD SUCCESSFUL, normal exit.
- WorldLootProcessRestartGameTests is extended with an internal locked managed HDD beside its world and legacy disks. In oc-restart-world-loot-managed-lock-20261020-01, prepare PID59468 and fresh-JVM verify PID64684 each pass all831 required tests and exit normally without optional providers. All3 open read handles remain at offset1, native local731 and filesystem identities survive, and exact remaining binary bytes are read after resume. The HDD lock owner persists in the loaded item and after another actual host save; write-open after restart is denied. The external world source files remain independently unchanged. Logs managed-lock-restart-{prepare,verify}-{build,latest,debug}; this world is verified and must not be reused as fresh preparation.

The restart commands use the tracked seeder and guarded restartWorldLoot/restartPhase/restartWorld profile documented in [[World loot disks]]. Only the source version changes; choose a new world name for each prepare/verify pair.

## Remaining technical work

Floppy FileSystemLike GUI/mode/lock parity and mode-switch formatting/address semantics remain separate. Native HDD/floppy/RAID save-directory/bufferChanges backend compatibility requires its own migration plan, preserving existing NBT content and source files. Full client/survival/multiplayer/platform/load acceptance remains open. No real client, Modrinth install, OS Computer Use or production operation occurred. The full port remains active; cosmetic polish stays deferred. Beta estimate3-6 concentrated workweeks, low confidence, not a feature completion percentage.

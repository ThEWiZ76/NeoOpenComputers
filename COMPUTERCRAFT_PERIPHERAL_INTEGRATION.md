# ComputerCraft peripheral integration

Checkpoint be937fd62efab97248ecfe65a1916bb7ab60dc51, 2026-10-20. CC:Tweaked1.120.2, NeoForge21.1.234, Minecraft1.21.1, Java21. Related: [[../Project]], [[../02 plans/ComputerCraft integration]], [[ComputerCraft relay integration]].

## Implemented contract

The adapter discovers loaded block entities through the sided PeripheralCapability and CC's generic fallback. Adapter direction is translated to the target's opposite face. Native OC environments remain excluded. Object-level BlacklistedPeripheral policy takes precedence over configurable and IMC class policies; subclasses/interfaces are included. integration.peripheralBlacklist defaults to the modern CommandBlockEntity and recognizes the old TileEntityCommandBlock name. CC's generic inventory/fluid supplement preserves existing OC component names; explicit native drivers retain callback/name priority.

The pinned ServerContext.peripheralMethods dispatcher handles dynamic peripherals and CC's annotated methods, including optional/coerced arguments and generic inventory methods. compileOnly uses the full pinned Forge artifact because these dispatcher/generic APIs are internal implementation contracts. Nothing foreign is bundled or made a mandatory published dependency. Upgrading CC requires rerunning this integration matrix.

Off-thread calls throw OC's existing LimitReachedException before a world action. LuaJ/native callers use their existing synchronized retry. Main-thread annotations execute on that thread. CC's guarded generic contexts use the default task continuation: the bridge completes only task_complete waits for work requested by that invocation, with a bounded continuation count. Explicit asynchronous tasks remain deferred until a later tick and send task_complete(id, success, ...) to their owner. This spelling follows the pinned runtime's TaskCallback, despite API comments saying task_completed. Arbitrary event waits/yields remain explicitly unsupported, matching the legacy UnsupportedLuaContext limitation; they never block the server thread.

Each visible OC context gets one IComputerAccess attach/detach lifecycle, legacy address-hash ID and attachment address. Available peripherals are keyed by visible compound OC component addresses, allowing native CC pushItems/pullItems targets. Events stay isolated to the owning context. WorkMonitor reflects running/paused/detached state and charges OC's normal0.01 peripheral-operation budget when work is reported. Pending jobs are removed on detach. Peripheral identity/capability changes invalidate stale calls and refresh the adjacent adapter without loading missing chunks. Mount cleanup survives foreign detach exceptions.

The direct Java peripheral boundary preserves UTF-8 Java text, unlike the relay's actual CC Lua VM8-bit boundary. A dedicated IArguments snapshot keeps raw getBytes/drop access and Unicode getString/getAll/table values. Results/events normalize ByteBuffer without advancing its position; recursive maps and cycles remain supported.

MountFileSystem wraps native public Mount/WritableMount channels. mount() is read-only even if the supplied object is writable. It supports exact binary I/O, seek/append, native capacity metrics, listing/mutations and channel cleanup. Paths are sanitized and root escapes rejected. Saved handle metadata restores offsets and opens existing write handles without CREATE/TRUNCATE; a missing file or unsupported restore mode invalidates that handle rather than truncating data. It owns its channels, never the shared mount.

## Verification

- Initial configurable-blacklist unit test failed before config implementation (cc-peripheral-settings-red.log evidence). Initial740-test CC profile failed exactly the five missing-driver cases, with normal failed-process exit; retained cc-peripheral-red-* logs.
- Six final CC GameTests use an actual native Lua5.3 OC computer/adapter/CC printer (real printed page and exact ink/paper accounting), native generic chest transfers (exact5=2+3 conservation), sided dynamic probes for Unicode/binary/cyclic tables/drop/events/tasks, per-context cleanup/cancel, real save-directory mounts with read AND write handle restoration, deliberate foreign detach failure, class/object/legacy blacklist rules, native exclusion, missing-chunk lookup and peripheral replacement.
- build/2143 units,787 mixed-mod GameTests,741 CC-only and730 no-optional-mod tests pass. All three server/Gradle handles terminate normally. See cc-peripheral-mixed/alone/absent-build/latest/debug.log under build/finish-port-implementation. Packaging tests reject bundled foreign classes and required foreign POM dependencies.
- Mixed profile enables withComputerCraft, withAE2, withMekanism, withEnderStorage and withProjectRed. CC-only uses withComputerCraft; absent uses none. All use --no-daemon --console=plain and separate retained worlds; run only one Gradle at a time.

## Remaining acceptance

The actual CC media item driver and Lua-object converter are separate open work packages. External transient attachment mounts are not claimed to survive a world/process/native-heap restart; the handle regression reloads the same live mount. Full media restart proof belongs to the next package. Other native peripheral families, broader async/task load, protection, client visuals, survival and multiplayer remain acceptance gates. No client, Modrinth, Computer Use or window movement occurred in this checkpoint. Client testing remains required on screen3 with focus coordination.

Whole-port beta estimate remains3-6 concentrated workweeks with low confidence for full implementation plus acceptance. Counts of passing tests are evidence for named behaviors, not a completion percentage.

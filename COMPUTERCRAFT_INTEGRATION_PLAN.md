# ComputerCraft integration plan

Goal: restore the pinned MC1.12 ComputerCraft contracts on CC:Tweaked 1.120.2 / NeoForge 1.21.1, including server/native and real-client acceptance. Existing bundled transport remains covered separately.

Related: [[../Project]], [[Complete portering]], [[../04 ai-context/Modern integration scope]].

## Contract and implementation order

- [x] Compare upstream ModComputerCraft, RelayPeripheral, DriverPeripheral, DriverComputerCraftMedia, both mount adapters, CallableHelper and ConverterLuaObject against the current implementation. Only bundled redstone is currently registered.
- [x] Relay modem: expose a stable per-relay IPeripheral through CC's public NeoForge PeripheralCapability. Keep optional classes behind the mod-present check. Per-computer channel sets, 128-channel limit, port0..65535, attach/detach cleanup, source/destination filtering, modem_message and UTF-8 payloads. Reuse the relay queue/TTL/capacity and wireless/tunnel forwarding; do not create another packet network.
- [x] Relay remote access: the original14 methods, visible components from all six networks, one-based result lists, correct component calling context/events and original modem/OC capability flags. Execute world/network mutations on the server thread through CC's Lua context. Reject removed/detached use.
- [x] Peripheral driver: loaded-chunk capability lookup, blacklist/native-OC precedence, dynamic methods, per-computer attach/detach, signals/mounts and supported modern Lua task semantics. Use real native peripherals, not a test-only production component.
- [ ] Media: CC media lookup, read/write mount adapters, filesystem labels, stable node address, errors/channel closing, item data writeback and persistence. Test actual CC disks through OC hardware.
- [ ] Lua object conversion: modern ILuaObject invocation/arguments/results, supported task semantics and explicit behavior after persistence when original context cannot be restored.
- [ ] Real client on screen3: menus, modem/peripheral programs, media access, survival placement/removal, reconnect and multiplayer. This remains required even when automated server tests pass.

## Relay tests and files

Write server tests first in ComputerCraftRelayGameTests; require an actual CC computer program to discover the relay through peripheral APIs, exercise channels and remote methods, exchange real OC packets and assert actual Lua output/events. Add server-side cases for queue overflow/TTL, port boundaries, 128-channel limit, per-computer isolation, directed delivery/self exclusion, detach and component visibility. Capture the baseline missing-feature failures before production edits.

Implementation: common/integration/ComputerCraftRelayIntegration.java provides optional capability registration and a CC-free relay receiver interface; common/integration/computercraft/RelayPeripheral.java owns CC state/methods; ModCapabilities registers the optional provider; RelayBlockEntity exposes enqueue success and forwards received packets to the attached receiver. No hard CC types in relay state or absent-mod class signatures.

The old transmitter appends an Integer reply channel while the receiver consumes a leading Double. Resolve this proven internal mismatch explicitly in implementation/tests: use a leading Double reply channel for newly transmitted packets, matching the legacy OC-to-CC receive convention. Document this bugfix and the raw wire-layout difference, rather than claiming byte-identical old behavior. Arbitrary tables remain rejected by OC packet validation; no undocumented serialization protocol is invented.

Run one Gradle at a time, await terminal exit, preserve latest/debug logs. Require full mixed and CC-only profiles plus no-CC build/load, artifact isolation and focused tests. Actual process restart is required for persisted state, not transient CC attachments/channel sets. Then update canonical manuals/parity/gap/scope, mirror repository docs, commit/push and verify local/remote equality.

Whole-port beta estimate:3-6 concentrated workweeks, low confidence. Open unload observation, other integrations, semantic/resource/API audit and full platform/client/survival/multiplayer acceptance remain in scope.

## Relay checkpoint

Implemented/tested at68650919030cd096146fcada74ea159726017ffa; five new tests, build/2142 units/778 mixed/732 CC-only/727 absent pass. See [[../03 manuals/ComputerCraft relay integration]] for scope, encoding/wire bugfixes and exact evidence. Client and remaining CC families above stay open.

## Media work package

Use the public NeoForge MediaCapability (the same lookup as CC itself), not the obsolete assumption that the Item implements IMedia. Keep the driver behind the existing mod-present registration. Match the legacy floppy slot/tier and server-only creation. Reuse MountFileSystem, retain native CC labels and file storage, persist the OC node address in item data and close owned handles on removal. CC's native media ID allocation/label changes must remain on the actual inventory stack and mark the host dirty.

First reproduce missing CC disk acceptance and ordinary OC floppy data loss on removal. Cover removeItem, removeItemNoUpdate, replacement and clearing before implementing a shared save-before-inventory-mutation correction. Existing block destruction already saves before dropping contents. Use actual OC diskdrive/case hardware and native Lua for real CC disks; verify exact binary bytes and labels via CC's native media API, item serialization/reinsertion and stable OC identity. Use a tagged test-only media capability over a real CC resource mount for immutable readonly media; native treasure Lua assets are not present in the pinned artifact, so do not invent a treasure-disk acceptance claim. Check audio/non-data media and null/client hosts explicitly.

Extend the existing opt-in two-JVM process probe with an explicit CC-media flag: retain native Lua locals and an external file offset, reload the exact saved world/disk, then independently read the same bytes/label through CC. Require separate PIDs and normal shutdown. Finish with mixed, CC-only and absence profiles, artifact isolation, canonical documentation and pushed commits. Media acceptance does not close the Lua-object or real-client gates.

## Peripheral driver work package

Restore the old DriverPeripheral contract through the pinned CC runtime's own ServerContext.peripheralMethods dispatcher, supporting annotated methods and dynamic peripherals without maintaining a competing reflection engine. This requires compileOnly of the pinned full Forge artifact; nothing is bundled or made a required published dependency. Lookup follows CC's explicit capability plus generic fallback; translate the adapter direction to the physical target face. Guard missing chunks, native OC environments, object-level blacklist overrides and configurable/IMC class blacklists (modern CommandBlockEntity blocked by default).

Use OC's existing LimitReachedException retry before any peripheral action when invoked off the server thread. The existing LuaJ/native synchronization path then retries once on the server thread; no new kernel scheduling API is needed. Execute supported main-thread CC tasks there; queue explicit asynchronous tasks for a later environment update and forward task_complete to the owning OC context. Legacy event-wait/yield operations remain explicitly unsupported, as the old UnsupportedLuaContext did; never block the server awaiting a CC event.

The direct Java peripheral boundary differs from the relay's actual CC Lua VM boundary. Preserve the old UTF-8 text-to-Java-String argument behavior, while providing binary-safe IArguments.getBytes and recursive tables. Keep Java text results/events as text and normalize ByteBuffer results without mutating buffers. Per-context IComputerAccess retains attach/detach, mount cleanup, address/ID and visible bridged peripheral lookup for native CC inventory transfers. Modern WorkMonitor must respect context lifecycle and OC call accounting.

Implement shared MountFileSystem over public Mount/WritableMount channels, using native CC path sanitization with root-escape rejection. Cover readonly/writable operations, exact byte I/O, seek/append, capacity, handle closure and restoring handle metadata without truncating a file. Reuse this adapter for the later real-media driver. Per-context attachment state is transient; do not claim external mount/world/heap persistence without an actual acceptance test.

Regression order: configurable blacklist unit failure; real CC printer via an actual OC native computer and adapter; native generic chest transfers with exact item conservation; side-aware dynamic test provider for attach/detach, text/binary arguments, events/tasks and mount cleanup; blacklist/native-OC exclusion and absence/missing-chunk safeguards. Record the missing-feature baseline before production implementation, then run full mixed/CC-only/absence profiles and relevant persistence checks. Broader native device, client/multiplayer and media/Lua-object work stays in the full plan.

CC peripheral checkpoint be937fd62efab97248ecfe65a1916bb7ab60dc51: optional sided/native/generic adapter bridge with original blacklist/native precedence, per-context lifecycle/events/tasks and binary-safe Java arguments. Actual native53 printer and chest transfer pass; transient mounts prove readonly/binary/seek/append/non-truncating read/write handle restore and detach-failure cleanup. Six new tests; build/2143 units/787 mixed/741 CC-only/730 absent pass with normal exit. See [[../03 manuals/ComputerCraft peripheral integration]]. Media/Lua-object, process persistence of external mounts and broader client/survival/multiplayer/platform/load acceptance remain open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.

CC media block-drive checkpoint a6f0c3905487863bcbaad501506c8af137914766: public media-capability driver with native labels/storage/ID and stable OC identity. Fixed block-drive save-before-eject/split/take/replacement/clear and deferred world-assignment initialization. Five new regressions; build/2143 units/792 mixed/731 absent and actual750-test CC-only prepare/verify JVMs (PIDs4208/50868) pass with normal exit, native Lua local/external open-file offset and CC-native byte/label readback. See [[../03 manuals/ComputerCraft media and diskdrive persistence]]. Rack/internal-host media removal/load/restart is next and the media checklist remains open; then Lua-object and broader client/survival/multiplayer/platform/load gates. Beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.

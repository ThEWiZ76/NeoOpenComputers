# ComputerCraft integration plan

Goal: restore the pinned MC1.12 ComputerCraft contracts on CC:Tweaked 1.120.2 / NeoForge 1.21.1, including server/native and real-client acceptance. Existing bundled transport remains covered separately.

Related: [[../Project]], [[Complete portering]], [[../04 ai-context/Modern integration scope]].

## Contract and implementation order

- [x] Compare upstream ModComputerCraft, RelayPeripheral, DriverPeripheral, DriverComputerCraftMedia, both mount adapters, CallableHelper and ConverterLuaObject against the current implementation. Only bundled redstone is currently registered.
- [x] Relay modem: expose a stable per-relay IPeripheral through CC's public NeoForge PeripheralCapability. Keep optional classes behind the mod-present check. Per-computer channel sets, 128-channel limit, port0..65535, attach/detach cleanup, source/destination filtering, modem_message and UTF-8 payloads. Reuse the relay queue/TTL/capacity and wireless/tunnel forwarding; do not create another packet network.
- [x] Relay remote access: the original14 methods, visible components from all six networks, one-based result lists, correct component calling context/events and original modem/OC capability flags. Execute world/network mutations on the server thread through CC's Lua context. Reject removed/detached use.
- [ ] Peripheral driver: loaded-chunk capability lookup, blacklist/native-OC precedence, dynamic methods, per-computer attach/detach, signals/mounts and supported modern Lua task semantics. Use real native peripherals, not a test-only production component.
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

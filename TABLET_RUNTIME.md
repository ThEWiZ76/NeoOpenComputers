# Tablet runtime

2026-09-22. Related: [[../02 plans/Complete portering]], [[Native Lua persistence]], [[../05 memory/OpenComputers Memory]].

## Implemented foundation

TabletRuntime hosts a real SimpleMachine with stable decoded item components, a separate tablet component, integrated 80x25 four-bit ScreenItemEnvironment, keyboard connection, player-relative rotation/position, item charging reconciliation and explicit save/disposal. TabletItem exposes its existing data read/write helpers within its package; assembly data format stays intact. Runtime snapshots live inside oc:tablet/runtime and contain machine, screen and tablet state. Machine.save saves component environments before components are encoded back into the item.

close(true) snapshots before disconnect/disposal and permits a replacement runtime to resume. close(false) stops before saving so ordinary eviction does not restart the tablet. Both close the old VM and remove the private network nodes. This follows the distinction in upstream common/item/Tablet.scala between dimension handoff and ordinary cache eviction. This is not yet wired to actual player/dimension/server events.

Computer node data must load before onHostChanged builds the network. The controlled tablet-address-index-red.log run without this ordering failed both lifecycle cases with 'Tablet computer address not indexed'. Restoring that ordering passed the direct node lookup and resume assertions. A Lua proxy of the machine's own address is not an appropriate index assertion because the component listing excludes its own node; the final test checks the network directly.

## Verified

TabletRuntimeGameTests runs native EEPROM code, binds a GPU to the integrated screen, checks GPU/screen component slots, reads the tablet yaw callback, writes screen text and keeps a local value and binary tmp-file handle. It serializes the entire ItemStack through Minecraft NBT, destroys the old runtime and reconstructs another. The handoff case retains machine/screen addresses, screen contents, local state and the open binary read offset; ordinary eviction restores stopped. A separate test checks actual and simulated ItemStack charging against the live battery with a powered-off screen and stopped machine.

Final tablet-runtime-verified.log: build/test succeeded, 2106 unit tests and all 530 GameTests passed. No client/install/push/merge. These are runtime GameTests, not live client or player lifecycle acceptance.

## Remaining tablet work

- Server cache with correct item identity/rebinding, duplicate handling and expiry; inventory ticking and item-use start/stop.
- TerminalMenu integration using existing itemScreen and TerminalNetworking; tier-dependent component editing and delayed block analysis using the live runtime instead of temporary TabletAnalysisHost.
- Actual player save/logout/drop/dimension/server-stop ordering; full OpenOS tablet flow and native lifecycle tests through those hooks.
- Visual/input acceptance in the Modrinth client on display3. Focus coordination remains pending.

One intermediate run also timed out in nativeRackRetainsUptimeDeadlineAfterReload with marker=waiting and no Lua error (17:31:54). Later full runs passed, which does not resolve that symptom. Investigate zero-tick execution-delay versus accelerated game-time deadlines separately; do not call the entire native runtime stable on test counts alone.

# Tablet runtime

2026-09-22. Related: [[../02 plans/Complete portering]], [[Native Lua persistence]], [[../05 memory/OpenComputers Memory]].

## Rack timing investigation resolved - 2026-09-22

The earlier rack deadline symptom mentioned below is now diagnosed as a test clock mismatch; those open-investigation notes are historical. A controlled fractional-deadline reproducer reached the restored Lua timer loop, then waited on a 12ms monotonic execution delay while accelerated game uptime had reached 5.6 seconds. NativeRackPersistenceGameTests now advances its injected scheduler clock by 50ms per world tick and deliberately exercises a fractional deadline. The original 60-tick assertion is unchanged. Final native-rack-clock-verified.log passed build, 2108 unit tests and all 539 GameTests. See [[Native Lua persistence]] for evidence and limits. This is not a claim that all native runtime or full-port acceptance is complete.

## Current player integration - 2026-09-22

Latest OpenOS coverage: TabletOpenOsGameTests boots the shipped Lua BIOS and read-only OpenOS media inside an assembled tier2 tablet via actual item use, terminal menu and inventory ticks. LuaJ accepts a typed echo command. Native Lua accepts `set ocresume=731`, moves the ServerPlayer into the Nether, disposes the old VM, rejects the old menu, reopens the tablet and evaluates `echo $ocresume` to a standalone 731 output line. It checks the complete component address/type map remains identical and closes the final VM on logout. Long fixtures recharge through the item charging API. This is server-side keyboard/screen acceptance, not real-client rendering/input or an OpenOS installation onto writable media.

This exposed a production gap: FloppyItem returned unbacked environments for read-only loot media. Machine.save called environment.save, but its node identity and handles never reached the item; restoring the internal OpenOS disk assigned another address. The red tablet-openos-verified.log showed `no such component` when the restored shell tried executing echo. Read-only floppy paths now load driver state and use the same StackBackedEnvironment persistence wrapper as writable floppies, including legacy loot path/factory formats. Filesystem contents remain read-only. The modern OpenOS format is directly exercised by this new lifecycle test; legacy full-shell lifecycle variants were not added.

Final tablet-openos-fixed.log: build, 2108 units and all541 GameTests passed. Still open: actual process restart, external automation, wider port parity and real-client visual acceptance. Tier2 editor and writable installation coverage are described below. No client/install/push/merge.

TabletItem now ticks a server-cached runtime from inventoryTick. A short use/release starts the computer and opens TabletTerminalMenu through the existing TerminalScreen/TerminalNetworking transport. Sneak short-use stops it and opens the expansion editor on tier2/creative tablets. Holding a block target for ten ticks then releasing sends analysis through the live component network and queues tablet_use. The null-player analyzeBlock utility retains its existing fixture path; actual player use no longer constructs a temporary analysis VM.

TabletRuntimeRegistry uses per-item UUIDs, rebinds an existing runtime when a carried item object is replaced, separates simultaneously carried copies, expires inactive runtimes after 200 server ticks, and closes on logout/world unload/server stop. World changes take the snapshot handoff path; an actual ServerPlayer dimension round trip is covered below. TabletTerminalMenu checks holder identity, item possession, world, runtime lifetime and machine permissions; keyboard input also requires an attached keyboard.

PlayerTabletSaveMixin flushes active tablet state at the head of Player.addAdditionalSaveData, before Inventory NBT is encoded. Local NeoForge/Minecraft sources confirm PlayerList.remove fires PlayerLoggedOutEvent before saving the player. ItemTossEvent rebinds the runtime to the real dropped stack, saves the newest components and stops it. Inventory.split leaves a zero-count source object: isCarried must reject empty stacks even if their object reference remains in a slot. tablet-drop-red.log failed on the retained runtime before this fix.

SimpleMachine.popSignal now applies registered converters when consuming a signal, matching upstream Machine.Signal.convert. CompoundTag arguments remain raw in the queue and are saved/restored as compound NBT, so nested tablet scan data survives a save without being flattened into strings or lost. Both cases of nbtSignalsConvertWhenConsumedAndSurviveReload failed before the fix (tablet-signal-nbt-red.log).

TabletUseGameTests exercises LuaJ and native short-use/menu creation, real inventory/held-item processing through ServerPlayer.doTick (embedded mock connections do not call it), screen keyboard delivery, current EEPROM data in actual player serialization, native snapshots, long-use sign scans, sneak-stop, logout disposal, replacement-stack rebinding, copy isolation, invalid menus after removal, idle eviction and actual Q-drop preserving data modified after the prior save. The final tablet-use-lifecycle-verified.log passed build, all 2108 unit tests and all 533 GameTests. No client install, push or merge.

Remaining: actual server-restart lifecycle coverage and non-menu automation transfers, and live visual/input acceptance on display3. Death/keepInventory respawn, dimension coverage, writable installation and the component editor are described below. The rack timing diagnosis is recorded above. Do not equate this tested subset with complete tablet or complete port acceptance.

## Writable OpenOS installation - 2026-09-22

TabletInstallGameTests uses a native CPU tier2, two tier2 memory sticks, BIOS, GPU/keyboard and internal HDD tier1 in an assembled tablet with a disk-drive expansion container. It inserts the actual OpenOS floppy through TabletMenu shift-click, opens the terminal via item use, pastes `install --noreboot`, and confirms the shipped installer's own prompt. After installation it stops the tablet, removes the floppy through the editor, checks that only HDD and tmp filesystems remain, and starts again from the installed HDD.

The installed shell must show its writable home, execute `echo installed > /home/install-proof`, then produce the standalone installed line immediately after `cat /home/install-proof`. The output check is scoped to cat so earlier terminal text cannot satisfy readback. This exercises the real bundled installer, filesystem copy, media removal, reboot and write/read path; it does not prepopulate an installed disk. The tablet is assembled through the item assembly API rather than the assembler GUI. Charging uses the normal item API to keep the long fixture powered. No production change was needed for this test. Final tablet-install-final.log: build, 2108 unit tests and all545 GameTests passed. Actual process restart, full assembler-to-client survival flow and visual acceptance remain open. No client install, push or merge.

## Expansion editor - 2026-09-22

Upstream common/container/Tablet.scala exposes only the final component slot. Its type and maximum tier come from the assembled container; built-in CPU/memory/screen and other assembled components are not editable. TabletMenu implements those rules for slot31, including host-specific driver blacklists and screen exclusion. Sneak short-use checks machine ownership, stops/saves the runtime, then opens the editor only above tier1. The server validates holder, world, stopped runtime and possession. Both held-hotbar slot movement and offhand swaps are blocked. Client construction receives the locked inventory index, expansion type and tier through NeoForge's advanced menu packet.

TabletRuntime supplies a one-item expansion inventory; changes rebuild the stopped machine's component network and immediately persist item state. The one-slot layout at80,35 and player inventory at8,84 reuse DiskDriveMenu/Screen through a typed constructor, a player-slot hook and generic screen class. Normal disk-drive behavior remains unchanged. This is implemented and registered, but real-client visuals/input and the slot's visual tier/type cues still require inspection.

TabletEditorGameTests covers tier2 editor opening/stopping, one-slot exposure, wrong-type/higher-tier/screen/blacklisted wired-card rejection, allowed wireless-card shift insertion with exact item counts and live modem attachment, removal/persistence/disconnection, unchanged fixed CPU, main-hand pickup/shift lock, offhand swap lock and invalidation after tablet removal. Tier1 only stops and a tablet without a container rejects expansion items. Embedded connections explicitly advertise advanced_open_screen in these fixtures and the earlier sneak-stop fixture; they do not perform a real NeoForge handshake. An initial test incorrectly chose the intentionally blacklisted wired card, so the fixture was corrected rather than relaxing tablet rules.

Final tablet-editor-final.log: build, 2108 unit tests and all544 GameTests pass. Use: hold a tier2 or creative tablet and briefly sneak-right-click; release before the ten-tick block-analysis threshold. No client/install/push/merge. Actual process restart and full visual acceptance remain open; writable OpenOS installation is covered above.

## Container transfers - 2026-09-22

TabletContainerTransferMixin wraps the server-side AbstractContainerMenu.clicked operation. Before an item can be split/copied, cached inventory and cursor tablets save their current state. Afterwards the registry finds the actual destination by tablet UUID: a player/cursor destination keeps and rebinds the same VM; a container destination is saved there and stopped, its slot marked changed, and the cache entry removed. A failed move leaves the carried VM alive. Client-side container and unload/drop/logout paths do not access the server cache. Creative cursor copies use the existing separate-identity path rather than taking over a carried original.

TabletContainerGameTests uses real ChestMenu clicks and a native EEPROM that writes data after the item was originally assembled. The shift-click and cursor cases first failed with a retained stored VM / lost cursor binding (tablet-container-red.log). Final tests prove same runtime on the cursor, closed old VM in storage, newest component data in chest NBT and a reconstructed chest, stopped fresh runtime after retrieval, and no shutdown/replacement when the chest is full. Full tablet-container-integrated.log: build, 2108 unit tests and all 536 GameTests passed.

This covers normal menu clicks, not every possible external mod or automation extraction path. Remaining: actual server restart, tier-dependent editing, complete tablet OpenOS flow, automation outside menu clicks, and client visual acceptance. The earlier intermittent rack deadline symptom remains open. No client/install/push/merge.

## Death drops and keepInventory respawn - 2026-09-22

Minecraft Inventory.dropAll uses Player.drop(stack, true, false), bypassing ItemTossEvent. PlayerTabletSaveMixin now closes and saves cached tablets at Player.dropEquipment HEAD when keepInventory is false, before equipment can be dropped or removed by vanishing. This prevents the dropped item retaining a live VM and stale component data until cache expiry. With keepInventory true, the existing runtime remains available for rebinding to the respawned player.

TabletDeathGameTests runs native firmware which changes EEPROM data after boot. The ordinary death test failed before the fix with 'Death drop retained live tablet VM' (tablet-death-red.log). It now verifies the actual ServerPlayer.die drop, immediate VM disposal, stopped item state, newest EEPROM data and item NBT roundtrip. The second test uses actual PlayerList.respawn under keepInventory, verifies the same VM is rebound to the new player, and resumes Lua with retained local value 731. The gamerule is restored in a finally block in the same synchronous action. This does not claim coverage for external mods that replace death drops or inventory retention rules.

Full tablet-death-verified.log: build, 2108 unit tests and all 538 GameTests passed. No client/install/push/merge. Server restart, tier2 editing, full tablet OpenOS and visual acceptance remain open, as does the earlier intermittent native rack deadline symptom.

## Actual dimension round trip - 2026-09-22

TabletDimensionGameTests moves the mock ServerPlayer through ServerPlayer.changeDimension into the Nether and back. ServerPlayer.doTick drives the actual inventory hook; the test checks the old VM is disposed before looking up the new cached runtime. Both handoffs retain machine/screen addresses and correct network indexing, keyboard connection, screen contents, native Lua locals, tmp-filesystem identity and the advancing read offset of an open binary file. The restored GPU proxy writes the final result; logout then closes the final VM. No production change was required.

The first test script incorrectly passed both component.list iterator return values (address and type) to gpu.bind; assigning the address first fixed that fixture error. That same run reproduced the separate nativeRackRetainsUptimeDeadlineAfterReload failure at 18:17:04 with marker=waiting, error=null (tablet-dimension-verified.log). Its helper asserts completion after a fixed 60 ticks; whether real-clock executionDelay contributes remains unproven. The final tablet-dimension-final.log passed build, 2108 unit tests and all 539 GameTests, which does not resolve that intermittent failure. This is server runtime acceptance, not a client portal animation or screen3 visual test.

## Original runtime foundation (historical)

TabletRuntime hosts a real SimpleMachine with stable decoded item components, a separate tablet component, integrated 80x25 four-bit ScreenItemEnvironment, keyboard connection, player-relative rotation/position, item charging reconciliation and explicit save/disposal. TabletItem exposes its existing data read/write helpers within its package; assembly data format stays intact. Runtime snapshots live inside oc:tablet/runtime and contain machine, screen and tablet state. Machine.save saves component environments before components are encoded back into the item.

close(true) snapshots before disconnect/disposal and permits a replacement runtime to resume. close(false) stops before saving so ordinary eviction does not restart the tablet. Both close the old VM and remove the private network nodes. This follows the distinction in upstream common/item/Tablet.scala between dimension handoff and ordinary cache eviction. This is not yet wired to actual player/dimension/server events.

Computer node data must load before onHostChanged builds the network. The controlled tablet-address-index-red.log run without this ordering failed both lifecycle cases with 'Tablet computer address not indexed'. Restoring that ordering passed the direct node lookup and resume assertions. A Lua proxy of the machine's own address is not an appropriate index assertion because the component listing excludes its own node; the final test checks the network directly.

## Verified

TabletRuntimeGameTests runs native EEPROM code, binds a GPU to the integrated screen, checks GPU/screen component slots, reads the tablet yaw callback, writes screen text and keeps a local value and binary tmp-file handle. It serializes the entire ItemStack through Minecraft NBT, destroys the old runtime and reconstructs another. The handoff case retains machine/screen addresses, screen contents, local state and the open binary read offset; ordinary eviction restores stopped. A separate test checks actual and simulated ItemStack charging against the live battery with a powered-off screen and stopped machine.

Final tablet-runtime-verified.log: build/test succeeded, 2106 unit tests and all 530 GameTests passed. No client/install/push/merge. These are runtime GameTests, not live client or player lifecycle acceptance.

## Original follow-up list (superseded by current integration)

- Server cache with correct item identity/rebinding, duplicate handling and expiry; inventory ticking and item-use start/stop.
- TerminalMenu integration using existing itemScreen and TerminalNetworking; tier-dependent component editing and delayed block analysis using the live runtime instead of temporary TabletAnalysisHost.
- Actual player save/logout/drop/dimension/server-stop ordering; full OpenOS tablet flow and native lifecycle tests through those hooks.
- Visual/input acceptance in the Modrinth client on display3. Focus coordination remains pending.

One intermediate run also timed out in nativeRackRetainsUptimeDeadlineAfterReload with marker=waiting and no Lua error (17:31:54). Later full runs passed, which does not resolve that symptom. Investigate zero-tick execution-delay versus accelerated game-time deadlines separately; do not call the entire native runtime stable on test counts alone.

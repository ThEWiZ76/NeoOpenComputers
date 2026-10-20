# Block inventory menu access

Runtime 84351b9281e9e29ffb46dd51ba95aad5421acbc1, 2026-10-20. Related: [[RAID facing and overlay geometry]], [[Minecraft test window]].

## Contract and changes

Upstream tileentity/traits/Inventory.scala limits player inventory use to distance squared64 from the block center. Nine port hosts only checked !isRemoved: adapter, assembler, charger, computer case, disassembler, disk drive, printer, rack and relay. RAID, microcontroller and waypoint also accepted another-dimension players at matching coordinates.

BlockInventoryAccess now shares the check for a present player, same level, nonremoved actual current host, distance at most8 and a loaded chunk. The chunk-presence check precedes block-entity lookup. Those nine hosts plus RAID, microcontroller, robot and waypoint use it. Robot keeps its existing player/dimension/range/host behavior and gains the loaded-chunk guard. Client-side SimpleContainer snapshots and unbound waypoint menus keep their existing behavior. Physical-screen terminals, portable item ownership and wireless terminal range remain separate contracts.

Rack server and rack disk-drive menus already delegate to the rack container and therefore inherit its distance/dimension/host checks while retaining their module-identity checks. Existing computer/rack/waypoint payload handlers call menu.stillValid(player); no packet format or handler dispatch changed. AgentInventoryWorldControl's InvWrapper usability gate also calls Container.stillValid, so the integration suite is part of acceptance.

## Evidence

Thirteen initial real-server menu tests ran in a776-test no-optional baseline and produced12 failures: nine distance failures and three dimension failures. Robot's existing range/dimension/host behavior passed. Final sixteen tests cover13 host menus, both actual installed rack module menus and an unloaded-host check for all13 block types. Each bound-menu test checks nearby/exact8/8.125, another dimension, missing player and a live stale object after replacement. The unloaded test uses an unregistered Survival mock player and verifies that no absent chunk becomes loaded.

The first mixed run found three fixture issues: an existing rack test passed null instead of a player, an existing rack-start interaction used a distant default player position, and the new unloaded fixture moved a registered player, itself loading chunks. Existing assertions were retained with actual nearby players; the unloaded fixture now asserts its chunk remains absent immediately after positioning its unregistered player. The running-rack interaction fixture also now stands next to its clicked rack.

Build and2146 unit tests pass. All844 mixed-provider tests with CC/AE2/Mekanism/EnderStorage/ProjectRed and all779 no-optional tests pass in separate fresh oc-tests-menu-access-* worlds; both Gradle/server processes exit normally. Logs: build/finish-port-implementation/menu-access-red-*, menu-access-mixed-first-*, menu-access-mixed-* and menu-access-absent-*. No persistence layout changed and no new two-process continuation claim is made.

## Remaining acceptance

Actual client menu closing and payload rejection across distance/replacement/dimension transitions, external protection, survival and multiplayer acceptance remain open. This checkpoint does not establish every menu or public addon contract. Full-port beta remains3-6 concentrated workweeks, low confidence; counts are not a feature-completion percentage.

The named Modrinth client was launched at the preceding RAID build with an installed hash check, and the user confirmed moving it to screen3. Minecraft MCP connected via IPv6 ::1:8081 and opened testworld. Desktop control is off. pauseOnLostFocus is still true at this checkpoint and the manual F3+P request is pending; no new GPU/visual acceptance is claimed. See [[Minecraft test window]] for current connection and placement evidence.

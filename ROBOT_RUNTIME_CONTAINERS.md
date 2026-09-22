# Robot runtime component containers

Upstream common/tileentity/Robot.scala containerSlotType/containerSlotTier resolve the assembler-installed container driver and its provided slot/tier. Runtime slots 1..3 accept only matching drivers at or below that tier, excluding screens and keyboards. Tool slot and ordinary cargo accept arbitrary item stacks; storing CPU/card/upgrade hardware there does not install it.

The port previously used one broad assembler-hardware exclusion for all twenty mutable slots, so supported runtime cards/upgrades were rejected, empty container slots accepted arbitrary items, and even cargo rejected hardware items. RobotBlockEntity.canPlaceItem now consults the matching assembled container in hardwareItems for slots 1..3 and enforces provided type/tier plus the screen/keyboard exclusion. The generic static prefilter used by quick-move now allows valid nonempty mutable-slot candidates; authoritative per-instance canPlaceItem determines runtime container acceptance. Tool/cargo accept ordinary stored hardware as upstream does.

New real-robot GameTest installs a tier-two card container and tier-one upgrade container, then verifies supported network card/hover upgrade, rejection of too-high GPU tier and wrong type, screen/keyboard exclusion, unavailable third slot, and hardware storage in tool/cargo. robot-container-rules-red.log reproduced rejection of a supported card. An older GameTest expectation banning CPU cargo was corrected to upstream behavior.

This is only the placement-rule stage. Remaining: create/disconnect runtime environments without rebooting Lua, persist component data on removal and reload, stable runtime/assembled slot addressing, one-item component stack rules, screen/client slot type/tier display and authoritative client synchronization. Direct Container.setItem behavior and previously stored invalid contents need deliberate handling; this change does not delete any existing items.

robot-container-rules-final.log passed all 493 GameTests but exposed an obsolete unit test that searched source text for the old hardware ban. Removed that contradictory text-pattern test; the real runtime GameTest now verifies the behavior. Unit-test count consequently decreases by one, intentionally.

Verification: robot-container-rules-verified.log full test/build/GameTest success; 2057 unit tests zero failures/errors and all 493 required GameTests. Artifact SHA256 A94F81591CC341815D5920E3A0DB58CA477F8165396C14294E454F7BBFBB649F. git diff --check clean. Not installed/live-tested; client remains closed.

## Incremental runtime component lifecycle

RobotBlockEntity now owns managed environments for valid single-item runtime slots 1..3 independently of SimpleMachine's assembled hardware rebuild. Insertion loads driver data and connects the node; replacement/removal saves environment data into the source item before splitting/removing it, removes the old node and mapping, and connects the replacement without onHostChanged. Active environments tick when requested. Save writes their state before inventory serialization. Reload and unload tear down old instances; onLoad/reload reconstruct from item data. Robot relocation retains internal connections. Inventory clear disconnects runtime components.

Runtime environment slots occupy MAX_HARDWARE_SLOT_COUNT + zero-based runtime index in componentSlot/getComponentInSlot, avoiding collision with the existing hardware namespace. Broader legacy combined-index reconciliation remains open. Existing direct hardware helpers such as internal tank ordering must be audited for runtime component support.

The menu caps component slots at one item. Oversized stacks injected through direct container APIs remain intact but inactive, avoiding a shared node identity on multiple physical items. Full automation insertion/overflow semantics still require verification.

Real running robot test: insert modem, open port123, remove and reinsert it with identical address and retained port, verify new environment identity and unchanged running machine/architecture, save/load the robot with the port/address retained, then clear inventory and verify disconnection. Additional checks cover menu stack limit and oversized-stack nonactivation. RED robot-hot-swap-red.log reproduced absent modem connection. Prior intermediate full runs passed 494 GameTests; final verification follows below.

Remaining: real Lua coroutine-local continuity and event delivery, other managed upgrade types/tick behavior, runtime tank access and hardware namespace, client type/tier display, direct API oversized insertion handling, multiplayer and live visuals. Tests here prove the modem lifecycle and architecture identity, not full runtime component parity or serialized Lua execution.

Verification: robot-hot-swap-verified.log full test/build/GameTest success; 2057 unit tests zero failures/errors and all 494 required GameTests. Artifact SHA256 99EB43556627CF26AA1B2F34919B9E4264DD9148A5BF248034A657D8A5FBA87F. git diff --check clean. Not installed/live-tested; Minecraft remains closed.

## Real Lua hot-swap continuity and component signals

Added a real EEPROM/Lua GameTest, beyond Java architecture identity checks. EEPROM data records first boot and rejects any reboot. A local Lua table starts at731 and is checked after removal and reinsertion. The script consumes component_added, opens modem port123, consumes component_removed for the exact address, confirms the modem disappears from component.list, then consumes component_added for the same address and verifies the port is still open and the local value reaches733.

The test harness waits for explicit Lua phase colors before each inventory change; no guessed sleep durations or wall-clock assertions. This checks actual VM execution, signal delivery and runtime proxy visibility during a complete remove/reinsert cycle. It does not test saving a live Lua coroutine across world reload, which remains a separate requirement.

The initial test fixture reused a previously booted robot and saved its old architecture boot source before replacing EEPROM hardware, so it kept executing the previous waiting program. The test now constructs an unbooted robot and installs the intended EEPROM before first start. Cached EEPROM source refresh after stopped-machine reload/reprogramming remains a separate issue to investigate.

The corrected fixture then exposed a real failure: Lua received component_added but never component_removed for the modem (robot-hot-swap-lua-fresh.log). NetworkRegistry removes the edge before invoking onDisconnect, so Component visibility Neighbors is already false when SimpleMachine checked it. SimpleMachine now remembers visible component identities from connection notifications and uses that prior visibility for removal, clearing entries on disconnect. Two focused unit tests cover exactly one removal notification for a directly visible neighbor-only component and no notification for an unseen component beyond a bridge.

Verification: robot-hot-swap-lua-signals.log full test/build/GameTest success; 2059 unit tests zero failures/errors and all 495 required GameTests. Real Lua reaches the final733 sentinel with no reboot and preserved modem port/address. Artifact SHA256 63ECD8DF84A6E8E5BDA78480C4EF2AB773BE78928044BC22E7BCE1751930828E. git diff --check clean. Not installed/live-tested; Minecraft client remains closed.

## Runtime tank access and emptied-tank persistence

The robot tank view previously filtered only raw assembled-hardware slot numbers. Added connected runtime IFluidTank environments in equipment-slot order before the assembled tanks, matching upstream component-array enumeration. Removed runtime tanks disappear immediately and assembled tank order remains stable.

Runtime environment persistence now passes the driver's existing data tag to environment.save rather than merging a fresh tag afterwards. This lets an environment explicitly remove obsolete keys. TankUpgradeEnvironment.save removes fluid/amount keys when empty, preventing an emptied, previously saved tank from resurrecting old fluid on reinsertion. Other item-data keys remain intact.

Real robot test installs one assembled tank plus one runtime tank, verifies tank count/order and removal, fills exactly1000mB water, removes/reinserts with exact content preserved, drains all1000mB, then removes/reinserts again expecting zero. robot-runtime-tank-red.log reproduced missing runtime tank access. Existing modem hot-swap/reload tests also cover the changed runtime save path.

Verification: robot-runtime-tank-final.log full test/build/GameTest success; 2059 unit tests zero failures/errors and all 498 required GameTests. Artifact SHA256 578200B62F1770E60138AF76C9342E4E4A19740ECE36AA6AF227AB35359D918D. git diff --check clean. Not installed/live-tested; client remains closed.


## 2026-09-22 client container metadata

Robot menu opening data now includes the three assembled container type strings and full VarInt tiers. These containers are fixed during normal robot use, so metadata is sent when opening; runtime contents continue using vanilla slot synchronization. Arbitrary driver slot names and Integer.MAX_VALUE tiers are preserved without short truncation. No assembled inventory contents are exposed.

RobotBlockEntity.RuntimeSlot shares the existing acceptance rules with the client menu. RobotScreen uses per-instance descriptors for overlays and tooltips: missing containers use none/-1 (the existing unavailable icon), installed containers use their actual type/tier. Client prediction rejects wrong type/tier and screen/keyboard; server remains authoritative. Existing invalid items remain removable. Live changes to assembled hardware via external commands while a menu is open are not synchronized; reopen the menu after such changes.

The new real-world GameTest serializes the actual opening buffer into a client menu for card/upgrade/missing containers and a floppy container with unlimited tier. It verifies expected metadata, client/server placement parity across seven item types, cargo access, one-item cap, and complete payload consumption.

Verification: robot-client-containers-final.log, full test/build/runGameTestServer success, 2059 unit tests and 500 required GameTests. Artifact SHA256 CA494386F1E59AD80FEA13FF7F0DC2264E630B39B6017C6C65FBCB680CCD7AA3. Not installed or visually tested; Minecraft remains closed pending safe screen3 placement.

Separate observed defect: test-server chunk loading logs Failed to load data for block entity neoopencomputers:rack. ServerRackMountableEnvironment.load:446 invokes ContainerHelper.loadAllItems with a null registry provider while RackBlockEntity.loadAdditional creates mountables before the rack has a level. This survives a green GameTest summary and needs a dedicated reproduction/fix next; no complete-port claim.

## Generator fuel conservation and removal ordering - 2026-09-22

GeneratorUpgradeItem now restores and saves its environment in item driver data. Two actual robot regressions failed before the fix: native reload lost the generator proxy, and removing/reinserting a mutable generator duplicated the three coal it had dropped (generator-persistence-red.log). Reload now retains the old proxy and queue of two coal after one starts burning, with empty cargo and no coal drops. The synchronous removal test inserts three coal, removes the upgrade, checks exactly three world drops, reinserts it and checks an empty queue/cargo.

Upstream ComponentInventory.onItemRemoved removes the node before saving, letting disconnect callbacks clear fuel and close handles. RobotBlockEntity.detachRuntimeComponent and SimpleMachine.onHostChanged now follow that order. The existing unit test now asserts that save happens after disconnect. Two older modem hot-swap tests incorrectly required ports to stay open after physically removing the card; upstream NetworkCard.onDisconnect explicitly clears them. Updated tests preserve address/Lua state, require closed ports after removal, reopen the port, and still require port preservation through ordinary NBT reload. This supersedes earlier claims of port preservation through item removal; see Robot runtime containers.

The first run also exposed a test-isolation problem: the chunk-ticket helper counted tickets from neighboring owners. It now reads NeoForge's saved ticket data and matches the exact chunkloader controller and owner BlockPos. Final generator-persistence-final.log: build/2109 unit tests/all560 GameTests passed. No client launch/install/push/merge. Actual process restart, all other hosts and full upgrade/visual parity remain open.

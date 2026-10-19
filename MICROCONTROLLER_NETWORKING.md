# Microcontroller networking

Related: [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Port gap triage]], [[Network addresses]], [[Process restart test]].

Previous assembled-item acceptance, 2026-10-20: runtime d7a8cb73490e035718b5e95c1514817212e94d1c passes build, 2131 units and all 712 ordinary GameTests with ProjectRed and ComputerCraft together. Evidence: microcontroller-item-roundtrip-verified.log under build/finish-port-implementation/. This is server acceptance for the tested paths below, not complete port or real-client acceptance.

Previous rotation/restart production checkpoint156db2b6b passed build, 2131 units, 708 ordinary GameTests and both 711-test processes with ComputerCraft in oc-restart-microcontroller-20261020-01. Prepare PID58500 and verify PID56588 both shut down normally. Logs: microcontroller-rotation-verified.log and microcontroller-process-restart-{prepare,verify}.log. That two-JVM pair is evidence for its documented revision, not a new restart run of d7a8cb734.

Earlier core implementation checkpoint be5ceb8c3babae9d39d4959388f11ea446e43961 passed build, 2131 unit tests and all 707 ordinary GameTests (microcontroller-network-verified.log).

## Immutable assembly and player interactions - 2026-10-20

Runtime bc17524aeb24e2c1bebcbab3f3579661754a671d: build and 2131 unit tests pass; all 718 ordinary GameTests pass with ProjectRed and ComputerCraft together. Fresh world oc-restart-microcontroller-immutable-20261020-01 passes all 721 tests in prepare PID16144 and verify PID44436, both with normal shutdown. Logs: microcontroller-immutable-verified.log and microcontroller-immutable-restart-{prepare,verify}.log under build/finish-port-implementation/. This is tested server behavior, not complete port or client acceptance.

Upstream common/tileentity/Microcontroller.scala rejects normal inventory mutations; common/block/Microcontroller.scala toggles power on normal activation and changes EEPROM on sneak-held-chip activation. ComponentInventory changes only the affected environment. Three regressions reproduced editable hardware, missing normal-click power and missing sneak EEPROM swap in microcontroller-immutable-interactions-red.log.

The assembled CPU/RAM/cards are now locked through Container mutation, inventory wrappers and menu pickup/shift-click. The dedicated EEPROM swap consumes one held chip and returns the old chip after flushing its latest data. SimpleMachine tracks the source stack of each environment and replaces only that EEPROM environment; the native VM, locals and unrelated modem state survive. Machine adds a default onEepromChanged hook that falls back to onHostChanged for custom machine implementations; that fallback does not promise hot-swap continuation for third-party engines.

Normal click toggles power, applicable wrenches remain delegated, and sneak empty-hand opens the existing modern menu as read-only inspection. That inspection UI is an extension, not an original upstream GUI. It requires the same block entity within eight blocks. A regression reproduced distant menu access before the range fix (microcontroller-inspection-red.log). Offhand EEPROM swap with a full main inventory drops exactly one old chip with its data intact and leaves the inventory unchanged. Five new interaction tests include a real native program that continues after hot-swap and subsequently boots the replacement code on restart. These dispatch real block/menu callbacks with Survival mock players; they do not establish live packet/protection-mod or multiplayer acceptance.

Existing fixtures now install immutable hardware through serialized assembly loading instead of the public Container setters. Slot-mapping fixtures still check the layout/driver contract; the separate interaction regression verifies actual mutation rejection. The old card-removal redstone fixture is now serialized assembly replacement, not player-removable hardware.

A new real chunk-edge regression independently reproduced the vanilla redstone sampler loading its unloaded east neighbor (microcontroller-input-boundary-red.log). Sampling skips an unloaded first-hop neighbor and retains its last known vanilla value. The original live suite and an inspection-red run also entered an active ChunkMap.processUnloads loop; logs/thread dumps are retained. The guarded build and both restart processes finish normally. The sampler reproduction proves an unsafe lookup, not the exact cause of every prior hang, and the guard does not constrain third-party signal implementations.

Still open: complete assembler GUI/energy timing, real-client GUI/analyzer/cable visuals, full Survival/protection/multiplayer flow, arbitrary configuration/reference migration, wider platforms and load/crash acceptance. Beta estimate remains 3-6 concentrated workweeks with low confidence; full semantic/config/resource/API and integration scope audits remain open. No client launch, Modrinth change, Computer Use or window movement occurred in this checkpoint.

## Assembled-item pickup and replacement - 2026-10-20

Upstream common/block/Microcontroller.scala uses CustomDrops, saves components and stores integer snooper energy in MicrocontrollerData. The port instead dropped four hardware stacks plus a plain chassis. Its pick-block item had no assembled payload. Both failures were reproduced in microcontroller-item-roundtrip-red.log. A genuine assembler-template output placed and booted successfully before the break failure, so initial assembler hardware placement was not the missing feature.

MicrocontrollerBlock now returns one assembled item from normal loot, pick-block and Creative destruction. Removing a microcontroller no longer separately scatters its container contents. MicrocontrollerBlockEntity flushes live component data and builds a minimal BLOCK_ENTITY_DATA payload with Items and oc:storedEnergy; if loot runs after removal, it uses the preserved unload snapshot. Stored energy truncates to an integer, matching upstream (317.75 becomes317). Component hardware IDs/data and CPU architecture stay on the hardware. No oc:machine, snooper/plug/control IDs, output flags, VM heap or tmp filesystem is carried in the item. Placement creates a fresh stopped machine/network that can boot its preserved EEPROM.

The first implementation exposed another real placement defect: BlockItem merges the item payload into a new block entity's default NBT, which already has an empty snooper. That empty saved buffer overrode the portable energy. The loader now gives oc:storedEnergy priority during this item load; ordinary world saves still serialize oc:snooper and do not write the portable field. Red evidence: microcontroller-item-placement-energy-red.log. Existing legacy computer-node buffer loading remains supported.

Three new tests cover a tier-one Survival player destroying and replacing an actual assembled device, tier-two pick/Creative destruction and creative-tier non-player destruction/replacement. They check exactly one assembled drop, tier/hardware/custom-name preservation, consumption of one item on real placement, portable energy, no runtime-state payload, original EEPROM/modem identities, latest binary EEPROM bytes and fresh native boot after replacement. The native program changes binary markers from seed to first to second; a copied original seed cannot pass. Survival mock-player cleanup runs on success and failure. Fixture assembly uses the real template with immediate completion; this does not establish assembler GUI/energy timing or real-client player acceptance.

Immutable hardware and click/sneak-EEPROM flow are covered by the newer interaction checkpoint above. Complete assembler/survival/multiplayer/client acceptance, arbitrary reference/custom-config migration and crash/platform/load matrices remain open.

## Power/config and provider acceptance - 2026-10-20

Test checkpoint 7cd2fa09d14afb49722fe7b68a51a181cafb2654 adds one focused microcontrollerPower batch; production behavior is unchanged. Build and 2131 units pass with ProjectRed. Both the ProjectRed-only and ProjectRed+ComputerCraft ordinary runs pass all 709 required GameTests. Logs: microcontroller-power-projectred-verified.log and microcontroller-power-projectred-cc-verified.log under build/finish-port-implementation/.

The installed native CPU/RAM/EEPROM host has custom private capacity64, generic computer cost9, microcontroller cost0.25 or0, and tick frequency4. The fixture manually advances a host not registered for natural world ticks, so exactly twelve running updates and four paused updates determine consumption independently of wall time or neighboring fixtures. It verifies the private capacity/initially empty buffer, the separate cost setting, running and paused energy, no stopped consumption, zero-cost operation, ignorePower with an empty buffer, atomic NoEnergy failure without partial drain, and legacy energy317 bounded to configured capacity64. All temporary settings restore in finally blocks; the host is removed and its native VM/nodes disposed.

These focused configuration/power cases and current ProjectRed/mixed-provider regressions are accepted. This is not proof of arbitrary live config reload, all historical custom-capacity/reference migration, every platform or visual/multiplayer behavior. The prior actual two-JVM pair remains evidence for production checkpoint156db2b6b; this newer checkpoint changes tests only.

## Rotation checkpoint - 2026-10-20

Runtime 156db2b6b093fea856337e6dd69b340bf196452c fixes a reproduced post-placement rotation defect: the formerly open plug/control component stayed attached when its face became the new front. MicrocontrollerBlockEntity now responds to a facing-state change by removing the new front plug/control, rejoining open sides and their real neighboring networks, and invalidating sided energy capabilities. The private machine/snooper network is retained.

The new physical fixture surrounds the microcontroller with six actual cable blocks and rotates through east/south/west/north. It checks removal of the new front nodes, one external control component on every remaining side, cable edges and actual world connection geometry, sided FE availability and retained machine identity/energy731. Build, 2131 units and all 708 ordinary GameTests pass; microcontroller-rotation-verified.log. The original failure is microcontroller-rotation-front-red.log.

The geometry extension initially read the cable block entity's cached state. Its diagnostic showed cached up=none while the authoritative world blockstate already had up=device on the same cable instance. The fixture now reads the world state used for rendering. The retained shape/cache diagnostic logs distinguish this test-observation issue from the reproduced front-network defect; they are not proof of a remaining world-renderstate failure. Actual visual client acceptance is still separate.

A new opt-in MicrocontrollerProcessRestartGameTests joins the existing processRestart batch. It keeps a native Lua local, internal control proxy/output flag, modem proxy/open port and open tmp binary-file offset. A pre-tick ChunkDataEvent.Load observer copies raw saved energy/flags without querying a world. The fixture also records the machine and all five external plug/control IDs, and verifies the isolated restored networks. Test power starts on the first world tick, while raw disk observation prevents it masking an absent saved buffer. It requires a fresh isolated world and a second JVM. This probe is absent in ordinary runs.

The actual two-JVM pair above passes this microcontroller probe, the existing native computer/external-floppy/tablet probe and the four-host redstone probe with real CC sources. The verification log explicitly confirms pre-tick disk energy/flags on the microcontroller and pre-tick disk input arrays on all four redstone hosts. These are actual world/player loads after normal process shutdown, not an NBT-only in-memory reconstruction. The probe verifies stored positive energy, not an exact running-consumption/config matrix or crash recovery.

## Original reproduced gaps

The port returned machine.node() as its shared external node, exposing internal hardware on the outside. Analyzing an external face returned a computer component, with none of the six upstream microcontroller callbacks. Two GameTests reproduced these failures before implementation: microcontroller-side-networks-red.log.

Upstream references are common/tileentity/Microcontroller.scala, common/tileentity/traits/Hub.scala and util/ExtendedArguments.scala at master-MC1.12 revision 7d9eac5584e238a890a388a0f697c27c7391caa4. Microcontroller overrides Hub.onPlugMessage without calling its superclass, so it does not passively queue/relay packets between external networks. Forwarding requires its internal programmed modem.

## Implemented behavior

MicrocontrollerBlockEntity implements SidedEnvironment and returns null from its common node(), matching upstream. The front has no network or FE connector. Each other face has its own plug network; installed hardware and the computer are on a private network with a microcontroller snooper component/connector. MicrocontrollerNetwork owns this topology and its persistence.

Each external network has one primary plug/control component, including after merge and split. External control does not expose the internal EEPROM, modem or computer. The analyzer shows the computer on the front and the side's microcontroller component on other faces. A non-primary side's analyzer component is deliberately detached, matching the upstream per-side component model.

| Callback | Contract |
| --- | --- |
| start() | Returns whether start changed state; does not resume a paused machine |
| stop() | Returns whether stop changed state |
| isRunning() | Direct callback reporting the internal machine state |
| lastError() | Direct callback returning the machine crash reason or nil |
| isSideOpen(side) | Direct callback; reads an outbound flag using world-side 0..5 |
| setSideOpen(side, open) | Changes the outbound flag and returns its previous value |

Values outside 0..5 raise invalid side; the front raises unsupported side. Closing an output does not remove its physical control connection or block inbound packets. Inbound network.message packets go to the private network, once per external network. Internal packets go through all open non-front outputs. The original Packet object/source/payload/TTL is retained. There is no passive external-to-external relay or hub queue/TTL hop.

Power uses power.buffer.mcu (default1000), power.cost.microcontroller (default0.1) and case-tier-one FE throughput. The machine's own connector has zero local capacity; new microcontrollers start with an empty private buffer. Every normal tick interval, the plugs draw available external network energy into the private network, recomputing demand for each plug. Two plugs sharing one external energy pool cannot duplicate it. Receive-only sided FE uses the same private buffer; simulation does not mutate it.

Persistence stores oc:plugs, oc:componentNodes, oc:snooper and oc:outputs alongside the existing machine/hardware state. Save allocates missing node identities without leaving detached nodes connected. Unload snapshots precede detaching all private/external nodes and closing the machine. Detached loading and loading the same instance reconnect the snooper before native architecture restoration; component hardware rebuilds also retain its connection.

Earlier port saves lack oc:snooper and stored energy in oc:machine.node.buffer. Loading transfers that value into the new private buffer, bounded by the configured microcontroller capacity just like other Connector loads. Default old capacity500 fits the new default1000. A custom old energy value above the new configured capacity is clamped; no arbitrary custom-capacity migration is claimed. The machine/hardware IDs are preserved internally. Old external references to the exposed computer/hardware must target the new analyzed control component; preserving the previous hardware leak would defeat the upstream network separation.

## Network lifecycle prerequisite

The initial wiring exposed callback ordering defects in NetworkRegistry: split notifications arrived before the nodes moved to their separate networks, and removal notifications arrived while the removed node was still attached. A callback creating a replacement control component could act on obsolete topology. Two focused unit regressions failed in network-topology-callback-order-red.xml.

Splits now move the graph before notifying hosts. Removal detaches the node and partitions all remaining subgraphs before notifications; callbacks can safely inspect the final topology and attach replacement components. Existing routing, component signals, hotplug, chunkloader and persistence regressions all pass in the full suite. This follows upstream's topology-before-callback ordering.

## Evidence and remaining acceptance

Six new GameTests cover separated physical side nodes/private hardware, single-primary merge/split, callback results/scheduling/errors, closed-output input, no passive relay, unchanged packet/TTL, shared-network deduplication, actual FE capability/simulation/throughput, external energy transfer, detached/same-instance restoration, plug/control/private identity and legacy energy317.

One of those six boots two real native Lua machines with installed modems. An external case discovers the control component without seeing the microcontroller's EEPROM, closes its west output, starts it, receives ready, sends ping, receives pong and stops it. Power reaches the private buffer from their external network. A west observer receives no packets. Native MicrocontrollerPersistenceGameTests additionally retain the internal microcontroller proxy and a closed output flag through all three existing continuation scenarios, along with locals, temporary binary-file offset and running state. Their fixtures now supply explicit test power; microcontroller-empty-power-fixtures-red.log preserves the three expected not-enough-energy failures before this fixture correction.

Still required: real-client analyzer/GUI/cable rendering and complete assembler/survival/multiplayer interactions. Assembled-item break/pick/replace and immutable hardware/click/sneak EEPROM callbacks have the newer server checkpoints above. Earlier new-topology process restart, provider regressions, focused running-power/config cases and four-state rotation retain their documented revision evidence. Broader addon, arbitrary live-config changes, legacy reference migration, crash recovery and platform/load matrices remain open.

No Minecraft client was launched or controlled, no Modrinth profile changed, and no desktop window moved for this checkpoint.

## How to run/use this

Build and run the integrated tests from M:/development/OpenComputers:

~~~powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer --no-daemon --console=plain
exit $LASTEXITCODE
~~~

Only one Gradle process may run at a time. For an actual restart, use a fresh isolated world as described in [[Process restart test]]; the phase now adds the explicit microcontroller scenario beside the original computer/tablet probe.

In Lua, component.proxy(component.list("microcontroller")()) supplies the control component. A computer must be connected to a non-front face and have enough power. The microcontroller also needs valid assembled CPU/RAM/EEPROM hardware and an internal modem for packet handling. Use world-side numbers for output flags; changing a flag controls output only.

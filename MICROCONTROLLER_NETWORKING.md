# Microcontroller networking

Related: [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Port gap triage]], [[Network addresses]], [[Process restart test]].

Runtime checkpoint be5ceb8c3babae9d39d4959388f11ea446e43961, 2026-10-20. Build, 2131 unit tests and all 707 ordinary GameTests pass. Evidence: build/finish-port-implementation/microcontroller-network-verified.log. This is server acceptance for the implemented paths below, not complete port or real-client acceptance.

## Reproduced gaps

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

Still required: actual process-restart acceptance for this new microcontroller topology/native program, optional-provider regression runs, post-placement rotation/physical cable reconnection checks, real-client analyzer/GUI/cable rendering and survival/multiplayer flows. Existing port container/player editing behavior is unchanged and is not established as full upstream immutable-assembly parity by these network tests. Broader addon, legacy custom-capacity/reference migration and platform/load matrices remain open.

No Minecraft client was launched or controlled, no Modrinth profile changed, and no desktop window moved for this checkpoint.

## How to run/use this

Build and run the integrated tests from M:/development/OpenComputers:

~~~powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer --no-daemon --console=plain
exit $LASTEXITCODE
~~~

Only one Gradle process may run at a time. For an actual restart, use a fresh isolated world as described in [[Process restart test]]; the existing generic probe alone does not prove the new microcontroller scenario.

In Lua, component.proxy(component.list("microcontroller")()) supplies the control component. A computer must be connected to a non-front face and have enough power. The microcontroller also needs valid assembled CPU/RAM/EEPROM hardware and an internal modem for packet handling. Use world-side numbers for output flags; changing a flag controls output only.

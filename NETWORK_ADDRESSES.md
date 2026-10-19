# Network address generation

Latest acceptance, 2026-10-20: checkpoint c125614e99c802ab724818be2548433b2bef25ed passes build, 2129 unit tests, 701 ordinary GameTests and both 702-test processes of a fresh restart pair. Connected-node restoration and chunkloader lifecycle details follow below. Earlier checkpoint 9a1df219b fixed reproduced non-loading discovery failures in network/cable/screen/adapter paths and recovered the original hanging restart world; see [[Loaded chunk discovery]]. The failed attempt below remains historical diagnostic evidence.

Related: [[../Project]], [[../02 plans/Complete portering]], [[Process restart test]], [[../04 ai-context/Port gap triage]].

## Connected-node address restoration - 2026-10-20

Runtime checkpoint c125614e99c802ab724818be2548433b2bef25ed fixes another reproduced load defect. BaseNode.load changed a connected node's address without updating its wired network's address map. Direct routing by the restored address failed, and removal could leave stale map entries. Three deterministic unit regressions failed before the change; their original report is network-connected-remap-red.xml under build/finish-port-implementation/.

The network now resolves address conflicts before detaching the node, preserving an existing address owner. It removes the restoring node with normal disconnect notifications, restores its address, joins it again and reconnects its previous neighbors. This follows the upstream remove/rejoin remap model. Blank and incorrectly typed stored addresses leave the current valid identity intact. Tests verify direct routing, address lookup, collision ownership, chain topology and cleanup after removing the restored bridge.

The full suite then reproduced a chunkloader lifecycle interaction: restoring the parent machine's address transiently disconnected its upgrade and cleared the restored active flag. ChunkloaderUpgradeEnvironment now separates the persisted activation choice from actual tickets: detaching releases tickets, reconnecting reacquires them, and detached upgrades cannot reacquire tickets or drain power during updates. Explicit computer stop and energy exhaustion still disable the upgrade. The strengthened server fixture attaches a real Machine node and checks all nine actual ticking tickets through detach, reconnect, address restoration and disabling. The existing native robot persistence test also passes without weakening its restored-ticket checks.

Build, all 2129 unit tests and all 701 ordinary GameTests pass. Log: network-connected-remap-verified.log. The focused chunkloader red report is chunkloader-detach-active-red.xml; the first full-suite robot failure is network-connected-remap-chunkloader-red.log. Subsequent fixture failures, chunkloader-unattached-fixture-red.log and chunkloader-host-network-fixture-red.log, are retained separately: the original synthetic host had no machine, and the replacement fixture initially omitted joining its new machine node to a network.

Fresh world oc-restart-remap-20261020-01 passes all 702 required GameTests in prepare PID34520 and verify PID48236, both normal shutdowns. The second process confirms native Lua locals and an external open binary-file offset, plus the saved tablet/player state through actual world/player loading. Logs: network-connected-remap-restart-prepare.log and network-connected-remap-restart-verify.log. This wider restart probe does not independently exercise every host/addon remap case.

This establishes these connected-load and upgrade lifecycle paths. It does not repair duplicated legacy IDs across separate saved networks, migrate their references, or prove all addon/component cache implementations and unload orders. It is not full chunkloader parity: upstream's dimension whitelist/blacklist policy and its blocked-dimension error have no corresponding port setting yet. Microcontroller network separation remains a separate implementation task.

## Reproduced gap

NetworkRegistry generated node-1, node-2, etc. from a per-process counter, checking collisions only within the receiving wired network. After a restart, a loaded node-1 on one network and a newly created node on a different network could both have node-1. Global device registries, including TerminalServerRegistry, cannot distinguish those addresses.

The deterministic unit reproduction loads a node with persisted address node-1 into one network, then creates a fresh node on a separate network in the same newly initialized registry. It failed because both addresses were node-1. Evidence: build/finish-port-implementation/network-address-restart-red.xml. This establishes an address-reuse bug independently of the intermittent terminal GameTest failure observed during item-inventory work. That terminal failure's pattern is consistent with the global collision, but the original failing run did not record enough state to attribute its exact instance retrospectively.

## Change

New addresses use UUIDs, matching upstream server/network/Network.scala, instead of a restartable counter. Existing persisted addresses are not renamed by this change. A package-private UUID supplier lets focused tests use deterministic values; production uses UUID.randomUUID. Wired-network collision checks remain.

The terminal snapshot GameTest now checks that the new terminal-server address resolves to its current instance and that its binding survives a screen update, before checking the resulting menu. The focused address test also proves that the loaded legacy node-1 address is preserved.

Runtime checkpoint df9151c8ca0b95365262220aeeba0abb2ec0d4da passes the ordinary integrated build, 2122 unit tests and all 699 mandatory GameTests. Evidence: network-address-default-verified.log.

The two-process acceptance attempt is incomplete. In world oc-restart-address-20261020-01, prepare PID37536 passed all 700 tests and exited normally. Verify PID59180 stopped advancing during defaultBatch:13, before the processRestart batch. Two thread dumps show the server thread continuously active in ChunkMap.processUnloads (queue polling and CompletableFuture completion), not Lua. Source inspection shows scheduleUnload can requeue itself when isReadyForSaving is false while processUnloads continues draining. After preserving the log and both dumps, only the identified owned test-server process was terminated; Gradle session24200 then exited -1. This is not a successful verify or proof of a Lua/address defect. Files: network-address-restart-prepare.log, network-address-restart-verify-hang.log and network-address-restart-verify-threads{,-2}.txt under build/finish-port-implementation/.

The follow-up hypothesis was unguarded world lookups loading chunks during unload. At df9151c8c, NetworkRegistry.adjacentNode called world.getBlockEntity on adjacent positions without a loaded check. Focused reproductions subsequently proved those accesses, sided callback ordering, cable shape-update propagation and adapter scanning; the resulting changes are documented in [[Loaded chunk discovery]]. [NeoForge issue 1403](https://github.com/neoforged/NeoForge/issues/1403) reports the same unload-loop mechanism; its author traced their case to getBlockEntity without checking isLoaded. That is supporting context, not retrospective stack evidence for every lookup in our original occurrence.

This prevents newly generated addresses from repeating old sequential IDs. It does not repair already duplicated legacy addresses across separate saved networks or provide a world migration tool. Connected-node loading is now covered by the later checkpoint above; wider compatibility remains acceptance work. Do not rewrite saved addresses without preserving their references.

## How to run/use this

From M:/development/OpenComputers:

```powershell
rtk proxy cmd /d /c gradlew.bat test --tests li.cil.oc.common.NetworkRegistryTest --no-daemon --console=plain
exit $LASTEXITCODE
```

Run the ordinary build/GameTests and the two-process probe described in [[Process restart test]] before accepting runtime persistence changes. Only one Gradle process may run at a time. No client launch is required by these server commands.

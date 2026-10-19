# Network address generation

Latest acceptance, 2026-10-20: checkpoint 9a1df219b10f0a58dd60e29988dd57ca8bea3d1a also fixes reproduced non-loading discovery failures in network/cable/screen/adapter paths. Build and 2125 units pass. A fresh prepare/verify pair passes 702 GameTests in both processes; the original world that hung below also passes 702 after the fix, including its retained native continuation probe. See [[Loaded chunk discovery]] for logs, exact PIDs, changes and remaining limits. The failed attempt below is historical diagnostic evidence, no longer the latest acceptance result.

Related: [[../Project]], [[../02 plans/Complete portering]], [[Process restart test]], [[../04 ai-context/Port gap triage]].

## Reproduced gap

NetworkRegistry generated node-1, node-2, etc. from a per-process counter, checking collisions only within the receiving wired network. After a restart, a loaded node-1 on one network and a newly created node on a different network could both have node-1. Global device registries, including TerminalServerRegistry, cannot distinguish those addresses.

The deterministic unit reproduction loads a node with persisted address node-1 into one network, then creates a fresh node on a separate network in the same newly initialized registry. It failed because both addresses were node-1. Evidence: build/finish-port-implementation/network-address-restart-red.xml. This establishes an address-reuse bug independently of the intermittent terminal GameTest failure observed during item-inventory work. That terminal failure's pattern is consistent with the global collision, but the original failing run did not record enough state to attribute its exact instance retrospectively.

## Change

New addresses use UUIDs, matching upstream server/network/Network.scala, instead of a restartable counter. Existing persisted addresses are not renamed by this change. A package-private UUID supplier lets focused tests use deterministic values; production uses UUID.randomUUID. Wired-network collision checks remain.

The terminal snapshot GameTest now checks that the new terminal-server address resolves to its current instance and that its binding survives a screen update, before checking the resulting menu. The focused address test also proves that the loaded legacy node-1 address is preserved.

Runtime checkpoint df9151c8ca0b95365262220aeeba0abb2ec0d4da passes the ordinary integrated build, 2122 unit tests and all 699 mandatory GameTests. Evidence: network-address-default-verified.log.

The two-process acceptance attempt is incomplete. In world oc-restart-address-20261020-01, prepare PID37536 passed all 700 tests and exited normally. Verify PID59180 stopped advancing during defaultBatch:13, before the processRestart batch. Two thread dumps show the server thread continuously active in ChunkMap.processUnloads (queue polling and CompletableFuture completion), not Lua. Source inspection shows scheduleUnload can requeue itself when isReadyForSaving is false while processUnloads continues draining. After preserving the log and both dumps, only the identified owned test-server process was terminated; Gradle session24200 then exited -1. This is not a successful verify or proof of a Lua/address defect. Files: network-address-restart-prepare.log, network-address-restart-verify-hang.log and network-address-restart-verify-threads{,-2}.txt under build/finish-port-implementation/.

The follow-up hypothesis was unguarded world lookups loading chunks during unload. At df9151c8c, NetworkRegistry.adjacentNode called world.getBlockEntity on adjacent positions without a loaded check. Focused reproductions subsequently proved those accesses, sided callback ordering, cable shape-update propagation and adapter scanning; the resulting changes are documented in [[Loaded chunk discovery]]. [NeoForge issue 1403](https://github.com/neoforged/NeoForge/issues/1403) reports the same unload-loop mechanism; its author traced their case to getBlockEntity without checking isLoaded. That is supporting context, not retrospective stack evidence for every lookup in our original occurrence.

This prevents newly generated addresses from repeating old sequential IDs. It does not repair already duplicated legacy addresses across separate saved networks, provide a world migration tool, or establish all connected-node address-remapping behavior. Those cases remain compatibility acceptance work; do not rewrite saved addresses without preserving their references.

## How to run/use this

From M:/development/OpenComputers:

```powershell
rtk proxy cmd /d /c gradlew.bat test --tests li.cil.oc.common.NetworkRegistryTest --no-daemon --console=plain
exit $LASTEXITCODE
```

Run the ordinary build/GameTests and the two-process probe described in [[Process restart test]] before accepting runtime persistence changes. Only one Gradle process may run at a time. No client launch is required by these server commands.

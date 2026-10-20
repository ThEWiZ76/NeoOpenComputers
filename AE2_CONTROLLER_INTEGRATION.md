# AE2 controller integration

Runtime checkpoint: 5b60fee0b7577cd2a95c831969903a9237e695eb. Related: [[../02 plans/AE2 integration]], [[../02 plans/Complete portering]], [[../04 ai-context/Modern integration scope]].

This is the first AE2 implementation checkpoint, not the complete integration. Pinned target: AE2 19.2.18 on Minecraft1.21.1/NeoForge21.1.234. Uses the publisher's Maven Central API artifact; all foreign linkage stays behind an installed-mod guard. No appeng classes are bundled and AE2 is absent from the published POM dependencies.

## Implemented contract

- Real controller adapter component me_controller, NamedBlock priority5. Resolve current AE2 grid via IN_WORLD_GRID_NODE_HOST; do not force-load chunks. Removed/replaced/unloaded original controller yields no controller; missing grid node yields no ae grid. Generic capability callbacks may coexist on the compound adapter.
- getCpus: name/storage/coprocessors/busy maps from the live crafting service.
- getItemsInNetwork(filter): converted item descriptions with long size and isCraftable, stored keys plus craftable-only item keys with zero stored quantity. Recursive subset filters handle Lua UTF-8 strings and one-based table/array entries, retain numeric precision and reject fractional quantity mismatches.
- getFluidsInNetwork: converted fluid descriptions; AEFluidKey uses1000 units per bucket, so amount is directly millibuckets. Long amounts overwrite the bounded temporary FluidStack description amount.
- store(filter,dbAddress,startSlot,count): defaults empty filter, first reachable database, slot1, unlimited count. Skip occupied entries, advance after each copy, stop at bounds/full database and never extract network items or create physical database ownership. Addressed invalid/missing/non-database errors are no such component/not a database; automatic lookup failure is no database upgrade found; slot outside1..size is invalid slot. Nonpositive count copies nothing. ItemStack-backed database quantity is bounded to the int representation, while network listing retains long size.
- getAvgPowerInjection/getAvgPowerUsage/getIdlePowerUsage/getMaxStoredPower/getStoredPower/isNetworkPowered/getEnergyDemand delegate to live energy service. getEnergyDemand consumes1.5 call budget. Callbacks are synchronized server-thread operations, not direct worker-thread calls.

## Verification

- Missing integration reproduced before production edits: real controller exposed generic energy_device; native Lua could not find me_controller (ae2-controller-red.log).
- Two focused filter units plus existing suite:2140 unit tests, zero failures/errors. Large integral quantities do not wrap or compare equal to truncated/fractional values.
- Real controller, drive with real item/fluid cells, energy cell and1k crafting CPU: exact item counts123stone/7diamonds,1500mB water, filters, live CPU/energy snapshots and1.5budget; database occupied/full/zero-count/addressed/filter/bounds/errors, no physical ownership or changed ME contents; removed controller guard.
- Native Lua in a real case, physical adapter and separately powered AE2 drive: discovers controller and filters actual stored items, including fractional non-match, reads energy/CPU/fluid APIs and completes EEPROM marker.
- build plus all730 required GameTests with AE2+EnderStorage+ProjectRed+CC;728 AE2-only;726 with none of these optional mods. All exit0. Logs: ae2-mixed-verified.log, ae2-standalone-verified.log, ae2-absent-verified.log under build/finish-port-implementation.
- Fixture correction: injecting power and immediately inserting items precedes AE2's active-channel/storage mounting update. Tests now wait for the actual active drive, rather than assuming a fixed initial tick count. First failed fixture log retained; assertion exceptions remain GameTest failures rather than wrapping them into server crashes.
- check_ae2_packaging.py checks artifact/POM isolation and unit XML totals. No new two-process restart or real-client acceptance was run. No client/Computer Use/profile install/window movement occurred.

## Run

From the repository in PowerShell:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithAE2 --no-daemon --console=plain
exit $LASTEXITCODE
```

For a mixed profile append -PwithEnderStorage -PwithProjectRed -PwithComputerCraft. Omit flags to verify optional absence. Run one Gradle process at a time and preserve latest.log before another server run.

## Still open

getCraftables and Craftable/CraftingStatus userdata, real recipe/craftable-only positive fixtures, async calculation/submission/cancel, persisted links and process-restart recovery; block/part interfaces and import/export buses, cell converters, wrench/spatial hooks and item EnvironmentProviders. Controller dataset ordering is not promised. Broader custom component/nested filter/addon storage types, extreme real storage quantities, chunk-unload/reconnect matrices, protection and multiplayer remain acceptance work. Full real-client/visual acceptance must use screen3 with user desktop coordination. See the complete AE2 plan; no legacy feature is silently dropped.

Whole-port beta estimate:3-6 concentrated workweeks, low confidence, including full implementation, bugfixes and real ingame/visual acceptance. This checkpoint does not close the AE2 or overall integration gate.

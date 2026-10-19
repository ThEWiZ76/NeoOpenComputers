# Item inventory control

Related: [[../Project]], [[../02 plans/Complete portering]], [[Inventory controller transfers]], [[../04 ai-context/Port gap triage]].

## Implemented contract

Runtime checkpoint a714219f21da18fb59147bd132e3eaa44efee33d implements upstream server/component/traits/ItemInventoryControl.scala through the installed robot/drone inventory controller:

- `getItemInventorySize(inventorySlot)` returns the item-provider/capability size.
- `dropIntoItemInventory(inventorySlot, slot[, count=64])` transfers from selected cargo to exactly one slot in the container item.
- `suckFromItemInventory(inventorySlot, slot[, count=64])` transfers into cargo starting at the selected slot and wrapping, keeping the carrier slot intact.
- Slots are one-based. Count clamps to 0..64, with omitted/nil count defaulting to 64. Transfers return the actual count, including zero. Missing item inventory returns `0, "no item inventory"`; invalid slot arguments retain `invalid slot`. Unlike world transfers, upstream item transfers request no delay.
- Uses Driver.itemHandlerFor, supporting OC inventory providers and NeoForge ItemHandler.ITEM capabilities. Shulker boxes therefore use NeoForge's actual ComponentItemHandler and immutable container-data writeback. Server items retain hardware slot/tier restrictions and CPU architecture metadata.
- Simulation checks capacity before actual extraction. The final carrier is reassigned into cargo to preserve component writes on hosts that return inventory copies. Self-insertion is refused. A writable stack of carrier items is separated into one carrier and its unchanged remainder before mutation; without an empty cargo slot for the remainder, return zero without transferring.

## Database duplication fix and compatibility boundary

The first integrated reproduction showed that database descriptions and copied entries could be extracted as real items. DatabaseEnvironment now tracks physical ownership separately from description data. Only actual inventory insertions set a persisted `physical` entry flag; catalogue writes/copies/clones do not create ownership. Identical self-copy retains an existing owned item. DatabaseInventoryProvider exposes only physically deposited entries for item transfers, with one item per slot, and refuses direct writes/extractions on an unsplit stack of database items.

Existing entries without the flag remain readable descriptions; their data is not deleted or automatically converted into extractable items. Old data does not distinguish legitimately deposited items from copied descriptions, so recoverability/migration for ambiguous historical physical contents remains a release-acceptance issue. Do not silently infer physical ownership from a positive count. This is an intentional duplication fix rather than strict reproduction of the unsafe provider behavior.

## Evidence and limits

Eight required GameTests cover robot/drone component invocations, actual native programs with installed upgrades, shulker component writeback and item serialization, partial/full cargo, selected-first insertion, named items, invalid slots, self-insertion, stacked carriers, server hardware/architecture data, database description/copy non-materialization and a physical database round trip.

The missing-callback red run is retained as build/finish-port-implementation/item-inventory-red.log. That run also exposed an intermittent existing terminal lookup failure. Database duplication failures are in item-inventory-database-duplication-red.log. A CPU metadata assertion initially compared against a source stack that extraction had emptied; it now checks the extracted item's actual processor architecture directly (item-inventory-cpu-fixture.log).

Final item checkpoint passed full build, 2121 unit tests and all 699 mandatory GameTests: item-inventory-verified.log. The terminal lookup failure was intermittent, not fixed by this item checkpoint; address-generation investigation follows separately. No client/Modrinth profile/Computer Use action occurred. Client/survival, third-party container/protection mods, ambiguous historical database migration and broader restart/platform acceptance remain open.

## How to run/use this

From M:/development/OpenComputers in PowerShell:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer --no-daemon --console=plain
exit $LASTEXITCODE
```

Run one Gradle process at a time and verify its terminal exit code plus all-required-tests-passed server log. This starts a headless test server, not the Minecraft client.

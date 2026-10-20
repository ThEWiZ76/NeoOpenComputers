# AE2 cell descriptions and auxiliary hooks

Runtime checkpoint: cf37b92c85bf5cbfb0979427e9b811959a569b97. Minecraft 1.21.1 / NeoForge 21.1.234 / AE2 19.2.18. Related: [[AE2 manual export]], [[AE2 interfaces and bus configuration]], [[../02 plans/AE2 integration]].

## Cell descriptions

The optional registered converter handles cell ItemStacks, live StorageCell objects and actual DriveWatcher wrappers. ItemStack conversion opens a copy so reading descriptions cannot change the held item. Preserve upstream fields: storedItemTypes, storedItemCount, remainingItemCount, remainingItemTypes, getTotalItemTypes, getAvailableItems, totalBytes, freeBytes, usedBytes, unusedItemCount, canHoldNewItem, fuzzyMode and name. The legacy name field is the cell display name, including its custom name, rather than the general item registry-name field.

BasicCellInventory supplies real metrics; no fabricated zero capacity for special cells without the basic-cell capacity API. Generic StorageCell instances expose their actual description and contents but omit unavailable metrics. Contents use regular converted item/fluid descriptions with exact long quantities; item size is item count, fluid amount is millibuckets. The modern fluid cells also work, unlike the old item-channel-only ItemStack lookup. Modern 1k cells have 63 item types or 18 fluid types; fluid accounting uses 8000 mB per byte, versus 8 items per byte. The converter does not mutate physical cell contents.

## Wrench compatibility

Register OC wrench checks/usage for native AE2 QuartzWrenchItem/NetworkToolItem against the modern InteractionUtil.canWrenchDisassemble tag contract. A network tool's special exclusion from AE2 block rotation does not prevent its use for OC wrench interactions. AE2's native wrench tools do not consume durability. Restrict this provider to AE2 tool classes so other tagged mods keep their own eligibility/consumption callbacks; the mixed test explicitly excludes ProjectRed's screwdriver from the AE2 provider. No foreign classes are linked until the installed-mod guard is satisfied.

## Spatial movement

Modern AE2 defaults to moving block entities through BlockEntityMoveStrategies, with a spatial blacklist rather than the old explicit whitelist. OC prints already work with the default strategy, so no redundant registration or special mover was added. A real PrintBlockEntity with label, tinted shape and west orientation is saved, removed and reconstructed at another position through the actual AE2 strategy; NBT except coordinates and block orientation are preserved. This tests the movement contract, not a complete powered spatial-pylon/dimension cycle.

## Evidence and use

Four added GameTests. Before implementation, real item/fluid cells lacked totalBytes and a certus wrench was unrecognized; ae2-auxiliary-api-red-build.log records three failures. The spatial test already passed before production edits. A prior fixture compile typo is separately preserved in ae2-auxiliary-red-build.log. The first implementation exposed converter registration order overwriting the legacy cell display-name field; specialized AE2 conversion now follows generic Minecraft conversion, with name assertions protecting it (ae2-auxiliary-converter-order-red.log).

The item/fluid tests cover empty, two-type and byte-full cells, exact counts/bytes/free capacity/rounding/type limits, custom name/fuzzy mode, unmodified source ItemStack, contents and direct/wrapper conversions. Actual survival players use all three AE2 tools on an OC adapter with exact side-toggle and unchanged tool checks. Mixed build: 2141 unit tests and 750 GameTests pass. No-optional-mod profile: 726 GameTests pass. Logs: ae2-auxiliary-mixed-verified-build.log and ae2-auxiliary-absent-build.log. Build includes optional artifact/POM isolation checks.

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithAE2 -PwithEnderStorage -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
exit $LASTEXITCODE
```

Descriptions appear through existing inventory/database/network ItemStack conversion paths; no new component is required for storage-cell items. The five existing AE2 device EnvironmentProviders remain the upstream device lookup surface; cells/wrenches/prints do not expose a new component API.

## Open acceptance and beta estimate

Full powered spatial dimension transfer, real client menus/models/tool interactions, survival/multiplayer/protection and other platforms remain open acceptance. Crafting computing-save/CPU_OFFLINE/removed-target, true chunk eviction, history/cleanup/load and the intermittent unload-loop cause remain open. Other modern integration families and the full semantic/config/resource/API matrix also remain. No client/Computer Use/Modrinth install/window movement occurred. Minecraft client acceptance remains required on screen 3 with desktop coordination.

Whole-port beta estimate stays **3-6 concentrated workweeks, low confidence**, for full implementation plus bugfixes and real-client acceptance. Re-estimate after each milestone; test counts are not completion percentages.

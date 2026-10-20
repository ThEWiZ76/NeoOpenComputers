# AE2 manual export

Runtime checkpoint: 37fb64458a819bd3e45377174a1cabdf69e7bf41. Minecraft 1.21.1 / NeoForge 21.1.234 / AE2 19.2.18. Related: [[AE2 interfaces and bus configuration]], [[../02 plans/AE2 integration]], [[../04 ai-context/Port gap triage]].

## Contract

`me_exportbus.exportIntoSlot(side[, slot])` operates on the actual export bus on world side 0..5. The optional target slot is one-based; omission/nil allows all sided slots. It uses the target's standard item capability on the opposite face. Unloaded destination chunks are refused before capability lookup, without forcing loads. Missing part returns nil, `no export bus`; missing/unloaded inventory returns nil, `no inventory`; invalid side/slot raises `invalid side` / `invalid slot`.

Configured item keys determine exports. Speed cards select budgets 1/8/32/64/96. A fuzzy card uses AE2's actual configured FuzzyMode and available keys, preserving the original item's components. No matching stored items, full/rejecting inventory or insufficient power returns nil, `no items moved`. Success returns the actual inserted count and pauses 0.25 seconds once. Fluid filter entries are skipped: this legacy callback inserts items into item slots.

The bus must have an active powered/channelled node. Extraction uses AE2 StorageHelper.poweredExtraction and the actual bus machine action source. Standard sided automation capabilities govern destination insertion. Manual calls can operate while a redstone card has put automatic bus ticking to sleep. They do not initiate autocrafting or bypass power consumption.

Unlike the old insert-before-extract implementation, simulate destination acceptance, extract the actual available quantity, then insert only that extracted quantity. Refund honest rejected remainders to ME; if the storage cannot accept a refund, drop the remaining exact item key at the destination. This guarantees conservation for truthful capability remainders. An arbitrary external handler that mutates inventory and then throws or lies about its result is not transactionally recoverable through IItemHandler.

## Verification

Ten new real-server tests cover six world directions, all five speeds, explicit/default slots, partial/full inventories, missing part/inventory, sided furnace insertion, exact successful pauses, power deficit, fuzzy thresholds/component preservation, native Lua through a physical adapter, and an unloaded neighboring chunk. Recovery tests use a real ME grid/drive/cell with truthful changing target handlers: partial acceptance, complete rejection, actual extraction lower than simulation and a real cell filled during commit so refund requires a drop. Exact item totals are checked in one synchronous server operation.

The initial red run reproduced missing callbacks/native Lua API. Subsequent direction failures exposed a fixture wiring error: a drive cannot connect through its front. Drives now face perpendicular to their test network chain, and fixtures wait for the actual energy cell to join before powering operations. Preserved logs include ae2-export-red-build.log, ae2-export-green-build.log and ae2-export-fixture-orientation-red.log. These fixture failures were not labelled as product fixes.

Full build / 2141 unit tests / 746 mixed AE2+EnderStorage+ProjectRed+ComputerCraft GameTests pass. No-optional-mod profile passes 726 GameTests. Logs: ae2-export-mixed-verified-build.log and ae2-export-absent-verified-build.log. Build's artifact/POM tests retain optional-dependency isolation. No new process-restart pair was needed for this synchronous callback; the existing crafting restart evidence remains in [[AE2 crafting restart]].

## Run

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithAE2 -PwithEnderStorage -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
exit $LASTEXITCODE
```

Run absent-profile GameTests by omitting the four with-mod properties. Use one Gradle process at a time and preserve latest.log before another server run.

## Remaining beta gates

Cell converters and auxiliary AE2 contracts; computing-save/CPU_OFFLINE/removed-target and broader restart/chunk/history/load acceptance; unresolved intermittent unload-loop investigation; other modern integrations and full semantic/resource/config/API audit; real client visual/survival/multiplayer/platform acceptance. No Minecraft client, Computer Use, Modrinth install or window move occurred in this checkpoint. Client tests remain required on screen 3 with desktop coordination.

Whole-port beta estimate remains **3-6 concentrated workweeks, low confidence**, including full implementation, bugfixes and real client acceptance. Re-estimate at completed milestones or when scope/blockers materially change. Test counts are evidence, not completion percentages.

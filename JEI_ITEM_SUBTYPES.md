# JEI item subtypes

Runtime 202ae8d15b6ac5c8975a4673b5fd03aeac238a7b, 2026-10-20. Related: [[JEI documentation integration]], [[Addon floppy factory persistence]], [[Loot disk cycling]], [[Minecraft test window]], [[../02 plans/JEI integration]].

## Upstream contract and implementation

Upstream ModPluginOpenComputers.scala registerItemSubtypes uses full NBT for preconfigured robot/microcontroller/drone/tablet items. Its floppy interpreter uses only the loot factory, keeping ordinary mutable floppies in one subtype. Modern JEI19.21.0.246 supplies ISubtypeInterpreter.getSubtypeData with object equality/hash keys and Ingredient/Recipe contexts, registered through ISubtypeRegistration.

OpenComputersJeiPlugin now registers six assembled item bases: robot, all three microcontroller tiers, drone and tablet. ItemSubtypes.COMPONENTS returns the complete immutable component patch, or null for an empty patch; stack count is excluded. This carries custom data, hardware/configuration and other item components rather than picking an incomplete subset. Changing a stack's components does not mutate an already obtained comparison key. Base item identity remains JEI's responsibility.

ItemSubtypes.FLOPPY returns only the factory string. Mutable names, labels, storage data/node identities and count are excluded; ordinary floppies return null. Legacy oc:lootPath maps to the corresponding neoopencomputers:loot/<path> bundled identity. When fields coexist, precedence matches the actual FloppyItem driver: lootPath, then lootFactory, then current oc:factory. HDD/EEPROM and unassembled cases are not added to the subtype registry. The modern interpreters have no old port string-UID format to import; their legacy-string compatibility methods return empty as permitted by JEI's API. This does not establish migration of old1.12 JEI config/bookmarks.

## Evidence

Three JEI-conditional GameTests invoke the actual plugin registration method with an API registration capture and use its returned real interpreters against registered Minecraft ItemStacks in both UID contexts. They cover all six assembled bases, component equality with count changes, hardware/name differences and stable old keys; ordinary floppy grouping, different factories, ignored mutable data, legacy/current precedence; and exactly seven registered bases with no unrelated media.

All three tests fail before implementation in805 tests with JEI only. Build/all2188 units and all880 mixed GameTests then pass with JEI/CC/AE2/Mekanism/EnderStorage/ProjectRed. The legacy-path assertion was strengthened to conflict with the legacy-factory value, and the final805-test JEI-only run passes. The final build/all2188 units and all802 no-optional tests also pass. Every cited run has a complete GameTest summary and normal terminal exit, except the intentionally failing red baseline. Logs: build/finish-port-implementation/jei-subtypes-{red,mixed,final,absent}-{build,latest,debug}.log.

These are registry/interpreter contract tests, not execution of JEI's live client ingredient UID manager, overlay deduplication/bookmarks or recipe transfer. Actual client acceptance remains open. No new process-restart claim is made.

## Current artifact and next work

Prepared tested jar build/finish-port-implementation/neoopencomputers-jei-subtypes.jar SHA2569FD4B12DE72DFCFEC2FDF6A0985F0BD3CDC686064730A126C96045F448377936 includes the preceding image, factory and JEI-link fixes too. It supersedes the older manual-images snapshot as the newest candidate, but neither has been installed in the running client. PID68248 still runs9bc57db; the pending user shutdown/restart coordination remains unresolved. Do not infer permission/readiness merely from elapsed time. Desktop Computer Use remains off and future Minecraft starts must be placed on screen3.

Next JEI gates remain actual cycling recipe display, runtime additions/client metadata synchronization, hidden ingredients, real extra GUI areas, icon parity and full client acceptance. Before implementing cycling display, reproduce a source-audit discrepancy: LootDiskCyclingRecipe.factoryId currently prefers oc:factory over oc:lootFactory, whereas the disk driver and subtype interpreter prefer the legacy factory. Conflicting fields could therefore identify the same mounted filesystem yet recraft differently. Do not claim this discrepancy is fixed without a real crafting regression.

Full beta estimate remains3-6 concentrated workweeks with low confidence, including full implementation and acceptance. Do not derive completion percentages from test counts.

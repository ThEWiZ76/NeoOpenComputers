# JEI loot disk cycling

Crafting precedence4c7f161f9fea439ace4a8df8e0baf5b81f4ca0c3; JEI extension bfbddb03febc2254965a263882e0ac4d97bdc193, 2026-10-20. Related: [[Loot disk cycling]], [[JEI item subtypes]], [[Minecraft test window]], [[../02 plans/JEI integration]].

## Factory precedence correction

An actual registered-floppy test mounts a disk with different valid legacy/current factory fields. Factory-call counters prove FloppyItem selects the legacy factory; the old crafting recipe then advanced from the current factory instead. This fails one of803 no-optional tests before correction. LootDiskCyclingRecipe.factoryId now gives a valid legacy factory priority, matching the disk driver for these overlapping factory fields. The fixed build/all2188 units,881 mixed tests and803 absent tests pass. Evidence: loot-precedence-{red,mixed,absent}-{build,latest,debug}.log. This resolves the discrepancy previously identified in [[JEI item subtypes]]; it is not a blanket migration claim for arbitrary malformed or older path-only disks.

## Actual recipe extension and paired layout

OpenComputersJeiPlugin registers LootDiskCyclingExtension for the actual LootDiskCyclingRecipe class through IVanillaCategoryExtensionRegistration. It adds no synthetic crafting recipes. The exact pinned JEI19.21.0.246 publisher sources were inspected: VanillaRecipes enumerates the live recipe manager; CategoryRecipeValidator accepts special recipes for extension handling; CraftingRecipeCategory selects the class extension. Thus the integration uses recipes that actually exist in the synchronized recipe manager rather than creating a recipe when a data pack removed it. Live data-pack reload/UI behavior still needs client acceptance.

The extension lazily reads JEI's current ingredients and the current client world. Every candidate is checked with the real recipe.matches and assembled with the real recipe. Empty results are excluded. It shows a shapeless disk+wrench input and the corresponding actual output, using JEI's own crafting-grid helper. A focus link connects equal-length disk input/output lists, keeping rotations and focused lookups paired; the constant wrench slot is separate. Registered opted-out disks remain valid inputs but never become outputs. Plain writable floppies do not enter the list. All accepted input stacks are copied. An empty output cycle or absent world is unhandled and generates no fake layout.

ClientWorld isolates the direct Minecraft.level access in a nested class. Dedicated-server tests can load plugin registration without verifying ClientLevel; actual client world retrieval remains lazy. This is a class-loading boundary, not replacement of real client acceptance.

## Verification and retained failures

Three JEI-conditional tests cover actual plugin extension registration, layout/input/output/remaining-wrench correctness against the real crafting recipe, and rejection of empty output cycles. Layout tests invoke the actual pinned CraftingGridHelper.INSTANCE with a slot/layout capture; they assert the two slots focus-linked are the disk and its paired output, all pairs match actual assembly, opted-out output exclusion and foreign-stack isolation. Fixtures restore the previous API registry in finally and create no world structures beyond normal empty GameTest templates.

The corrected red registration fixture fails before implementation: JEI cannot display the actual loot disk crafting recipe (807 tests, one failure). An earlier fixture incorrectly proxied IIngredientManager, whose Rect2i signature is stripped on dedicated servers; that dist failure is not evidence of the missing feature. The first implemented mixed run also caught eager verification of ClientLevel from the plugin's lambda, failing four registration/subtype tests. It is superseded by nested client-world isolation. Those failure logs are retained rather than counted as passing checks.

Final build/all2188 units and all884 mixed GameTests pass with CC/AE2/Mekanism/EnderStorage/ProjectRed/JEI, full summary and normal exit. All803 no-optional tests also pass with the new optional fixtures excluded safely. Final logs: jei-loot-mixed-isolated-{build,latest,debug}.log and jei-loot-absent-{build,latest,debug}.log. Earlier evidence: jei-loot-red-registration-*, jei-loot-red-build.log with jei-loot-fixture-dist-latest/debug.log, and jei-loot-mixed-build.log with jei-loot-clientlevel-fixture-latest/debug.log. No new process-restart proof is claimed.

Primary publisher sources are cached in build/finish-port-implementation/jei-api/19.21.0.246: common-api and the full runtime sources jar, including pinned-VanillaRecipes.java, pinned-CategoryRecipeValidator.java, pinned-CraftingRecipeCategory.java and pinned-CraftingGridHelper.java. Publisher artifact: https://maven.blamejared.com/mezz/jei/jei-1.21.1-neoforge/19.21.0.246/jei-1.21.1-neoforge-19.21.0.246-sources.jar . The official1.21.1 VanillaPlugin source was also consulted; exact compatibility decisions use the pinned artifact, not the moving branch.

## Open port/config and client gates

Upstream Settings.scala reads misc.lootRecrafting and application.conf defaults it to true, explicitly controlling whether disks can be cycled with a wrench. Current ModSettings has no equivalent; NeoOpenComputers registers its present spec as COMMON. Restore this setting and prove the authoritative enabled state seen by clients, rather than checking a client-local default that can disagree with the server. Investigate upstream recipe-registration semantics and NeoForge conditional recipe loading before adding a new synchronization mechanism. Full cycling parity is therefore still open despite the tested display extension.

Actual client animation/focused output lookup, recipe transfer, config/data-pack disabling/reload, reconnect/minimal/mixed profiles and authoritative runtime disk additions/metadata remain open. The normal JEI callback/manual/client gates also remain. Do not infer graphical acceptance from headless slot captures.

Prepared newest candidate build/finish-port-implementation/neoopencomputers-jei-loot-cycling.jar SHA2561E7C68A54CABE5C46FDA8656628ACE7EC7C883A2B8D9F96F5B58F5EBAF696A4A supersedes older staged snapshots and includes their factory/manual/image/subtype corrections. It is NOT installed. Current client remains PID68248,9bc57db at last verification; pending user shutdown coordination has no response yet. Desktop Computer Use stays off; continue independent work, coordinate installation only when ready, then user restart/place screen3 and MCP acceptance.

Whole-port beta remains3-6 concentrated workweeks, low confidence, for full implementation and acceptance. No test-derived completion percentage.

# Disassembler recipe recovery Implementation Plan

> Execute inline under the active full-port goal. User already requested plan plus autonomous execution and intermediate pushes. AGENTS.md overrides extra approval, worktree and subagent/review loops; keep feature/finish-port and preserve unrelated changes.

**Goal:** Restore ordinary crafting-recipe disassembly with upstream item eligibility and ingredient protections, preserving explicit template priority.

**Architecture:** Add one focused DisassemblerIngredients helper. Keep existing DisassemblerTemplates and one-argument external templates intact; the host resolves an explicit template first, otherwise ordinary eligible ingredients. Retain the existing powered queue and ownership repairs from b15a1bad1. No UI redesign.

**Tech stack:** Java21, Minecraft1.21.1/NeoForge234 plus mixed248, real GameTests and existing2240 unit suite.

## Grounded contracts

Read actual MC1.12 ItemUtils.getIngredients and Disassembler.isItemValidForSlot, Settings/application.conf. Defaults: misc.disassembleAllTheThings=false; misc.disassemblerInputBlacklist=[minecraft:fire]. Opt-out oc:undisassemblable always wins. Without opt-in, a normal output requires api.Items.get(stack), while explicitly registered templates may accept other items.

Recognize ordinary shaped/shapeless crafting recipes; ignore non-crafting/special dynamic recipes. Filter empty candidates, bucket items and inputCount/outputCount==0 BEFORE merging, matching the original conservative handling of multi-output recipes. Blacklisted chosen inputs reject that candidate recipe. Reject self-ingredient feedback. Merge equal selected inputs, divide by result count and split into individual work items. Preserve input stack/data and static recipe ingredient objects.

Modern tag alternatives and recipe candidates use stable registry/recipe-ID order so readiness and start agree. Runtime OC custom data does not prevent matching a normal item recipe; damaged-item identity must still be respected. A small per-RecipeManager weak index may use getRecipes() snapshot identity: verified local1.21.1 bytecode returns immutable byName.values(), replaced on reload/replaceRecipes. Indexing must not hide same-ID changed recipes, removal or an empty manager. No generic metadata cache across item variants.

## Steps

- [ ] Add DisassemblerRecipeGameTests.java. Scope temporary RecipeManager replacements/config changes with finally restoration before any yield. Cover ordinary shaped/shapeless OC recovery, repeated ingredients, tagged used OC input, default foreign-item rejection and opt-in acceptance, blacklist/alternative recipe, bucket exclusion, self recipe, fractional multi-output rejection, template priority and same-ID changed/removed recipe visibility. Verify meaningful behavior failures on the current source. New config field declarations/default wiring may precede that run solely so config tests compile; fallback behavior remains absent for red.
- [ ] Add default-false DISASSEMBLE_ALL_THE_THINGS and default minecraft:fire DISASSEMBLER_INPUT_BLACKLIST in ModSettings, with getters and existing namespaced-ID validation conventions. Add DisassemblerIngredients.java; share the existing opt-out check, add host fallback in input predicates/start/instant helper without duplicating provider execution or weakening queue guards. Ingredient expansion must not mutate live/source stacks.
- [ ] Run the corrected real tests/build; confirm exact queue item counts, configured paid release, no input/output duplication and recipe replacement invalidation. Use fresh isolated C: worlds. Relevant baseline:884 ordinary,1109 mixed; actual new totals must be read from results, never inferred as feature completion.
- [ ] Run mixed available integrations and final default234 build/tests sequentially. Observe original terminal exits and full saves, archive logs/worlds with hashes. Record actual source/server scope canonically, mirror root docs, commit/push and prove origin equality; refresh source inventory pin. Combine actual client acceptance with the following template/IMC batch if no client-specific regression requires an earlier run.

Representative command (change isolated world/log for red/fixed/mixed/final):

    rtk proxy cmd /d /c "gradlew.bat --no-daemon build runGameTestServer -Pneoopencomputers.gameTestRunDirectory=C:/Users/rolan/AppData/Local/Temp/oc-gametest-working/printer-parity-20261021-02 -Pneoopencomputers.gameTestWorld=oc-tests-disassembler-recipes-red-20261021-01 > build\finish-port-implementation\disassembler-recipes-red.log 2>&1"

Use PowerShell exit $LASTEXITCODE outside RTK. Only one Gradle invocation at a time. Red must fail the new runtime requirements; final must have no assertion/compilation/IO failures and complete saves. No extra reviewer agents after focused verification.

## Separate remaining slices

Legacy IMC two-argument callback and immediate-drop distinction; robot/drone/microcontroller/server/navigation default templates; larger-job queue/internal-output restriction; creative instant semantics. Then broader device/workflow and multiplayer/protection/platform/load/crash/migration gates; final polish last. This plan does not declare the disassembler or full port complete. Current installed/client-tested runtime b15a1bad1/PID45768 with exact original common config restored/default loss0.05. Beta3-6 concentrated workweeks, low confidence. Overnight any-screen/focus client authority remains until user reports returning.

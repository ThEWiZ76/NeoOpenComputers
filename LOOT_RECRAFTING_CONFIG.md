# Loot recrafting config

Runtime fe19f1346cb67f898332374a6ae73ff176e8804d, 2026-10-20. Related: [[JEI loot disk cycling]], [[Loot disk cycling]], [[Minecraft test window]], [[../02 plans/JEI integration]].

## Setting and application boundary

Upstream Settings.scala reads misc.lootRecrafting; application.conf defaults true. Recipes.scala only registers the loot cycling recipe when enabled. The port had no equivalent option and always loaded its recipe.

ModSettings now exposes LOOT_RECRAFTING and lootRecrafting(), using the same misc.lootRecrafting path and true default. The setting remains in neoopencomputers-common.toml, with the other existing settings. It controls the shipped recipe when SERVER recipes load/reload, rather than becoming an independent client-local matching rule.

To disable the shipped cycling recipe, merge this value into the existing misc section of the server's neoopencomputers-common.toml:

```toml
[misc]
lootRecrafting = false
```

The value takes effect at the next server recipe/data-pack reload or server start. Editing the config alone does not immediately rebuild the current recipe manager. Restore true and reload recipes to enable it again. Loading loot program contents and registering addon disk factories are unaffected.

## Server authority without another synchronization packet

LootRecraftingCondition implements NeoForge ICondition, reading the current setting at data load. Its codec is deferred-registered through ModRecipeSerializers under neoopencomputers:loot_recrafting; the shipped loot_disk_cycling JSON includes that neoforge:conditions entry. NeoForge's conditional recipe loader omits the recipe when false. See the official [NeoForge1.21.1 condition documentation](https://docs.neoforged.net/docs/1.21.1/resources/server/conditions/).

The standard Minecraft recipe update packet then carries the server's actual recipe set. The client handler replaces its recipe manager from that packet; it does not evaluate server recipe JSON conditions against its own common config. The existing JEI extension enumerates that actual synchronized recipe manager, so no additional enable-state payload is needed for this flag. Do NOT add a local config check to LootDiskCyclingRecipe.matches or the JEI extension: that would reject an enabled server recipe when the client's local config differs.

This mirrors upstream's conditional registration, rather than globally prohibiting every programmatically constructed or data-pack-replaced recipe of this class. A modpack replacing the shipped JSON controls that replacement's own conditions.

## Verification

One unit regression first fails because LOOT_RECRAFTING is missing, then verifies the exact upstream path and unloaded true default. After adding the setting alone, two server regressions fail against the still-unconditional shipped JSON: disabling leaves the real recipe loaded, and the resulting packet still contains it. Those failures distinguish a real registration gap from a missing getter only.

LootRecraftingConfigGameTests uses a separate RecipeManager subclass with NeoForge's actual conditional apply pipeline and real server registry/context injection. It reads the actual shipped recipe resource, loads false/true/false into the same manager, checks no decoder errors, asserts recipe presence and performs real crafting lookup. It does not replace the running world's manager or disturb other tests' recipes. The fixture also checks the real server-start recipe set against its initially loaded config.

The packet case serializes/deserializes ClientboundUpdateRecipesPacket through its actual STREAM_CODEC and RegistryFriendlyByteBuf. For disabled and enabled server states, it flips the local setting before receiving the packet, replaces a separate receiver RecipeManager exactly as ClientPacketListener.handleUpdateRecipes does, and asserts recipe presence and crafting lookup still match the server. Buffers are released and config values restored in finally. This proves the recipe-set/protocol contract; it is not a running multiplayer client's GUI-event test.

Final build/all2189 unit tests and all886 mixed-provider GameTests pass with JEI/CC/AE2/Mekanism/EnderStorage/ProjectRed. A fresh no-optional server JVM with the actual GameTest-server TOML temporarily set false passes all805 tests, including the real startup absence assertion. The server loaded1448 recipes in this disabled profile. The final fresh enabled mixed JVM also passes the startup assertion. Every final process exits normally and has a complete GameTest summary.

Only run/gameTestServer/config/neoopencomputers-common.toml misc.lootRecrafting was temporarily edited for the cold-disabled phase. A preimage was retained as build/finish-port-implementation/loot-config-original.toml. After the disabled process exited, the original value was restored; both immediate and post-final-run comparisons confirm the ENTIRE config matches the original bytes. Restored SHA256D7051888C1FC94F588A0EAEFD28B8CCB92EA557F89A18B623BD414B6677D9CDF. No Modrinth profile config was changed.

Evidence prefixes: loot-config-unit-red-build.log and loot-config-unit-red.xml; loot-config-server-red-{build,latest,debug}.log; loot-config-mixed-* (initial green); loot-config-cold-disabled-* (805/no optional); loot-config-final-* (886/mixed). Readable local helper loot_config_cold_probe.py bounds its target to the GameTest config directory and changes only the one bool. The retained cold world is oc-tests-loot-config-cold-disabled-20261020-01; final mixed world oc-tests-loot-config-final-20261020-01.

## Remaining acceptance and installed state

Actual in-game config/data-pack reload, JEI GUI redraw/focus/transfer after reload and reconnect, minimal/mixed profiles and real multiplayer remain open. The server-only packet receiver test does not execute ClientLevel, client recipe events or JEI's live GUI. Runtime disk metadata/additions, hidden ingredients, GUI extra areas, icon parity and the wider port gates remain open.

Newest staged tested candidate: build/finish-port-implementation/neoopencomputers-loot-config.jar, SHA2565AFF117C9FDB586201A98D0EEB890DF577AE48F62640B001357513B7082B5D6D. Includes all prior pending factory/manual/image/subtype/cycling fixes. It is not installed: client PID68248 remains9bc57db at last verification, and the user's shutdown coordination question remains unanswered. Keep Desktop Computer Use off; future restart/place screen3 stays coordinated with the user. No actual-client config change, profile write or restart occurred in this checkpoint.

Whole-port beta remains3-6 concentrated workweeks, low confidence, including full implementation and acceptance. Test counts do not imply feature completion percentages.

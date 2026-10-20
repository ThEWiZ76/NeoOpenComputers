# Ender IO wrench integration

Related: [[../Project]], [[../02 plans/Ender IO integration]], [[../04 ai-context/Modern integration scope]].


## Ender IO server checkpoint - 74bf7efb8542466eddcef3cde8d291dd81b50241

Actual Ender IO8.2.12-beta YetaWrenchItem now registers through OC's existing wrench checks/usage, including subclasses. This ports the entire original two-file ITool integration; earlier power/bundled classification was incorrect. Modern item has no canUse/used hook or durability: acknowledgment performs no second block action or mutation. Other mods' common wrench tags are not used to bypass their own eligibility/consumption rules.

Two real-provider regressions fail on baseline, then pass actual adapter-side/charger-signal actions, unchanged stack data/no eligibility mutation, ordinary-item/offhand rejection and exactly one packed drone on sneaking pickup. Build2239 units,857 standalone/950 mixed248/855 absent234 required tests all green with normal exits; latest/debug logs archived. -PwithEnderIO supplies only optional provider runtime; its own nested EnderCore/modded-conduits/GraphLib load normally, never bundled in OC. Optional classloading, metadata and artifact/POM guards remain. Runtime74bf7efb8542466eddcef3cde8d291dd81b50241; see [[../03 manuals/Ender IO wrench integration]]. Actual client/multiplayer/protection/platform/load and other full-port gates remain open. No client installation/restart or OS focus/mouse action. Beta3-6 concentrated workweeks, low confidence; technical function before polish.


## Published target and contract

Publisher https://www.curseforge.com/minecraft/mc-mods/ender-io links Team-EnderIO/EnderIO. File8832691, curse.maven:ender-io-64578:8832691, actual6598496 bytes SHA2565fe098153dddc2e7ccd9d38b0341dbc61f3072150958c916e5ef63f395e4eab1. Its descriptor declares Minecraft[1.21.1], NeoForge21.1.216 and EnderCore8.2.12-beta. EnderCore, modded conduits and GraphLib3 are already provider JarJar libraries; no separate runtime download or OC bundling. Actual nested EnderCore221671 bytes SHA256b5b3a7b0d971dcd3d9dbcec029a36d450ee72600182c565cb29264de79171a7f. Public source1.21.1 reference b59e212c99307411e4d3bb8d97da3da0761381af was checked against published YetaWrenchItem methods using javap, not claimed exact artifact build commit. Evidence under build/finish-port-implementation/integration-discovery/enderio.

Original OC baseline7d9eac5 has only ModEnderIO.scala and EventHandlerEnderIO.scala: ITool recognition/canUse/used registration, zero component callbacks. There is no family-specific energy or bundled-redstone API to invent. Modern YetaWrenchItem extends Item, stack-size1, common wrench tag; its item-first action cycles only actual Ender IO side-config hosts. It has no legacy ITool interface or durability hook. OC recognizes the actual published class and subclasses and returns eligibility/usage acknowledgment without invoking another block action. Unrelated tagged tools continue using their own existing handlers. Generic FE and all remaining source families are separate acceptance scopes.

## Verification

enderio-red-build.log reports exactly two required failures among857: actual Yeta Wrench unrecognized, actual drone not picked up. No provider code/descriptor/assertion weakened. enderio-fixed-build.log passes build2239 units and857 required tests with the actual provider/EnderCore on default234, normal exit0. Two new conditional cases require the actual YetaWrenchItem, invoke actual adapter-side and charger-signal actions, check no mutation during eligibility/notification/no extra durability or NBT change, reject ordinary mainhand/offhand bypass, and verify exactly one packed drone with unchanged wrench. This family has no native component callback or new component to test; no Lua API is fabricated.

enderio-final-mixed-build.log passes build2239 units/950 required tests on248 with Ender IO, Railcraft/JEI322, Forestry+Butterflies/Patchouli, TIS, AE2, CC, Mekanism, EnderStorage and ProjectRed. enderio-final-absent-build.log passes build2239 units/855 required tests on234 without optional runtimes. Both BUILD SUCCESSFUL/process exit0; latest/debug archived, test-server mods folder empty. Core default loader234 and other profiles' JEI246 remain unchanged. Real client/rendering/multiplayer/protection/platform/load acceptance is not inferred from server tests or in-process fixtures.

## How to run

Use gradlew.bat --no-daemon build runGameTestServer -PwithEnderIO for the provider profile. Use -Pneo_version=21.1.248 when also including TIS. The fixture property rejects a missing requested provider; successful launch without a real required-test summary is not a passing gate. User's Modrinth instance is not changed by these Gradle profiles.


## Combined integration client update installed - 2026-10-20

Closed dedicated Modrinth NeoOpenComputers test instance verified through readable require_test_client_closed.ps1 (MCP8081 and matching Java command line; no auth args printed). Installed core234/runtime74bf7efb8 SHA256dffc17f53d64b6a3112291072bb859d21e318c7619c62d7af8d2ceb18548a592 plus actual pinned Forestryalpha8/Patchouli93/Butterfliesalpha8/Railcraft1.2.13/EnderIO8.2.12-beta. Existing client JEI19.27.0.343 SHA256137f60018968a50c6f00e3a8ca9be790391792d2f3e6eff66237f2e9b3bc1353 already meets Railcraft's minimum and was preserved; do not replace it with server-test322. No loader/TIS/client-launch change. Other mods, disabled status, saves/config all fresh-hash unchanged.

Backup C:\Users\rolan\AppData\Local\Temp\oc-client-backups\integrations-74bf7efb8-20261020T204124318814Z contains731 mods/saves/config files in mods-saves-config.zip (CRC/per-file SHA readback, original mtime_ns manifest). All six installed artifacts independently hash-verified; stage/installation.json retains exact hashes. Expected old coread42a56f8 was verified before replacement; stale/external file changes would abort. No broad cleanup/deletion or OS focus/mouse action. Combined real-client startup/mod-loading, functional block/wrench/adapter acceptance and cleanup are pending the user's manual start on screen3; server tests950 mixed/857 EnderIO/858 Railcraft/858 ForestryButterflies/855 absent and2239 units are not client acceptance. Beta upstream policy remains the user's explicit non-blocking/open choice.

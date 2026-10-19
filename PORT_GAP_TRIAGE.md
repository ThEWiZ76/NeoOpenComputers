# Port gap triage - 2026-10-19

Baseline: master-MC1.12 Git 7d9eac5584e238a890a388a0f697c27c7391caa4. Runtime source inspected at 00d88ac5c255e777f9c443b09ea568bacc96e4d7. This is a source audit and work queue; missing callback-name candidates are not runtime reproductions.

## Corrected assumptions

- The reference checkout is sparse: it includes Scala/resources but excludes Java. Git still contains the full Java source. A read-only git archive of src/main/java yielded 144 Java API files. Every API path has a port file, but this does not prove signature/behavior/addon compatibility.
- LuaJ and native Lua 5.2 are both registered. CpuItem takes the first registered architecture for an unspecified CPU, so LuaJ remains the default. Native continuation works in the documented tests; the port is not globally missing heap persistence.
- The native loader runs 5.2/5.3/5.4, but NativeLuaArchitecture.initialize creates LUA52. Upstream server/machine/luac/NativeLuaArchitecture.scala defines all three concrete architectures. Loader support alone is not machine support.

## Confirmed source gaps

| Priority | Gap and required behavior | Source evidence and next acceptance |
| --- | --- | --- |
| High | Native 5.3 and 5.4 machine architectures, registration and version-specific runtime behavior | Upstream NativeLua53Architecture/NativeLua54Architecture versus port NativeLuaArchitecture LUA52 construction. Add selectable versions with real OpenOS/callback/save/load tests, platform/error coverage and an explicit safe default/migration policy. Do not silently switch existing CPUs. |
| High | Inventory-controller targeted world transfer: dropIntoSlot, suckFromSlot | Upstream traits/InventoryWorldControlMk2.scala and UpgradeInventoryController Drone/Robot mixins. Port InventoryControllerEnvironment has analytics/equip but neither callback. Preserve direction/fromSide, selected/insertion slots, capability simulation, entity/block protection, counts, errors and delays; reproduce on actual robot/drone components. |
| High | Item-contained inventory control: getItemInventorySize, dropIntoItemInventory, suckFromItemInventory | Upstream traits/ItemInventoryControl.scala mixed into Drone/Robot controller. No corresponding port callbacks. Include container-item writeback, self-insertion rejection, metadata preservation and no duplication under partial acceptance. |
| High | Microcontroller external control/side routing | Upstream Microcontroller.scala exposes start/stop/isRunning/lastError/isSideOpen/setSideOpen and external snooper/side nodes. Port node() returns the internal machine node and has no equivalent annotated callbacks or side-routing state. This requires real network separation/routing and persistence, not only adding method names. |
| Normal | Robot durability callback | Upstream server/component/Robot.scala:75 uses ToolDurabilityProviders and exact no-tool/undamageable results. The port has the provider registry but no robot callback. Add real component/Lua invocation coverage for damaged, undamageable and custom-provider tools. |
| Open integration scope | Wireless redstone callbacks/provider | Upstream RedstoneWireless exposes input/output/frequency; current bridges cover CC and ProjectRed bundled transport only. Verify modern provider availability and record replacement/not-applicable decisions explicitly. |
| Open integration scope | AE2 storage/crafting and other specific drivers | Upstream includes 171 integration source files. No appeng-specific port source/dependency was found; generic energy/item/fluid capabilities do not establish AE2 network/crafting APIs. Audit each modern integration and unavailable legacy mod separately. |

RobotProxy getName/setName, debug-card getDrone/access helpers and integration callbacks in the generated review list still need context-specific review. A method such as RobotBlockEntity.setName(String) is not automatically a Lua callback. Conversely, matching a name globally may hide a missing callback on another host.

## Inventory coverage and remaining work

PORT_SURFACE_INVENTORY.json contains the file/declaration crosswalk for 461 upstream source files: blocks, items, tiles, component/traits, machine and integrations. It records local callback declarations and all same-name annotated port declarations, with line numbers. It includes the API path crosswalk. The scanner rejects unparsed callback markers instead of silently dropping them and handles synchronized Java callbacks; manual checks caught and corrected the initial modifier omission for InternetCard.request/connect and Hologram.setRaw.

This is not a complete semantic matrix. Next passes must map concrete hosts and inherited callbacks; compare arguments/defaults/results/errors/events/energy; inventory configuration, recipes, registration/tier variants and resources/Lua; compile/load a reference addon; and complete client/survival/multiplayer/platform/restart/load acceptance. Global name matches and file counts must not be used as a completion percentage.

## Reproduce source enumeration

Run from the repository root. Export only to the named build cache; the reference checkout stays unchanged:

~~~powershell
rtk git -C .upstream-opencomputers-master-MC1.12 archive --format=zip --output=../build/finish-port-implementation/upstream-java-7d9eac5.zip 7d9eac5584e238a890a388a0f697c27c7391caa4 src/main/java
rtk proxy pwsh -NoProfile -Command  Expand-Archive -LiteralPath build/finish-port-implementation/upstream-java-7d9eac5.zip -DestinationPath build/finish-port-implementation/upstream-java-7d9eac5 -Force
rtk proxy python tools/port_surface_inventory.py --help
~~~

Supply the exported root, exact baseline/port-source revisions and an output directory to the script. Durable outputs are written to the project Obsidian 04 ai-context folder first and copied to the repository. Scratch reruns can use build/finish-port-implementation/inventory; do not label a changed runtime with an older source revision.

The previous beta range of 3-6 concentrated workweeks remains a low-confidence planning estimate. These confirmed functional gaps must be included; it is not merely a client QA tail. Re-estimate after the native-version and integration scope is resolved. No client, Modrinth profile or Computer Use action occurred in this audit.

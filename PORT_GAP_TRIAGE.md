# Port gap triage - 2026-10-20

Baseline: master-MC1.12 Git 7d9eac5584e238a890a388a0f697c27c7391caa4. Initial audit at 00d88ac5c255e777f9c443b09ea568bacc96e4d7; source enumeration refreshed at 9a1df219b10f0a58dd60e29988dd57ca8bea3d1a after non-loading discovery fixes. This is a source audit and work queue; missing callback-name candidates are not runtime reproductions.

## Corrected assumptions

- The reference checkout is sparse: it includes Scala/resources but excludes Java. Git still contains the full Java source. A read-only git archive of src/main/java yielded 144 Java API files. Every API path has a port file, but this does not prove signature/behavior/addon compatibility.
- LuaJ and native Lua 5.2 are both registered. CpuItem takes the first registered architecture for an unspecified CPU, so LuaJ remains the default. Native continuation works in the documented tests; the port is not globally missing heap persistence.
- Native 5.3/5.4 now have selectable machine subclasses using the shared native runtime. Their tier-1 execution, OpenOS and unload/resume tests pass on Windows x64. LuaJ remains the default. Platform, wider host and process-restart coverage remain acceptance work; loader support alone was not machine support.

## Confirmed source gaps

| Priority | Gap and required behavior | Source evidence and next acceptance |
| --- | --- | --- |
| Fixed reproduced paths: 9a1df219b | Non-loading network/cable/screen/adapter discovery; original restart hang recovered | Three unit regressions reproduced unloaded-origin/neighbor/sided-callback access. Real chunk-edge tests exposed cable shape-update propagation and adapter scanning; deferred adapter discovery is verified after its neighbor loads. Build, 2125 units and both 702-test prepare/verify processes pass in oc-restart-unloaded-20261020-02. The original hanging oc-restart-address-20261020-01 also verifies with 702 tests in PID15444. Original failure logs/dumps retained. All unload orders, third-party queries and client acceptance remain separate. See LOADED_CHUNK_DISCOVERY.md and PROCESS_RESTART_TEST.md. |
| Fixed generation: df9151c8c | New sequential node addresses collide with loaded addresses after process restart | Deterministic test reproduced loaded node-1 and fresh node-1 on separate networks. New addresses now use upstream-style UUIDs with deterministic supplier in focused tests; existing persisted IDs are preserved. This does not repair already duplicated legacy IDs or establish connected-node remapping/migration. |
| Implemented; acceptance remains | Native 5.3 and 5.4 machine architectures | Registered selectable subclasses, versioned operators, 5.4 const/close syntax, real OpenOS and tier-1 unload/resume with locals/cycles/open binary handles. Fixed 5.4 native string-metatable persistence after a reproduced server crash. Versioned sandbox, allocation, timeout and exactly-once synchronized-callback unit tests pass. Wider host/platform/restart/client acceptance and default/migration policy remain open; existing CPUs are not silently switched. See NATIVE_LUA_PERSISTENCE.md. |
| Implemented: 7d4cef092 | Inventory-controller targeted world transfer: dropIntoSlot, suckFromSlot | Both callbacks reproduced missing, then implemented on robot/drone environments using sided block/entity capabilities, robot-relative actions/world fromSide, selected-first cargo insertion, simulation, protection and success-only delays. Seven new GameTests include real native Lua programs with installed upgrades. Build, 2121 units and 691 GameTests pass. See INVENTORY_CONTROLLER_TRANSFERS.md. Broader inspection API parity, protection mods and client acceptance remain open. |
| Implemented: a714219f2 | Item-contained inventory control: getItemInventorySize, dropIntoItemInventory, suckFromItemInventory | Eight new tests cover actual native robot/drone programs with installed upgrades, shulker capabilities/writeback/serialization, OC server hardware, partial capacity, data, self-insertion and stacked carriers. Fixed reproduced database description/copy materialization by tracking physically deposited entries separately. Existing unmarked entries remain descriptions; ambiguous historical physical-content migration remains an acceptance gate. Build, 2121 units and 699 GameTests pass. See ITEM_INVENTORY_CONTROL.md. |
| High | Microcontroller external control/side routing | Upstream Microcontroller.scala exposes start/stop/isRunning/lastError/isSideOpen/setSideOpen and external snooper/side nodes. Port node() returns the internal machine node and has no equivalent annotated callbacks or side-routing state. This requires real network separation/routing and persistence, not only adding method names. |
| Closed: 22b295fdb | Robot durability callback | Restored the missing upstream callback through ToolDurabilityProviders, with exact no-tool/undamageable results. Two GameTests reproduced the missing component callback and native Lua nil-call, then passed real component/native invocation, fixed damage fraction, save/load metadata and IMC-provider cases. Build, 2112 units and all 678 ordinary GameTests pass. Logs: robot-durability-red.log and robot-durability-verified.log under build/finish-port-implementation/. |
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

The beta range remains 3-6 concentrated workweeks, with low confidence, for full implementation plus beta acceptance. Native versions, targeted world transfers and item-contained inventory callbacks have implementation/server proof. This does not prove complete port parity. Microcontroller networking, the full semantic/config/resource/API audit, modern integration scope and ambiguous historical database-content migration still carry substantial uncertainty. Real-client visual/survival/multiplayer acceptance and broader restart/platform/load checks remain. Re-estimate at each completed milestone and when scope or a blocker changes the range; report implementation, automated proof and client acceptance separately. No client, Modrinth profile or Computer Use action occurred in these checkpoints.

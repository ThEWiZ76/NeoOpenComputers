# RAID facing, overlay geometry and menu access

Runtime c8c9bfd95800c97fad20839ecdc75daf3a4907ca, 2026-10-20. Related: [[RAID formatting and lifecycle persistence]], [[Process restart test]].

## Contract and changes

Upstream block/Raid.scala uses horizontal PropertyRotatable.Facing. block/Item.scala inverts non-robot rotation after reading player yaw, so a placed RAID faces the player. tileentity/traits/Inventory.scala limits usable range to eight blocks. client/renderer/tileentity/RaidRenderer.scala rotates error/activity overlays around the center according to raid.yaw.

The port used a nondirectional blockstate, while the orientable block model's front is NORTH and its renderer always drew on SOUTH. Its quad vertex winding pointed inward despite a positive-Z lighting normal; cutout face culling can therefore hide that surface from outside. RaidBlock now uses HorizontalDirectionalBlock, faces the placing player, supports vanilla rotation/mirror and supplies four model variants. Default facing is NORTH, matching the existing model front. Error and activity overlays rotate with the block around its center. Quad winding now agrees with the outward normal and UVs retain the existing three 4/16-wide slot bands, starting at2/16.

RaidBlockEntity.stillValid now requires the actual current loaded host, a nonremoved entity and player distance squared at most64. It does not load an absent chunk merely to keep a menu alive. This closes remote menu use beyond the upstream eight-block boundary and rejects a replaced host.

## Evidence

Two unit regressions fail before implementation: actual emitted quad winding and missing four-direction model variants. Their red XML is retained. Four ordinary server regressions fail independently in the763-test baseline: actual RAID BlockItem placement from all four player headings; rotation/mirror state contract; actual wrench-item use; and menu use beyond eight blocks. No screenshot or source-text match is used as proof of the geometry.

Three final geometry/resource unit tests capture actual VertexConsumer positions/normals/UVs and parse the shipped blockstate JSON. For all four directions and all three slots, the quad faces outward, its normal agrees with the selected face, every vertex is0.00125 outside the model plane and UV bands are preserved. These are headless geometry checks, not real GPU/rendered-pixel acceptance.

Four ordinary GameTests perform actual Survival BlockItem placement, every vanilla rotation/mirror, four direct ItemStack.useOn wrench calls (superseded by full player-interaction proof below) and exact menu boundaries at8 and8.125 blocks. Wrench rotation retains the same block entity, all three disks, same live filesystem and an open binary read offset. Tests also reject a replaced menu host.

Build/2146 unit tests and all828 mixed-provider tests pass with CC/AE2/Mekanism/EnderStorage/ProjectRed. Both no-optional-mod restart JVMs pass768 tests in fresh world oc-restart-raid-facing-20261020-01 (prepare PID44468, verify PID60156). The existing native Lua5.3 RAID probe now rotates the actual host three times before preparing and strictly verifies persisted WEST blockstate after restart, together with its retained local731, label, node identities and open binary offset/bytes. Both Gradle/server processes exit normally. This includes763 ordinary tests and five core process probes. Logs: build/finish-port-implementation/raid-facing-*-build/latest/debug.log and raid-facing-unit-red.xml.

Use the two-phase commands in [[RAID formatting and lifecycle persistence]] with a NEW oc-restart-raid-facing-* world; verify must use the same world. Run only one Gradle at a time and retain server logs before the next phase. No extra restart flag is needed.

## Remaining acceptance

Client/GPU visual checks of all four fronts, opacity/lighting/resource packs and activity behavior, live comparator circuits/neighbor updates, external protection and multiplayer behavior, arbitrary addon drives, broader historical-world migration and platform acceptance remain open. Other menu families require their own range/caller audit. This package is not a claim of complete RAID or whole-port visual acceptance. No Minecraft client, Modrinth, Computer Use or window movement occurred; actual client acceptance remains required on screen3 with focus coordination.

Whole-port beta remains3-6 concentrated workweeks, low confidence, for full implementation plus acceptance; no feature percentage is derived from test counts.


## 2026-10-20 actual player interaction correction

Runtime 9bc57db926942585d97c1f41a6825c009b50d0b6. After the user disabled pause-on-lost-focus, actual client MCP right-click with a wrench opened RaidScreen rather than rotating the empty north-facing RAID. The old direct ItemStack.useOn fixture bypassed the menu-first pipeline. A strengthened Survival fixture uses ServerPlayerGameMode.useItemOn and reproduces one failure among797 tests before the fix; do not treat the older direct-item test as complete player-interaction proof.

WrenchItem now implements NeoForge onItemUseFirst, matching upstream Wrench.scala's early hook. The existing useOn handler is retained for direct callers and both paths check Level.mayInteract before rotation. NeoForge's ordinary interaction event remains ahead of the early item hook. Successful rotation consumes the action once, preventing the RAID menu from opening; a nonrotatable crafting table still opens its normal menu. Four Survival turns retain the block entity, three disks, live filesystem and unread binary offset/bytes.

Build/2187 unit tests and all873 mixed-provider GameTests (CC, AE2, Mekanism, EnderStorage, ProjectRed and JEI) pass; all798 no-optional GameTests also pass, with complete summaries and normal terminal exits. The initial mixed run lost its daemon before completing and is not counted; retained as wrench-mixed-interrupted-latest/debug.log. The failed red GameTest Gradle invocation finished with BUILD FAILED; its RTK wrapper lingered after its child exited and was interrupted separately. No additional test claim derives from that wrapper.

Actual GPU inspection of the unchanged running c8 jar now covers the empty RAID's NORTH/EAST/SOUTH/WEST fronts with Sodium/Iris. Each state was set explicitly using test commands, verified by a unique conditional chat marker, then photographed from its outward face: all three dark red error indicators align with the model front, without the previous culling loss. This proves the old geometry fix for this setup, not the new wrench behavior. Screenshots: build/finish-port-implementation/raid-manual-state-<north|east|south|west>.png. The earlier raid-client-empty-east.png captures the failing GUI and is not orientation proof. The temporary empty RAID at124,-60,124 was removed; a fresh area read confirmed no nonair block. The player's test wrench remains intentionally.

Logs use wrench-red-*, wrench-mixed-retry-build.log with wrench-mixed-latest/debug.log, and wrench-absent-*. The running client has not received this newer jar; fixed-client wrench interaction, active disk indicators, lighting/resource-pack matrix, protection plugins and multiplayer remain open. See [[Minecraft test window]]. Full-port beta remains3-6 concentrated workweeks, low confidence.

Actual-client follow-up: the user restarted the tested9bc57db jar on screen3. MCP verified four Survival wrench turns EAST/SOUTH/WEST/NORTH with no menu, then empty-hand RaidScreen and complete temporary-block cleanup. Front screenshots show aligned error indicators. This closes the focused fixed-wrench client interaction gate; active disks, resource packs, multiplayer and wider RAID/port acceptance remain open. Installed hash/PID and evidence are in [[Minecraft test window]].

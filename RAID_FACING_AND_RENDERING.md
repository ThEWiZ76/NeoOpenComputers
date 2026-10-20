# RAID facing, overlay geometry and menu access

Runtime c8c9bfd95800c97fad20839ecdc75daf3a4907ca, 2026-10-20. Related: [[RAID formatting and lifecycle persistence]], [[Process restart test]].

## Contract and changes

Upstream block/Raid.scala uses horizontal PropertyRotatable.Facing. block/Item.scala inverts non-robot rotation after reading player yaw, so a placed RAID faces the player. tileentity/traits/Inventory.scala limits usable range to eight blocks. client/renderer/tileentity/RaidRenderer.scala rotates error/activity overlays around the center according to raid.yaw.

The port used a nondirectional blockstate, while the orientable block model's front is NORTH and its renderer always drew on SOUTH. Its quad vertex winding pointed inward despite a positive-Z lighting normal; cutout face culling can therefore hide that surface from outside. RaidBlock now uses HorizontalDirectionalBlock, faces the placing player, supports vanilla rotation/mirror and supplies four model variants. Default facing is NORTH, matching the existing model front. Error and activity overlays rotate with the block around its center. Quad winding now agrees with the outward normal and UVs retain the existing three 4/16-wide slot bands, starting at2/16.

RaidBlockEntity.stillValid now requires the actual current loaded host, a nonremoved entity and player distance squared at most64. It does not load an absent chunk merely to keep a menu alive. This closes remote menu use beyond the upstream eight-block boundary and rejects a replaced host.

## Evidence

Two unit regressions fail before implementation: actual emitted quad winding and missing four-direction model variants. Their red XML is retained. Four ordinary server regressions fail independently in the763-test baseline: actual RAID BlockItem placement from all four player headings; rotation/mirror state contract; actual wrench-item use; and menu use beyond eight blocks. No screenshot or source-text match is used as proof of the geometry.

Three final geometry/resource unit tests capture actual VertexConsumer positions/normals/UVs and parse the shipped blockstate JSON. For all four directions and all three slots, the quad faces outward, its normal agrees with the selected face, every vertex is0.00125 outside the model plane and UV bands are preserved. These are headless geometry checks, not real GPU/rendered-pixel acceptance.

Four ordinary GameTests perform actual Survival BlockItem placement, every vanilla rotation/mirror, four actual wrench item interactions and exact menu boundaries at8 and8.125 blocks. Wrench rotation retains the same block entity, all three disks, same live filesystem and an open binary read offset. Tests also reject a replaced menu host.

Build/2146 unit tests and all828 mixed-provider tests pass with CC/AE2/Mekanism/EnderStorage/ProjectRed. Both no-optional-mod restart JVMs pass768 tests in fresh world oc-restart-raid-facing-20261020-01 (prepare PID44468, verify PID60156). The existing native Lua5.3 RAID probe now rotates the actual host three times before preparing and strictly verifies persisted WEST blockstate after restart, together with its retained local731, label, node identities and open binary offset/bytes. Both Gradle/server processes exit normally. This includes763 ordinary tests and five core process probes. Logs: build/finish-port-implementation/raid-facing-*-build/latest/debug.log and raid-facing-unit-red.xml.

Use the two-phase commands in [[RAID formatting and lifecycle persistence]] with a NEW oc-restart-raid-facing-* world; verify must use the same world. Run only one Gradle at a time and retain server logs before the next phase. No extra restart flag is needed.

## Remaining acceptance

Client/GPU visual checks of all four fronts, opacity/lighting/resource packs and activity behavior, live comparator circuits/neighbor updates, external protection and multiplayer behavior, arbitrary addon drives, broader historical-world migration and platform acceptance remain open. Other menu families require their own range/caller audit. This package is not a claim of complete RAID or whole-port visual acceptance. No Minecraft client, Modrinth, Computer Use or window movement occurred; actual client acceptance remains required on screen3 with focus coordination.

Whole-port beta remains3-6 concentrated workweeks, low confidence, for full implementation plus acceptance; no feature percentage is derived from test counts.

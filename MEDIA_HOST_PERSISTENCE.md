# Media host persistence and rack main bus

Checkpoint c1094c68a1a419a178c50e3c43e88d24e142cbb4, 2026-10-20. Related: [[../Project]], [[../02 plans/ComputerCraft integration]], [[ComputerCraft media and diskdrive persistence]], [[Process restart test]].

## Corrected contracts

The original server/component/Server.scala uses machine.node as the rack server's node. The port instead created a separate outer node, isolating configured main-bus components from the computer. ServerRackMountableEnvironment now exposes the machine/connector itself. The nested machine snapshot owns restoration of its address, energy and execution state; the obsolete outer node tag is not loaded into that connector. Loading the old outer node's missing buffer as zero caused a real existing LuaJ rack boot regression during development; the authoritative nested snapshot preserves the proper behavior. Existing rack power/boot and full profiles pass after correction.

Machine.saveComponentData is an additive default hook for persisting a component before its stack is copied, shrunk or replaced. The default saves the machine, while SimpleMachine overrides it to save only the installed component environment, without serializing the Lua VM. Computer cases and rack servers call it before split/take/replacement/clear. Rack diskdrives save their own stack-backed media environment before the same mutations. This closes data/identity loss for ordinary OC floppies and internal HDDs as well as supporting CC media.

ComputerCaseBlockEntity now retains pending full machine state when loadAdditional runs without a world. Inventory/client fields are decoded, while world-bound component creation and machine restoration are replayed by the existing onLoad path after a server world is assigned. Saved state remains available to serialization while deferred. This prevents the tier3 case's CC floppy from being silently skipped during real chunk load.

## Evidence

Five regular regressions failed against the preceding implementation: rack floppy bytes after removal; a disconnected configured rack main bus; tier3 internal CC filesystem absent after a world-less load; case floppy identity loss; and internal server HDD bytes after removal. The final ordinary CC profile passes751 cases. Core storage tests exercise all four removal paths, stable addresses and exact0/128/255 bytes for rack floppies, case floppies/HDDs and server HDDs. The native Lua5.3 rack program uses actual server/drive mountables and configured SOUTH main-bus mappings to write/read exact bytes; it does not bypass the bus with direct test graph connections.

Two added opt-in process probes use a tier3 case's internal native CC floppy and a native rack-server/rack-drive. Both retain Lua local731, machine/media addresses and an open read offset across normal process restart, then independently check the native CC mount's full bytes and label. World oc-restart-media-hosts-20261020-03, prepare PID6864, verify PID47060; both757-test server and Gradle handles exit normally. The original external block-drive, tablet, microcontroller, bundled-redstone and chunkloader probes also run in this profile.

Fresh forced chunks expose their NBT before queued Forge onLoad creates/initializes the live hosts. Early versions of the new test inspected the case or rack too soon. Read-only region/NBT inspection proved saved case IDs/running state and rack inventories were intact; final tests wait two real level ticks BEFORE obtaining the live hosts, then perform the same strict identity/continuation/byte assertions. Failed worlds01/02 and first/second logs remain retained. These were test-readiness errors, not claimed storage-loss fixes. The first green attempt also retained an inappropriate direct post-program read exceeding the real call budget; byte verification now runs inside the native program itself.

Final build/2143 units,797 mixed-mod and735 no-optional-mod tests pass;757 CC-only tests pass in each restart phase. The mixed profile enables CC/AE2/Mekanism/EnderStorage/ProjectRed. All final process handles terminate normally. Evidence files under build/finish-port-implementation are media-host-red/green/mixed/absent/restart-prepare/restart-verify-build/latest/debug.log. The original five failures and intermediate diagnostics are retained separately.

Use the documented two-phase CC-media restart command with a NEW oc-restart-* world and -Pneoopencomputers.restartComputerCraftMedia. See [[ComputerCraft media and diskdrive persistence]]; the flag now includes these two host probes. Run one Gradle at a time and retain logs before the next profile.

## Remaining gates

TabletRuntime creates components with an actual server player/world; robot runtime component sync explicitly requires a server world and detaches/saves before stack mutation. Those source checks do not prove complete native CC-media player flows. Tablet expansion replacement/removal and robot media access, full rack-module detach, older-world/addon migration, crash/load/protection and client/survival/multiplayer/platform acceptance remain follow-ups before closing the media checklist. Lua-object conversion and the wider integration/resource/API audit remain required. No client/Modrinth/Computer Use/window movement occurred. Actual client acceptance remains on screen3 with focus coordination.

Whole-port beta remains3-6 concentrated workweeks, low confidence; test counts are not a completion percentage.

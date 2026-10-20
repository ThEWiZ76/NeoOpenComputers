# Adapter upgrade ownership and persistence

Runtime 1c688f06d2768623e553d31dd13ffed049b7a2c6, 2026-10-20. Related: [[Portable media and menu persistence]], [[Network addresses]], [[Process restart test]].

## Restored behavior

Upstream common/tileentity/Adapter.scala uses ComponentInventory. Its save/remove paths persist each component into its actual driver-owned item tag, and a new item gets its own environment. The port reused an environment whenever the driver was identical, even if the installed ItemStack was replaced. It also reused host-level itemEnvironmentData for later items, leaking database contents/node identities between upgrades. Saving the inventory before the live component produced an outdated item snapshot.

AdapterBlockEntity now tracks the installed stack by identity, saves the current environment to that driver's dataTag and refreshes CustomData before split/take/replacement/clear or menu copying. Disposal clears the active reference before disconnecting the old node. Serialization updates the component before encoding inventory. The existing oc:itemComponent snapshot is retained for backward compatibility, consumed only for the inventory loaded with it and cleared on item mutations; later upgrades do not inherit it. Driver-specific root data outside the driver's tag is preserved. Clearing the environment data tag before replacing its snapshot matches upstream ComponentInventory.save semantics.

World-bound initialization is deferred until the world exists. onLoad creates/restores the actual upgrade and rejoins its real neighbor network. It does not require a player click or a test to call refresh after a chunk reload. Server-side loaded-chunk guards in network discovery remain unchanged.

## Evidence

Seven new regressions fail against the preceding implementation: current data lost by split/shift copy/save snapshot; old data leaking after take/clear; same-driver replacement retaining the old environment; and different-driver replacement inheriting its node identity. The corrected baseline is749 tests with exactly7 failures. A first implementation fixes these seven; two added world-less chunk-load tests then fail because the loaded adapter never initializes its upgrade (751 tests, exactly2 failures). onLoad restoration fixes both, including a legacy save whose newest data exists only in oc:itemComponent.

Ten ordinary tests now cover these cases plus actual NeoForge InvWrapper extraction, simulation without ownership changes, both menu return/destination snapshots and exactly one moved item. The wrapper test constructs the supported wrapper around the Container; it does not claim this patch registers a new block capability or that every third-party simulation/copy caller is audited. Catalogue entries are item descriptions, not claims of physical item transfer.

Build/2143 units and all817 mixed-provider GameTests pass with CC/AE2/Mekanism/EnderStorage/ProjectRed. A new core two-process probe passes with all756 tests in each no-optional-mod restart phase: world oc-restart-adapter-upgrade-20261020-01, prepare PID3732, verify PID10524. This includes752 ordinary no-optional-mod tests and four process probes. The native Lua5.3 computer retains local731 and its actual database proxy/hash, adapter/database/computer addresses survive, and the resumed Lua callback clears the actual restored catalogue. The test does not manually refresh/reconnect restored hosts. Both Gradle/server processes exit normally. Evidence: build/finish-port-implementation/adapter-upgrade-*-build/latest/debug.log.

## Run the process probe

Use a NEW world name for prepare; verify must use that same world. Run one Gradle at a time, retain run/gameTestServer/logs/latest.log and debug.log before the next run, and wait for terminal exit.

~~~powershell
rtk proxy cmd /d /c "gradlew.bat build runGameTestServer -Pneoopencomputers.restartPhase=prepare -Pneoopencomputers.restartWorld=oc-restart-adapter-upgrade-NEW --no-daemon --console=plain"
rtk proxy cmd /d /c "gradlew.bat build runGameTestServer -Pneoopencomputers.restartPhase=verify -Pneoopencomputers.restartWorld=oc-restart-adapter-upgrade-NEW --no-daemon --console=plain"
~~~

## Remaining gates

Other inventory families, arbitrary addon drivers, simulated extraction snapshot consistency and live automation/client/survival/multiplayer/platform acceptance remain open. The new process probe runs in all explicit restart profiles; this checkpoint's pair used no optional mods, while optional integration behavior has the mixed ordinary proof above. Wider CC/AE2 process combinations remain their separate acceptance gates. Adapter distance/access and external-driver behavior still require the broader API/interaction audit.

RAID is intentionally separate: upstream Raid.scala destroys array data on disk removal and wipes individual disks on assembly. Do not apply a generic preserve-each-disk fix to RAID without checking that contract. This checkpoint makes no RAID runtime change or full RAID parity claim.

No client, Modrinth, Computer Use or window movement occurred. Real visual acceptance remains required on screen3 with focus coordination. Whole-port beta remains3-6 concentrated workweeks, low confidence, for full implementation plus acceptance; no feature percentage is inferred from test counts.

# Mekanism integration and FE item charging

Runtime checkpoint: 8e852cb544df38e7bba0becd166a4e6ab64d0465. Minecraft 1.21.1 / NeoForge 21.1.234 / Mekanism 1.21.1-10.7.19.85. Related: [[../02 plans/Mekanism integration]], [[../02 plans/Complete portering]], [[../04 ai-context/Modern integration scope]].

## Scope and pinned contract

The upstream four Mekanism integration files register a GasStack converter and IMekWrench recognition/usage. They do not implement a Mekanism machine component. Modern ChemicalStack and item abilities replace those APIs. getGasesInNetwork belongs to ExtraCells scope and is not silently counted here. The upstream direct-Joule power trait is commented out; modern native FE block/item interoperability is verified instead of claiming that commented code was an active reference feature.

The [publisher's setup](https://github.com/mekanism/Mekanism/tree/1.21.x) links ModMaven. Publisher metadata and exact api/sources/POM artifacts verified 1.21.1-10.7.19.85 on 2026-10-20. Pinning is a tested target, not a perpetual latest-version claim. The full compileOnly artifact exposes the public implementation helpers used by real configurator/cube/cable tests; runtime is opt-in with withMekanism. The mod is not bundled or published as a required dependency. Discovery files and hashes are preserved under build/finish-port-implementation/integration-discovery/mekanism.

## Implemented

- Optional ChemicalStack converter: amount as exact Java long, name as modern registered chemical ID, label from the chemical display component, and numeric registry id only when insertIdsInConverters is enabled. Empty stacks report Mekanism's normalized empty sentinel with amount0. No mutation or unit scaling. Hydrogen, oxygen, pigment red and dirty-iron slurry prove the unified chemical mapping; nested conversion and count9007199254740993 are covered.
- Wrench recognition follows Mekanism configure/empty/rotate/dismantle abilities. Tool identity remains recognized across all eight configurator modes, while block usage requires WRENCH_DISMANTLE, equivalent to the [old configurator WRENCH check](https://raw.githubusercontent.com/mekanism/Mekanism/1.12/src/main/java/mekanism/common/item/ItemConfigurator.java). The native configurator is not consumed. Drone pickup deliberately retains upstream type-only recognition, including the default configuration mode.
- Adapter/charger interactions now require holdsApplicableWrench before mutation. The charger also calls wrenchUsed, so ProjectRed's screwdriver consumes exactly one additional durability on a charger interaction. The existing same-side/opposite-side behavior remains. Wrong configurator modes and an offhand-only tool cannot bypass main-hand eligibility.
- The OC wrench API usage callback is restored to eligibility/notification, independent of rotation. Its ordinary item use path still performs rotation. A new core GameTest proves a nonrotatable adapter remains eligible and charger inversion/usage notification do not rotate the charger. The old callback conflated these operations and would break the restored eligibility checks.
- Generic NeoForge FE item charging restored in ModItemCharges after the existing OC Chargeable path. It honors canReceive, simulation and actual receiveEnergy return values, returning unspent energy in OC units. OC's precise fractional charge path keeps priority. Charger slots and normal charge cycles now accept real external FE items without a Mekanism-specific item-charge provider.

## Real server and native proof

Seven Mekanism tests and one core wrench test, plus an expanded ProjectRed wrench test:

- Actual registered chemicals, exact long quantities, nested conversion, debug-ID policy and unchanged source. A test component feeds a real ChemicalStack through the ordinary OC callback conversion path to a native Lua5.3 machine; Lua sees an integer64 amount exactly. This fixture is not a newly exposed production Mekanism component.
- All eight configurator modes on actual adapter/charger blocks with a survival mock player; eligibility, actual mutation/result, offhand rejection and unchanged item data. Default-mode drone pickup yields exactly one packed drone, as upstream specifies.
- Real energy tablet FE simulation without mutation, actual accepted amount/surplus, near-full two-FE acceptance, full rejection and non-energy rejection. OC battery fraction0.125 proves the FE fallback does not preempt the native Chargeable provider.
- Actual charger cycle, full target and empty source. Native tablet energy is exact; OC double-buffer accounting is checked within1e-9. The fixture attaches the charger's real OC network before transferring energy.
- Real basic energy cube and universal cable feed exactly1000FE into an OC power converter; cube and cable end empty. The fixture configures the native cube's output side/ejection explicitly, because direct block placement does not apply item placement configuration.

The corrected isolated baseline (mekanism-isolated-red-build.log) executes734 tests and fails the six missing Mekanism cases plus the core wrench API case. Cable interoperability already passes before production edits. First implementation failures were fixture details: the integer test initially selected Lua5.2, which has no math.type, and the charger needed its OC network attached and double accounting tolerance. Native integer proof explicitly uses Lua5.3.

Build/2142 unit tests and773 mixed Mekanism+AE2+EnderStorage+ProjectRed+CC GameTests pass, with clean process exit. Mekanism-only profile passes734; no optional mods passes727. The added packaging/POM unit guard rejects bundled Mekanism/AE2/ProjectRed/CC/CodeChicken classes and required published dependencies on those optional groups. Logs: mekanism-mixed-verified-build.log, mekanism-alone-verified-build.log and mekanism-absent-verified-build.log, with saved latest/debug logs. These are real server/native checks, not client visual or multiplayer acceptance.

## Headless fixture networking

Mekanism sends required tracking packets. Existing deprecated mock players use EmbeddedChannel without a negotiated mod-channel setup. First mixed tests exposed that harness issue as player_data/reset_client rejection. Only the explicit withMekanism GameTest profile registers ModdedGameTestConnections; only in-memory/EmbeddedChannel mock connections use NeoForge's public configureMockConnection test helper, before tracking. Real TCP connections and normal production startup are unaffected. This allows the existing server suite to run with actual registered payloads; it does not prove real client negotiation/transport acceptance.

## Run in an isolated ordinary world

```powershell
rtk proxy cmd /d /c "gradlew.bat build runGameTestServer -PwithMekanism -PwithAE2 -PwithEnderStorage -PwithProjectRed -PwithComputerCraft -Pneoopencomputers.gameTestWorld=oc-tests-mekanism-review-20261020-01 --no-daemon --console=plain"
exit $LASTEXITCODE
```

Use a fresh oc-tests-* name for independent ordinary runs. The property validates its prefix and cannot be combined with restartPhase; actual restart probes retain their separate oc-restart-* mechanism. One Gradle process at a time: wait for terminal exit and check the expected test summary, then preserve latest.log before the next run. The verified worlds are oc-tests-mekanism-mixed-20261020-02, oc-tests-mekanism-alone-20261020-01 and oc-tests-mekanism-absent-20261020-01.

## Unresolved unload observation and remaining gates

A reused default-world red run completed733 tests, then hung while saving worlds. Two thread dumps of verified owned PID45648 show ChunkMap.scheduleUnload/processUnloads continuously executing CompletableFuture callbacks during stopServer. Saved as mekanism-shutdown-threads.txt and -second.txt. The owned server was stopped after the evidence; its Gradle handle then returned terminal failure. The reused world remains available for investigation. A successor was started too early, hit the world lock and returned a misleading BUILD SUCCESSFUL without executing tests; that run is discarded (mekanism-core-contract-red-build.log). This procedural mistake is not counted as verification.

All final isolated runs terminate cleanly. This does not establish the unload-loop cause or prove Mekanism/mock networking caused or fixed it; the earlier independent unload observations remain relevant beta gates.

Client rendering/menus/tool interaction, survival/multiplayer/protection, cross-platform and broader restart/load/configuration variants remain open. No client/Computer Use/Modrinth install/window move occurred. Actual Minecraft client acceptance remains required on screen3 with desktop coordination. The other integration families, CC peripheral/media/relay audit and full semantic/config/resource/API audit remain in the global plan.

Whole-port beta estimate remains **3-6 concentrated workweeks, low confidence**, including full implementation, bugfixes and client acceptance. Test counts are evidence, not a completion percentage.

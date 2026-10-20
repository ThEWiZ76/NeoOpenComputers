# Addon floppy factory persistence

Runtime 80f2679c62cee8ebab07a288fff860f502e5e797, 2026-10-20. Related: [[Loot disk cycling]], [[Process restart test]], [[../02 plans/JEI integration]].

## Contract and correction

Upstream common/Loot.scala registerLootDisk stores active mod ID plus the supplied name as the persistent factory key. Public ItemRegistry.registerFloppy instead generated a new UUID on every registration. A saved disk therefore retained an ID absent from the next JVM's registry, and FloppyItem could not recreate its environment.

Public registration now uses ModLoadingContext.get().getActiveNamespace() + ':' + name. Addons must register from their owning mod's loading context with a stable name unique within that mod on every startup. The out-of-context namespace is minecraft. Names remain plain factory strings, including spaces, rather than ResourceLocation paths. The explicit bundled factory IDs, item data keys and API signatures remain unchanged. Same names from different mods do not collide. The public facade documents the registration contract.

## Evidence

Opt-in AddonFloppyProcessRestartGameTests registers a public addon factory before the restart world's hosts load. Its fresh read-only memory filesystem contains bytes97,0,128,255,42. A real disk drive and native Lua5.3 computer open probe.bin, consume the first byte and wait with a retained local731 and open handle. A chest checkpoint records factory/drive/computer identities and process ID. Fresh-JVM verify requires a different PID, resolves the saved disk against the new registry, checks identities and resumes the VM to read the remaining four bytes without reboot or reopening the file.

The unchanged UUID implementation prepared successfully in oc-restart-addon-floppy-red-20261020-01 (PID33008,804 tests), then failed exactly one required test in a different verify JVM: Saved addon floppy no longer resolves its factory in the fresh JVM. Preserve this failed world as evidence; it is not the green fixture.

With stable identities, fresh world oc-restart-addon-floppy-fixed-20261020-01 passes all805 no-optional tests in both prepare PID46984 and verify PID42364, with full summaries and normal Gradle exits. This includes799 ordinary cases and six process probes. The new ordinary case checks actual neoopencomputers/neoforge registration contexts, equal names, serialized ItemStack round trips, correct factory resolution and cycling opt-out isolation. A unit checks deterministic minecraft fallback across two fresh registries.

Build/all2188 units and all874 mixed-provider ordinary GameTests pass with JEI, CC, AE2, Mekanism, EnderStorage and ProjectRed. Logs: build/finish-port-implementation/addon-floppy-{red,fixed}-{prepare,verify}-{build,latest,debug}.log and addon-floppy-mixed-{build,latest,debug}.log. New test registrations are opt-in for restart probes; the ordinary namespace fixture restores the previous API/container in finally.

## Reproduce

Run one Gradle at a time. Choose a NEW oc-restart-* world for prepare, then use that same world for verify. Save latest/debug logs before starting the next process. Verify complete GameTest summaries as well as terminal exit codes.

```powershell
rtk proxy cmd /d /c "gradlew.bat --no-daemon runGameTestServer -Pneoopencomputers.restartAddonFloppy -Pneoopencomputers.restartPhase=prepare -Pneoopencomputers.restartWorld=oc-restart-addon-floppy-NEW"
exit $LASTEXITCODE
```

In a subsequent invocation replace prepare with verify, keeping the exact world name. The property requires valid isolated restart phase/world arguments; it does not require optional mods.

## Remaining acceptance

Old random UUID keys do not encode the owning mod and name. This fix does not guess aliases, repair the retained failed UUID world, or establish arbitrary1.12 world migration. Addon rename/missing registration/collision policy outside the established mod/name contract, external loot directories, authoritative client metadata synchronization, JEI runtime disk display and broader multiplayer/addon/platform/client gates remain open. The currently installed client is the earlier wrench checkpoint9bc57db, not this factory fix. Full beta estimate remains3-6 concentrated workweeks, low confidence.

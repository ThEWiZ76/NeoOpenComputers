# TIS-3D serial integration

Related: [[Manuals]], [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Modern integration scope]], [[../04 ai-context/Port parity matrix]].

Runtime `748f5e993819e9d097fe512f2d9bbd8efee3116c` restores the upstream adapter serial protocol for TIS-3D2.0.0 on Minecraft1.21.1. Pinned optional target maven.modrinth:tis3d:MC1.21.1-neoforge-2.0.0, MarkdownManual1.2.8 and Architectury13.0.11. The publisher artifacts require NeoForge>=21.1.248; both published TIS1.7.7 and2.0 were inspected. Core default remains21.1.234. No provider metadata was weakened, and no dependency is bundled or published as required. Optional mod metadata accepts TIS2.x and rejects incompatible1.x/3.x.

Sources: original OpenComputers SerialInterfaceProviderAdapter.scala at baseline7d9eac5; [published NeoForge release](https://modrinth.com/mod/tis3d/version/MC1.21.1-neoforge-2.0.0), [pinned TIS source](https://github.com/MightyPirates/TIS-3D/tree/fff58fd3dbca7ba08a5ed0262274555c873641c9), [author setup](https://github.com/MightyPirates/TIS-3D/blob/1.21.1/README.md). Release source/API, Modrinth metadata, both mod descriptors and SHA512-verified downloaded artifacts are retained under build/finish-port-implementation/integration-discovery/tis3d. The README's older example is not used as release-version evidence.

## Contracts

- TIS2's serial_interface_provider registry receives neoopencomputers:adapter through a deferred register, gated behind installed-mod classloading. Only an actual Adapter at an already loaded position matches; stillValid checks instance identity, so replacing the block invalidates the old interface. No forced chunk load.
- Each TIS serial interface exposes an OC serial_port component. Its node joins the adapter network when TIS polls canRead, including empty-output polls; reset clears both queues and detaches the node. Reading enable remains as upstream across reset.
- setReading(boolean) returns no values. read() returns the oldest signed short or no values. write(integer) wraps to16bits and returns true, or false,"buffer full" at128 queued values. Passive TIS input is gated by reading enable and128 capacity; output peek does not consume and skip consumes once. Queue operations and state snapshots synchronize on the interface.
- TIS owns interface persistence. The original flat node fields plus readBuffer/writeBuffer int arrays and isReading boolean are retained. Load reconstructs signed shorts and rejects oversized queues before mutation. No new inline disk backend or separate SavedData store was added.
- Client-only MarkdownManual document registration serves the existing en_us/ru_ru/zh_cn protocol resources under doc/tis3d, and the provider supplies a localized-path protocol reference. Actual client/manual navigation is still unverified; no visual polish was performed.

## Evidence

tis3d-red-build.log: the real TIS2 registry cannot find a provider for an actual OC adapter, exactly1 required failure among852 tests, normal failure exit. Initial new-loader cache creation took10m6s; one retained Gradle thread dump showed source packing, not a deadlock. tis3d-fixed-build.log then passes all852. tis3d-native-build.log is an Architectury compile-classpath fixture error, not a runtime regression; adding compileOnly Architectury resolves it.

Final tis3d-native-fixed-build.log: build2239 units (0 failures/errors/skips) and all853 required tests pass on21.1.248. The contract case covers signed FIFO128 in both directions, explicit receive enable, empty reads, full error, peek/skip, saved address/buffers and adapter removal. The native case uses a real powered TIS controller/casing/SerialPortModule with real pipes and a small test endpoint module. A physical adjacent OC computer discovers serial_port through its adapter; native Lua writes65535, the endpoint receives exactly one-1, and Lua receives-32768. Endpoint/serial modules, power and VM are cleaned after success. This is real scheduled pipe transport with a test endpoint, not a TIS assembly-program/client acceptance test.

Final tis3d-absent-build.log: build/all units and851 required tests pass on the unchanged21.1.234 core without optional providers. Final tis3d-final-mixed-build.log: all938 required tests pass on21.1.248 with TIS/MarkdownManual/Architectury plus JEI, CC, AE2, Mekanism, EnderStorage and ProjectRed. Each run has a complete required summary, BUILD SUCCESSFUL and normal terminal exit; archived build/latest/debug logs. Packaging guard includes TIS/manual/Architectury class prefixes and maven.modrinth in the excluded required-POM groups.

## Run

```powershell
rtk proxy cmd /d /c "gradlew.bat --no-daemon build runGameTestServer -PwithTIS3D -Pneo_version=21.1.248 -Pneoopencomputers.gameTestWorld=oc-tests-tis3d-your-fresh-name"
```

Use a fresh owned test-world name. Optional flags for the mixed profile: -PwithJEI -PwithComputerCraft -PwithAE2 -PwithMekanism -PwithEnderStorage -PwithProjectRed. Only one Gradle at a time. Core absence omits all optional flags and neo_version override. Installing TIS in the user testinstance requires a coordinated loader/dependency update; this checkpoint installs only the independently tested21.1.234 core JAR.

## Functional client batch prepared

After successful core absence build, captured build/finish-port-implementation/client-core-748f5e993/neoopencomputers-0.1.0.jar, SHA256 b39575aa72a12d6de30becc9d77efe2741c5c7ac4426be2b6600e03058f23cd8. The closed Modrinth NeoOpenComputers test instance received this exact JAR, without changing its21.1.234 loader or other mods; no TIS/manual/Architectury JARs are installed there. Old JAR plus all162 saves/config files were backed up and ZIP-CRC checked at C:/Users/rolan/AppData/Local/Temp/oc-client-backups/748f5e993-20261020T182845869891Z; installation.json records old/new hashes and target. Installer rechecks the exact profile is closed and hashes the temporary and final JAR. First attempt stopped before replacement because the local Python lacks Path.is_junction; corrected to the Windows reparse attribute guard. No user world was opened or modified by the install. User subsequently started on screen3; core Survival storage acceptance now has real-client evidence in [[Native volume import]]. TIS-specific client/manual acceptance remains pending because TIS is not installed in this profile. Computer Use/focus remained off.

## Remaining acceptance

Real client protocol/manual navigation, actual player installation/protection/multiplayer, TIS assembly-program endpoint, true TIS-world JVM restart/buffer restoration, all casing rotations/chunk boundaries and prolonged load remain open. No TIS process-restart claim follows from same-process interface NBT. Other legacy provider families still require implementation or an evidence-backed compatibility decision. Full port goal active; technical functionality precedes polish. Beta estimate3-6 concentrated workweeks, low confidence.

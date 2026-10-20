# EnderStorage adapter integration

Related: [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Modern integration scope]], [[../04 ai-context/Port gap triage]].

Runtime 820fee37bb656273720d460aaede4eeaa80ae4e8, 2026-10-20: build and all2138 unit tests pass. With EnderStorage, ProjectRed and ComputerCraft, all728 ordinary GameTests pass; with EnderStorage alone, all728 pass; without optional mods, all726 pass. All servers shut down normally. No client, Modrinth profile or Computer Use action occurred.

## Source and supported dependency

The upstream integration is integration/enderstorage/DriverFrequencyOwner.java plus ModEnderStorage.scala at7d9eac5584e238a890a388a0f697c27c7391caa4. The current official [1.21.1 source](https://github.com/TheCBProject/EnderStorage/tree/1.21.1) was inspected atd801697cac7493095d8c98b8d4f31a9f6f76f757. Published Maven metadata and sources were fetched from the publisher's [repository](https://maven.covers1624.net/io/codechicken/EnderStorage/maven-metadata.xml), not an unrelated project slug. The pinned test/compiler artifact is io.codechicken:EnderStorage:1.21.1-2.13.0.191 with CodeChickenLib1.21.1-4.6.1.529. The published Frequency record and TileFrequencyOwner API were checked against those sources.

EnderStorage is optional. Metadata allows its2.13 line starting at2.13.0.191 and orders loading after it; missing EnderStorage does not load its API-dependent nested classes. No EnderStorage or CodeChickenLib binaries are embedded, and neither becomes a hard dependency in the published POM. A Gradle withEnderStorage switch only adds the pinned development/test runtime. No user profile was changed or external release deployed.

## Lua contract

Connect an adapter beside a real Ender Chest or Ender Tank. The component's preferred name is ender_chest or ender_tank; its five upstream callbacks remain synchronized rather than direct:

| Callback | Result / behavior |
| --- | --- |
| getFrequency() | Table of three color numbers in left/middle/right order; numbers are0-15. |
| setFrequency(left,middle,right) | Accepts three0-15 values, or one packed0-4095 integer. No return values. Invalid input raises invalid frequency before changing the channel. |
| getOwner() | global for a public frequency, stored owner-name text for a named private frequency, or its UUID string if modern data contains no cached owner name. |
| getFrequencyColors() | Three color-name strings in left/middle/right order. |
| getColors() | Zero-based map0-15 to names such as WHITE and BLACK, retaining the enum-string conversion used by upstream. |

Changing frequency constructs a new modern Frequency with both original owner UUID and owner-name component retained, then calls the mod's setFreq API so normal persistence/update hooks run. No owner-changing callback is added. Callback access checks that the original tile is still present in a loaded chunk; discovery does not deliberately load a missing first-hop chunk. This does not establish non-loading behavior inside every third-party method.

## Evidence and limits

Two optional GameTests use actual EnderStorage blocks. One checks both chest/tank preferred names and all five callbacks, all16 color mappings, packed/triple arguments, negative/overflow rejection without state mutation, named private and public owners, and EnderStorage's real save/load of the frequency and owner. The standalone run also includes a UUID-only owner fixture added after the mixed run; production integration code was unchanged between those runs. The second boots a native Lua computer, discovers its real adjacent adapter's Ender Chest and exercises frequency/color/public-owner/error behavior through component.proxy. It requires zero return values from setFrequency.

Before implementation, both tests failed: no OC driver for the real chest, and native Lua could not discover ender_chest. Logs under build/finish-port-implementation/: enderstorage-driver-red.log, enderstorage-mixed-verified.log, enderstorage-standalone-verified.log and enderstorage-absent-verified.log. Packaging checks separately confirm no optional-mod classes/nested jars or hard POM entries. The absent run verifies classloading with no EnderStorage/CCL/ProjectRed/CC installed.

This proves the tested server adapter contract, not live client/analyzer presentation, every protection/mod interaction, dedicated EnderStorage disk/JVM restart, arbitrary addon ownership data or long-load acceptance. Other modern integration groups remain independent work.

## How to run/use this

Install compatible EnderStorage and its own CodeChickenLib dependency alongside the port, then connect an adapter to the chest/tank and your computer. From Lua, find component.list('ender_chest') or component.list('ender_tank'), obtain a proxy and call the methods above. Three color arguments map to packed left*256 + middle*16 + right.

From M:/development/OpenComputers, run each profile sequentially:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithEnderStorage --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithEnderStorage -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat build runGameTestServer --no-daemon --console=plain
```

The EnderStorage-specific probes are absent without the mod; requesting the profile without loading the mod is a test setup failure, not a silent skip.

# OpenComputers NeoForge

Java-first community port of [MightyPirates/OpenComputers](https://github.com/MightyPirates/OpenComputers) to Minecraft 1.21.1 on NeoForge.

## Current State

This repository is a clean NeoForge 1.21.1 scaffold. It intentionally does not compile the old Scala implementation. The old mod is used as a behavioral reference while systems are ported to Java incrementally.

Chosen upstream reference branch:

- `master-MC1.12`

Reason: it is the newest useful maintained OpenComputers line. The branch still targets Minecraft 1.12.2, but it has newer maintenance and build tooling than the misleading `master-MC1.16` branch.

## Requirements

- JDK 21
- Git
- Gradle wrapper included in this repository

## Build

```powershell
.\gradlew.bat build
```

The built mod jar is written to `build/libs`.

## Run Development Client

```powershell
.\gradlew.bat runClient
```

## Run Development Server

```powershell
.\gradlew.bat runServer
```

On first server run, accept the generated EULA in the run directory.

## Porting Approach

1. Keep NeoForge 1.21.1 project Java-only.
2. Port OpenComputers systems in small slices from `master-MC1.12`.
3. Start with stable contracts and internal model code before UI and integrations.
4. Re-add optional mod integrations only when current 1.21.1 APIs exist.

## Licensing

This port is MIT licensed. Original OpenComputers code is MIT licensed by Florian "Sangar" Nücke and contributors. Keep upstream notices and third-party license files when porting code, assets, Lua runtimes, or bundled libraries.

The NeoForge MDK template license remains in `TEMPLATE_LICENSE.txt`.

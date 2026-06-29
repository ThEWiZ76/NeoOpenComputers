# NeoOpenComputers

Java-first community port of [MightyPirates/OpenComputers](https://github.com/MightyPirates/OpenComputers) to Minecraft 1.21.1 on NeoForge.

The project name intentionally differs from the original mod: **NeoOpenComputers** identifies this as a newer NeoForge-focused port.

## Current State

This repository is an active Java-first NeoForge 1.21.1 port. It intentionally does not compile the old Scala implementation. The old mod is used as a behavioral reference while systems are ported incrementally.

Current pushed first-smoke base:

- Latest pushed `develop`.
- The Obsidian port dashboard records the exact last verified commit and verification timestamp.

Current verification evidence:

- Unit/build gate: `.\gradlew.bat test build --no-daemon --console=plain`
- GameTest gate: `.\gradlew.bat runGameTestServer --no-daemon --console=plain`
- Latest GameTest result: `428/428` required tests passed.
- Bounded client launch smoke reaches resource reload and texture atlas creation without print/model/missing-texture/error matches.
- Tier 2 and tier 3 screens open their terminal GUI after a fresh client restart.

The port has broad API, machine, network, filesystem, terminal, screen/GPU/input, modem/redstone, storage, inventory/tank/transposer, rack/server, nanomachine, printer, print, manual, player-facing failure feedback, and packaging slices in place. It is ready for first technical smoke testing, not release-ready.

Robots, drones, and block microcontrollers are not available in this alpha build. Computer cases, servers, racks, screens, keyboards, storage, cards, upgrades, and microcontroller case items are the current alpha focus.

See the [First Alpha Testing Guide](ALPHA_TESTING.md) for the current install, smoke-test, known-gap, and issue-reporting checklist.

GitHub Actions are intentionally disabled until the mod is ready enough for CI. Do not add `.github/workflows` yet.

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

## First Technical Smoke Test

Use this for local crash finding and first in-world proof. This is not a community beta checklist.

1. Build the jar:

```powershell
.\gradlew.bat test build --no-daemon --console=plain
```

2. Run GameTests:

```powershell
.\gradlew.bat runGameTestServer --no-daemon --console=plain
```

3. Start the development client:

```powershell
.\gradlew.bat runClient --no-daemon --console=plain
```

Restart the Minecraft client after replacing the mod jar; a running JVM keeps the old jar loaded.

4. In a local test world, check these flows:

- Computer case, screen, and keyboard place without crashing.
- OpenOS/Lua prompt boots.
- Basic filesystem, EEPROM, floppy, and disk-drive actions work.
- Screen output and keyboard input survive save/reload.
- Terminal item key input, paste, mouse click/drag/release, and scroll reach the bound computer.
- Disk-drive floppy data survives save/reload.
- Redstone, modem, inventory, tank, and transposer each get one basic smoke pass.
- Texture picker reports usable atlas texture names such as `minecraft:block/stone`.
- Printer creates a print item.
- Print item visually uses configured shape data.
- Placed print rotates, renders configured shape data, drops configured data, handles button-mode redstone activation, held-item activation, beacon-base setting, tooltip data, opacity setting, and legacy texture names.

5. Save logs and screenshots for any crash, missing texture, client/server error, or unexpected visual behavior.

## Run Development Server

```powershell
.\gradlew.bat runServer
```

On first server run, accept the generated EULA in the run directory.

## Porting Approach

1. Keep NeoForge 1.21.1 project Java-only.
2. Port OpenComputers systems in small slices from `master-MC1.12`.
3. Keep `li.cil.oc.api` for source compatibility where practical.
4. Start with stable contracts and internal model code before UI and integrations.
5. Re-add optional mod integrations only when current 1.21.1 APIs exist.

## Licensing

This port is MIT licensed. Original OpenComputers code is MIT licensed by Florian "Sangar" Nücke and contributors. Keep upstream notices and third-party license files when porting code, assets, Lua runtimes, or bundled libraries.

The NeoForge MDK template license remains in `TEMPLATE_LICENSE.txt`.

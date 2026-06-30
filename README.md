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
- Bounded client launch smoke reaches resource reload and texture atlas creation without NeoOpenComputers-owned missing model, missing texture, or mod loading errors.
- MCP evidence shows tier 2 and tier 3 screens open their terminal GUI after a fresh client restart. World-render screenshot proof for tier 2 and tier 3 screens is still pending, so screen visuals are not treated as verified alpha evidence yet.

The port has broad API, machine, network, filesystem, terminal, screen/GPU/input, modem/redstone, storage, inventory/tank/transposer, rack/server, nanomachine, printer, print, manual, player-facing failure feedback, and packaging slices in place. It is ready for first technical smoke testing, not release-ready.

Robots, drones, and block microcontrollers are not available in this alpha build. Microcontroller case items are present for recipe/API compatibility only and are not alpha smoke targets. Computer cases, servers, racks, screens, keyboards, storage, cards, and upgrades are the current alpha focus.

See the [First Alpha Testing Guide](ALPHA_TESTING.md) for the current install, smoke-test, known-gap, and issue-reporting checklist. See the [Alpha Smoke Matrix](ALPHA_SMOKE_MATRIX.md) for evidence mapping, [Alpha Release Checklist](ALPHA_RELEASE_CHECKLIST.md) for first-alpha handoff gates, [Visual Smoke Runbook](VISUAL_SMOKE_RUNBOOK.md) for screenshot proof, [Screen Work Protocol](SCREEN_WORK_PROTOCOL.md) for the anti-loop screen evidence gate, [Alpha Finding Template](ALPHA_FINDING_TEMPLATE.md) and the GitHub `Alpha finding` issue form for reproducible issue reports, [Contributing](CONTRIBUTING.md) for community development rules, and [Changelog](CHANGELOG.md) for alpha scope notes.

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

Install `build/libs/neoopencomputers-0.1.0.jar` for smoke testing. This jar includes the bundled runtime libraries. Do not install `neoopencomputers-0.1.0-thin.jar`; it is the developer thin jar and does not include bundled runtime libraries.

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

Verify the installed jar hash matches the built jar hash after copying, before launching Minecraft:

```powershell
$built = Get-FileHash .\build\libs\neoopencomputers-0.1.0.jar -Algorithm SHA256
$installedPath = "C:\Users\rolan\AppData\Roaming\ModrinthApp\profiles\NeoOpenComputers test instance\mods\neoopencomputers-0.1.0.jar"
Copy-Item .\build\libs\neoopencomputers-0.1.0.jar $installedPath -Force
$installedAfter = Get-FileHash $installedPath -Algorithm SHA256
if ($built.Hash -ne $installedAfter.Hash) {
    throw "Installed NeoOpenComputers jar hash does not match the built jar hash after copying."
}
```

4. In a local test world, check these flows:

- Computer case, screen, and keyboard place without crashing.
- Logs show no NeoOpenComputers-owned missing model, missing texture, or mod loading errors. Third-party profile warnings should be recorded separately and only block alpha if they break NeoOpenComputers testing.
- OpenOS/Lua prompt boots.
- Basic filesystem, EEPROM, floppy, and disk-drive actions work.
- Screen output and keyboard input survive save/reload.
- Terminal item key input, paste, mouse click/drag/release, and scroll reach the bound computer.
- Computer case hard-disk data survives save/reload.
- Disk-drive floppy data survives save/reload.
- Power and charging smoke passes: a computer accepts Forge Energy or runs from a powered OC network, and one chargeable item accepts charger energy.
- Redstone, modem, inventory, tank, and transposer each get one basic smoke pass.
- Texture picker reports usable atlas texture names such as `minecraft:block/stone`.
- Printer creates a print item.
- Print item visually uses configured shape data.
- Placed print rotates, renders configured shape data, drops configured data, handles button-mode redstone activation, held-item activation, beacon-base setting, tooltip data, opacity setting, and legacy texture names.

5. Save logs and screenshots for any crash, missing texture, client/server error, or unexpected visual behavior.

For every first-smoke finding, include:

- NeoOpenComputers jar version or commit.
- Jar SHA256.
- current jar install timestamp.
- Crash report timestamp.
- Crash report path or full crash log.
- Client log section around the failure.
- Screenshot or short video for visual bugs.
- Exact steps to reproduce in a fresh world when possible.

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

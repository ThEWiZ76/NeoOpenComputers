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
- Latest GameTest result: `314/314` required tests passed.
- Bounded client launch smoke reaches resource reload and texture atlas creation without print/model/missing-texture/error matches.

The port has broad API, machine, network, filesystem, terminal, screen/GPU/input, modem/redstone, storage, inventory/tank/transposer, rack/server, nanomachine, printer, print, manual, and packaging slices in place. It is ready for first technical smoke testing, not release-ready.

GitHub Actions are intentionally disabled until the mod is ready enough for CI. Do not add `.github/workflows` yet.

Before pushing, verify Actions are still disabled:

```powershell
.\scripts\check-actions-disabled.ps1
```

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

To package a local first-smoke tester bundle with install jar, API jar, Javadoc jar, checksums, and instructions:

```powershell
.\scripts\package-first-smoke-kit.ps1
```

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

For an interactive smoke session that automatically packages logs after the client exits:

```powershell
.\scripts\run-first-smoke-client.ps1
```

This stores Gradle client stdout/stderr under `build\first-smoke-sessions` and includes those logs in the first-smoke report bundle.

To include the local MCP server mod helper during that interactive smoke session:

```powershell
.\scripts\run-first-smoke-client.ps1 -WithLocalMcpServerMod
```

The helper jar is expected at `M:\development\mcp-server-mod\build\libs\mcp-server-mod-neoforge-1.1.0+neoforge.mc1.21.1.jar`. You can also pass any helper jar with `-ExtraMod <path>`. Copied helper jars are removed from `run\client\mods` after the client exits unless `-KeepExtraMods` is used. The copied helper list, helper jar SHA-256, byte length, and UTC timestamp are included in the evidence report.

For a bounded launch/log smoke that stops after resource reload and texture atlas creation:

```powershell
.\scripts\run-client-smoke.ps1
```

For a bounded MCP helper smoke that starts the client, verifies `http://localhost:8080/mcp`, and stops after `ping`, `initialize`, and `tools/list` pass:

```powershell
.\scripts\run-mcp-client-smoke.ps1
```

For a bounded MCP world smoke, first create a local singleplayer test world whose save folder is `run\client\saves\NeoOCSmoke`, then run:

```powershell
.\scripts\run-mcp-world-smoke.ps1
```

This starts the client with the MCP helper mod and `--quickPlaySingleplayer NeoOCSmoke`, waits until `get_player_info` proves the client has entered the world, then captures `get_player_info`, `get_blocks_in_area`, and `execute_commands` evidence under `build\mcp-world-smoke`.

For a bounded MCP device smoke that enters the same world, places a small NeoOpenComputers core/peripheral layout beside the player, and verifies the placed blocks via MCP block scan:

```powershell
.\scripts\run-mcp-device-smoke.ps1
```

This writes placement command and block-scan evidence under `build\mcp-device-smoke`. The layout covers computer case, screen, keyboard, disk drive, printer, redstone I/O, cable, adapter, transposer, rack, RAID, relay, geolyzer, and print.

MCP device smoke does not prove OpenOS prompt boot. The current helper can run commands, move, scan blocks, and read chat, but it cannot right-click GUIs, insert computer components, type into screens, or read screen text. Capture OpenOS prompt boot with the hands-on checklist, or extend the helper with GUI/use/inventory/screen-read tools before automating that proof.

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

5. Save logs for any crash, missing texture, client/server error, or unexpected visual behavior.

To package first-smoke evidence after testing:

```powershell
.\scripts\collect-first-smoke-report.ps1
```

This writes a timestamped report under `build\first-smoke-reports`, copies available client and bounded-smoke logs, copies up to 20 recent Minecraft screenshots, scans for hard failure patterns, and creates a zipped evidence bundle by default.

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

# First Alpha Testing Guide

This guide tracks the first technical alpha test path for NeoOpenComputers. It is for crash finding and basic OpenOS proof, not a public beta promise.

## Target

- Minecraft 1.21.1
- NeoForge 21.1.234
- NeoOpenComputers 0.1.0 from `develop`
- Java 21

GitHub Actions stay disabled until the mod is stable enough for CI noise. Do not add `.github/workflows` during alpha hardening.

## Build Artifact

Build and verify the jar before installing it:

```powershell
.\gradlew.bat test build --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
```

The alpha jar is:

```text
build/libs/neoopencomputers-0.1.0.jar
```

This is the installable jar with bundled runtime libraries. Do not install `neoopencomputers-0.1.0-thin.jar` for smoke testing; it is the developer thin jar and does not include bundled runtime libraries.

Copy that jar into a clean Minecraft 1.21.1 NeoForge instance, then fully restart Minecraft. Replacing the jar while the game is running does not reload the mod.

## Must Smoke Before Sharing

- Game starts without missing model, missing texture, or mod loading errors.
- Creative tab opens and item stacks can be picked up.
- Computer case, screen, keyboard, disk drive, and basic cards can be placed.
- Tier 1 computer boots OpenOS from valid boot media.
- Computer and server boot failures show a player-visible last-error message.
- Tier 2 and tier 3 screens open their terminal GUI after a fresh client restart.
- MCP terminal-open evidence alone does not verify screen world rendering; capture readable world-render screenshots before treating tiered screen visuals as alpha-ready.
- Keyboard input reaches the bound computer.
- Screen output and keyboard input survive save/reload.
- Terminal item input, paste, mouse click/drag/release, and scroll reach the bound computer.
- Computer case hard-disk data survives save/reload.
- Disk-drive floppy data survives save/reload.
- Power and charging smoke passes: a computer accepts Forge Energy or runs from a powered OC network, and one chargeable item accepts charger energy.
- Redstone, modem, inventory, tank, and transposer get one basic smoke pass.
- Texture picker reports usable atlas texture names such as `minecraft:block/stone`.
- Printer creates a print item and placed prints keep their configured data.

## Known Alpha Gaps

Robots, drones, and block microcontrollers are not available in this alpha build.

Screen world rendering is still considered visually sensitive. Do not change screen renderer, model, glyph, or multiblock code without fresh evidence and a focused failing test.

## Reporting Findings

Report crashes and clear regressions at:

```text
https://github.com/ThEWiZ76/NeoOpenComputers/issues
```

Include:

- NeoOpenComputers jar version or commit if known.
- Crash report path or full crash log.
- Client log section around the failure.
- Screenshot or short video for visual bugs.
- Exact steps to reproduce in a fresh world when possible.

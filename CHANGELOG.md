# Changelog

## 0.1.0-alpha

First technical alpha line for NeoOpenComputers on Minecraft 1.21.1 with NeoForge 21.1.234.

This is a crash-finding and basic OpenOS proof build, not a public beta promise.

### Included Scope

- Java-first NeoForge port foundation under the MIT license.
- OpenOS boot path using the bundled Lua/OpenOS assets.
- Computer cases, servers, racks, screens, keyboards, storage, cards, and upgrades.
- Basic filesystem, EEPROM, floppy, disk-drive, hard-drive, and persistence smoke coverage.
- Terminal GUI, terminal item, keyboard input, mouse events, paste, and screen save/reload paths.
- Redstone, modem, inventory, tank, transposer, nanomachine, printer, print, rack, and server smoke coverage.
- Power and charging smoke coverage for Forge Energy input, powered OC networks, charger flow, and chargeable items.
- Texture picker smoke coverage for atlas texture names such as `minecraft:block/stone`.
- Player-visible boot failure feedback.
- Installable jar with bundled runtime libraries at `build/libs/neoopencomputers-0.1.0.jar`.

### Known Gaps

- Robots, drones, and block microcontrollers are not available in this alpha build.
- Microcontroller case items are present for recipe/API compatibility only and are not alpha smoke targets.
- Screen world rendering is visually sensitive. Do not change screen renderer, model, glyph, or multiblock code without a focused repro, focused failing test, narrow patch, and screenshot evidence.
- First alpha testing should use the checklist in `ALPHA_TESTING.md`.

### CI State

GitHub Actions are intentionally disabled until the port is stable enough for CI without build-failure noise.

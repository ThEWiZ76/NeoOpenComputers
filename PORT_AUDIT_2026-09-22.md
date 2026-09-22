# Port audit — 2026-09-22

Baseline: `feature/finish-port`, code commit `69680c045343acbc4089e1e5ad687704390c63e9`, Minecraft 1.21.1, NeoForge 21.1.234.

This is an initial gap audit, not a declaration of complete upstream parity. No runtime code was changed. Findings distinguish live observations from source-confirmed gaps and untested behavior.

## Verification

- `gradlew.bat test build --no-daemon --console=plain`: passed; JUnit XML reports **2,040 tests, zero failures/errors/skips**.
- `gradlew.bat runGameTestServer --no-daemon --console=plain`: **440/440 required tests passed**.
- Installable JAR SHA256: `51E043E801128116FB5E7D0D3C1A72E0E10C9FF45D1C72FB61EA7B14E05D5B6E`. Built and installed hashes matched before client launch.
- Real Modrinth test client, existing local `testworld`, creative mode for fixture construction; no browser/desktop control. This profile includes Sodium, Iris, JEI and other mods, so performance findings need a minimal-profile comparison before attributing all cost to one implementation.
- Fresh client startup reached the world without an observed NeoOpenComputers missing-model/texture or loading error. A Powah `minecraft:block` model warning belongs to a companion mod.
- Tier 3 case + tier 3 GPU/CPU + tier 3 memory + bundled Lua BIOS + internal OpenOS floppy booted to `/home #`. `echo port-audit-ok` returned the expected text. Lua REPL assignment and output also worked.
- The loaded manual and case/microcontroller/robot GUIs opened without a crash in these checks.

Local evidence: `build/finish-port-baseline.log`, `build/finish-port-gametest.log`, and `build/finish-port-audit/` (ignored). Screenshots are real MCP captures. Not every earlier capture is useful: captures made immediately after screen changes can show the preceding frame.

## Prioritized findings

### P1 — Terminal GUI performance collapses

**Live reproduced.** Opening the tier 3 terminal with only the OpenOS boot banner/prompt reduced the displayed frame rate to **4–7 FPS**. Closing the GUI, retaining the same running computer and looking at its world screen for three seconds, returned to **60 FPS**. Looking away also gave 60 FPS. This is a GUI-path finding; the initial low-FPS screenshot immediately after closing the GUI is not evidence of a world-renderer regression.

- Evidence: `fps-gui-repeat.png` (4 FPS), `fps-world-powered.png` (60 FPS), `fps-away-powered.png` (60 FPS), `terminal-echo.png` (successful input).
- Source: `client/TerminalFont.java:72`, `drawGuiCell`, emits one `GuiGraphics.fill` per lit glyph pixel. `drawWorldCell` already has an atlas path. This is a concrete optimization candidate, not a measured CPU profile proving exclusive causality.
- Reproduction fixture: case `(104,-60,100)`, screen `(104,-59,100)` with `pitch=north,yaw=south`, keyboard `(104,-59,101)`, charged power converter `(104,-60,101)`. Camera `(104.5,-60,104.5)`, yaw 180, pitch 0. Use a sustained energy supply; the converter's injected energy is bounded by its configured buffer.
- Next: profile the GUI text path and change only that layer, following `SCREEN_WORK_PROTOCOL.md`. Compare the same text, resolution and screenshot angle before/after.

### P1 — Robot terminal is not connected to its GUI

**Source-confirmed functional gap; GUI layout captured live.** `RobotScreen.drawScreenPanel` paints a black rectangle when graphics hardware exists, but never renders a terminal snapshot. The class has no terminal keyboard forwarding. `TerminalNetworking` accepts `TerminalMenu`, while `RobotMenu` extends `AbstractContainerMenu` and synchronizes eight scalar status fields, not screen contents.

- Evidence: `robot-with-gpu.png`; this fixture had GPU/CPU/memory but no EEPROM, so the black screenshot alone is not a failed boot reproduction. The missing render/input path is established by source.
- Next: prove the internal screen/keyboard component path and add terminal synchronization/input for an assembled, bootable robot. Current assembly and callback GameTests do not establish interactive robot OpenOS usability.

### P1 — Lua execution state is not persisted

**Source-confirmed gap against the bundled manual/upstream native runtime.** `LuaArchitecture.save` stores boot source/address and metadata but no Lua heap/coroutine execution state. `load` initializes a new runtime and resets `booted` to false. The bundled manual explicitly promises continuation after chunk unload.

- Source: `common/machine/LuaArchitecture.java:264–290`; `assets/neoopencomputers/doc/en_us/index.md` persistence paragraph; upstream `server/machine/luac/NativeLuaArchitecture.scala` load/save.
- Filesystem persistence is separate and has automated coverage. Do not describe this as loss of all disk files.
- A powered save/reload sentinel test was **not completed** in this audit; an attempted GUI escape did not open the pause menu through this MCP build. Do not treat `lua-before-reload.png` or `reload-initial.png` as before/after persistence proof.
- Next: capture a powered unload/reload reproduction, then explicitly decide whether full execution persistence is required or the alpha documents must disclose restart semantics.

### P2 — Capacitor blocks are missing

**Source and live registry confirmed.** Upstream has `Capacitor` and `CarpetedCapacitor`; the port has no corresponding block registrations or implementation. `setblock ... neoopencomputers:capacitor` was rejected as an unknown block. The bundled English manual recommends capacitors for storage, assemblers and network power.

- Source: `common/ModBlocks.java`, upstream `common/block/Capacitor.scala` and `CarpetedCapacitor.scala`; bundled `doc/en_us/block/capacitor.md` and linked pages.
- Next: port storage/capacity/adjacency behavior, recipes and visuals, or explicitly mark this feature unavailable until implemented. A power converter is not proof of capacitor parity.

### P2 — Untranslated player-facing text

**Live reproduced.** The microcontroller GUI displays `gui.neoopencomputers.microcontroller.title`, extending outside its window. This exact key is missing from `lang/en_us.json`.

Separately, draining a running computer's energy produced `Computer error: gui.Error.NoEnergy` in chat. `SimpleMachine` supplies the error key and `ComputerCaseBlockEntity.machineErrorFeedback` wraps it in literal text. This needs error localization, not only a title entry.

- Evidence: `microcontroller-gui.png`; `client.log` entries at 09:23:12 and 09:26:09.
- Source: `client/MicrocontrollerScreen.java:25`, `common/SimpleMachine.java:695`, `common/blockentity/ComputerCaseBlockEntity.java:470`.
- Static translation scan found this one missing complete NeoOpenComputers GUI key; two rack-prefix matches are dynamically completed keys, not missing translations.

### P2 — Robot without graphics still uses the full screen layout

**Live and source confirmed.** The no-graphics robot GUI leaves a large blank gray region. `ROBOT_NO_SCREEN_TEXTURE` is declared but unused; dimensions and background remain the full-screen variant regardless of hardware.

- Evidence: `robot-gui.png`, existing robot at `(31,-60,31)`, left unchanged.
- Source: `client/RobotScreen.java:16,35–49,155`.
- Next: choose the no-screen layout and matching slot coordinates when graphics hardware is absent.

### P2 — Bundled-redstone input and optional integrations are incomplete

**Source-confirmed gap.** `RedstoneControllerHost.bundledRedstoneInput` returns zero; there are no overriding implementations in the port. Computer cases store bundled output values, but this does not establish interaction with an external bundled cable provider. Upstream wireless-redstone callbacks/integration classes are not present either.

- Source: `common/component/RedstoneControllerHost.java:13`, `RedstoneCardEnvironment`, `ComputerCaseBlockEntity` bundled-output methods; upstream integration directory and `RedstoneWireless.scala`.
- Next: scope supported 1.21.1 integrations explicitly and test against those mods. Do not label every historical 1.12 integration a required regression fix; Forge Energy and generic inventory/fluid capability support already exist.

## Visual and documentation follow-ups

- GitHub issue #1, higher-tier case tint: yellow/cyan **block** tint confirmed in `case-tiers.png`. Item tint was not separately verified. Issue remains open; no external issue changes made.
- GitHub issue #2, slot-watermark contrast: empty overlays still look strong in `case-gui.png`. Source sets alpha to 0.35, but `drawSlot` does not explicitly establish blending. Treat as a candidate requiring a focused rendering comparison, not as a confirmed root cause or a resolved issue.
- Manual front page is readable (`manual.png`), but legacy persistence/capacitor claims conflict with the implementation. Only English UI localization is present; translated manual folders are not equivalent to translated GUI text.
- README reports 439 GameTests; current run has 440. Older notes saying robots/drones/microcontrollers are entirely absent are stale: implementations and automated coverage now exist.
- Still needed: full player-driven robot/drone/microcontroller assembly and actions; printer/print visual lifecycle; tier 1/2 world-screen and save/reload proof; sustained FE/charging; storage reload; modem, transposer, tank and redstone interactions in the client. Automated coverage is not a substitute for these manual checks.

## Suggested implementation order

1. Fix terminal GUI performance with a focused reproduction/profile and before/after evidence.
2. Complete robot terminal output/input and no-screen layout.
3. Fix microcontroller title and machine error localization; retest slot-overlay contrast.
4. Port capacitors and define execution-persistence behavior.
5. Finish the remaining visual matrix and explicitly scoped integrations.

Temporary fixture blocks are removed after the audit and the cleared area is read back. The newly built test JAR remains installed for follow-up work; the prior profile JAR is backed up locally. No push, merge, release, or GitHub issue mutation is part of this audit.

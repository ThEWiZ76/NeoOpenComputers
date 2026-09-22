# Terminal GUI batching evidence

## Before changing code

- Baseline/rollback: `6a03a544bda79adb4b18e15c6164bb1836052e28`; installed JAR SHA256 `51E043E801128116FB5E7D0D3C1A72E0E10C9FF45D1C72FB61EA7B14E05D5B6E`.
- Modrinth profile `NeoOpenComputers test instance`, Minecraft 1.21.1 / NeoForge 21.1.234, world `testworld`. Tier-3 case at `(104,-60,100)`, tier-3 screen above it, keyboard at `(104,-59,101)`, charged converter at `(104,-60,101)`. Tier-3 CPU/GPU/RAM, OpenOS floppy, bundled BIOS.
- Reproduction: start case, open screen from south. OpenOS terminal shows 6 FPS; clearing to one line raises FPS to 52. Earlier audit reproduced 4-7 FPS in GUI and 60 FPS in the same powered world.
- Before screenshots: `build/finish-port-implementation/terminal-before.png`, `terminal-before-fixed.png`; captured directly by the connected Minecraft MCP, showing live OpenOS output and the FPS overlay. Installed JAR hash independently checked before capture.
- Root cause: `TerminalFont.drawGuiCell` calls `GuiGraphics.fill` for every lit bitmap pixel. Minecraft 1.21.1 `GuiGraphics.fill` calls `flushIfUnmanaged`, which flushes the buffer for every pixel. `TerminalScreen` is unmanaged; its scissor boundary already flushes queued vertices safely.
- Owner/layer: terminal GUI glyph renderer. Batch the same position/color pixel quads in the existing GUI buffer. Preserve font metrics, bitmap data, transforms, textures, world renderer and multiblock behavior. No previously reverted visual choice is reintroduced.
- The live reproduction is the graphical performance failure; existing glyph/terminal tests cover bitmap metrics, wide characters and input. Add a headless regression that rejects per-pixel immediate fills and checks emitted geometry.

## Verification

First batching-only build: SHA256 `22855212333D21CE47128FCB680573635E4512C4F0DA2B293DB2A7BB992C8C02`; 2041 unit tests and 440 GameTests pass. Live GUI remains at 9 FPS (`terminal-after.png`, repeated capture), so batching alone is insufficient and the issue is not resolved.

A 20-second JFR profile (`build/finish-port-implementation/terminal.jfr`, extracted `profile.json`) explains the remaining CPU cost: render-thread samples are dominated by `ImmutableCollections$MapN.probe` (802) and `TerminalFont.pixel` (786), versus 96 in vertex writing. The glyph map is a large immutable map with dense integer keys; every pixel repeats glyph lookups, including eagerly evaluating the fallback glyph. The inner loop also repeatedly looks up glyph width.

Second adjustment, same GUI glyph-renderer layer: obtain the glyph and width once per cell, then rasterize that immutable glyph directly. No font/map data or world rendering changes. The common pixel helper is split to preserve existing callers' behavior. Re-run the geometry regression and full gates, then measure the client before claiming completion.

## Completed first implementation slice

- Final built/installed JAR SHA256: `21F63EA7AE3A29330CA6ED691EF4D1503A153B911340CD7023CA1830FFA8C20E` (both files read back).
- `test build`: 2041 tests, zero failures/errors/skips. `runGameTestServer`: all 440 required tests passed. Logs: `build/finish-port-implementation/build-final.log`, `gametest-final.log`.
- Geometry regression covers ASCII, a wide Unicode glyph, space, color and translated/scaled coordinates against the original bitmap masks. Initial test rejected the old immediate-fill path. The final test exercises the extracted vertex writer directly to avoid bootstrapping a Minecraft/OpenGL context in JUnit.
- OpenOS boot terminal: **60 FPS**, versus **6 FPS** before. Screenshots `terminal-final-boot.png`, `terminal-final-input.png`; `echo after-batch-input-ok` visibly returned the expected output. Same profile/world/hardware layout; capture size changed from 1920×1011 to 2048×1080 after launcher restart, so this is an observed usability improvement, not a controlled benchmark ratio.
- Lua `g.fill(1,1,160,50,"X")` rendered a full screen at **31 FPS** (`fullscreen-final.png`). Full-density optimization remains open; do not describe every terminal workload as 60 FPS.
- `unicode-final.png` shows wide `界`, box drawing, accented/Greek characters, white output and green REPL/input text. `world-final.png` confirms powered block-face output remains visible and the world view runs at 60 FPS; this does not replace close-up tests of every tier/rotation.
- A long MCP `type_text` burst was truncated before Enter. Short commands with intervals worked. Investigate signal-queue saturation/automation pacing separately before calling clipboard/input parity complete.
- No client crash observed. Test fixture cleared; fresh `get_blocks_in_area` returned `total_blocks: 0`. Client closed normally; log confirms all dimensions saved. Original `pauseOnLostFocus:true` restored on disk.
- The optional local `scripts/check-actions-disabled.ps1` is absent. Equivalent explicit checks found no workflows in working tree, HEAD or origin/develop. `git diff --check` passed.

Remaining acceptance work: dense/full-screen profiling, lower tiers, multiple GUI scales, nonblack background-heavy rendering, minimal modset, and all other parity tasks. This slice resolves the severe ordinary OpenOS prompt slowdown; it does not declare the complete port finished.

## Dense-output follow-up

The 31-FPS fully filled terminal is the remaining reproduction. The JFR evidence above identified immutable `MapN` probing as expensive for this dense integer-key font table. Even one lookup per glyph still repeats it 8000 times on a full screen, and the fallback argument is eagerly evaluated. Keep the font data unchanged, retain a read-only HashMap for lookup and evaluate the fallback only for missing glyphs. This stays within the glyph lookup layer; no texture, transform or screen geometry changes. Verify with the existing bitmap/geometry tests and a fresh full-screen capture; do not infer performance from code alone.

Follow-up JAR `731F3F1EF4E792BE9F4B88B18AEC9B3E78EE896791FAA949617D57CA94CA0372` still measured 24–26 FPS on a filled terminal (`dense-profile-before.png`, same 2048×1080 profile/layout). A confirmed live full-screen 20-second JFR (`dense2.jfr`) now shows 772 samples in BufferBuilder.beginVertex, 557 in TerminalFont.pixel, only 6 in HashMap.getNode: map probing is removed, but emitting/rasterizing per-pixel geometry dominates.

Architecture decision before another patch: retain the terminal GUI glyph renderer as sole owner. Use the already-shipped, nearest-filtered world font atlas for GUI code points present in it, one textured quad per cell; retain the tested pixel fallback for other Unicode. Test every atlas bitmap against the original font masks before switching. Textures/metrics/world rendering/layout/network remain unchanged. This replaces the costly raster path rather than applying more map tuning. The earlier glyph-cache change succeeded for normal OpenOS output; full-density acceptance remains explicitly unproven until the new capture.

Atlas follow-up verified: build/installed JAR `1DC44925070A869D5F992767B77DB2D078B102E3852480FC5C04F193D7469998`, 2047 unit tests and 442 GameTests green. `atlas-dense-verified.png` shows the actual 160×50 X-filled terminal at **60 FPS**. Atlas pixel equivalence test passed for every mapped glyph; wide Unicode retains the raster fallback. Capture size is now 1920×1011 after launcher restart; same number of terminal cells. Background-heavy output revealed a separate remaining bottleneck: **15 FPS** (`background-before-verified.png`).

## Separate background-renderer patch evidence

- Same last verified JAR, profile, world, tier/layout and renderer rollback as above. MCP screenshots confirm actual current Minecraft framebuffer pixels.
- Reproduction: Lua `g.setBackground(0x330000)` followed by `g.fill(1,1,160,50,"X")`; screen fills correctly but FPS drops from 60 to 15.
- Root cause: `TerminalScreen.renderCellBackgrounds` calls unmanaged `GuiGraphics.fill` once per nonblack cell: up to 8000 separate flushes. Glyph atlas already fixed; this is cell-background submission, not another glyph patch.
- Owner: terminal GUI cell-background renderer. Use Minecraft's existing managed drawing scope around this pass so the same fill calls queue until the pass boundary. No geometry, color calculation, clipping, transforms, font or network change.
- Verification pending: repeat same filled red-background scene, Unicode/input and powered world view; rerun focused/full gates. The captured live failure is the rendering regression test for this one-line submission change.

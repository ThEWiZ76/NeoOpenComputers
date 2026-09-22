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

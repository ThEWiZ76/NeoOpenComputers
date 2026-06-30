# Visual Smoke Runbook

Use this runbook for the remaining first-alpha proof that cannot be trusted to unit tests, GameTests, or MCP block scans. It is not a screen-fix plan. If a screen visual issue appears, follow `SCREEN_WORK_PROTOCOL.md` before changing implementation code.

## Inputs

- Jar: `build/libs/neoopencomputers-0.1.0.jar`
- Jar SHA256: record with `Get-FileHash .\build\libs\neoopencomputers-0.1.0.jar -Algorithm SHA256`
- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Branch: `develop`
- Commit: latest pushed commit from `git log -1 --oneline`

Record the world name, seed, coordinates, component tiers, and any companion mods used.

## Required Screenshots

Save screenshots with these exact names in the smoke report folder:

- `01-openos-prompt.png`: tier 1 computer booted to a readable `/home #` prompt on a connected screen or terminal.
- `02-computer-gui.png`: computer case GUI open with component tooltip visible and readable.
- `03-screen-after-reload.png`: same computer and screen after save/reload, proving screen output and keyboard path still look sane.
- `04-creative-tab.png`: NeoOpenComputers creative tab open with current alpha items visible and no missing-texture cubes.
- `05-manual.png`: in-game manual opened from the alpha jar with the alpha availability note visible.
- `06-printer-print.png`: printer-created print item placed in-world with shape/texture visible from player view.

Optional screenshots can use a descriptive suffix, for example `07-disk-drive-media.png` or `08-redstone-smoke.png`.

## Manual Checks

1. Install only the bundled jar, not `neoopencomputers-0.1.0-thin.jar`.
2. Fully restart Minecraft after replacing the jar.
3. Boot a tier 1 computer from valid OpenOS media and capture `01-openos-prompt.png`.
4. Open the computer case GUI, hover at least one component, and capture `02-computer-gui.png`.
5. Save and reload the world, verify the screen still shows usable output, type one key through the keyboard or terminal, and capture `03-screen-after-reload.png`.
6. Open the creative tab and capture `04-creative-tab.png`.
7. Open the manual and capture `05-manual.png`.
8. Create or load a print item, place it, rotate/activate it once, and capture `06-printer-print.png`.
9. Run `.\scripts\collect-first-smoke-report.ps1` after screenshots are present so the report archive contains logs and images together.

## Stop Conditions

Stop and file a finding if any of these happen:

- Crash report appears.
- Screen text, GUI text, tooltip text, or manual text is unreadable.
- Missing texture, magenta/black cube, untranslated key, or hard client/server error appears.
- Computer cannot boot with valid media and enough power.
- Keyboard or terminal input cannot reach the computer.
- Save/reload loses visible terminal output, media, or print data.

Do not patch screen renderer, model, glyph, terminal projection, placement, or multiblock code from this runbook alone. A screen fix requires `SCREEN_WORK_PROTOCOL.md`.

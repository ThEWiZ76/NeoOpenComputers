# Visual Smoke Runbook

Use this runbook for the remaining first-alpha proof that cannot be trusted to unit tests, GameTests, or MCP block scans. It is not a screen-fix plan. If a screen visual issue appears, follow `SCREEN_WORK_PROTOCOL.md` before changing implementation code.

## Inputs

- Jar: `build/libs/neoopencomputers-0.1.0.jar`
- Jar SHA256: record with `Get-FileHash .\build\libs\neoopencomputers-0.1.0.jar -Algorithm SHA256`
- Minecraft: 1.21.1
- NeoForge: 21.1.234
- Branch: `develop`
- Commit: latest pushed commit from `git log -1 --oneline`
- MCP endpoint: read the active profile's `config/mcp-client.json` and use the configured `server.port`. In the current Modrinth test profile this is `8081`; do not assume `8080`, because it can be owned by other software such as NVIDIA Broadcast.

Record the world name, seed, coordinates, component tiers, and any companion mods used.

Compare the installed profile jar hash to the built jar hash before starting the smoke pass. Close Minecraft before replacing the installed jar; a running JVM keeps the old jar loaded and may lock the file on Windows.

```powershell
$built = Get-FileHash .\build\libs\neoopencomputers-0.1.0.jar -Algorithm SHA256
$profileRoot = Join-Path $env:APPDATA "ModrinthApp\profiles\<profile-name>"
$installedPath = Join-Path $profileRoot "mods\neoopencomputers-0.1.0.jar"
$installed = Get-FileHash $installedPath -Algorithm SHA256
$built.Hash
$installed.Hash
Copy-Item .\build\libs\neoopencomputers-0.1.0.jar $installedPath -Force
$installedAfter = Get-FileHash $installedPath -Algorithm SHA256
$installedAfter.Hash
if ($built.Hash -ne $installedAfter.Hash) {
    throw "Installed NeoOpenComputers jar hash does not match the built jar hash after copying."
}
```

Verify the installed jar hash matches the built jar hash after copying before launching Minecraft.

Create the smoke report folder before capturing screenshots. Save every required screenshot directly in `$report`:

```powershell
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$report = "build\first-smoke-reports\first-smoke-$stamp"
New-Item -ItemType Directory -Force -Path $report | Out-Null
```

## Required Screenshots

Save screenshots with these exact names in the smoke report folder from the setup step:

- `01-openos-prompt.png`: tier 1 computer booted to a readable `/home #` prompt on a connected screen or terminal.
- `02-computer-gui.png`: computer case GUI open with component tooltip visible and readable.
- `03-screen-after-reload.png`: same computer and screen after save/reload, proving screen output and keyboard path still look sane.
- `04-creative-tab.png`: NeoOpenComputers creative tab open with current alpha items visible and no missing-texture cubes.
- `05-manual.png`: in-game manual opened from the alpha jar with the alpha availability note, microcontroller entry, and robot/drone `(alpha unavailable)` markers visible.
- `06-printer-print.png`: printer-created print item placed in-world with shape/texture visible from player view.

Optional screenshots can use a descriptive suffix, for example `07-disk-drive-media.png` or `08-redstone-smoke.png`.

## Manual Checks

1. Install only the bundled jar, not `neoopencomputers-0.1.0-thin.jar`.
2. Fully restart Minecraft after replacing the jar.
3. If using MCP, verify the real endpoint from `mcp-client.json` before testing. Use `8081` for the current Modrinth test profile unless that config changes.
4. Boot a tier 1 computer from valid OpenOS media and capture `01-openos-prompt.png`.
5. Open the computer case GUI, hover at least one component, and capture `02-computer-gui.png`.
6. Save and reload the world, verify the screen still shows usable output, type one key through the keyboard or terminal, and capture `03-screen-after-reload.png`.
7. Open the creative tab and capture `04-creative-tab.png`.
8. Open the manual, verify microcontrollers are linked and robots/drones are marked `(alpha unavailable)`, and capture `05-manual.png`.
9. Assemble a tier 1 microcontroller, install a CPU, memory, and programmed EEPROM, then verify it starts.
10. Create or load a print item, place it, rotate/activate it once, and capture `06-printer-print.png`.
11. Archive the screenshots, logs, crash reports, and checksum in one folder after screenshots are present in `$report`:

```powershell
$profileRoot = Join-Path $env:APPDATA "ModrinthApp\profiles\<profile-name>"
Copy-Item (Join-Path $profileRoot "logs\latest.log") $report -ErrorAction SilentlyContinue
Copy-Item (Join-Path $profileRoot "crash-reports\*.txt") $report -ErrorAction SilentlyContinue
Get-FileHash .\build\libs\neoopencomputers-0.1.0.jar -Algorithm SHA256 | Format-List | Out-File "$report\jar-sha256.txt"
Compress-Archive -Path "$report\*" -DestinationPath "$report.zip" -Force
```

## Stop Conditions

Stop and file a finding if any of these happen:

- Crash report appears.
- Screen text, GUI text, tooltip text, or manual text is unreadable.
- Missing texture, magenta/black cube, untranslated key, or hard client/server error appears.
- Computer cannot boot with valid media and enough power.
- Keyboard or terminal input cannot reach the computer.
- Save/reload loses visible terminal output, media, or print data.

Do not patch screen renderer, model, glyph, terminal projection, placement, or multiblock code from this runbook alone. A screen fix requires `SCREEN_WORK_PROTOCOL.md`.

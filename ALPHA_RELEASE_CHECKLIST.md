# Alpha Release Checklist

Use this checklist before sharing the first NeoOpenComputers alpha jar outside the current test loop.

## Target

- Minecraft 1.21.1
- NeoForge 21.1.234
- NeoOpenComputers 0.1.0 from `develop`
- Java 21

## Required Gates

Run these from the repository root:

```powershell
.\gradlew.bat test build --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
Test-Path .github
git ls-tree -r HEAD .github
git fetch origin develop
git ls-tree -r origin/develop .github
git status --short --branch
```

All tests must pass, GameTests must pass, GitHub Actions must stay disabled, and the working tree must only contain intentional release notes or artifact updates. `Test-Path .github` must print `False`; both `git ls-tree` commands must print no workflow files.

Do not add `.github/workflows` for this alpha.

## Artifact

Ship this jar:

```text
build/libs/neoopencomputers-0.1.0.jar
```

Do not ship `neoopencomputers-0.1.0-thin.jar`; it does not include bundled runtime libraries.

Record the artifact checksum with:

```powershell
Get-FileHash .\build\libs\neoopencomputers-0.1.0.jar -Algorithm SHA256
```

Copy the installable jar into a clean Minecraft 1.21.1 NeoForge profile, then fully restart Minecraft before testing.

## Smoke Handoff

Use `ALPHA_SMOKE_MATRIX.md` as the required manual smoke list and `VISUAL_SMOKE_RUNBOOK.md` as the required screenshot proof list. Save crash reports, client logs, screenshots, and exact reproduction steps for every mismatch.

MCP terminal-open evidence alone is not enough for screen visual handoff. The screenshot proof must show readable in-world screen output and the terminal GUI after a fresh client restart.

Minimum handoff proof:

- Client reaches a local world without NeoOpenComputers-owned missing model, missing texture, or mod loading errors. Third-party profile warnings should be recorded separately and only block alpha if they break NeoOpenComputers testing.
- Creative tab opens and current alpha items can be picked up.
- Tier 1 computer boots OpenOS from valid media.
- Tier 2 and tier 3 computers open their terminal GUI after a fresh client restart.
- Power and charging smoke passes: a computer accepts Forge Energy or runs from a powered OC network, and one chargeable item accepts charger energy.
- Texture picker reports usable atlas texture names such as `minecraft:block/stone`.
- Storage, floppy, redstone, modem, inventory, tank, transposer, printer, and print paths get one smoke pass.

## Screen Loop Guard

Screen renderer, model, glyph, and multiblock code stay frozen unless all of these are true:

- There is a fresh reproducible root cause.
- There is one focused failing test or one captured live reproduction.
- The patch changes one screen layer only.
- A before/after screenshot proves the exact behavior changed.

Do not fix screen visuals by flipping textures, offsets, transforms, or render layers without that evidence.

Follow `SCREEN_WORK_PROTOCOL.md` before any screen patch.

## Stop Conditions

Stop the release handoff if any of these happen:

- Minecraft crashes during startup, creative tab use, computer boot, screen interaction, or save/reload.
- The artifact path or checksum is unclear.
- Power and charging cannot be proven with the current profile setup.
- Texture picker atlas names cannot be proven with the current profile setup.
- `.github/workflows` exists locally, in `HEAD`, or on `origin/develop`.
- Screen work starts without the screen loop guard evidence.
- The working tree has unexplained changes.

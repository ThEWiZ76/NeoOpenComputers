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
.\scripts\check-actions-disabled.ps1
git status --short --branch
```

All tests must pass, GameTests must pass, GitHub Actions must stay disabled, and the working tree must only contain intentional release notes or artifact updates.

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

Use `ALPHA_SMOKE_MATRIX.md` as the required manual smoke list. Save crash reports, client logs, screenshots, and exact reproduction steps for every mismatch.

Minimum handoff proof:

- Client reaches a local world without missing model, missing texture, or mod loading errors.
- Creative tab opens and current alpha items can be picked up.
- Tier 1 computer boots OpenOS from valid media.
- Tier 2 and tier 3 computers open their terminal GUI after a fresh client restart.
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
- `.github/workflows` exists locally, in `HEAD`, or on `origin/develop`.
- Screen work starts without the screen loop guard evidence.
- The working tree has unexplained changes.

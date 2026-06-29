# Screen Work Protocol

Screen work is frozen until evidence proves the next change. This file exists because screen visibility, multiblock, glyph, and model fixes have looped before.

## Gate

No screen code changes before evidence. Before editing renderer, model, glyph, placement, terminal projection, or multiblock code, record all of this in the working notes or issue:

- one reproducible root cause
- exact Minecraft profile, jar SHA256, world, coordinates, tier, block layout, and action used to reproduce
- before screenshot showing the failing behavior
- one focused failing test, or one captured live reproduction when the bug cannot be unit-tested
- rollback point, usually the latest clean commit and jar hash
- target layer: model, renderer, glyph sizing, terminal projection, placement, networking, or multiblock state

Do not change textures, transforms, offsets, render layers, glyph sizing, or multiblock state in the same patch.

## Patch Rule

Each screen patch changes one screen layer only.

Examples:

- Model-only: block model, blockstate, or texture reference.
- Renderer-only: quad position, render layer, depth, or culling.
- Glyph-only: font metrics, scale, or text cell layout.
- Multiblock-only: connected dimensions, origin state, neighbor updates, or persistence.
- Terminal-only: screen snapshot, delta sync, GUI opening, or keyboard forwarding.

If a fix appears to require multiple layers, stop and split the proof into separate failing checks before editing.

## Verification

Before handing a jar to testers, capture:

- after screenshot from the same angle and layout as the before screenshot
- focused test result
- `.\gradlew.bat test build --no-daemon --console=plain`
- `.\gradlew.bat runGameTestServer --no-daemon --console=plain`
- `.\scripts\check-actions-disabled.ps1`
- installed jar SHA256 in the Modrinth test profile

If the after screenshot proves one behavior but another screen behavior regresses, revert the screen patch and restart from root-cause evidence.

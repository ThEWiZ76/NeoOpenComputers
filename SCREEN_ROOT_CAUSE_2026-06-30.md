# Screen Root-Cause Note - 2026-06-30

This note records why screen work is paused before another implementation patch.

## Trigger

Recent screen changes repeatedly fixed one visible symptom and regressed another:

- screen placement direction
- multiblock front texture joining
- front/side transparency
- in-world terminal text readability
- tier 2 and tier 3 click/render behavior

This is now treated as an architecture issue, not an offset or texture tweak.

## Evidence

- Current branch: `develop` at `6c2ba504c docs: expand print smoke evidence`.
- Installed test-profile jar: `neoopencomputers-0.1.0.jar`, timestamp `2026-06-30 03:11`.
- MCP server is live at `http://localhost:8081/mcp`.
- MCP player state confirmed current test world near the screen rig at block position `5 -60 -14`.
- MCP block scan confirmed tiered screen multiblocks exist in-world:
  - Tier 2: `neoopencomputers:screen_tier2` at `x=1`, `y=-59/-58`, `z=-17..-15` (3x2)
  - Tier 3: `neoopencomputers:screen_tier3` at `x=1`, `y=-59/-58`, `z=-12..-10` (3x2)
- OS-level screenshot capture is unreliable in this run. Captures of the Minecraft window handle produced a desktop wallpaper/stale surface, not game pixels:
  - `build/visual-smoke-live/minecraft-screen-before-20260630-044809.png`
  - `build/visual-smoke-live/minecraft-screen-foreground-20260630-044837.png`

## Upstream Pattern

OpenComputers 1.12 uses a dynamic screen block model:

- `client/renderer/block/ScreenModel.scala`
- `client/renderer/tileentity/ScreenRenderer.scala`

The upstream block model chooses the front and side texture per screen block using:

- local connected-screen position
- connected width and height
- local face
- pitch/yaw
- special flip/rotation for up/down faces

The tile-entity renderer draws text only from the origin screen.

## Port Pattern

The current port split upstream behavior between:

- static JSON block model: `screen_panel.json`
- block-entity renderer: `ScreenBlockEntityRenderer`
- server/client layout cache: `ScreenBlockEntity`

The static JSON model cannot know connected-screen width, height, or local position. It currently paints fixed front/body textures for every screen block. The block-entity renderer then tries to draw a dynamic front overlay and text on top.

This split means there are three competing render responsibilities:

- static model body/front
- dynamic block-entity front overlay
- dynamic terminal text/background

That explains the observed loop: changes that improve one layer can expose or cover another layer.

## Current Hypothesis

The root problem is not one wrong texture name or one wrong z offset. The root problem is that multiblock screen visuals need one owner for connected-screen model geometry.

The likely durable fix is one of:

1. Implement a NeoForge 1.21.1 dynamic baked/model path equivalent to upstream `ScreenModel`, then keep the block-entity renderer responsible only for terminal text.
2. Move the full connected-screen face/body rendering into the block-entity renderer and make the static JSON model neutral enough that it cannot fight the dynamic renderer.

Do not keep patching both paths independently.

## Stop Rule

Before the next screen implementation change:

- choose exactly one architecture path above
- add a failing focused test for that path
- verify one live reproduction through MCP or screenshot
- change one render layer only
- if the after screenshot regresses another screen behavior, revert that patch


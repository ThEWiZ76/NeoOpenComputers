# Robot afterimage interaction

Upstream common/block/RobotAfterimage.scala leaves a temporary invisible air-like block at the old position, redirects interactions to the adjacent robot and expires after max(floor(moveDelay*20),1)-1 ticks.

Added internal robot_afterimage block (no item/loot or blockentity), initial source replacement inside the existing rollback-safe relocation, scheduled expiry, dynamic matching collision/selection surfaces at source and destination, pick delegation and right-click menu/power delegation. Normal target break path handles permissions/events/drops before removing the proxy. Failed protected target break preserves both robot and proxy. Robot shape now follows interpolated movement position; renderer bounds were expanded in previous step. Blockstate references existing empty robot model to avoid missing client resource.

RED robot-afterimage-red.log: movement left plain air. GREEN robot-afterimage-final.log: full build, 2055 unit tests zero failures/errors, all 452 required GameTests pass. Tests verify air-like proxy without duplicate BE/inventory, matching world-space collision bounds, old-position click opens actual destination RobotMenu, target BreakEvent cancellation prevents proxy bypass, expiry leaves destination robot, and breaking via proxy drops exactly three stored diamonds once. Mock players removed in finally. Menu fixture explicitly declares neoforge:advanced_open_screen because embedded mock connections skip the client handshake.

Built artifact 32BBD01DF8B148EC442DCB8C848A5C8B89E8123220E2D974352C41D2BAE1B2F2. Not installed or live-viewed. Minecraft remains closed; profile remains A8582A212D3267D316B894CE8C0580AA31A30C931C52A76E8AA241E9183488EE.

Remaining: live motion/shape/click/break on screen 3; fast-overlap guard and swing animation; movement direction/blockContent/replaceable/protection parity; chunk-boundary/unload/reload including transient animation handling. Lua execution persistence and full port remain incomplete. Earlier movement docs describing immediate plain air are superseded by this step.

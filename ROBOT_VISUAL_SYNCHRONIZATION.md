# Robot visual state synchronization

Source comparison found three defects: RobotBlockEntity inherited empty client update tags and no update packet; renderer queried an unstarted client-side machine; running light was fixed green and held stack incorrectly used the cargo selection index as an inventory slot. Upstream RobotRenderer renders inventory slot 0 and robot.info.lightColor (default 0xF23030).

Implemented standard blockentity update packet plus initial chunk update tag. Payload contains only running/paused appearance, tier, light color and tool slot. No machine NBT, EEPROM/hardware, cargo or ownership is transmitted. Client handler changes render fields/tool only; never loads or starts the Lua machine. Server tick compares running/color/tier and a copied tool stack, sending updates when these change. Renderer uses synchronized state, configured color and TOOL_SLOT. New robots default to upstream red; explicit saved black is retained.

RED: build/finish-port-implementation/robot-visual-red.log, robotSendsVisualStateWithoutPrivateMachineData failed because running state was absent. GREEN: robot-visual-first.log; full test/build and all 449 GameTests passed. Unit reports: 2055 tests, zero failures/errors. GameTest runs real EEPROM to set 0x123456, checks tool durability, restricted payload, client initial tag, update packet removing tool, clearing running state, and no client Lua start.

Built JAR SHA256 9CAF1FD790A41FA94869447DA73C9E8D68DE2F032D78AF4013FAF4CA913E8019. Not installed or viewed in live client yet; Modrinth client remains closed on prior A8582A212D3267D316B894CE8C0580AA31A30C931C52A76E8AA241E9183488EE. Do not call screenshots/visual appearance verified from these server tests.

Remaining: render check in real Minecraft on display 3, tool/color/running updates while moving, movement/turn/swing animation and upstream action costs/delays. Current move and turn callbacks lack upstream energy/time semantics; inspect server/component/Robot.scala before choosing animation duration. Full OpenOS assembly, chunk-boundary and reload/persistence scenarios remain open.

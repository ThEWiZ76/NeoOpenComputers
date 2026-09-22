# Robot terminal port

## Reproduction and scope

The existing RobotScreen paints only a black rectangle. It neither consumes a terminal snapshot nor forwards keyboard, clipboard or mouse input. RobotMenu was an inventory-only menu. The terminal layer owns this fix; world models, textures and glyph rendering remain unchanged.

Baseline: commit b2ceb5cc, installed JAR SHA256 EDD0D05A574B859634BFFF35CEF2317DC5532E37C478D08CFAA1BBD602A0E067. Modrinth profile `NeoOpenComputers test instance`, Minecraft 1.21.1, world `testworld`, robot tier 1 at (108,-60,100), facing south; screen and keyboard upgrades, GPU, CPU and RAM. Right-click its south face. `build/finish-port-implementation/robot-terminal-before.png` is the current client's MCP framebuffer, inspected alongside its `li.cil.oc.client.RobotScreen` result. The blank panel is only evidence of the existing inventory UI, not proof of a booted computer.

Regression gate: RobotScreen must use the tested terminal input/rendering path; server tests additionally require real screen/keyboard/GPU wiring, snapshots and input gates. Rollback point is b2ceb5cc and the baseline JAR above.

The shared terminal path must use the robot's upper panel for both rendering and mouse coordinates. Robot controls and inventory stay outside that panel. Verify actual boot, typed input, screen-tier mouse behavior and regular screen regressions after installation. Compact no-screen layout and following a moving robot remain separate work.

Live verification with intermediate JAR 536E7CC1F61B16F9CA12E59BC391CBFC56E209256B1420A2A9638BD56E29357D proved text and keyboard input (`robot-terminal-typed-final.png`). Higher-tier mouse clicks failed (`robot-terminal-tier2-touch-final.png`): AbstractContainerScreen.mouseClicked unconditionally returns true, so TerminalScreen's old superclass-first handler never reached terminal mouse dispatch. Handle terminal coordinates first, then delegate remaining events to inventory handling. Drag/release must also fall back to inventory handling. This is the same terminal/input layer, with no world-render changes.

## Verified result

Final installed and built JAR SHA256: D3DA35C3952B32BD3693C1253F2CB6AB34A402F032D3BB1F7806D48669BE1E90. Full build, 2052 unit tests and all 443 required GameTests pass. No GitHub Actions workflows; optional scripts/check-actions-disabled.ps1 is absent.

Live tests in the same Modrinth profile/world at (108,-60,100):
- Tier-1 robot with tier-1 screen renders the EEPROM test program and displays typed Robot123 (intermediate artifact).
- Final artifact: tier-3 robot with tier-2 screen displays Typed OK and TOUCH 7,3 for GUI click (245,90). Screenshot robot-terminal-mouse-verified.png; GUI dimensions 683x360. Screen/GPU/CPU/RAM/keyboard/EEPROM fixture is recorded by setup-terminal-robot.ps1; this is not a survivalcrafting test.
- Grass block moved from player slot 48 to robot cargo slot 4 and back using actual mouse events. Readback verifies original slot restored, cargo empty, cursor empty. JSON robot-inventory-transfer and robot-inventory-return.
- Ordinary tier-3 physical screen: real OpenOS boot, Lua input and touch event (59,20) verified. Full 160x50 text on dark red background remains 60 FPS. World block still displays the framebuffer. Screenshots robot-change-regular-terminal, robot-change-regular-mouse, robot-change-regular-dense-final and robot-change-world-regression.
- An earlier dense capture was blank because the temporary power supply ran out (client reports Not enough energy); it is not successful evidence. Recharged and repeated, final capture above succeeds.

All artifacts are under build/finish-port-implementation. Fixture volume (100,-60,100)..(109,-56,109) cleared and fresh readback reports total_blocks=0. Client closed normally with all dimensions saved; pauseOnLostFocus restored true. Original robot outside the fixture area remains untouched.

Remaining robot work: compact no-screen layout; menu following movement; full live OpenOS assembly/crafting, clipboard, drag/scroll and access-permission matrix; running Lua persistence. This patch does not close the complete robot parity row.
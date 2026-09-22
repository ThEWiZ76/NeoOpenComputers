# Robot movement continuity

RED evidence: robot-movement-red.log, 2026-09-22. Real EEPROM completes first physical move but restarts (light=16711680/reboot sentinel); second step never happens. Existing 446 GameTests passed; new runningRobotMovesTwiceWithoutRebootAndKeepsItsOpenTerminal failed.

Root cause: moveRobot serialized, removed and recreated RobotBlockEntity. LuaArchitecture.load builds fresh globals/coroutine; open menus retained old BE. Source was removed before target placement was checked. Upstream instead keeps the live Robot object while changing proxy/position.

Implemented: retain the same BE, machine and component environments. A scoped mutable BlockEntity position accessor updates registration; machine teardown is suppressed only during synchronous relocation. Destination placement precedes source removal; failed transfer restores source registration and removes the staged destination. Recheck occupancy and world border after the cancellable pre-event. Preserve internal computer/ROM graph, disconnect previous external neighbors and join destination neighbors. Successful movement does not serialize/load Lua.

Verified: two physical steps from one real EEPROM coroutine; stable machine, architecture and component addresses; preserved framebuffer/viewport/cargo; nearby menu remains valid and targets the same screen. Old power converter disconnects and new adjacent converter connects. A pre-event that occupies the target leaves source/runtime/cargo intact, emits no success event, and can be followed by a successful move emitting exactly one Post event for the retained robot.

Full test/build/GameTest run: build/finish-port-implementation/robot-movement-verified.log; 2055 unit tests, zero failures/errors; all 448 required GameTests pass. Built JAR SHA256 4A0BA7523CC98B3326ADDF51F37908C7309D5BBF1821D3EA52F5A33EFF46FB1E. Not installed in Modrinth yet; client stayed closed.

Verification also exposed an existing fixture collision: keyboardDefaultUsabilityMatchesUpstreamRange registered near/far players and left the far player in the adjacent debug-card water scan. Failure diagnostic showed EntityLivingBase/test-mock-player instead of liquid. Distance-only test now uses unregistered mock players; debug-card diagnostic retains actual callback result. Replaced brittle source-string Post-event assertion with behavioral GameTest coverage.

Remaining: live client movement/input after movement; client running/light/tool state synchronization and movement animation; explicit chunk-boundary and reload scenarios; complete assembler/OpenOS interaction. World unload/restart Lua execution persistence remains a separate, unimplemented requirement. This is movement continuity evidence, not a full robot parity claim. Minecraft must use display 3; blocked startup helper must not be bypassed.

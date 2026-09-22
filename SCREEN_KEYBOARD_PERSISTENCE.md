# Screen and keyboard component persistence

Root cause: SimpleMachine.saveComponentEnvironment passes a temporary CompoundTag to each environment; unlike the GPU driver, screen/keyboard drivers never wrote it back to their ItemStack. Recreating either component therefore lost its node address, and the screen also lost its framebuffer/settings. Separate from Lua heap/coroutine persistence.

Fix follows the existing GPU dataTag/saveData callback pattern. Screen additionally saves palette, current palette flags and color depth. Missing legacy width/height/viewport fields retain tier defaults. Keyboard saves its component node but deliberately does not restore pressed-key state.

Verification: ScreenItemEnvironmentTest, KeyboardItemPersistenceTest and ScreenKeyboardPersistenceGameTests. Actual registered drivers create/save/copy/recreate ItemStacks; assertions cover stable screen/keyboard addresses, text/cell colors/viewport, backing resolution, palette/depth and powerstate. Full combined layout/persistence build: 2055 unit tests and 446 required GameTests passed. Artifact A8582A212D3267D316B894CE8C0580AA31A30C931C52A76E8AA241E9183488EE installed in NeoOpenComputers test instance. Logs: build/finish-port-implementation/robot-layout-persistence-build.log and robot-layout-persistence-gametest.log.

Running Lua still restarts when RobotBlockEntity.moveRobot recreates the block entity. This patch repairs saved component data, not live-runtime continuity or rendering of powered-off item screens.
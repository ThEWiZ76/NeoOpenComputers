# Robot break and replace persistence

Found missing robot loot handling: RobotBlock only dropped runtime inventory, while robot had no loot resource/override. Breaking discarded assembled hardware. Upstream RobotProxy returns an assembled robot item, preserving component state, and drops mutable inventory separately.

RobotBlock now produces the assembled item from its blockentity through standard getDrops; creative removal also returns the chassis, matching upstream. The item uses the same BLOCK_ENTITY_DATA placement format as RobotAssemblerTemplate. It retains tier/name/color/owner/node/ROM and saved hardware component data, but excludes separately dropped Items and clears running, pending signals and serialized architecture. On removal the machine saves component data and stops. Successful relocation still avoids teardown because source BE has already been transferred out of the block removal path.

Roundtrip test exposed EEPROM node identity loss: EepromEnvironment never loaded its node tag from item data or wrote node state through the item callback during save. Constructor now restores node metadata; save persists it without replacing EEPROM code/data/readonly fields.

Evidence: robot-drop-red.log fails because no assembled item drops. robot-drop-addresses.log isolates EEPROM address change while all other compared addresses remain stable. robot-drop-final.log: full build, 2057 unit tests zero failures/errors, all 454 required GameTests passed. Survival GameMode break produces exactly one robot plus exactly three loose diamonds; real BlockItem use places the dropped robot with CPU/RAM/EEPROM/screen, same framebuffer and component addresses, empty cargo and stopped machine. Explicit restart executes preserved EEPROM and sets expected light color. Test removes created server player in finally. Existing movement/afterimage and creative proxy cargo tests still pass.

Artifact SHA256 85477F1AB18C6294F43302AE3EABAF34D7C11574283B46F5E861897216606EAC. Not installed/live-viewed. Client remains closed; Modrinth profile still A8582A212D3267D316B894CE8C0580AA31A30C931C52A76E8AA241E9183488EE.

Remaining: live pickup/replacement in addition to motion/render/menu checks on display 3; creative pick-block copy semantics; explicit creative chassis-count/automation/explosion variants; full OpenOS HDD and filesystem state roundtrip; chunk unload/reload and Lua execution persistence; swing and other open parity matrix items. A stopped robot item is not proof of suspended Lua continuation persistence.

## HDD roundtrip verification

Extended the same survival break and real item-placement test with a managed HDD. It writes init.lua plus home/data.bin containing NUL and high bytes through the installed robot filesystem, then verifies the original filesystem address, disk label and exact bytes after replacement. Preserved EEPROM reads and executes init.lua from the restored disk; its light-color effect proves execution. Component identity comparison now retains every address instead of overwriting filesystems that share a component name.

No production change was needed for this HDD path. The first fixture used a 17-character label and failed discovery because ItemDiskLabel correctly truncates to 16; corrected fixture label robot-disk. This was a test setup issue, not an HDD persistence defect.

Evidence: build/finish-port-implementation/robot-hdd-final.log, test build runGameTestServer successful, 2057 unit tests (zero failures/errors), all 454 required GameTests. Artifact SHA256 0F00E34B5A9FB5FA008B03382787CBB2C71DDD7F2C02F383B5AB90DBDF0B2E83. Not installed/live-tested; Minecraft stays closed. This covers managed disk data and a disk-loaded program, not full installed OpenOS roundtrip, unmanaged sectors, chunk reload or suspended Lua state.

## Complete OpenOS boot after pickup

robotBootsOpenOsAndRunsBundledGoAfterPickup now boots the full bundled OpenOS image twice around a real block drop and item placement. First boot mounts robot ROM, runs go left and writes /home/pickup.txt; second boot reads exact saved content and runs go again. It uses real assembler inputs and Lua BIOS. The fixture copies OpenOS files onto HDD; interactive installation and live UI remain unverified. Evidence and current artifact: ROBOT_BLOCK_SWING.md / robot-swing-protection.log (2057 unit tests, 456 GameTests).

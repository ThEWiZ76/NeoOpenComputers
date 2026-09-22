# Robot runtime component containers

Upstream common/tileentity/Robot.scala containerSlotType/containerSlotTier resolve the assembler-installed container driver and its provided slot/tier. Runtime slots 1..3 accept only matching drivers at or below that tier, excluding screens and keyboards. Tool slot and ordinary cargo accept arbitrary item stacks; storing CPU/card/upgrade hardware there does not install it.

The port previously used one broad assembler-hardware exclusion for all twenty mutable slots, so supported runtime cards/upgrades were rejected, empty container slots accepted arbitrary items, and even cargo rejected hardware items. RobotBlockEntity.canPlaceItem now consults the matching assembled container in hardwareItems for slots 1..3 and enforces provided type/tier plus the screen/keyboard exclusion. The generic static prefilter used by quick-move now allows valid nonempty mutable-slot candidates; authoritative per-instance canPlaceItem determines runtime container acceptance. Tool/cargo accept ordinary stored hardware as upstream does.

New real-robot GameTest installs a tier-two card container and tier-one upgrade container, then verifies supported network card/hover upgrade, rejection of too-high GPU tier and wrong type, screen/keyboard exclusion, unavailable third slot, and hardware storage in tool/cargo. robot-container-rules-red.log reproduced rejection of a supported card. An older GameTest expectation banning CPU cargo was corrected to upstream behavior.

This is only the placement-rule stage. Remaining: create/disconnect runtime environments without rebooting Lua, persist component data on removal and reload, stable runtime/assembled slot addressing, one-item component stack rules, screen/client slot type/tier display and authoritative client synchronization. Direct Container.setItem behavior and previously stored invalid contents need deliberate handling; this change does not delete any existing items.

robot-container-rules-final.log passed all 493 GameTests but exposed an obsolete unit test that searched source text for the old hardware ban. Removed that contradictory text-pattern test; the real runtime GameTest now verifies the behavior. Unit-test count consequently decreases by one, intentionally.

Verification: robot-container-rules-verified.log full test/build/GameTest success; 2057 unit tests zero failures/errors and all 493 required GameTests. Artifact SHA256 A94F81591CC341815D5920E3A0DB58CA477F8165396C14294E454F7BBFBB649F. git diff --check clean. Not installed/live-tested; client remains closed.

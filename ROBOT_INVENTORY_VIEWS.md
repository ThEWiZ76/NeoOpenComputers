# Robot agent inventory views

During attack-XP parity inspection, equipmentInventory was found to be an independent SimpleContainer(1), and mainInventory returned the whole 20-slot menu container. selectedSlot is cargo-relative (0..15). Therefore inventory-controller equip wrote an invisible tool slot and swapped the wrong main slot; upgrades reading selected cargo also used indices four slots too early.

Upstream common/tileentity/Robot.scala defines equipment as a four-slot InventoryProxy and main inventory as a proxy starting after those four slots. The port now exposes two live Container views backed by the actual RobotBlockEntity slots: equipment 0..3, cargo 4..19 with relative API indices 0..15. Reads, writes, removals, clear, validity and placement checks delegate to the real container. View bounds prevent invalid cargo/equipment indices from crossing into another region. Menus, serialization and block inventory indices remain in their existing combined layout.

GameTests on a real running RobotBlockEntity reproduce and verify inventory-controller swapping a sword/pickaxe through the last selected cargo slot in both directions, preserving unrelated cargo. A second test verifies both live ranges, removals, boundary rejection and independently clearing cargo/equipment. robot-inventory-views-red.log reproduced both defects before implementation.

Follow-up discoveries still open: getComponentInSlot currently returns null; hardware is stored separately while some legacy-facing handlers use combined component indices. Resolve those mappings independently with real hardware tests. Attack XP handler registration is also still missing. The upstream attack Post awards action XP for removed targets; it does not collect arbitrary living-mob XP orbs like the ore-XP path. Keep that distinction when implementing the remaining handler.

Verification: robot-inventory-views-final.log test build runGameTestServer successful; 2058 unit tests zero failures/errors and all 482 required GameTests. git diff --check clean. Artifact SHA256 A3F357E38A1C4240A01F31FCA37DEA1D00268DA3A0538B84FE707D8439DD425F. Not installed or live-tested; Minecraft remains closed.

## Installed hardware environment lookup

RobotBlockEntity.getComponentInSlot was an unconditional null stub. It now resolves the requested hardware slot using the existing componentSlots address map and current direct neighbors of the machine node, rejecting invalid slot indices and returning null for slots without connected environments. It does not retain a second cache of environments that can outlive machine rebuilds.

Real robot with EEPROM and installed experience upgrade: every mapped connected environment must be returned by identity for componentSlot(address), invalid indices resolve nothing, and onHostChanged must replace those environments with new connected instances. robot-component-lookup-red.log reproduced the unconditional null result.

This restores the existing port hardware-slot namespace shared by componentSlot and internal tank ordering. It does not claim the legacy combined inventory/component namespace is fully reconciled. componentCount semantics, hover handler access to separately stored hardware, and runtime container components remain further parity work.

The first lookup implementation still failed the identity test (robot-component-lookup-final.log). Investigation found shared pendingComponentSlot state mutated by nested hardware enumeration during architecture.recomputeMemory inside SimpleMachine.onHostChanged. Multiple environments could therefore map to the same wrong slot; a lookup alone was insufficient.

MachineHost now has a backward-compatible default onMachineConnect(Node, ItemStack) overload that delegates to the original callback for existing hosts. SimpleMachine supplies the exact source stack. RobotBlockEntity resolves that stack by identity in hardwareItems, registers its address/slot, then performs the existing connection setup. Its iterator no longer mutates a shared pending-slot field. Other hosts keep their existing callback behavior; their own iterator-based mappings warrant a separate audit.

Verification: robot-component-lookup-mapping.log full test/build/GameTest success; 2058 unit tests zero failures/errors and all 484 required GameTests. Artifact SHA256 7EFBF20CF7ED3F58CCFA783F8AF45E29D01127999FE0B2A5D7195584135CD878. git diff --check clean. Not installed or live-tested; client remains closed.

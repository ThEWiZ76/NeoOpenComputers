# Machine host component-slot mapping

After the robot fix, the same shared pendingComponentSlot hazard was found in ComputerCaseBlockEntity, MicrocontrollerBlockEntity, DroneEntity and ServerRackMountableEnvironment. Their hardware iterators wrote a mutable field used later by onMachineConnect. SimpleMachine architecture initialization calls recomputeMemory(host.internalComponents()), re-entering that enumeration before the processor environment is connected. The processor can then be assigned another component slot.

All four hosts now implement the source-stack callback added to MachineHost. They find the exact stack by identity in their real inventory, map its connected node address to that slot, then call their existing connection callback. Iterators are read-only. Rack bus-connectable registration stays in its original callback and still runs. Existing default-overload compatibility for external hosts is unchanged.

ComponentSlotMappingGameTests installs real CPU, RAM and EEPROM stacks into each concrete host, rebuilds the machine twice, and verifies unique mapping of exactly the installed slots plus rejection of unknown/null addresses. RED host-component-mapping-red.log reproduced duplicate slots in all four: case 6, microcontroller 2, rack 8, drone 4. These are fixture slots, not universal slot constants.

Remaining port work includes legacy combined component-slot semantics for robots, runtime container hardware, world interactions, combat details, persistence and the broader parity plan. Client visual/in-game tests remain pending while monitor placement is unavailable.

Verification: host-component-mapping-final.log full test/build/GameTest success; 2058 unit tests zero failures/errors and all 492 required GameTests. Main artifact SHA256 B1F5E7199132A46B3F56091D99D21859A61626420541EAF6A00E13C4FA9A59D7. git diff --check clean; no pendingComponentSlot references remain in production Java. Not installed or live-tested; client remains closed.

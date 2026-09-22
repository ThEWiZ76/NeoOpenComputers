# Rack loading before world attachment

2026-09-22, feature/finish-port. Related: [[../Project]], [[../02 plans/Complete portering]], [[../05 memory/OpenComputers Memory]].

## Failure and fix

Vanilla BlockEntity.loadStatic calls loadWithComponents before a block entity has a Level. RackBlockEntity.loadRackData immediately created rack mountables. ServerRackMountableEnvironment.load and DiskDriveMountableEnvironment.load deserialize ItemStacks through world().registryAccess(); populated nested inventories therefore caused a null-provider exception, and vanilla discarded the rack. Earlier GameTests loaded NBT into already placed blocks, missing this lifecycle. The issue appeared in robot-client-containers-final.log even though all 500 tests passed.

RackBlockEntity now defers mountable creation while level is null. onLoad creates missing mountables once the world is available. Saved raw mountableData remains intact during the delay, including saves before attachment. Repeated onLoad does not reconstruct existing mountables. Normal changes to a rack already in a world still refresh immediately.

The regression additionally found that server saves serialized items before machine.save flushed environment data into them, losing current component state such as EEPROM node identity. ServerRackMountableEnvironment.save now flushes the machine before serializing inventory. DiskDriveMountableEnvironment uses the same ordering for filesystem state and its disk stack. Server load calls machine.onHostChanged after inventory deserialization and before machine.load so restored hardware and architecture exist when machine state is restored.

## Evidence

RackPersistenceGameTests.rackLoadsBeforeLevelAssignmentWithoutLosingServerInventory uses the real BlockEntity.loadStatic path, saves and reloads again while still unattached, attaches to the level and calls onLoad. It checks every server inventory slot, EEPROM component availability, the actual persisted floppy stack with custom name/components, server/drive addresses, and repeat-onLoad identity. Expected stacks are captured from the actual inventories after environment state has flushed; the original floppy input is not the drive's stored copy.

rack-detached-load-red.log fails with Vanilla discarded the saved rack during level-less loading. Subsequent diagnostic run exposed EEPROM node data loss. rack-persistence-verified.log passes test/build/runGameTestServer: 2059 unit tests, zero failures/errors; 501 required GameTests. No ERROR or Exception entries and no rack-load failure in that final log. Artifact SHA256: 767C7A1A50DE85C7099A1A58F453A15B6B083454A63CBB3AD2F89996FE7EFE22. Not installed into Modrinth or tested visually.

## Remaining

This proves nested data preservation and component reconstruction through the detached vanilla load phase. It does not prove full live Lua heap/coroutine continuation, all third-party rack mountables, or complete port parity. Follow-up: running rack server restart/resume through the same lifecycle and broader persistence matrix. Minecraft client remains closed until screen3 placement is assured.

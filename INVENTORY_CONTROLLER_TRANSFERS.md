# Inventory-controller targeted transfers

Related: [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Port gap triage]], [[../04 ai-context/Port parity matrix]].

## Contract

Upstream reference: server/component/traits/InventoryWorldControlMk2.scala, UpgradeInventoryController.scala and util/InventoryUtils.scala at master-MC1.12 revision 7d9eac5584e238a890a388a0f697c27c7391caa4.

The inventory-controller upgrade exposes these synchronized callbacks on robots and drones. Adapters retain inspection-only behavior.

- `dropIntoSlot(facing, slot[, count[, fromSide]])`: insert from selected cargo into exactly one target slot. Return true after any insertion, false for empty selected cargo, or false plus `inventory full/invalid slot` when the target accepts nothing.
- `suckFromSlot(facing, slot[, count[, fromSide]])`: extract from exactly one target slot into internal cargo, beginning at the selected slot and wrapping. Return the actual transferred count, or false when nothing fits/can be extracted.
- Count defaults to 64 (including explicit nil) and clamps to 0..64. Slot is one-based within the exposed item-handler capability, which may expose a subset of physical slots (e.g. furnace top/fuel/output).
- Robot facing accepts bottom/top/front (0/1/3), transformed to its world orientation. Drone facing accepts all six world sides. The optional fromSide is a world side; default is opposite the resolved facing, matching upstream.
- Missing, unloaded or protected inventories return nil plus `no inventory`. Invalid slot/side arguments throw the upstream argument error. Empty selected cargo returns false before drop target-slot validation.
- Both block and entity item-handler capabilities are supported. Fake-player block cancellation/use-block denial, entity cancellation and InvWrapper usability are checked before transfers. No adjacent inventory means no loose-item pickup or ordinary world-drop fallback.
- Successful transfers request the configured robot drop/suck delay once. Rejected operations do not pause. Extraction is simulated against internal capacity before mutating the source; any unexpected excess after real extraction is offered back to the source before a world spill.

Implementation: InventoryControllerEnvironment.AgentInventoryControllerEnvironment delegates to AgentInventoryWorldControl. This preserves the existing unrestricted robot/drone suck/drop paths and shares their capability lookup, fake-player events and selected-first insertion behavior.

## Verification

The first two required GameTests failed on the absent `dropIntoSlot` component callback for both hosts. Log: build/finish-port-implementation/inventory-controller-targeted-red.log. Additional tests cover native installed-controller Lua programs, sided furnace capabilities, robot orientation, entity inventories, protection, partial capacity, named-item data, exact counts/slots/defaults and delays. An initial native-robot fixture incorrectly tried to install a tier-2 upgrade into a tier-1 robot; the fixture now explicitly uses a tier-2 robot, preserving real hardware restrictions.

Final runtime checkpoint 7d4cef09263e77c1798c8f38792b01941a91443f: full build, 2121 unit tests (zero failures/errors), all 691 mandatory GameTests pass. Log: build/finish-port-implementation/inventory-controller-targeted-verified.log. Seven new tests cover both hosts, native installed upgrades, furnace sides/rotation, selected-first insertion, block cancellation/denial, entity protection/capabilities, count/default/error contracts and delays. Server evidence does not establish visual/client, multiplayer/protection-mod or every modded item-handler acceptance. Item-contained inventory callbacks are a separate remaining gap. The broader existing inventory inspection API still needs its own side/capability/error parity review.

## How to run/use this

From M:/development/OpenComputers in PowerShell:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer --no-daemon --console=plain
exit $LASTEXITCODE
```

Use only one Gradle process at a time. Check its terminal exit code and the server log for all required tests passing. No Minecraft client or Modrinth profile is launched by this command.

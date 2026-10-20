# Portable media, menu transfers and rack module cleanup

Runtime checkpoint aeae0a776273805c475b11cae42883fc5b5d7938, 2026-10-20. Related: [[Media host persistence and rack main bus]], [[ComputerCraft media and diskdrive persistence]].

Vanilla quickMoveStack copies and shrinks the source before Container removal callbacks. Saving only in those callbacks loses current component state from the transferred copy. ComponentInventory now exposes a targeted saveItemData hook before copying in diskdrive/tablet, computer case, rack, rack server and robot menus. The hook persists the installed environment, not the Lua VM. Rack modules flush their nested mountable state into the actual item component before copying; replacing CustomData also detaches the immutable item snapshot.

Tablet expansion inventories save before split, take, replacement and clear; removeItemNoUpdate also updates the runtime through the existing listener. Removing a whole rack diskdrive removes its child filesystem node, closing its owned channels rather than leaving an orphan network.

## Verification

Seven corrected baseline regressions failed before these changes: block-drive, rack-drive, case/internal storage and robot-runtime shift moves; tablet expansion removal/shift move; whole rack module transfer; and orphan child filesystem after module removal. Tests inspect both the returned menu snapshot and actual destination item, exact item count, stable filesystem identity and binary bytes 0/128/255. The initial robot fixture accidentally selected the readonly ROM; the corrected runtime-slot fixture supplies the causal baseline.

Three native CC tests cover tablet item serialization/handoff with initial charge preserved, robot block reload, and whole rack-drive detach/reinsert. Tablet/robot Lua programs retain local731, machine/media identities and an open read offset; independent native CC mounts verify bytes and labels. These two portable-host tests already passed before the production fixes and are acceptance evidence, not claimed bug fixes. The rack-detach test verifies an expired handle cannot write, Windows/native file deletion succeeds after cleanup, deleted stale write handles cannot recreate a file, and kept binary data/address survive reinsertion. Whole-module removal intentionally clears rack bus mapping: the fixture reconnects the actual SOUTH bus after reinsertion, instead of bypassing topology or adding artificial connector capacity.

Build and 2143 unit tests pass. All807 mixed-provider and742 optional-mod-absent GameTests pass. Both normal JVM restart phases pass767 tests in fresh world oc-restart-portable-menu-20261020-01 (prepare PID39120, verify PID9584), including existing case/rack/external CC-media continuation probes. All Gradle/server processes terminate normally. These existing process probes protect the changed rack shutdown behavior; portable tablet/robot tests here are same-process serialization/reload, not new two-JVM portable-host proof.

Evidence is retained under build/finish-port-implementation/portable-menu-*: corrected red760/7; first green760/1 (module cache not yet flushed into item); mixed first807/1 (rack test bus mapping omitted); a second mixed807/1 (existing AE2 missing-CPU test returned incomplete_plan); final mixed, absent and both restart profiles with build/latest/debug logs. The AE2 test now waits for real ingredients and the recipe to be visible after the CPU-removal grid rebuild before requesting a no-CPU job. The final mixed profile passes the original strict no_cpu_found assertion. No AE2 runtime behavior was changed.

## Remaining acceptance

Other inventory/menu families and capability/automation callers still need the same copy-before-removal audit. Portable-host two-JVM persistence, older-world/addon migration, Lua-object conversion, transient mounts, crash/load/protection, visual GUI/model/resource checks and real survival/multiplayer/platform acceptance remain open. This package does not close the complete media or integration checklist. No Minecraft client, Modrinth, Computer Use or window movement was used; real client acceptance remains required on screen3 with focus coordination.

Whole-port beta estimate remains3-6 concentrated workweeks, low confidence, for full implementation plus acceptance. Test totals do not establish a completion percentage.

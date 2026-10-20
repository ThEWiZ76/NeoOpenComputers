# Screen keyboard routing and terminal hosts

Runtime cdff547e46b0c925912799ee2b3a7f6ca97a5900, 2026-10-20. Related: [[Block inventory menu access]], [[Minecraft test window]].

## Upstream contract and changes

Upstream common/component/TextBuffer.scala sends keyboard.keyDown, keyboard.keyUp and keyboard.clipboard to neighboring nodes. For a physical screen it does this for every member of that actual merged multiblock. Mouse events remain computer.checked_signal messages to reachable computers. common/tileentity/Screen.scala checks keyboard presence within its merged screens. The old Screen GUI is purely client-side; its range contract is different from block inventory containers. Keyboard.scala separately limits default keyboard use to eight blocks.

ScreenInputDispatcher incorrectly broadcast keyboard events to every reachable node. Physical screens and item screens could therefore trigger unrelated keyboards on the same wired network. The dispatcher now sends those three messages only to neighbors. Physical-screen dispatch visits every member of its own merged rectangle, preserving input through keyboards on non-origin members. getKeyboards now lists all member-neighbor keyboards. hasKeyboard no longer borrows presence from another rectangle that merely touches this group. The shared rectangle traversal checks chunk presence before each entity lookup. Mouse routing, coordinate precision, username settings and keyboard usability overrides/range are unchanged. Existing tablet, robot and rack terminal code already connects its own keyboard directly to its screen.

Bound physical TerminalMenu now requires a player in the same level, the nonremoved actual current screen in a loaded chunk, and keyboard presence. It rejects an old screen object after replacement and an old menu after player dimension change. It deliberately does not acquire the inventory-container eight-block limit: upstream's physical GUI has no such container check. Keyboard signal acceptance retains its existing separate range check. Unbound client snapshots, portable-screen ownership and wireless-terminal contracts are unchanged.

## Evidence

The focused dispatcher unit first failed its neighbor-versus-reachable assertion (three tests, one failure); its XML is retained. Six new actual-network/server tests cover physical and item screen isolation, both keyboards of a two-member screen receiving each event once and appearing in getKeyboards, separate rectangular groups in an L shape, another-dimension menu rejection and live stale-host/missing-player rejection. The physical fixture keeps both keyboards within eight blocks, connects them through a shared network, and verifies the unrelated keyboard is not a direct neighbor. Actual keyboard forwarding signals are captured with their source node identities; no fake dispatcher implements the expected result.

The initial785-test baseline had six failures: five real physical/group/menu regressions and an item fixture whose graph had not been joined to a network. After explicitly joining that fixture, a second785-test run retained the old reachable dispatcher with the group/menu fixes applied. It reproduced three routing failures: both physical and item leakage plus duplicate multiblock events. This distinguishes the fixture error from runtime failures. Evidence prefix: build/finish-port-implementation/screen-routing-. Logs use *-build.log, *-latest.log and *-debug.log; the first baseline uses red-first filenames. The failing unit XML is screen-routing-unit-red.xml.

Final build and2146 unit tests pass. All850 mixed-provider tests (CC/AE2/Mekanism/EnderStorage/ProjectRed) and all785 no-optional tests pass in fresh oc-tests-screen-routing-* worlds; both Gradle/server processes exit normally. No persistent-state format changed and no new two-JVM continuation claim is made.

## Remaining acceptance

Actual client text/paste/key release across multiblocks and transitions, GPU/resource-pack behavior, external protection/addon keyboards, survival/multiplayer, larger-layout/unload stress and broader full-port/API/platform gates remain open. The named Modrinth client remains at the preceding RAID build on user-confirmed screen3; MCP is reachable via IPv6 ::1:8081, but background testing awaits the manual F3+P toggle. No GPU/client acceptance is inferred from these server tests. Whole-port beta remains3-6 concentrated workweeks, low confidence; test counts are not completion percentages.

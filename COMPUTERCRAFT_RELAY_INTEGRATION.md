# ComputerCraft relay integration

Runtime checkpoint `68650919030cd096146fcada74ea159726017ffa`. Target: pinned CC:Tweaked1.120.2 / Minecraft1.21.1 / NeoForge21.1.234.

Related: [[../Project]], [[../02 plans/ComputerCraft integration]], [[../02 plans/Complete portering]], [[../04 ai-context/Modern integration scope]].

## Implemented relay contract

The original14 RelayPeripheral methods are exposed as a modem through the public NeoForge PeripheralCapability. One peripheral instance belongs to the relay across its faces. Optional CC classes are behind the mod-present boundary; relay fields/methods carry only a CC-free Receiver interface. forge-api is compileOnly, not a required published or bundled dependency.

Each attached computer has independent open channels; detach/removal clears them. Ports0..65535,128-channel cap, duplicate open/close results, closeAll, source `cc<ID>_<attachment>`, self exclusion, directed recipient filtering and the original modem_message layout are implemented. isWireless remainsfalse for legacy wired remote-access compatibility; isAccessPoint/isTunnel reflect the installed card, and maxPacketSize reflects OC configuration.

Incoming packets notify matching CC attachments before forwarding eligibility, following the old receive contract, then use the existing OC relay queue/cooldown/TTL/size and wireless/tunnel paths. CC transmit returns enqueue success and cannot bypass queue capacity or OC packet type/size validation. Arbitrary tables are rejected as packet payloads; no new table serialization protocol is invented.

Remote lookup unions the six OC networks, deduplicates addresses, respects Component visibility and returns one-based names/method lists. callRemote invokes the actual component using the relevant relay plug as Context.node; signals go to that attached CC computer. Missing components return no values. Lifecycle calls/pause/call budget follow the old CC context behavior. All world/network method work goes through ILuaContext.executeMainThreadTask, rather than running on a CC Lua worker thread. Concurrent attachment storage handles CC's attach/detach threading contract.

## Explicit compatibility fixes

- Old RelayPeripheral.transmit appended an Integer reply channel, but Relay.tryEnqueuePacket consumed a leading Double. New transmission puts a leading Double reply channel before the payload, matching OC-to-CC receive convention. This is a documented raw wire-layout correction, not byte-identical preservation of that old inconsistency. Reception accepts modern Lua5.3 integer Number values too; without a numeric prefix, reply is-1 and all payload parts remain data.
- OC byte strings must not be decoded to a Java Unicode string and fed straight to CC: pinned Cobalt's Java-string conversion is eight-bit. The first real CC test received `Euro ?`. Transmit captures original Lua bytes; receive preserves byte strings and encodes Java text as UTF-8. Binary0/128/255 and Unicode work in both directions.
- Remote callback arguments follow native OC conventions: raw string bytes, UTF-8 table keys and recursive table identity. Returned strings/keys, nested arrays/maps and forwarded signals are encoded for CC, retaining cycles. OC's existing converters describe ordinary callback results before the CC boundary. A superficial echo-only fixture initially passed because two wrong encodings canceled out; the corrected baseline asserts the actual Unicode seen inside the OC callback and fails before this fix. Broader OC Value/userdata adaptation remains part of the separate Lua-object work package.

## Verification

Five new server tests: actual CC Lua discovery/all14 methods/channel boundaries/128-channel limit and real remote relay mutation; actual CC-to-OC packet capture and OC-to-CC modem events with UTF-8/binary bytes; attachment isolation/directed/self-filtering/detach; queue/size/visibility/context/events/card flags; and a native Lua5.3 OC computer with installed modem/EEPROM exchanging ping/pong with a real CC computer. The native program's EEPROM data marker and the CC program's actual output both must agree. A test-only OC callback also checks the actual Unicode arguments, Unicode table keys, binary content and cyclic tables/results; it is not an added production component.

Baseline cc-relay-red-build.log:731 tests, precisely4 missing-capability failures, terminal failure. First implementation:777 tests, one received-message encoding failure. Diagnostic-values run:732 tests, one failure showing `Euro ?`; native53 roundtrip passes. Corrected remote-encoding baseline:732 tests, one failure `OC callback did not receive UTF-8 text` (cc-relay-remote-encoding-verified-red-build.log). Earlier roundtrip-only run is not evidence for internal encoding correctness.

Final build/2142 unit tests,778 full mixed-mod tests,732 CC-only tests and727 no-optional-mod tests pass with terminal process exit0. Profiles/logs: cc-relay-final-mixed-build.log (world oc-tests-cc-relay-mixed-20261020-03), -alone-build.log (world oc-tests-cc-relay-alone-20261020-01, reused after the failed shutdown described below), -absent-build.log (oc-tests-cc-relay-absent-20261020-01); latest/debug logs preserved alongside each. The successful CC-only retry enabled a loopback-only JDWP listener through an ignored init script; it did not reproduce the loop and no live breakpoint was attached. Existing packaging/POM tests guard optional dependency/class isolation. No extra controlled JVM restart/persistence pair is claimed for transient attachment/channel state.

## How to run

```powershell
rtk proxy cmd /d /c "gradlew.bat build runGameTestServer -PwithComputerCraft -PwithMekanism -PwithAE2 -PwithEnderStorage -PwithProjectRed -Pneoopencomputers.gameTestWorld=oc-tests-cc-relay-review-01 --no-daemon --console=plain"
exit $LASTEXITCODE
```

Use a fresh oc-tests-* name for independent runs. One Gradle at a time; await terminal exit, verify expected count and preserve latest.log before another run. CC-only retains just withComputerCraft; absence removes all optional flags. Primary public API guidance: [CC peripheral registration/threading](https://www.tweaked.cc/mc-1.21.x/javadoc/dan200/computercraft/api/peripheral/package-summary.html). Pinned publisher API/source artifacts and Cobalt0.9.9 diagnostics are cached locally under integration-discovery/computercraft; foreign sources/binaries are not committed or bundled.

## Remaining full-port gates

This closes the implemented/tested relay bridge scope, not all CC integration: peripheral-driver/media/mount/Lua-object contracts remain in the integration plan. Real-client discovery/program use/menus, survival/protection, multiplayer/reconnect, wider addon callbacks and platform/load acceptance remain required on screen3 with desktop coordination. No client, Computer Use, Modrinth installation or window-moving script was used here.

The unload loop remains an open beta gate. Additional reused-default-world runs on the preceding checkpoint pass734 Mekanism-only and773 mixed tests with clean exit. A local JDI probe observes ordinary unload holders with generationRefCount0/completed saveSync; that run does not capture the hot loop. Preserved logs: unload-debug-build.log, unload-debug-probe.log, unload-debug-mixed-build.log and their latest/debug copies.

The first final CC-only run completes all732 tests in a NEW world, then hangs during Saving worlds. Two dumps of positively verified owned test JVM56496 show the same ChunkMap.scheduleUnload/processUnloads callback loop during stopServer, with Server-thread CPU65859ms then92000ms. Evidence: cc-relay-alone-hang-build.log, -latest.log, -debug.log and cc-relay-alone-shutdown-threads.txt / -second.txt. Only that verified test JVM was stopped after preserving evidence; the Gradle handle returned terminal failure before the retry started. The world was retained. Its retry, with only a loopback JDWP listener added, exits cleanly. This disproves the narrower hypothesis that only reused worlds can hang; it neither proves CC/this bridge caused it nor establishes a fix. No production unload-scheduler change was made. Next diagnostic: arm a shutdown-only generation-acquisition stack probe before stopServer so a recurrence can identify the holder/refcount and the code that reacquires generation while unloading.

Whole-port beta estimate remains3-6 concentrated workweeks, low confidence, including full implementation, bugfixes and client acceptance. Test counts do not establish a completion percentage.


## Robot query unload follow-up

Checkpoint ddc7515421376e939a2984387e76e082ff82cafa reproduces/fixes an unload-time robot collision/redstone lookup that could reload a chunk. Pre-fix shutdown trace records36 generation tasks; mixed781 and the historical735-test CC world now both exit normally with0 generation tasks. See [[Robot non-loading queries]]. This supersedes the earlier unknown-cause status for that specific robot route, while broader stability/other historical freezes remain open.

# Native Lua runtime and persistence

## Current verified state - 2026-09-22

Lua 5.2 (native) is now registered and selectable alongside Lua (LuaJ). CPU defaults remain LuaJ; native is not silently forced onto existing computers. computer.getArchitectures/getArchitecture/setArchitecture work in the native runtime, and changing architectures reboots into the selected class in both directions. Native execution-state persistence is tested for cases, racks, microcontrollers, drones and robots. This does not establish full-world restart, external filesystem unload ordering, all host APIs or client/platform acceptance.

Lua API selection: require('computer').setArchitecture('Lua 5.2 (native)'); switching back uses 'Lua'. A change reboots immediately; selecting the current architecture does not, and an unknown name returns nil plus 'unknown architecture'. No new artifact has been installed in the Modrinth client.

During selection validation, one robot resume hung and a later run crashed in native lua_gc during NativeLuaArchitecture.load (run/gameTestServer/hs_err_pid66948.log). Eris issue https://github.com/fnuecke/eris/issues/27 describes GC collecting incomplete objects during restoration. NativeLuaPersistence.restore now stops GC during graph reconstruction and restores the previous GC state in finally, including failures. Three versioned regressions first failed because GC was running inside the restore hook. This is an evidence-backed mitigation; two subsequent full GameTest runs passed without that crash/hang, not proof against every native failure.

native-restore-gc-integrated.log and architecture-selection-final.log passed all522 GameTests; full2106 unit tests/build green. Earlier sections below are historical implementation checkpoints and may describe capabilities that have since been implemented.

2026-09-22. Related: [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Rack detached loading]], [[../05 memory/OpenComputers Memory]].

## Confirmed parity gap

The running rack reproduction boots a real server EEPROM, retains a local table value 731, writes a waiting marker, yields for continue_probe, saves a running rack, destroys the old mountables, restores through vanilla BlockEntity.loadStatic before world assignment, attaches the replacement, and sends the continuation signal. The EEPROM refuses a second boot using its persisted marker. running-rack-persistence-probe.log fails exactly with `Lua continuation lost: boot:2 program restarted instead of resuming`. This is distinct from the now-fixed rack inventory loss.

LuaArchitecture.save stores initialized/booted flags, boot source/address and memory only; load calls initialize, creating new Globals/LuaThread. No heap, continuation or Lua locals are saved. Upstream's LuaJ fallback likewise reboots, while the native architecture uses Eris for kernel/stack persistence. Full requested port parity still requires the native behavior; preserving the running flag or rebooting is insufficient.

The failing parity reproduction is retained in tools/native-lua-probe/RunningRackPersistenceProbe.java outside the default source set. It is an explicit unresolved release gate, not a passing regression and not evidence of completion. Its temporary src/main copy was removed after verifying the archive hash. Promote it to a required GameTest once the native architecture exists. Ordinary 501 green GameTests do not cover this missing behavior.

## Verified native feasibility

Standalone tools/native-lua-probe/NativeLuaPersistenceProbe.java uses upstream OC-JNLua/Eris and only opens selected Lua libraries. Separate JVM processes save and restore, so no Java/Lua heap is retained between phases. It verifies a local closure/table count (731 -> 732), a nested suspended coroutine (40 -> 42), a self-referential table, and a host callback rebound to a fresh Java lambda via permanent-object tables (7 + 5 -> 12). Every value asserted is deterministic; no clock/random values are asserted.

Verified on Windows x64, Eclipse Adoptium Java 21.0.10: Lua 5.2, 5.3 and 5.4 all pass. Snapshot sizes observed 1637/1645/1415 bytes; sizes are diagnostics, not assertions. Output: build/native-lua-probe/verified.log. No Minecraft client launched. This is a dependency feasibility probe, not a registered Minecraft architecture or a compatibility claim for Linux/macOS.

The exact artifacts are the versions declared by the inspected upstream master-MC1.12 build.gradle, downloaded from its dependency mirror https://asie.pl/javadeps/:

- OC-JNLua-20230530.0.jar SHA256 41ABCE30160A8014ABD34E885F0F073FB35C8D9E2F78D230B7B9E64A79CCEDBB
- OC-JNLua-Natives-20220928.1.jar SHA256 43979D288FE06A8323FDF89E05BFEC0448C64F026762B43EA1FA65BDA3768A17

verify.ps1 checks these hashes before loading. Downloads, DLLs, class files and snapshots stay under ignored build/native-lua-probe. No native dependency has been added to the mod or distribution. JNLua/Eris license notices must accompany any eventual packaged dependency. Current maintained library versions and timeout fixes still require review before production selection; successful execution of a controlled probe does not establish sandbox safety.

## How to run/use this

From repository root in PowerShell 7, point JavaHome at an installed Java 21 JDK:

```powershell
./tools/native-lua-probe/verify.ps1 -JavaHome 'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
```

Expected output: one SAVED and one RESTORED line per Lua 52, 53 and 54. Every restore runs in its own process and asserts closure, coroutine, cycle and callback state. Nonzero subprocess exit is a failure.

To reproduce the known Minecraft parity failure (currently expected to fail):

```powershell
Copy-Item tools/native-lua-probe/RunningRackPersistenceProbe.java src/main/java/li/cil/oc/common/gametest/RunningRackPersistenceProbe.java
./gradlew.bat runGameTestServer
Remove-Item -LiteralPath src/main/java/li/cil/oc/common/gametest/RunningRackPersistenceProbe.java
```

Only remove that temporary copy if it is unchanged from the archived probe. Do not count the default suite after removal as proof that this gate passed.

## Implementation sequence

1. Native state loader: pinned, licensed artifacts; OS/architecture selection and explicit availability reporting; lifecycle/close, memory allocation limit and selected-library sandbox. Validate Windows x64 here and Linux in an available runner. Preserve LuaJ as an explicitly nonpersistent fallback rather than labeling it native Lua 5.x.
2. Native MachineBoundArchitecture and scheduler: yield from Lua wrappers around callbacks, never persist active Java/C invocation frames. Preserve signal waits/deadlines and synchronized callback phase; implement deterministic permanent/upermanent maps for libraries and callbacks. Serialize Lua-owned closure/table/thread graphs with Eris and OC Value objects through existing save/load contracts.
3. Bind real computer/component/unicode/system/OS/userdata APIs and the existing OpenOS boot path. Preserve direct versus synchronized callback budgets, timeout enforcement and memory caps; no unrestricted Java reflection/IO libraries. Match CPU architecture selection and version-specific API behavior. Do not switch the default architecture before behavior coverage is equivalent.
4. Promote the running-rack probe to a required test; add robot/case chunk reload and restart cases, local/coroutine/table/proxy/timer/open-file continuation, pending synchronized calls, Unicode/binary strings, malformed snapshot handling, memory exhaustion and instruction timeout regressions. Persistence must not replay world-changing callbacks.
5. Verify snapshots and dependencies across supported platforms, real client/server play and long-running save cycles. Old LuaJ saves contain no recoverable heap; migration semantics must be explicit. Full port completion remains gated by these tests plus the other outstanding plan items.

## Primary references

- https://github.com/MightyPirates/OpenComputers/wiki (upstream persistence behavior)
- https://github.com/MightyPirates/OC-JNLua (JNI runtime, MIT notice)
- https://github.com/MightyPirates/OC-Eris (coroutine/closure persistence and permanent objects)
- Local upstream src/main/scala/li/cil/oc/server/machine/luac/NativeLuaArchitecture.scala, PersistenceAPI.scala and LuaStateFactory.scala; luaj/LuaJLuaArchitecture.scala.


## Native state integration: 2026-09-22

The mod now bundles the pinned OC-JNLua classes and native resources. build.gradle resolves only the two named upstream artifacts from the existing mirror, verifies their SHA256 before compile/resource extraction, includes JNI classes in main output class directories, and packages native resources under assets/neoopencomputers/lib. LICENSE-jnlua and LICENSE-eris (all three upstream submodule notices plus Lua5.4 source notice) are included in META-INF. This supersedes the feasibility-stage statement above that no dependency is bundled.

NativeLuaState owns a positive-memory-limit Lua52/53/54 state. It selects sanitized platform/architecture names, extracts an embedded library to a process-specific temporary file, loads each version once per classloader, and closes states (including setup failures). Unsupported targets or missing embedded resources fail explicitly. Selected base/coroutine/math/string/table/bit32-or-utf8 libraries are available; debug/Eris are kept in the host registry, unavailable as script globals. Host IO/OS/package/Java libraries are not opened; base dofile/loadfile are removed and load accepts text only. This is library/memory isolation, not a complete execution sandbox: instruction deadlines and the native machine/kernel scheduler remain to be implemented before selecting this runtime for player programs.

Integration evidence: initial native-state-integrated.log passed unit tests but failed NeoForge with UnsatisfiedLinkError lua_registryindex. JarJar put JNI classes in NeoForge's library layer while the loader belonged to the mod layer. Merely copying class files into resource output still left them undiscoverable as mod classes (native-state-module-fixed.log). extractNativeLuaClasses adds their directory to sourceSets.main.output.classesDirs, keeping JNI and System.load in the same defining loader in dev runs and the packaged mod. NativeLuaLoadingGameTests asserts loader identity and executes all three VMs in actual NeoForge.

Final native-state-classdirs.log: test/build/runGameTestServer success, 2066 unit tests (seven native state cases added), 502 required GameTests. Native tests check exact version, valid text compilation, rejection of binary chunks, unavailable host libraries, host-only debug/Eris access, deterministic memory allocation failure, stack cleanliness, close/idempotent close, and platform name rejection. Packaging tests inspect JNI classes, native resource entries and license notices. Windows x64 execution verified; Linux/macOS resources packaged but not executed here. Artifact SHA25683E5BE84BD2BA4A2E5463191F4E0C0AA2B3B48019474A6849227037CF3D83A89; not installed in Modrinth.

No CPU architecture has been switched or registered. RunningRackPersistenceProbe still fails on current LuaJ and remains an unresolved release gate. Next: native architecture lifecycle, bounded kernel execution, deterministic permanent objects, callback/signal scheduler and actual save/load of suspended machine state. Use the patched upstream kernel behavior: https://github.com/MightyPirates/OpenComputers/security/advisories/GHSA-54j4-xpgj-cq4g documents a timeout escape through xpcall in the Lua sandbox, fixed in OpenComputers1.8.4; a working native loader alone does not address that boundary.


## Native object graph persistence: 2026-09-22

NativeLuaPersistence now binds Eris to an owned NativeLuaState. Construct it after installing host APIs and before user code. It walks initial globals in sorted stable key order, discovers native/Java functions (including those captured as upvalues by Lua wrappers), and stores bidirectional permanent-object tables in registry references. Only native functions are permanent: Lua closures and mutable tables/globals are serialized, avoiding the loss of user state through a permanently bound global table. Fresh bindings may be installed in a different order and still resolve the same saved callback names.

save(index) emits a versioned CompoundTag envelope containing format1, the exact Lua version enum, a private persistence metamethod key and binary Eris data. It preserves the caller's stack. restore(tag) checks format/version/key/empty input before native decoding, pushes exactly one root value on success and restores stack height on failure. It adopts the restored private key for subsequent saves. Closing the persistence helper releases registry references; VM ownership remains with NativeLuaState. Use try-with-resources so the helper closes before its VM.

The private Eris spkey prevents a user table's ordinary __persist metamethod from running during a world save. Production uses a fresh UUID-derived private key; tests inject a fixed key and assert deterministic results. This does not yet provide OC Value userdata persistence or an execution deadline. Host-defined persistence callbacks and the actual machine snapshot root must be designed alongside the native architecture; this generic codec is not a replacement for that integration.

Twelve parameterized tests (all Lua52/53/54) verify close-old-VM/fresh-VM restoration of globals, lexical local tables/closures, nested suspended coroutines, table cycles, zero/high-byte strings, Lua load wrappers and rebound Java callbacks. Callback counters prove save does not replay the program. The test replaces the global registry root with the restored table and proves a restored load closure reads restored globals. An additional save/restore after adopting a different fresh VM key succeeds. Further cases reject missing host bindings, incompatible version/empty envelope, closed helper and malformed binary while preserving a sentinel stack value; user __persist hooks are not called.

Verification: native-persistence-first.log passed nine initial cases; native-persistence-integrated.log full test/build/runGameTestServer success after adding key-retention and missing-binding coverage. 2078 unit tests, zero failures/errors; 502 required GameTests. Artifact SHA256976674617CF275269872792CB646FFA9FD0E0570B21B38694A6C8C4B4F3B7E47. Not installed/live-tested.

Remaining integration: native MachineBoundArchitecture with kernel/signal/callback scheduling and instruction deadlines; snapshot root covering machine execution and shared VM metatables; component/value proxies, handles, timers and pending synchronized calls; real running-rack probe must become green. The default remains LuaJ, so in-game continuation is still not implemented. Saving near the Lua memory ceiling and cross-platform snapshot compatibility also remain unverified.


## Native machine continuation: 2026-09-22

NativeLuaArchitecture now connects Lua5.2/Eris to MachineBoundArchitecture and SynchronizedCallAware using the patched upstream machine.lua kernel at assets/neoopencomputers/lua/native/machine.lua. It remains deliberately unregistered: default CPUs still select LuaJ. The native GameTest selects the class explicitly on its CPU; this is an integration stage, not player-ready native/OpenOS parity.

The scheduler measures the kernel memory baseline, applies installed RAM, resumes signal waits, returns sleep/shutdown, and preserves both pending synchronized callback and pending callback-result phases. NativeLuaValues transfers scalar, binary string, array and map values across the component/signal boundary. Basic computer/component APIs are installed. Unicode and userdata tables are still placeholders, OS date/table-time and architecture selection APIs are incomplete, and OC Value objects are not supported yet. Complete those before registering this architecture or booting the full OpenOS parity matrix.

Snapshots contain the kernel coroutine, globals, pending callback/result, private persistence key, baseline memory and boot address. Save grants bounded temporary serialization headroom (execution limit plus max(2MiB,4*execution limit)), collects temporary objects and restores the execution limit. The first real rack run crashed while saving at ordinary tier1 RAM without that reserve (native-rack-first.log); native-rack-save-reserve.log then passed all503 GameTests. Large heaps near the configured cap still require dedicated stress coverage, including restore headroom and failed-save handling.

Shared string metatables must also be part of the snapshot root and reattached to the new VM. Otherwise string method syntax uses the fresh state's original pattern functions rather than the restored kernel's bounded functions. Both pending-callback tests reproduced this defect in native-architecture-boundaries.log and passed after preserving stringMetatable. Saving/restoring before the callback or after its result invokes the world callback exactly once and preserves local cyclic tables. The old VM is closed before the new one loads.

NativeRackPersistenceGameTests now proves an actual running native rack survives saveWithFullMetadata -> removal -> BlockEntity.loadStatic before Level -> attachment/onLoad -> continuation signal. EEPROM sentinel rejects a fresh boot; a retained local value survives; the rack remains running after continuation. The archived default-LuaJ RunningRackPersistenceProbe still fails and is not superseded for default CPUs by this explicitly native fixture.

Seven architecture unit cases use an injected advancing deterministic clock. They terminate infinite loops, pcall loops, xpcall/message-handler loops and a nested coroutine; check restricted debug information exposes no functions/upvalue values or host registry/hooks, no IO/package/Java/Eris access and rejection of binary loading; and verify both synchronized callback persistence phases. The sandbox intentionally exposes upstream's restricted debug table, not unrestricted host debug.

Verification: native-architecture-integrated.log test/build/runGameTestServer succeeded,2085 unit tests with zero failures/errors and503 required GameTests. Packaged artifact SHA256 BBDB3AC1A2523F04DC558DF6C18294F835B7A74888632B8268AB1C2FFE2E1CB0. No Modrinth install or client launch; screen3 constraint remains. Windows x64 only.

Next: complete native Unicode/OS/userdata/Value APIs and OpenOS boot; test actual handles, timers, proxy continuity, memory pressure and malformed snapshots without server crashes; case/robot/repeated world reload, CPU architecture selection/migration, Lua53/54 and cross-platform validation. Default LuaJ heap persistence and the remaining full-port plan gates are still open.


## Native Unicode and game time: 2026-09-22

NativeLuaLibraries now installs all ten Unicode host functions used by the upstream kernel, and OS clock/time/date. Unicode covers code-point char/len/reverse/sub, server-default-locale lower/upper, FontWidths-based charWidth/isWide/wlen/wtrunc. The existing LuaJ code-point helpers moved unchanged into shared UnicodeStrings, so the two backends use the same range and width handling. Empty width arguments and truncation past string width fail, consistent with the existing upstream parity tests. Width truncation retains the existing whole-code-point behavior (avoids splitting surrogate pairs).

OS functions use Machine.cpuTime and Minecraft ticks with the six-hour epoch offset. os.time accepts a date table with required year/month/day, defaults to noon with zero minutes/seconds, and uses the existing lenient GameTimeFormatter normalization. os.date supports strings or *t/!*t tables, ignores the timezone prefix because Minecraft has no timezone, falls back for non-string formats/nonnumeric timestamps, and preserves the existing unsupported-format behavior. The kernel supplies difftime. Host OS/IO/package execution remains unavailable; no native OS library was opened.

Eight native-kernel scripts initially reproduced seven failing Unicode/OS cases (the invalid-argument-only case already passed because every missing function errored). Implemented bindings made them green. Two additional deterministic locale cases cover tr-TR/en-US with locale restored in finally. Existing pending callback snapshot tests now capture Unicode/date functions and supplementary-character text before closing the old VM, then successfully call the restored functions after loading both callback phases. This also proves that the native binding maps include the new libraries.

Verification: native-unicode-os-first.log passed focused native and LuaJ architecture suites; native-unicode-os-integrated.log full test/build/runGameTestServer succeeded,2095 unit tests with zero failures/errors and503 required GameTests. Artifact SHA256 E0A04A56CBAC61B3F657170DC0EF8625EFE79BEEC666720D4527F633799511F3. No Modrinth install/client launch.

Remaining native work: userdata/OC Value persistence and method dispatch, handle/timer/proxy lifecycle, remaining computer/architecture/component API details, real OpenOS boot and memory/error boundaries, case/robot/save cycles, CPU selection and platform/version coverage. The default remains LuaJ and has no heap persistence. This step supersedes the earlier Unicode/OS placeholder notes only; it does not close the full native integration or full-port release gates.


## Native OC Value and HDD handle continuation: 2026-09-22

NativeLuaUserdata replaces the empty host table. The upstream kernel remains responsible for opaque Lua proxies, alias reuse, weak ownership, finalization and custom Eris persistence. NativeLuaValues pushes/reads only raw Java objects implementing OC Value; general Java reflection is not exposed to programs. Host userdata implements save/load, apply/unapply/call/dispose, methods/invoke/doc. Saving writes the concrete class name and raw NBT bytes; loading verifies Value assignability before constructing via its no-arg constructor and calling load. Private constructors are supported for FileSystemEnvironment.FileHandleValue. This uses the existing trusted mod-class contract; malformed machine snapshots and save-failure isolation still require further coverage.

Value apply/call results go through the existing converter registry via OpenComputersApi.convert (now public for internal machine package access). Component and Value invocation share NativeLuaValues.invoke's upstream result protocol: zero results on direct-budget exhaustion, true/results on success, false/message for bad arguments, true/nil/message for other failures. Callback dispatch still uses Machine.invoke. LuaArguments moved unchanged from LuaArchitecture's private record into a shared package record; the two existing reflection-based validator tests now instantiate it directly. No semantic change to the LuaJ argument checks.

NativeLuaUserdataTest uses a stateful custom Value to exercise properties, callable values, annotated method dispatch/documentation, repeated Java-object identity and aliases nested in tables. Both before-callback and after-callback snapshots retain counter42 and a captured callback after the original VM closes. A restored call advances its new Java instance to43, while the old instance remains42; the checkpoint world callback executes exactly once. Initial native-userdata-red.log failed both cases as expected before the bridge. native-userdata-first.log made native cases green and exposed two obsolete test reflection class names after moving LuaArguments; those helpers were corrected.

NativeRackPersistenceGameTests adds actual HDD file continuation through the real native architecture and detached rack load. The program writes a/zero/255/z, closes and opens a read handle, reads only a, keeps two aliases, and waits. After save/remove/BlockEntity.loadStatic/attach, the same Lua program reads the remaining binary bytes from the saved offset, observes EOF, closes the handle and updates EEPROM. No reboot or file reopen is used to pass this test. Both rack fixtures have a tier1 HDD in slot6.

Verification: native-userdata-handles.log full test/build/runGameTestServer success,2097 unit tests zero failures/errors and504 required GameTests; no ERROR entries. Artifact SHA256 F735E70C6CFBB847B9CCB7548DC5FDDBA792FA9538CAF5ABB7553C6FA4119A8A. No client launch or Modrinth installation.

Still open: actual native OpenOS boot, remaining computer/component contracts and architecture selection; tmp filesystem continuity (SimpleMachine.save/load currently does not serialize its temporaryFileSystemEnvironment, so HDD success does not prove tmp); real inventory/trade Value converters and disposal/budget edge cases, cyclic boundary tables, repeated saves/memory pressure/error isolation, case/robot timers and platform/version matrix. Default CPUs remain LuaJ and therefore still lack heap persistence. Full port completion is not claimed.


## Temporary filesystem persistence: 2026-09-22

SimpleMachine now saves its temporary filesystem environment under the tmp compound before the architecture snapshot. This includes filesystem node identity, file contents, owner map and open input/output handles supported by MemoryFileSystem. Loading closes the old architecture first (old Value finalizers must run against old filesystems), replaces the old temporary environment, loads the saved tmp compound and connects the restored node before loading architecture state. Replacement also ensures legacy snapshots without tmp start with an empty temporary filesystem instead of retaining unrelated current-session files. Existing eraseTmpOnReboot behavior is unchanged.

The added real native rack test failed in native-tmp-red.log with 'tmp address changed'. The corrected native-tmp-first.log passes all505 GameTests, including both HDD and tmp binary handle continuation. The shared scenario obtains tmpAddress before enumerating filesystems, chooses the intended filesystem, saves two handle aliases after reading one byte, reloads the detached rack, asserts the tmp address is unchanged, reads the remaining zero/255/z bytes at the saved offset, checks EOF, and closes the handle. A surviving Lua local alone was not accepted as filesystem proof.

Two MachineRegistryTest cases verify repeated load restores the same node address and open-handle position each time, and loading a legacy snapshot without tmp detaches the previous tmp node and removes its unrelated files. The existing configured reboot-erasure regression remains green. native-tmp-first.log passed focused machine tests plus505 GameTests; native-tmp-integrated.log full test/build passed2099 unit tests with zero failures/errors. Artifact SHA256 057744C5794F818808EA3FA0CEC5B433037C96302DB99B19F9FBE2986D64297A. No client launch, install or push.

Next native integration gate is actual OpenOS boot using the existing fixtures in NeoOpenComputersGameTests: computerRunsWithLuaBiosAndOpenOsFloppy around10003, tier3ComputerBootsOpenOsFromInternalFloppy around10656, robotBootsOpenOsAndRunsBundledGoAfterPickup around11673. Revalidate line numbers. Remaining API/CPU architecture selection, timer/uptime continuity, memory-pressure and malformed-save isolation, supported Lua versions/platforms and full port plan remain open. Default LuaJ still does not retain the Lua heap; tmp persistence alone does not change that.


## Native OpenOS shell on tier1 hardware: 2026-09-22

nativeOpenOsTerminalRunsTypedCommand now runs the existing full BIOS/OpenOS floppy/keyboard/screen fixture with a CPU explicitly set to NativeLuaArchitecture. It uses the same tier1 CPU, GPU and192KiB memory module as the LuaJ variant, waits for /home shell prompt, types echo ocok through the screen keyboard signal path, submits Enter and requires output beyond the echoed input. The shared fixture now also reports stopped-machine errors. This is real NeoForge headless game behavior, not a visual client claim.

native-openos-first.log failed with 'not enough memory'. Upstream NativeLuaArchitecture.scala initializes ramScale using actual lua.getPointerWidth and Settings.ramScaleFor64Bit (default1.8); this was missing from our native integration. ModSettings now exposes computer.lua.ramScaleFor64Bit and maxTotalRam (default67108864 bytes). Native RAM recomputation caps logical user RAM; 64-bit physical allocation scales by1.8 while computer.totalMemory/freeMemory report unscaled installed/free bytes. Limits saturate within the JNI integer ceiling. Saved state records ramScale next to physical kernelMemory so restoration translates the baseline when the scale changes; old experimental snapshots without scale assume1. Serialization reserve remains host-only and bounded.

The same tier1 OpenOS fixture passes after this correction; no hardware/RAM upgrade was used to bypass the failure. native-openos-ram-scale.log passed focused native architecture/config tests and all506 required GameTests. A new native-kernel regression verifies reported512KiB RAM, bounded free memory, rejection of an8MiB allocation and unchanged reported RAM afterward. Native config defaults are asserted. native-openos-integrated.log full test/build passed2100 unit tests, zero failures/errors. Artifact SHA25686FF62FDB96712AF5CF34F7386B93DC4B00A161FC610A40E8E253F616238D7CA; no install/client/push.

Native is still not registered/default. Next: full OpenOS session save/reload with shell/timers/proxies, remaining computer/component and architecture APIs, memory-pressure/error and repeated-save cases, real client/platform/version coverage. This successful single boot/command flow does not complete the full port or prove all devices and visual/integration requirements.

## Native OpenOS shell reload: 2026-09-22

Native OpenOS now passes a detached case reload with a shell environment variable: set ocresume=731 before saving, echo $ocresume after restoring a fresh block entity, requiring standalone output 731. The extended fixture has a powered converter and asserts no network energy loss during synchronous reload. Initial NoEnergy was insufficient fixture power; export was also corrected to the bundled OpenOS set command.

SimpleMachine persists accumulated uptime across different clock epochs and repeated loads, resetting on restart. Fixed-clock regression reproduced 2.5 seconds becoming zero before the fix. Current wall-clock semantics still differ from upstream tick-based uptime; timer/deadline parity remains open. The case test does not reload the external floppy or the whole world. Native VM disposal on case removal also remains open.

Verification: native-openos-reload-powered.log all507 GameTests passed; native-openos-reload-integrated.log full2101 unit tests with zero failures/errors and successful build. Artifact SHA256 69B7C8A6B9D1A0E2A452B69A828375DA5BFB5675D3824411B4AC4A8EC9248BA7. No client/install/push; native still unregistered/default LuaJ. Next client launch awaits the requested coordination for brief focus use; independent development continues. Full port remains incomplete.

## Tick-based uptime and native deadline continuation: 2026-09-22

SimpleMachine now follows upstream Machine.scala uptime semantics: advance one tick per running update, including sleep/pause, report ticks/20. Stop/restart resets. Saves use a long uptime tick counter; previous uptimeSeconds snapshots migrate on load. Offline wall time does not count. This supersedes the earlier wall-clock uptime caveat.

uptime-ticks-red.log reproduced three focused failures against the old wall-clock implementation. Updated unit tests exercise fixed-clock jumps, sleeping/paused updates, stop/restart, repeated reloads and old seconds snapshots. nativeRackRetainsUptimeDeadlineAfterReload preserves a five-second deadline, checks elapsed ticks survived detached reload, then continues computer.pullSignal until the deadline. This proves basic native deadline continuation, not all OpenOS event timers or full-world restart.

uptime-ticks-integrated.log passed all2102 unit tests with zero failures/errors, all508 required GameTests and build. Artifact SHA256 0AB8E04BCF9DCDEAE27C1310988D293E4E1163C15161CC1EDDE6E8F8DDEE5788. No client/install/push; screen3 focus coordination is still pending. Next: native VM disposal on host removal, world restart, OpenOS timers, remaining architecture/API/platform/visual requirements. Full port incomplete.

## Rack VM disposal: 2026-09-22

RackBlockEntity.removeMountable now stops discarded Server machines after saveMountableData and before removing the node. Five native rack tests first failed because the old Lua VM remained initialized. They now assert disposal before restoring local variables, HDD/tmp open handles and deadlines. A new chunk-unload-before-save case verifies cached continuation survives this ordering and repeated removal notification. native-rack-disposal-integrated.log passed2102 units, all509 GameTests and build. No client/install/push. Case/robot/microcontroller/drone lifecycle coverage, full-world restart and full-port gates remain open.

## Case disposal and isolated shutdown: 2026-09-22

ComputerCaseBlockEntity now caches complete inventory/machine save data before disposing the old VM. Saves after unload reuse a copy, repeated removal preserves the original snapshot, and onLoad restores same-instance reattachments. Three native OpenOS tests verify disposal and continued shell variable/command behavior with save-before-remove, unload-before-save and same-instance reload. They initially failed because the VM remained initialized.

The first disposal fix blanked screens: machine.stop broadcast computer.stopped while still connected. Correct order is snapshot, disconnect, then stop; RackBlockEntity now also explicitly disconnects its machine node before stopping. native-case-disposal-isolated.log passed2102 units, all511 GameTests and build. No client/install/push. Robot/microcontroller/drone lifecycle, external filesystem ordering, full-world restart and remaining full-port requirements stay open.

## Microcontroller lifecycle and queued signals: 2026-09-22

MicrocontrollerBlockEntity now snapshots before disconnect/stop, preserves save-after-unload and same-instance reattachment. Three NativeMicrocontrollerPersistenceGameTests assert VM closure plus retained local state, tmp identity and binary open-file position. With disposal disabled, all three failed for a retained VM (native-micro-disposal-verified-red.log).

An intermittent resume hang exposed a shared scheduler bug: after consuming one event, SimpleMachine could sleep with more signals queued. World-time and fallback sleep gates now require signals.isEmpty(), matching upstream. A fixed-clock regression failed with only first received instead of first and second, and verifies sleeping resumes after the queue drains. No timeout increase or extra wakeup signal was used to hide the hang.

native-micro-queue-integrated.log passed2103 units, all514 GameTests and build. No client/install/push. Robot and drone lifecycle, external filesystem ordering, full-world restart and remaining full-port gates are still open.

## Drone disposal and hardware-before-VM restore: 2026-09-22

DroneEntity snapshots additional save data before disconnect/stop and reuses it if serialization follows removal. readAdditionalSaveData now rebuilds CPU/components before restoring machine state. Two NativeDronePersistenceGameTests spawn real entities, preserve local731/tmp identity/status, remove the old entity, assert VM closure and resume a freshly loaded entity without reboot. Save-before-removal and save-after-removal both failed retained-VM checks before correction. Entity removal and replacement are separated by two ticks for UUID registration cleanup.

native-drone-disposal-integrated.log passed2103 units, all516 GameTests and build. No client/install/push. Robot lifecycle, external filesystem ordering/full-world restart, platform/client coverage and remaining full-port requirements are still open.

## Robot disposal versus movement: 2026-09-22

RobotBlockEntity snapshots complete hardware/inventory/runtime state before disposal and restores same-instance onLoad. The existing relocating guard keeps the live VM intact during movement. Three NativeRobotPersistenceGameTests first failed retained-VM assertions and now verify disposal plus local731/tmp identity/open binary file offset/cargo3 continuation with save-before-remove, unload-before-save and same-instance reload.

The existing two-step physical movement fixture now has a native variant, retaining the same machine/architecture, component addresses, screen and viewport, cargo, open terminal/menu and correct new power adjacency. Both LuaJ and native movement variants pass. native-robot-disposal-integrated.log passed2103 units, all520 GameTests and build. No client/install/push. Basic host disposal is covered; full-world/external filesystem ordering, architecture selection/registration, wider APIs/platform/client checks and full-port requirements remain open.

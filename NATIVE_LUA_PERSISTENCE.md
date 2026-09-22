# Native Lua persistence feasibility

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

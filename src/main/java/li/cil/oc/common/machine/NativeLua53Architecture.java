package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Architecture;
import java.util.function.LongSupplier;

@Architecture.Name("Lua 5.3")
public final class NativeLua53Architecture extends NativeLuaArchitecture {
    public NativeLua53Architecture() { super(); }
    NativeLua53Architecture(LongSupplier nanoTime) { super(nanoTime); }
    @Override protected NativeLuaState.Version luaVersion() { return NativeLuaState.Version.LUA53; }
}

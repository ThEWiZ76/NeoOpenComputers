package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Architecture;
import java.util.function.LongSupplier;

@Architecture.Name("Lua 5.4")
public final class NativeLua54Architecture extends NativeLuaArchitecture {
    public NativeLua54Architecture() { super(); }
    NativeLua54Architecture(LongSupplier nanoTime) { super(nanoTime); }
    @Override protected NativeLuaState.Version luaVersion() { return NativeLuaState.Version.LUA54; }
}

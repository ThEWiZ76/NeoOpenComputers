package li.cil.oc.common.machine;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Value;
import li.cil.oc.common.OpenComputersApi;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Host-only OC Value operations used by the kernel's persistent userdata proxies. */
final class NativeLuaUserdata {
    private NativeLuaUserdata() {}

    static void install(final LuaState lua, final Machine machine) {
        lua.newTable();
        function(lua, "save", state -> {
            final Value value = value(state);
            final CompoundTag data = new CompoundTag();
            value.save(data);
            try {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                NbtIo.write(data, new DataOutputStream(bytes));
                state.pushString(value.getClass().getName());
                state.pushByteArray(bytes.toByteArray());
                return 2;
            } catch (IOException failure) { throw new IllegalStateException("Cannot save OC value", failure); }
        });
        function(lua, "load", state -> {
            try {
                final String className = state.checkString(1);
                final Class<?> type = Class.forName(className, false, Value.class.getClassLoader());
                if (!Value.class.isAssignableFrom(type)) throw new IllegalArgumentException(className + " is not an OC Value");
                final var constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                final Value value = (Value) constructor.newInstance();
                final byte[] bytes = state.checkByteArray(2);
                value.load(NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)), NbtAccounter.unlimitedHeap()));
                state.pushJavaObjectRaw(value);
                return 1;
            } catch (ReflectiveOperationException | IOException failure) {
                throw new IllegalStateException("Cannot restore OC value", failure);
            }
        });
        function(lua, "apply", state -> NativeLuaValues.invoke(state, () -> OpenComputersApi.convert(
            new Object[]{value(state).apply(machine, arguments(state, 2))})));
        function(lua, "unapply", state -> NativeLuaValues.invoke(state, () -> {
            value(state).unapply(machine, arguments(state, 2)); return null;
        }));
        function(lua, "call", state -> NativeLuaValues.invoke(state, () -> OpenComputersApi.convert(
            value(state).call(machine, arguments(state, 2)))));
        function(lua, "dispose", state -> {
            final Value value = value(state);
            try { value.dispose(machine); }
            catch (Throwable failure) { NeoOpenComputers.LOGGER.warn("Error disposing OC value of type {}", value.getClass().getName(), failure); }
            return 0;
        });
        function(lua, "methods", state -> {
            final Map<String, Boolean> methods = new LinkedHashMap<>();
            machine.methods(value(state)).forEach((name, callback) -> methods.put(name, callback.direct()));
            NativeLuaValues.push(state, methods); return 1;
        });
        function(lua, "invoke", state -> NativeLuaValues.invoke(state, () ->
            machine.invoke(value(state), state.checkString(2), NativeLuaValues.arguments(state, 3))));
        function(lua, "doc", state -> {
            final var callback = machine.methods(value(state)).get(state.checkString(2));
            if (callback == null) {
                state.pushNil(); state.pushString("no such method"); return 2;
            }
            NativeLuaValues.push(state, callback.doc().isEmpty() ? null : callback.doc()); return 1;
        });
        lua.setGlobal("userdata");
    }

    private static Value value(final LuaState lua) {
        if (lua.isJavaObjectRaw(1) && lua.toJavaObjectRaw(1) instanceof Value value) return value;
        throw new IllegalArgumentException("bad argument #1 (OC Value expected)");
    }

    private static LuaArguments arguments(final LuaState lua, final int start) {
        return new LuaArguments(NativeLuaValues.arguments(lua, start));
    }

    private static void function(final LuaState lua, final String name, final JavaFunction function) {
        lua.pushJavaFunction(function); lua.setField(-2, name);
    }
}

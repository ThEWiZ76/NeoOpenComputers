package li.cil.oc.common.machine;

import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.nbt.CompoundTag;

/** Eris object graph persistence, with native functions rebound to each fresh VM. */
public final class NativeLuaPersistence implements AutoCloseable {
    private static final int FORMAT = 1;
    private final NativeLuaState owner;
    private final LuaState lua;
    private final int permanent;
    private final int inverse;
    private boolean closed;
    private String persistenceKey;

    /** Capture bindings after installing host APIs, before executing any user program. */
    public NativeLuaPersistence(final NativeLuaState owner) {
        this(owner, "__oc_persist_" + java.util.UUID.randomUUID());
    }

    NativeLuaPersistence(final NativeLuaState owner, final String persistenceKey) {
        this.persistenceKey = persistenceKey;
        this.owner = owner;
        lua = owner.state();
        final int top = lua.getTop();
        try {
            lua.load("""
                local debug = ...
                local permanent, inverse, seen = {}, {}, {}
                local function visit(path, value)
                    local kind = type(value)
                    if kind ~= "function" and kind ~= "table" then return end
                    if seen[value] then return end
                    seen[value] = true
                    if kind == "function" then
                        if debug.getinfo(value, "S").what == "C" then
                            permanent[value], inverse[path] = path, value
                        else
                            local index = 1
                            while true do
                                local name, upvalue = debug.getupvalue(value, index)
                                if name == nil then break end
                                visit(path .. "/upvalue:" .. index, upvalue)
                                index = index + 1
                            end
                        end
                    else
                        local keys = {}
                        for key in pairs(value) do
                            local keyType = type(key)
                            assert(keyType == "string" or keyType == "number" or keyType == "boolean",
                                "unsupported host binding key at " .. path)
                            keys[#keys + 1] = key
                        end
                        local function keyName(key) return type(key) .. ":" .. tostring(key) end
                        table.sort(keys, function(a, b) return keyName(a) < keyName(b) end)
                        for _, key in ipairs(keys) do
                            local name = keyName(key)
                            visit(path .. "/" .. #name .. ":" .. name, rawget(value, key))
                        end
                        visit(path .. "/metatable", debug.getmetatable(value))
                    end
                end
                visit("globals", _G)
                return permanent, inverse
                """, "=native-persistence-bindings");
            owner.pushHostLibrary("debug");
            lua.call(1, 2);
            inverse = lua.ref(lua.getRegistryIndex());
            permanent = lua.ref(lua.getRegistryIndex());
        } finally {
            lua.setTop(top);
        }
    }

    /** Save the value at index without changing the caller's stack or resuming its coroutine. */
    public CompoundTag save(final int index) {
        ensureOpen();
        final int absolute = lua.absIndex(index);
        final int top = lua.getTop();
        try {
            configure(persistenceKey);
            pushErisFunction("persist");
            lua.rawGet(lua.getRegistryIndex(), permanent);
            lua.pushValue(absolute);
            lua.call(2, 1);
            final CompoundTag tag = new CompoundTag();
            tag.putInt("format", FORMAT);
            tag.putString("lua", owner.version().name());
            tag.putString("persistKey", persistenceKey);
            tag.putByteArray("data", lua.toByteArray(-1));
            return tag;
        } finally {
            lua.setTop(top);
        }
    }

    /** Push a restored value. Reject incompatible snapshots before entering native code. */
    public void restore(final CompoundTag tag) {
        ensureOpen();
        if (tag.getInt("format") != FORMAT || !owner.version().name().equals(tag.getString("lua"))) {
            throw new IllegalArgumentException("Incompatible native Lua snapshot");
        }
        final String key = tag.getString("persistKey");
        if (!key.startsWith("__oc_persist_") || key.length() > 128) {
            throw new IllegalArgumentException("Invalid native Lua persistence key");
        }
        final byte[] data = tag.getByteArray("data");
        if (data.length == 0) throw new IllegalArgumentException("Empty native Lua snapshot");
        final int top = lua.getTop();
        // Eris temporarily holds incomplete closures/prototypes outside Lua roots.
        // GC during reconstruction can free them (fnuecke/eris issue #27).
        final boolean gcRunning = lua.gc(LuaState.GcAction.ISRUNNING, 0) != 0;
        lua.gc(LuaState.GcAction.STOP, 0);
        try {
            configure(key);
            pushErisFunction("unpersist");
            lua.rawGet(lua.getRegistryIndex(), inverse);
            lua.pushByteArray(data);
            lua.call(2, 1);
            persistenceKey = key;
        } catch (RuntimeException | Error failure) {
            lua.setTop(top);
            throw failure;
        } finally {
            if (gcRunning) lua.gc(LuaState.GcAction.RESTART, 0);
        }
    }

    String persistenceKey() { return persistenceKey; }

    private void configure(final String key) {
        // Do not execute a program-supplied __persist metamethod during a world save.
        pushErisFunction("settings");
        lua.pushString("spkey");
        lua.pushString(key);
        lua.call(2, 0);
    }

    private void pushErisFunction(final String name) {
        owner.pushHostLibrary("eris");
        lua.getField(-1, name);
        lua.remove(-2);
    }

    private void ensureOpen() {
        if (closed || !lua.isOpen()) throw new IllegalStateException("Native Lua persistence is closed");
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (lua.isOpen()) {
            lua.unref(lua.getRegistryIndex(), permanent);
            lua.unref(lua.getRegistryIndex(), inverse);
        }
    }
}

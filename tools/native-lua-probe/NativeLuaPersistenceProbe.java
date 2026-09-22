import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaStateFiveThree;
import li.cil.repack.com.naef.jnlua.LuaStateFiveFour;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone Java 21 feasibility probe; not registered as a Minecraft architecture. */
public final class NativeLuaPersistenceProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("<native-library> <52|53|54> <save|restore> <snapshot>");
        System.load(Path.of(args[0]).toAbsolutePath().toString());
        LuaState lua = switch (args[1]) {
            case "52" -> new LuaState();
            case "53" -> new LuaStateFiveThree();
            case "54" -> new LuaStateFiveFour();
            default -> throw new IllegalArgumentException("Unsupported Lua version");
        };
        try {
            for (var library : new LuaState.Library[]{LuaState.Library.BASE, LuaState.Library.COROUTINE,
                    LuaState.Library.TABLE, LuaState.Library.STRING, LuaState.Library.ERIS}) {
                lua.openLib(library);
                lua.pop(1);
            }
            lua.pushJavaFunction(state -> {
                state.pushInteger(state.checkInteger(1) + 5);
                return 1;
            });
            lua.setGlobal("host");
            run(lua, """
                perms = {[_G] = "globals", [coroutine.yield] = "yield", [host] = "host"}
                uperms = {globals = _G, yield = coroutine.yield, host = host}
                """, 0);
            Path snapshot = Path.of(args[3]);
            if (args[2].equals("save")) {
                run(lua, """
                    thread = coroutine.create(function()
                        local state = {value = 731}
                        state.self = state
                        local function advance() state.value = state.value + 1; return state.value end
                        local child = coroutine.create(function()
                            local count = 40
                            coroutine.yield("child_wait")
                            return count + 2
                        end)
                        local ok, value = coroutine.resume(child)
                        assert(ok and value == "child_wait")
                        local callback = host
                        local command = coroutine.yield("ready")
                        assert(command == "resume")
                        local childOk, childValue = coroutine.resume(child)
                        assert(childOk)
                        return advance(), childValue, state.self == state, callback(7)
                    end)
                    local ok, value = coroutine.resume(thread)
                    assert(ok and value == "ready")
                    return eris.persist(perms, thread)
                    """, 1);
                byte[] data = lua.toByteArray(-1);
                if (data == null || data.length == 0) throw new AssertionError("Empty persistence data");
                Files.write(snapshot, data);
                System.out.println("SAVED Lua " + args[1] + " bytes=" + data.length);
            } else if (args[2].equals("restore")) {
                lua.pushByteArray(Files.readAllBytes(snapshot));
                lua.setGlobal("snapshot");
                run(lua, """
                    local thread = eris.unpersist(uperms, snapshot)
                    assert(coroutine.status(thread) == "suspended")
                    local ok, count, childCount, cycle, hostResult = coroutine.resume(thread, "resume")
                    assert(ok, count)
                    assert(count == 732 and childCount == 42 and cycle and hostResult == 12)
                    assert(coroutine.status(thread) == "dead")
                    """, 0);
                System.out.println("RESTORED Lua " + args[1] + " local=732 child=42 cycle=true reboundJavaCallback=12");
            } else throw new IllegalArgumentException("Unknown mode");
        } finally {
            lua.close();
        }
    }

    private static void run(LuaState lua, String source, int results) {
        lua.load(source, "native-persistence-probe");
        lua.call(0, results);
    }
}

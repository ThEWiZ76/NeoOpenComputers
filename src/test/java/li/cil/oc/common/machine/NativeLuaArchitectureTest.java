package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Value;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

final class NativeLuaArchitectureTest {
    @Callback(direct = true) public void get() {}
    @Callback public void write() {}
    @Callback(direct = true) public void value() {}

    @Test
    void reportsInstalledRamWhileEnforcingNativeAllocationLimit() throws Exception {
        exposesNativeUnicodeAndGameTime("""
            assert(computer.totalMemory() == 512 * 1024)
            assert(computer.freeMemory() > 0 and computer.freeMemory() <= computer.totalMemory())
            local ok = pcall(string.rep, 'x', 8 * 1024 * 1024)
            assert(not ok, 'native memory limit was bypassed')
            assert(computer.totalMemory() == 512 * 1024)
            """);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "local s = unicode.char(65, 0x1F600, 0x6C34); assert(unicode.len(s) == 3); assert(unicode.sub(s, 2, 2) == unicode.char(0x1F600)); assert(unicode.reverse(s) == unicode.char(0x6C34, 0x1F600, 65))",
        "assert(unicode.sub('abc', -2, -1) == 'bc'); assert(unicode.sub('abc', 0, 2) == 'ab'); assert(unicode.sub('abc', 99) == ''); assert(unicode.sub('abc', -99, -99) == '')",
        "assert(unicode.charWidth(unicode.char(0x231A)) == 2); assert(not unicode.isWide(unicode.char(0x2E9A))); assert(unicode.wlen('ab' .. unicode.char(0x6C34)) == 4); assert(unicode.wtrunc('ab' .. unicode.char(0x6C34) .. 'c', 4) == 'ab')",
        "assert(not pcall(unicode.charWidth, '')); assert(not pcall(unicode.isWide, '')); assert(not pcall(unicode.wtrunc, 'abc', 10)); assert(not pcall(unicode.char, 0x110000))",
        "assert(os.clock() == 0); assert(os.time() == 21600); assert(os.date('%F %T', 86400) == '1970-01-02 00:00:00'); assert(os.date('!%F %T', 86400) == os.date('%F %T', 86400)); assert(os.difftime(8, 3) == 5)",
        "local d = os.date('!*t', 86400); assert(d.year == 1970 and d.month == 1 and d.day == 2 and d.hour == 0 and d.wday == 6 and d.yday == 2); assert(os.time(d) == 86400)",
        "assert(os.time({year=1970,month=1,day=1}) == 43200); assert(os.time({year=1970,month=13,day=1,hour=0}) == 31536000); assert(not pcall(os.time, {})); assert(not pcall(os.time, 5))",
        "assert(os.date(false, 86400) == '02/01/70 00:00:00'); assert(os.date('%T', 'soon') == '06:00:00'); assert(os.date('%Q%z%U%W', 86400) == '')"
    })
    void exposesNativeUnicodeAndGameTime(final String program) throws Exception {
        final var architecture = architecture(program + "; computer.shutdown()", new AtomicInteger());
        try {
            assertTrue(architecture.initialize());
            architecture.runThreaded(false);
            final var result = architecture.runThreaded(false);
            assertInstanceOf(ExecutionResult.Shutdown.class, result,
                result instanceof ExecutionResult.Error error ? error.message : "Expected normal shutdown");
        } finally { architecture.close(); }
    }

    @ParameterizedTest
    @ValueSource(strings = {"tr-TR", "en-US"})
    void unicodeCaseMappingUsesServerLocale(final String locale) throws Exception {
        final Locale previous = Locale.getDefault();
        final String program = locale.equals("tr-TR")
            ? "assert(unicode.lower('I') == unicode.char(0x131)); assert(unicode.upper('i') == unicode.char(0x130))"
            : "assert(unicode.lower('I') == 'i'); assert(unicode.upper('i') == 'I')";
        try {
            Locale.setDefault(Locale.forLanguageTag(locale));
            exposesNativeUnicodeAndGameTime(program);
        } finally { Locale.setDefault(previous); }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "while true do end",
        "while true do pcall(function() while true do end end) end",
        "while true do xpcall(function() while true do end end, function() while true do end end) end",
        "local co = coroutine.create(function() while true do end end); local ok, reason = coroutine.resume(co); assert(ok, tostring(reason))"
    })
    void terminatesNonYieldingProgramsWithDeterministicClock(final String program) throws Exception {
        final var architecture = architecture(program, new AtomicInteger());
        try {
            assertTrue(architecture.initialize());
            assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));
            final var error = assertInstanceOf(ExecutionResult.Error.class, architecture.runThreaded(false));
            assertTrue(error.message.contains("too long without yielding"), error.message);
        } finally { architecture.close(); }
    }

    @Test
    void sandboxHasNoHostLibrariesOrBinaryLoader() throws Exception {
        final var architecture = architecture("""
            assert(io == nil and package == nil and java == nil and eris == nil)
            assert(debug.getregistry == nil and debug.sethook == nil and debug.setupvalue == nil)
            local secret = {value = 42}
            local function closure() return secret end
            assert(select("#", debug.getupvalue(closure, 1)) == 1)
            assert(debug.getinfo(closure, "f").func == nil)
            assert(dofile == nil and loadfile == nil)
            assert(load(string.dump(function() return 42 end)) == nil)
            computer.shutdown()
            """, new AtomicInteger());
        try {
            assertTrue(architecture.initialize());
            architecture.runThreaded(false);
            final var result = architecture.runThreaded(false);
            assertInstanceOf(ExecutionResult.Shutdown.class, result,
                result instanceof ExecutionResult.Error error ? error.message : "Expected normal shutdown");
        } finally { architecture.close(); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restoresPendingWorldCallbackWithoutRepeatingIt(final boolean afterCallback) throws Exception {
        final String program = """
            local retained = {value = 731}
            retained.self = retained
            local char, date = unicode.char, os.date
            local text = char(0x1F600, 0x6C34)
            local result = component.invoke("eeprom", "write")
            assert(result == 42 and retained.value == 731 and retained.self == retained)
            assert(("").find == string.find, "string metatable was not restored")
            assert(text == char(0x1F600, 0x6C34) and unicode.len(text) == 2)
            assert(date('%F', 86400) == '1970-01-02')
            computer.shutdown()
            """;
        final AtomicInteger calls = new AtomicInteger();
        final CompoundTag saved = new CompoundTag();
        final var original = architecture(program, calls);
        try {
            assertTrue(original.initialize());
            original.runThreaded(false);
            assertInstanceOf(ExecutionResult.Sleep.class, original.runThreaded(false));
            assertTrue(original.hasPendingSynchronizedCall());
            if (afterCallback) original.runSynchronized();
            original.save(saved);
            assertEquals(afterCallback ? 1 : 0, calls.get(), "Saving must not invoke the callback");
        } finally { original.close(); }
        final var restored = architecture(program, calls);
        try {
            restored.load(saved);
            assertEquals(!afterCallback, restored.hasPendingSynchronizedCall());
            assertEquals(afterCallback, restored.hasSynchronizedReturn());
            if (!afterCallback) restored.runSynchronized();
            final var result = restored.runThreaded(true);
            assertInstanceOf(ExecutionResult.Shutdown.class, result,
                result instanceof ExecutionResult.Error error ? error.message : "Expected normal shutdown");
            assertEquals(1, calls.get());
        } finally { restored.close(); }
    }

    private static NativeLuaArchitecture architecture(final String program, final AtomicInteger writes) throws Exception {
        return architecture(program, writes, null);
    }

    static NativeLuaArchitecture architecture(final String program, final AtomicInteger writes, final Value value) throws Exception {
        final AtomicLong clock = new AtomicLong();
        final var architecture = new NativeLuaArchitecture(() -> clock.getAndAdd(100_000_000L));
        final var memory = NativeLuaArchitecture.class.getDeclaredField("memoryBytes");
        memory.setAccessible(true);
        memory.setInt(architecture, 512 * 1024);
        final Map<String, Callback> callbacks = Map.of(
            "get", NativeLuaArchitectureTest.class.getMethod("get").getAnnotation(Callback.class),
            "value", NativeLuaArchitectureTest.class.getMethod("value").getAnnotation(Callback.class),
            "write", NativeLuaArchitectureTest.class.getMethod("write").getAnnotation(Callback.class));
        architecture.bind((Machine) Proxy.newProxyInstance(Machine.class.getClassLoader(), new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> Map.of("eeprom", "eeprom");
                case "methods" -> {
                    if (args[0] instanceof Value object) {
                        final Map<String, Callback> methods = new java.util.LinkedHashMap<>();
                        for (final var callback : object.getClass().getMethods()) {
                            if (callback.isAnnotationPresent(Callback.class)) methods.put(callback.getName(), callback.getAnnotation(Callback.class));
                        }
                        yield methods;
                    }
                    yield callbacks;
                }
                case "popSignal" -> null;
                case "upTime", "cpuTime" -> 0D;
                case "worldTime" -> 0L;
                case "invoke" -> {
                    if (args[0] instanceof Value object) {
                        yield object.getClass().getMethod((String) args[1], li.cil.oc.api.machine.Context.class,
                            li.cil.oc.api.machine.Arguments.class).invoke(object, proxy, new LuaArguments((Object[]) args[2]));
                    }
                    if (args[1].equals("get")) yield new Object[]{program};
                    if (args[1].equals("value")) yield new Object[]{value, value};
                    if (args[1].equals("write")) { writes.incrementAndGet(); yield new Object[]{42}; }
                    throw new AssertionError("Unexpected callback " + args[1]);
                }
                case "crash" -> throw new AssertionError(args[0]);
                default -> throw new AssertionError("Unexpected machine call " + method.getName());
            }));
        return architecture;
    }
}

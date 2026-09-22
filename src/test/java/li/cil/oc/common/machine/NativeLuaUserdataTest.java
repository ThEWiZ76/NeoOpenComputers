package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class NativeLuaUserdataTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void persistsOwnedValuesAndAliasesWithoutRepeatingWorldActions(final boolean afterCallback) throws Exception {
        final String program = """
            local value, alias = component.invoke('eeprom', 'value')
            assert(value == alias and value.type == 'userdata')
            value.counter = 40
            assert(value.counter == 40)
            assert(value(2) == 42)
            assert(value.count() == 42 and tostring(value.count) == 'Current counter')
            local callback = value.count
            local retained = {value, alias, child = {value}}
            assert(component.invoke('eeprom', 'write') == 42)
            assert(retained[1] == retained[2] and retained.child[1] == retained[1])
            assert(callback() == 42 and value.counter == 42)
            assert(value(1) == 43)
            computer.shutdown()
            """;
        final AtomicInteger calls = new AtomicInteger();
        final CounterValue originalValue = new CounterValue();
        final CompoundTag saved = new CompoundTag();
        final var original = NativeLuaArchitectureTest.architecture(program, calls, originalValue);
        try {
            assertTrue(original.initialize());
            original.runThreaded(false);
            final var result = original.runThreaded(false);
            assertInstanceOf(ExecutionResult.Sleep.class, result,
                result instanceof ExecutionResult.Error error ? error.message : "Expected checkpoint");
            assertTrue(original.hasPendingSynchronizedCall());
            assertEquals(42, originalValue.counter);
            if (afterCallback) original.runSynchronized();
            original.save(saved);
        } finally { original.close(); }
        final var restored = NativeLuaArchitectureTest.architecture(program, calls, null);
        try {
            restored.load(saved);
            if (!afterCallback) restored.runSynchronized();
            final var result = restored.runThreaded(true);
            assertInstanceOf(ExecutionResult.Shutdown.class, result,
                result instanceof ExecutionResult.Error error ? error.message : "Expected normal shutdown");
            assertEquals(1, calls.get());
            assertEquals(42, originalValue.counter, "Restored program must own a new Java value");
        } finally { restored.close(); }
    }

    public static final class CounterValue extends AbstractValue {
        int counter;
        public CounterValue() {}

        @Override public Object apply(final Context context, final Arguments arguments) {
            return arguments.checkString(0).equals("counter") ? counter : null;
        }
        @Override public void unapply(final Context context, final Arguments arguments) {
            if (arguments.checkString(0).equals("counter")) counter = arguments.checkInteger(1);
        }
        @Override public Object[] call(final Context context, final Arguments arguments) {
            counter += arguments.checkInteger(0);
            return new Object[]{counter};
        }
        @Callback(direct = true, doc = "Current counter")
        public Object[] count(final Context context, final Arguments arguments) { return new Object[]{counter}; }
        @Override public void save(final CompoundTag nbt) { nbt.putInt("counter", counter); }
        @Override public void load(final CompoundTag nbt) { counter = nbt.getInt("counter"); }
    }
}

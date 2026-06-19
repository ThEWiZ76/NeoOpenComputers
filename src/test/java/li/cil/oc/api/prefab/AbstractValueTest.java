package li.cil.oc.api.prefab;

import li.cil.oc.api.machine.EmptyArguments;
import li.cil.oc.api.machine.TestContext;
import li.cil.oc.api.machine.Value;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AbstractValueTest {
    @Test
    void providesNoopValueDefaults() {
        AbstractValue value = new AbstractValue();
        TestContext context = new TestContext();
        EmptyArguments arguments = new EmptyArguments();

        assertInstanceOf(Value.class, value);
        assertNull(value.apply(context, arguments));
        assertDoesNotThrow(() -> value.unapply(context, arguments));
        assertThrows(RuntimeException.class, () -> value.call(context, arguments));
        assertDoesNotThrow(() -> value.dispose(context));
        assertDoesNotThrow(() -> value.load(new CompoundTag()));
        assertDoesNotThrow(() -> value.save(new CompoundTag()));
    }
}

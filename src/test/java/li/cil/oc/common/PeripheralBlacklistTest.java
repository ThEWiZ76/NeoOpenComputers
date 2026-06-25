package li.cil.oc.common;

import li.cil.oc.api.IMC;
import net.neoforged.fml.InterModComms;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PeripheralBlacklistTest {
    @Test
    void registryMatchesUpstreamPeripheralBlacklistShape() throws Exception {
        Class<?> registry = Class.forName("li.cil.oc.common.PeripheralBlacklist");

        assertEquals(void.class, registry.getMethod("add", String.class).getReturnType());
        assertEquals(boolean.class, registry.getMethod("isBlacklisted", String.class).getReturnType());
        assertEquals(boolean.class, registry.getMethod("isBlacklisted", Class.class).getReturnType());
    }

    @Test
    void registryMatchesClassNamesAndAssignablePeripheralTypes() throws Exception {
        Class<?> registry = Class.forName("li.cil.oc.common.PeripheralBlacklist");
        Method add = registry.getMethod("add", String.class);
        Method isBlacklistedClass = registry.getMethod("isBlacklisted", Class.class);

        add.invoke(null, BlockedPeripheral.class.getName());

        assertTrue((boolean) isBlacklistedClass.invoke(null, BlockedPeripheral.class));
        assertTrue((boolean) isBlacklistedClass.invoke(null, BlockedChildPeripheral.class));
        assertFalse((boolean) isBlacklistedClass.invoke(null, AllowedPeripheral.class));
    }

    @Test
    void imcProcessorRegistersPeripheralBlacklistClassNames() throws Exception {
        Class<?> registry = Class.forName("li.cil.oc.common.PeripheralBlacklist");
        Class<?> processor = Class.forName("li.cil.oc.common.PeripheralBlacklistImc");
        InterModComms.IMCMessage message = new InterModComms.IMCMessage(
            "addon",
            "neoopencomputers",
            IMC.BLACKLIST_PERIPHERAL,
            () -> ImcBlockedPeripheral.class.getName()
        );

        processor.getMethod("process", Stream.class).invoke(null, Stream.of(message));

        Method isBlacklisted = registry.getMethod("isBlacklisted", String.class);
        assertTrue((boolean) isBlacklisted.invoke(null, ImcBlockedPeripheral.class.getName()));
    }

    static class BlockedPeripheral {
    }

    static final class BlockedChildPeripheral extends BlockedPeripheral {
    }

    static final class AllowedPeripheral {
    }

    static final class ImcBlockedPeripheral {
    }
}

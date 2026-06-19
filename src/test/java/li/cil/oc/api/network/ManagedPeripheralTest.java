package li.cil.oc.api.network;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.EmptyArguments;
import li.cil.oc.api.machine.TestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class ManagedPeripheralTest {
    @Test
    void exposesPeripheralMethodsAndInvocation() throws Exception {
        ManagedPeripheral peripheral = new TestManagedPeripheral();

        assertArrayEquals(new String[]{"ping"}, peripheral.methods());
        assertArrayEquals(new Object[]{"pong"}, peripheral.invoke("ping", new TestContext(), new EmptyArguments()));
    }

    private static final class TestManagedPeripheral implements ManagedPeripheral {
        @Override
        public String[] methods() {
            return new String[]{"ping"};
        }

        @Override
        public Object[] invoke(final String method, final li.cil.oc.api.machine.Context context, final Arguments args) throws Exception {
            if ("ping".equals(method)) {
                return new Object[]{"pong"};
            }
            throw new NoSuchMethodException(method);
        }
    }
}

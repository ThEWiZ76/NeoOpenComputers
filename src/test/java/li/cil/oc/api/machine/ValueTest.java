package li.cil.oc.api.machine;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ValueTest {
    @Test
    void valueExposesLuaStyleOperationsAndPersistence() {
        var value = new TestValue();
        Context context = new TestContext();
        Arguments arguments = new EmptyArguments();
        var tag = new CompoundTag();

        value.unapply(context, arguments);
        value.dispose(context);
        value.save(tag);
        value.load(tag);

        assertEquals("indexed", value.apply(context, arguments));
        assertArrayEquals(new Object[]{"called"}, value.call(context, arguments));
        assertTrue(value.unapplied);
        assertSame(context, value.disposedContext);
        assertTrue(value.loaded);
    }

    private static final class TestValue implements Value {
        private boolean unapplied;
        private boolean loaded;
        private Context disposedContext;

        @Override
        public Object apply(final Context context, final Arguments arguments) {
            return "indexed";
        }

        @Override
        public void unapply(final Context context, final Arguments arguments) {
            unapplied = true;
        }

        @Override
        public Object[] call(final Context context, final Arguments arguments) {
            return new Object[]{"called"};
        }

        @Override
        public void dispose(final Context context) {
            disposedContext = context;
        }

        @Override
        public void load(final CompoundTag nbt) {
            loaded = true;
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putBoolean("saved", true);
        }
    }
}

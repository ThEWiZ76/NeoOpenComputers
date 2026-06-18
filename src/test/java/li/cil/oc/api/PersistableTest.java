package li.cil.oc.api;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class PersistableTest {
    @Test
    void usesModernMinecraftCompoundTagForPersistence() throws NoSuchMethodException {
        Method load = Persistable.class.getMethod("load", CompoundTag.class);
        Method save = Persistable.class.getMethod("save", CompoundTag.class);

        assertArrayEquals(new Class<?>[]{CompoundTag.class}, load.getParameterTypes());
        assertArrayEquals(new Class<?>[]{CompoundTag.class}, save.getParameterTypes());
    }
}

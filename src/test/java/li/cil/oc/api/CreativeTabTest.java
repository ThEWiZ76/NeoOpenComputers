package li.cil.oc.api;

import net.minecraft.world.item.CreativeModeTab;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class CreativeTabTest {
    @Test
    void exposesMutableCreativeModeTabReference() throws NoSuchFieldException {
        Field instance = CreativeTab.class.getField("instance");

        assertEquals(CreativeModeTab.class, instance.getType());
        assertNull(CreativeTab.instance);
    }
}

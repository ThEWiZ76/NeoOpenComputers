package li.cil.oc.common;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkProvidersTest {
    @Test
    void registryMatchesUpstreamInkProviderShape() throws Exception {
        Class<?> registry = Class.forName("li.cil.oc.common.InkProviders");

        assertEquals(void.class, registry.getMethod("add", Method.class).getReturnType());
        assertEquals(int.class, registry.getMethod("inkValue", ItemStack.class).getReturnType());
    }

    @Test
    void defaultInkProvidersExposeUpstreamConstants() throws Exception {
        Class<?> defaults = Class.forName("li.cil.oc.common.ModInkProviders");
        Field cartridgeValue = defaults.getField("UPSTREAM_INK_CARTRIDGE_VALUE");
        Field dyeValue = defaults.getField("UPSTREAM_DYE_VALUE");

        assertEquals(50000, cartridgeValue.getInt(null));
        assertEquals(5000, dyeValue.getInt(null));
    }

    @Test
    void imcProcessorRegistersInkProviderCallbacks() throws Exception {
        Class<?> processor = Class.forName("li.cil.oc.common.InkProviderImc");

        assertTrue(processor.getMethod("process", java.util.stream.Stream.class).getReturnType() == void.class);
    }
}

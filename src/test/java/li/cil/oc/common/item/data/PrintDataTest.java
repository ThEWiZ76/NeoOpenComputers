package li.cil.oc.common.item.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrintDataTest {
    @Test
    void shapeRoundTripsThroughUpstreamCompactNbt() throws Exception {
        Class<?> printData = Class.forName("li.cil.oc.common.item.data.PrintData");
        Object shape = shape(printData, new AABB(0.125D, 0.25D, 0.375D, 0.5D, 0.625D, 0.75D), "minecraft:block/stone", 0x336699);

        CompoundTag tag = (CompoundTag) printData.getMethod("shapeToNbt", shapeClass(printData)).invoke(null, shape);
        Object decoded = printData.getMethod("nbtToShape", CompoundTag.class).invoke(null, tag);

        assertBoundsEquals(bounds(shape), bounds(decoded));
        assertEquals("minecraft:block/stone", texture(decoded));
        assertEquals(0x336699, tint(decoded));
    }

    @Test
    void computeApproximateOpacityMatchesUpstreamSampling() throws Exception {
        Class<?> printData = Class.forName("li.cil.oc.common.item.data.PrintData");
        Object shape = shape(printData, new AABB(0D, 0D, 0D, 0.25D, 0.25D, 0.25D), "minecraft:block/stone", null);

        float opacity = (float) printData.getMethod("computeApproximateOpacity", Iterable.class).invoke(null, List.of(shape));

        assertEquals(1F / 64F, opacity, 0.0001F);
    }

    @Test
    void computeCostsMatchesUpstreamVolumeSurfaceRules() throws Exception {
        Class<?> printData = Class.forName("li.cil.oc.common.item.data.PrintData");
        Object data = printData.getConstructor().newInstance();
        Object shape = shape(printData, new AABB(0D, 0D, 0D, 1D, 1D, 1D), "minecraft:block/stone", null);

        printData.getMethod("addStateOff", shapeClass(printData)).invoke(data, shape);
        Object costs = ((Optional<?>) printData.getMethod("computeCosts", printData).invoke(null, data)).orElseThrow();

        assertEquals(2048, material(costs));
        assertEquals(256, ink(costs));
    }

    @Test
    void computeCostsAddsCustomRedstoneAndNoclipMultiplierLikeUpstream() throws Exception {
        Class<?> printData = Class.forName("li.cil.oc.common.item.data.PrintData");
        Object data = printData.getConstructor().newInstance();
        Object shape = shape(printData, new AABB(0D, 0D, 0D, 1D, 1D, 1D), "minecraft:block/stone", null);

        printData.getMethod("addStateOff", shapeClass(printData)).invoke(data, shape);
        printData.getMethod("setRedstoneLevel", int.class).invoke(data, 7);
        printData.getMethod("setNoclipOff", boolean.class).invoke(data, true);
        Object costs = ((Optional<?>) printData.getMethod("computeCosts", printData).invoke(null, data)).orElseThrow();

        assertEquals(4696, material(costs));
        assertEquals(256, ink(costs));
    }

    @Test
    void inkProviderBridgeMatchesUpstreamPrintDataApi() throws Exception {
        Class<?> printData = Class.forName("li.cil.oc.common.item.data.PrintData");

        printData.getMethod("addInkProvider", Method.class).invoke(null, PrintDataTest.class.getMethod("inkValue", ItemStack.class));

        assertEquals(0, printData.getMethod("inkValue", ItemStack.class).invoke(null, new Object[]{null}));
    }

    public static int inkValue(final ItemStack stack) {
        return stack == null ? 12345 : 0;
    }

    private static Class<?> shapeClass(final Class<?> printData) throws ClassNotFoundException {
        return Class.forName(printData.getName() + "$Shape");
    }

    private static Object shape(final Class<?> printData, final AABB bounds, final String texture, final Integer tint) throws Exception {
        Constructor<?> constructor = shapeClass(printData).getConstructor(AABB.class, String.class, Integer.class);
        return constructor.newInstance(bounds, texture, tint);
    }

    private static AABB bounds(final Object shape) throws Exception {
        return (AABB) shape.getClass().getMethod("bounds").invoke(shape);
    }

    private static String texture(final Object shape) throws Exception {
        return (String) shape.getClass().getMethod("texture").invoke(shape);
    }

    private static Integer tint(final Object shape) throws Exception {
        return (Integer) shape.getClass().getMethod("tint").invoke(shape);
    }

    private static int material(final Object costs) throws Exception {
        return (int) costs.getClass().getMethod("material").invoke(costs);
    }

    private static int ink(final Object costs) throws Exception {
        return (int) costs.getClass().getMethod("ink").invoke(costs);
    }

    private static void assertBoundsEquals(final AABB expected, final AABB actual) {
        assertEquals(expected.minX, actual.minX, 0.0001D);
        assertEquals(expected.minY, actual.minY, 0.0001D);
        assertEquals(expected.minZ, actual.minZ, 0.0001D);
        assertEquals(expected.maxX, actual.maxX, 0.0001D);
        assertEquals(expected.maxY, actual.maxY, 0.0001D);
        assertEquals(expected.maxZ, actual.maxZ, 0.0001D);
        assertTrue(actual.maxX > actual.minX);
    }
}

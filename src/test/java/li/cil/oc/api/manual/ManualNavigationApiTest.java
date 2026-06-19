package li.cil.oc.api.manual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualNavigationApiTest {
    @Test
    void pathProviderUsesModernItemStackLevelAndBlockPos() throws NoSuchMethodException {
        Method itemPath = PathProvider.class.getMethod("pathFor", ItemStack.class);
        Method blockPath = PathProvider.class.getMethod("pathFor", Level.class, BlockPos.class);
        PathProvider provider = new TestPathProvider();

        assertArrayEquals(new Class<?>[]{ItemStack.class}, itemPath.getParameterTypes());
        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class}, blockPath.getParameterTypes());
        assertEquals("/item/%LANGUAGE%/manual.md", provider.pathFor((ItemStack) null));
        assertEquals("/block/%LANGUAGE%/manual.md", provider.pathFor(null, BlockPos.ZERO));
    }

    @Test
    void tabIconRendererCanRender() {
        TestTabIconRenderer renderer = new TestTabIconRenderer();

        renderer.render();

        assertTrue(renderer.rendered);
    }

    private static final class TestPathProvider implements PathProvider {
        @Override
        public String pathFor(final ItemStack stack) {
            return "/item/%LANGUAGE%/manual.md";
        }

        @Override
        public String pathFor(final Level world, final BlockPos pos) {
            return "/block/%LANGUAGE%/manual.md";
        }
    }

    private static final class TestTabIconRenderer implements TabIconRenderer {
        private boolean rendered;

        @Override
        public void render() {
            rendered = true;
        }
    }
}

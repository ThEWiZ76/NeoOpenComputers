package li.cil.oc.api.internal;

import li.cil.oc.api.Persistable;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextBufferTest {
    @Test
    void textBufferIsManagedPersistableEnvironment() {
        assertTrue(ManagedEnvironment.class.isAssignableFrom(TextBuffer.class));
        assertTrue(Persistable.class.isAssignableFrom(TextBuffer.class));
    }

    @Test
    void exposesResolutionColorAndRawBufferOperations() throws NoSuchMethodException {
        assertMethod("setMaximumResolution", void.class, int.class, int.class);
        assertMethod("setResolution", boolean.class, int.class, int.class);
        assertMethod("setViewport", boolean.class, int.class, int.class);
        assertMethod("setMaximumColorDepth", void.class, TextBuffer.ColorDepth.class);
        assertMethod("setColorDepth", boolean.class, TextBuffer.ColorDepth.class);
        assertMethod("setPaletteColor", void.class, int.class, int.class);
        assertMethod("setForegroundColor", void.class, int.class, boolean.class);
        assertMethod("setBackgroundColor", void.class, int.class, boolean.class);
        assertMethod("copy", void.class, int.class, int.class, int.class, int.class, int.class, int.class);
        assertMethod("fill", void.class, int.class, int.class, int.class, int.class, int.class);
        assertMethod("set", void.class, int.class, int.class, String.class, boolean.class);
        assertMethod("rawSetText", void.class, int.class, int.class, int[][].class);
        assertMethod("rawSetForeground", void.class, int.class, int.class, int[][].class);
        assertMethod("rawSetBackground", void.class, int.class, int.class, int[][].class);
    }

    @Test
    void inputMethodsUseModernPlayerType() throws NoSuchMethodException {
        assertMethod("keyDown", void.class, char.class, int.class, Player.class);
        assertMethod("keyUp", void.class, char.class, int.class, Player.class);
        assertMethod("clipboard", void.class, String.class, Player.class);
        assertMethod("mouseDown", void.class, double.class, double.class, int.class, Player.class);
        assertMethod("mouseDrag", void.class, double.class, double.class, int.class, Player.class);
        assertMethod("mouseUp", void.class, double.class, double.class, int.class, Player.class);
        assertMethod("mouseScroll", void.class, double.class, double.class, int.class, Player.class);
    }

    @Test
    void renderMethodsRemainOnContractWithoutClientOnlyAnnotations() throws NoSuchMethodException {
        assertMethod("renderText", boolean.class);
        assertMethod("renderWidth", int.class);
        assertMethod("renderHeight", int.class);
        assertMethod("setRenderingEnabled", void.class, boolean.class);
        assertMethod("isRenderingEnabled", boolean.class);
    }

    @Test
    void colorDepthMatchesOpenComputersDepths() {
        assertArrayEquals(
                new TextBuffer.ColorDepth[]{TextBuffer.ColorDepth.OneBit, TextBuffer.ColorDepth.FourBit, TextBuffer.ColorDepth.EightBit},
                TextBuffer.ColorDepth.values());
    }

    private static void assertMethod(final String name, final Class<?> returnType, final Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = TextBuffer.class.getMethod(name, parameterTypes);

        assertEquals(returnType, method.getReturnType());
        for (int i = 0; i < parameterTypes.length; i++) {
            assertSame(parameterTypes[i], method.getParameterTypes()[i]);
        }
    }
}

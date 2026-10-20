package li.cil.oc.client.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.client.ItemSearch;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.runtime.IIngredientListOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class JeiRuntimeBridgeTest {
    @Test void replacingRuntimeDetachesOldFocusAndHoverProviders() {
        final var plugin = new OpenComputersJeiPlugin();
        final var oldHover = new AtomicInteger();
        final var newHover = new AtomicInteger();
        try {
            plugin.onRuntimeAvailable(runtime(oldHover, true));
            assertTrue(ItemSearch.isInputFocused());
            assertTrue(ItemSearch.hoveredStack(null, 1, 2).isEmpty());
            assertEquals(1, oldHover.get());
            plugin.onRuntimeAvailable(runtime(newHover, false));
            assertFalse(ItemSearch.isInputFocused());
            assertTrue(ItemSearch.hoveredStack(null, 1, 2).isEmpty());
            assertEquals(1, oldHover.get()); assertEquals(1, newHover.get());
        } finally { plugin.onRuntimeUnavailable(); }
    }

    @Test void runtimeRemovalIsIdempotentAndStopsQueries() {
        final var plugin = new OpenComputersJeiPlugin();
        final var hover = new AtomicInteger();
        plugin.onRuntimeAvailable(runtime(hover, true));
        plugin.onRuntimeUnavailable(); plugin.onRuntimeUnavailable();
        assertFalse(ItemSearch.isInputFocused());
        assertTrue(ItemSearch.hoveredStack(null, 0, 0).isEmpty());
        assertEquals(0, hover.get());
    }

    @Test void manualLinkDoesNotOpenDuringSimulatedInputOrOtherButtons() {
        final var opened = new AtomicInteger();
        final var handler = new DocumentationCategory.ManualLinkInput(opened::incrementAndGet);
        assertTrue(handler.handleInput(5, 5, input(0, true))); assertEquals(0, opened.get());
        assertFalse(handler.handleInput(5, 5, input(1, false))); assertEquals(0, opened.get());
        assertTrue(handler.handleInput(5, 5, input(0, false))); assertEquals(1, opened.get());
    }

    private static IJeiUserInput input(int button, boolean simulate) {
        return proxy(IJeiUserInput.class, (name) -> switch (name) {
            case "getKey" -> InputConstants.Type.MOUSE.getOrCreate(button);
            case "isSimulate" -> simulate;
            default -> throw new AssertionError(name);
        });
    }

    private static IJeiRuntime runtime(AtomicInteger hover, boolean focused) {
        final var overlay = proxy(IIngredientListOverlay.class, name -> switch (name) {
            case "hasKeyboardFocus" -> focused;
            case "getIngredientUnderMouse" -> { hover.incrementAndGet(); yield null; }
            default -> throw new AssertionError(name);
        });
        return proxy(IJeiRuntime.class, name -> {
            assertEquals("getIngredientListOverlay", name); return overlay;
        });
    }

    private static <T> T proxy(Class<T> type, java.util.function.Function<String, Object> call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> call.apply(method.getName())));
    }
}

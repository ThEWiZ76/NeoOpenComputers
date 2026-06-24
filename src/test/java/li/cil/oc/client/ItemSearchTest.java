package li.cil.oc.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ItemSearchTest {
    @AfterEach
    void tearDown() {
        ItemSearch.clearStackFocusing();
        ItemSearch.clearFocusedInput();
    }

    @Test
    void hoveredStackIsEmptyWithoutProviders() {
        assertTrue(ItemSearch.hoveredStack(null, 1, 2).isEmpty());
    }

    @Test
    void hoveredStackQueriesRegisteredProvidersAndCanRemoveThem() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        final AutoCloseable registration = ItemSearch.registerStackFocusing((screen, mouseX, mouseY) -> {
            calls.incrementAndGet();
            assertEquals(4, mouseX);
            assertEquals(5, mouseY);
            return Optional.empty();
        });

        assertTrue(ItemSearch.hoveredStack(null, 4, 5).isEmpty());
        assertEquals(1, calls.get());

        registration.close();

        assertTrue(ItemSearch.hoveredStack(null, 4, 5).isEmpty());
        assertEquals(1, calls.get());
    }

    @Test
    void inputFocusUsesFirstFocusedProviderAndCanRemoveIt() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        final AutoCloseable registration = ItemSearch.registerFocusedInput(() -> {
            calls.incrementAndGet();
            return true;
        });
        ItemSearch.registerFocusedInput(() -> {
            calls.incrementAndGet();
            return false;
        });

        assertTrue(ItemSearch.isInputFocused());
        assertEquals(1, calls.get());

        registration.close();

        assertTrue(!ItemSearch.isInputFocused());
        assertEquals(2, calls.get());
    }
}

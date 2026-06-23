package li.cil.oc.common.component;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DebugCardEnvironmentTest {
    @Test
    void callbacksAllowAccessByDefaultLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        DebugCardEnvironment card = new DebugCardEnvironment(new TestEnvironmentHost());
        Component component = assertInstanceOf(Component.class, card.node());

        assertArrayEquals(new Object[]{10.5D}, component.invoke("getX", null));
    }

    @Test
    void callbacksHonorDeniedDebugCardAccess() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(debugCardAccessConfig(), "deny", () -> {
            DebugCardEnvironment card = new DebugCardEnvironment(new TestEnvironmentHost());
            Component component = assertInstanceOf(Component.class, card.node());

            Exception error = assertThrows(Exception.class, () -> component.invoke("getX", null));

            assertEquals("debug card is disabled", error.getMessage());
        });
    }

    @Test
    void whitelistAccessAllowsMatchingPersistedNonce() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(debugCardAccessConfig(), "whitelist", () ->
            withCachedConfig(debugCardWhitelistConfig(), List.of("alice nonce-1"), () -> {
                DebugCardEnvironment saved = new DebugCardEnvironment(new TestEnvironmentHost(), new DebugCardEnvironment.AccessContext("Alice", "nonce-1"));
                CompoundTag tag = new CompoundTag();
                saved.save(tag);
                DebugCardEnvironment loaded = new DebugCardEnvironment(new TestEnvironmentHost());
                loaded.load(tag);
                Component component = assertInstanceOf(Component.class, loaded.node());

                assertArrayEquals(new Object[]{10.5D}, component.invoke("getX", null));
            }));
    }

    @Test
    void isModLoadedReportsLoadedAndMissingModsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        DebugCardEnvironment card = new DebugCardEnvironment(new TestEnvironmentHost());
        Component component = assertInstanceOf(Component.class, card.node());

        assertArrayEquals(new Object[]{true}, component.invoke("isModLoaded", null, "neoopencomputers"));
        assertArrayEquals(new Object[]{true}, component.invoke("isModLoaded", null, "opencomputers"));
        assertArrayEquals(new Object[]{false}, component.invoke("isModLoaded", null, "definitely_missing_debug_card_test_mod"));
    }

    private static ModConfigSpec.ConfigValue<String> debugCardAccessConfig() throws Exception {
        Field field = ModSettings.class.getDeclaredField("DEBUG_CARD_ACCESS");
        return (ModConfigSpec.ConfigValue<String>) field.get(null);
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> debugCardWhitelistConfig() throws Exception {
        Field field = ModSettings.class.getDeclaredField("DEBUG_CARD_WHITELIST");
        return (ModConfigSpec.ConfigValue<List<? extends String>>) field.get(null);
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    private record TestEnvironmentHost() implements EnvironmentHost {
        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return 10.5D;
        }

        @Override
        public double yPosition() {
            return 64.0D;
        }

        @Override
        public double zPosition() {
            return -4.5D;
        }

        @Override
        public void markChanged() {
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

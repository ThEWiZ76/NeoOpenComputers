package li.cil.oc.common.item;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ItemDriverDataTest {
    @Test
    void fatalAndDurabilityFailuresEscapeWithoutBeingReplaced() {
        final var driver = new li.cil.oc.api.prefab.DriverItem() {
            @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) { return null; }
            @Override public String slot(ItemStack stack) { return "card"; }
        };
        for (final var failure : new Throwable[]{new OutOfMemoryError("fixture"), new ThreadDeath(),
            new UncheckedIOException(new IOException("fixture")), new CompletionException(new IOException("fixture"))}) {
            final var environment = new AbstractManagedEnvironment() {
                private void fail() {
                    if (failure instanceof RuntimeException runtime) throw runtime;
                    throw (Error) failure;
                }
                @Override public void load(CompoundTag data) { fail(); }
                @Override public void save(CompoundTag data) { fail(); }
            };
            assertSame(failure, assertThrows(Throwable.class,
                () -> ItemDriverData.loadEnvironment(driver, null, environment, new CompoundTag())));
            assertSame(failure, assertThrows(Throwable.class,
                () -> ItemDriverData.saveEnvironment(driver, null, environment)));
        }
    }
}

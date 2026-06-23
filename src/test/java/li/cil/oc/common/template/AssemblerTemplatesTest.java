package li.cil.oc.common.template;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AssemblerTemplatesTest {
    @Test
    void defaultTemplatesIncludeTabletAssembler() {
        assertTrue(AssemblerTemplates.defaultTemplateNames().contains("tablet"));
    }

    @Test
    void tabletAssemblerUsesConfiguredBaseAndComplexityCosts() throws Exception {
        withCachedConfig(ModSettings.TABLET_ASSEMBLY_BASE_COST, 100D, () ->
            withCachedConfig(ModSettings.TABLET_ASSEMBLY_COMPLEXITY_COST, 10D, () -> {
                assertEquals(4, TabletAssemblerTemplate.complexityOf(new ContainerDriver(1), null));
                assertEquals(0, TabletAssemblerTemplate.complexityOf(new ProcessorDriver(2), null));
                assertEquals(1, TabletAssemblerTemplate.complexityOf(new ItemDriver(li.cil.oc.api.driver.item.Slot.Memory, 0), null));
                assertEquals(3, TabletAssemblerTemplate.complexityOf(new ItemDriver(li.cil.oc.api.driver.item.Slot.Upgrade, 2), null));
                assertEquals(0, TabletAssemblerTemplate.complexityOf(new ItemDriver("eeprom", 0), null));
                assertEquals(180D, TabletAssemblerTemplate.energyForComplexity(8));
            }));
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record ItemDriver(String slot, int tier) implements DriverItem {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return false;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return slot;
        }

        @Override
        public int tier(final ItemStack stack) {
            return tier;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return new CompoundTag();
        }
    }

    private record ContainerDriver(int tier) implements li.cil.oc.api.driver.item.Container {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return false;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return li.cil.oc.api.driver.item.Slot.Container;
        }

        @Override
        public int tier(final ItemStack stack) {
            return tier;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return new CompoundTag();
        }

        @Override
        public String providedSlot(final ItemStack stack) {
            return li.cil.oc.api.driver.item.Slot.Tablet;
        }

        @Override
        public int providedTier(final ItemStack stack) {
            return tier;
        }
    }

    private record ProcessorDriver(int tier) implements li.cil.oc.api.driver.item.Processor {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return false;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return li.cil.oc.api.driver.item.Slot.CPU;
        }

        @Override
        public int tier(final ItemStack stack) {
            return tier;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return new CompoundTag();
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 0;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return null;
        }
    }
}

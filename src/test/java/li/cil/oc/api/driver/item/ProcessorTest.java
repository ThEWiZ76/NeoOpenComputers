package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProcessorTest {
    @Test
    void processorExposesArchitectureAndSupportedComponents() {
        TestProcessor processor = new TestProcessor();
        ItemStack stack = null;

        processor.setArchitecture(stack, TestArchitecture.class);

        assertInstanceOf(DriverItem.class, processor);
        assertEquals(8, processor.supportedComponents(stack));
        assertSame(TestArchitecture.class, processor.architecture(stack));
        assertEquals(List.of(TestArchitecture.class), processor.allArchitectures());
    }

    private static final class TestProcessor implements MutableProcessor {
        private Class<? extends Architecture> architecture;

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 8;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return architecture;
        }

        @Override
        public Collection<Class<? extends Architecture>> allArchitectures() {
            return List.of(TestArchitecture.class);
        }

        @Override
        public void setArchitecture(final ItemStack stack, final Class<? extends Architecture> architecture) {
            this.architecture = architecture;
        }

        @Override
        public boolean worksWith(final ItemStack stack) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int tier(final ItemStack stack) {
            return 2;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return null;
        }
    }

    private abstract static class TestArchitecture implements Architecture {
    }
}

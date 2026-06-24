package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaidBlockEntityTest {
    @Test
    void raidIsNetworkedMenuContainerHost() {
        assertTrue(ManagedEnvironment.class.isAssignableFrom(RaidBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(RaidBlockEntity.class));
        assertTrue(Container.class.isAssignableFrom(RaidBlockEntity.class));
        assertTrue(Analyzable.class.isAssignableFrom(RaidBlockEntity.class));
        assertFalse(DeviceInfo.class.isAssignableFrom(RaidBlockEntity.class));
        assertTrue(MenuProvider.class.isAssignableFrom(RaidBlockEntity.class));
    }

    @Test
    void raidAcceptsOnlyHardDiskSlots() {
        assertTrue(RaidBlockEntity.acceptsDriverSlot(Slot.HDD));
        assertFalse(RaidBlockEntity.acceptsDriverSlot(Slot.Floppy));
        assertFalse(RaidBlockEntity.acceptsDriverSlot(Slot.Card));
    }
}

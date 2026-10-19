package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** Fixture-only serialized assembly installation; never unlocks the public container. */
final class MicrocontrollerFixture {
    static boolean canPlaceItem(final Container inventory, final int slot, final ItemStack stack) {
        if (!(inventory instanceof MicrocontrollerBlockEntity host)) return inventory.canPlaceItem(slot, stack);
        final var driver = Driver.driverFor(stack, host.getClass());
        return driver != null && MicrocontrollerBlockEntity.slotType(host.tier(), slot).equals(driver.slot(stack))
            && driver.tier(stack) <= MicrocontrollerBlockEntity.slotTier(host.tier(), slot);
    }

    static void setItem(final Container inventory, final int slot, final ItemStack stack) {
        if (!(inventory instanceof MicrocontrollerBlockEntity host)) {
            inventory.setItem(slot, stack);
            return;
        }
        host.machine().stop();
        final var registries = host.getLevel().registryAccess();
        final var saved = host.saveWithoutMetadata(registries);
        final var items = NonNullList.withSize(host.getContainerSize(), ItemStack.EMPTY);
        for (int index = 0; index < items.size(); index++) items.set(index, host.getItem(index).copy());
        items.set(slot, stack.copy());
        ContainerHelper.saveAllItems(saved, items, registries);
        host.loadWithComponents(saved, registries);
    }

    private MicrocontrollerFixture() { }
}

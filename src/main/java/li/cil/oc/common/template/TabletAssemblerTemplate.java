package li.cil.oc.common.template;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.item.TabletCaseItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

final class TabletAssemblerTemplate implements AssemblerTemplate {
    @Override
    public String name() {
        return "tablet";
    }

    @Override
    public boolean matches(final ItemStack stack) {
        return stack.getItem() instanceof TabletCaseItem;
    }

    @Override
    public boolean validate(final AssemblerBlockEntity assembler) {
        return hasDriverSlot(assembler, Slot.CPU) && hasDriverSlot(assembler, Slot.Memory);
    }

    @Override
    public boolean canPlaceItem(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
        if (slot == AssemblerBlockEntity.SLOT_TEMPLATE) {
            return matches(stack);
        }
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null) {
            return false;
        }
        if (isContainerSlot(slot)) {
            return driver instanceof li.cil.oc.api.driver.item.Container;
        }
        if (isUpgradeSlot(slot)) {
            return Slot.Upgrade.equals(driver.slot(stack));
        }
        if (isComponentSlot(slot)) {
            final String driverSlot = driver.slot(stack);
            return !Slot.Container.equals(driverSlot) && !Slot.Upgrade.equals(driverSlot);
        }
        return false;
    }

    @Override
    public ItemStack assemble(final AssemblerBlockEntity assembler) {
        final ItemStack template = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE).copy();
        final ItemStack container = firstStoredStack(assembler, AssemblerBlockEntity.SLOT_CONTAINER_START, AssemblerBlockEntity.CONTAINER_SLOT_COUNT);
        final ItemStack[] components = componentStacks(assembler);
        return ModItems.TABLET.get().assembleFromCase(template, container, components);
    }

    private static ItemStack[] componentStacks(final AssemblerBlockEntity assembler) {
        final ArrayList<ItemStack> stacks = new ArrayList<>();
        for (int slot = AssemblerBlockEntity.SLOT_COMPONENT_START; slot < AssemblerBlockEntity.SLOT_COMPONENT_START + AssemblerBlockEntity.COMPONENT_SLOT_COUNT; slot++) {
            final ItemStack stack = assembler.getItem(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }
        return stacks.toArray(ItemStack[]::new);
    }

    private static ItemStack firstStoredStack(final AssemblerBlockEntity assembler, final int start, final int count) {
        for (int slot = start; slot < start + count; slot++) {
            final ItemStack stack = assembler.getItem(slot);
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasDriverSlot(final AssemblerBlockEntity assembler, final String expectedSlot) {
        for (int slot = AssemblerBlockEntity.SLOT_COMPONENT_START; slot < AssemblerBlockEntity.SLOT_COMPONENT_START + AssemblerBlockEntity.COMPONENT_SLOT_COUNT; slot++) {
            final ItemStack stack = assembler.getItem(slot);
            final DriverItem driver = Driver.driverFor(stack);
            if (driver != null && expectedSlot.equals(driver.slot(stack))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isContainerSlot(final int slot) {
        return slot >= AssemblerBlockEntity.SLOT_CONTAINER_START && slot < AssemblerBlockEntity.SLOT_CONTAINER_START + AssemblerBlockEntity.CONTAINER_SLOT_COUNT;
    }

    private static boolean isUpgradeSlot(final int slot) {
        return slot >= AssemblerBlockEntity.SLOT_UPGRADE_START && slot < AssemblerBlockEntity.SLOT_UPGRADE_START + AssemblerBlockEntity.UPGRADE_SLOT_COUNT;
    }

    private static boolean isComponentSlot(final int slot) {
        return slot >= AssemblerBlockEntity.SLOT_COMPONENT_START && slot < AssemblerBlockEntity.SLOT_COMPONENT_START + AssemblerBlockEntity.COMPONENT_SLOT_COUNT;
    }
}

package li.cil.oc.common.template;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.common.ModSettings;
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
        return hasDriverSlot(assembler, Slot.CPU)
            && hasDriverSlot(assembler, Slot.Memory)
            && isComplexityAllowed(caseTier(assembler), cpuTier(assembler), complexity(assembler));
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

    @Override
    public double energyRequired(final AssemblerBlockEntity assembler) {
        return energyForComplexity(complexity(assembler));
    }

    static double energyForComplexity(final int complexity) {
        return ModSettings.tabletAssemblyBaseCost() + Math.max(0, complexity) * ModSettings.tabletAssemblyComplexityCost();
    }

    static int maxComplexity(final int caseTier, final int cpuTier) {
        return deviceMaxComplexity(caseTier, cpuTier) / 2 + 5;
    }

    static boolean isComplexityAllowed(final int caseTier, final int cpuTier, final int complexity) {
        return complexity <= maxComplexity(caseTier, cpuTier);
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

    private static int caseTier(final AssemblerBlockEntity assembler) {
        final ItemStack stack = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE);
        return stack.getItem() instanceof TabletCaseItem item ? item.tier() : -1;
    }

    private static int cpuTier(final AssemblerBlockEntity assembler) {
        int tier = 0;
        for (int slot = 0; slot < AssemblerBlockEntity.CONTAINER_SIZE; slot++) {
            final ItemStack stack = assembler.getItem(slot);
            final DriverItem driver = Driver.driverFor(stack);
            if (driver instanceof Processor) {
                tier += driver.tier(stack);
            }
        }
        return tier;
    }

    private static int complexity(final AssemblerBlockEntity assembler) {
        int complexity = 0;
        for (int slot = AssemblerBlockEntity.SLOT_CONTAINER_START; slot < AssemblerBlockEntity.CONTAINER_SIZE; slot++) {
            final ItemStack stack = assembler.getItem(slot);
            complexity += complexityOf(Driver.driverFor(stack), stack);
        }
        return complexity;
    }

    static int complexityOf(final DriverItem driver, final ItemStack stack) {
        if (driver instanceof Processor) {
            return 0;
        }
        if (driver instanceof li.cil.oc.api.driver.item.Container) {
            return (1 + driver.tier(stack)) * 2;
        }
        if (driver != null && !"eeprom".equals(driver.slot(stack))) {
            return 1 + driver.tier(stack);
        }
        return 0;
    }

    private static int deviceMaxComplexity(final int caseTier, final int cpuTier) {
        if (caseTier < 0 || cpuTier < 0) {
            return 0;
        }
        return ModSettings.deviceComplexityByTier(caseTier) - (Math.min(2, caseTier) - cpuTier) * 6;
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

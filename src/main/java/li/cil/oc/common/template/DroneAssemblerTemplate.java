package li.cil.oc.common.template;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.item.DroneCaseItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class DroneAssemblerTemplate implements AssemblerTemplate {
    private static final String EEPROM_SLOT = "eeprom";
    private static final DroneSlot[][] UPGRADE_LAYOUTS = {
        {new DroneSlot(Slot.Upgrade, 1), new DroneSlot(Slot.Upgrade, 0)},
        {new DroneSlot(Slot.Upgrade, 2), new DroneSlot(Slot.Upgrade, 1), new DroneSlot(Slot.Upgrade, 0)},
        {
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2),
            new DroneSlot(Slot.Upgrade, 2)
        }
    };
    private static final DroneSlot[][] COMPONENT_LAYOUTS = {
        {
            new DroneSlot(Slot.CPU, 0),
            new DroneSlot(Slot.Memory, 0),
            new DroneSlot(EEPROM_SLOT, Integer.MAX_VALUE),
            new DroneSlot(Slot.Card, 1),
            new DroneSlot(Slot.Card, 0)
        },
        {
            new DroneSlot(Slot.CPU, 0),
            new DroneSlot(Slot.Memory, 0),
            new DroneSlot(Slot.Memory, 0),
            new DroneSlot(EEPROM_SLOT, Integer.MAX_VALUE),
            new DroneSlot(Slot.Card, 1),
            new DroneSlot(Slot.Card, 1)
        },
        {
            new DroneSlot(Slot.CPU, 2),
            new DroneSlot(Slot.Memory, 2),
            new DroneSlot(Slot.Memory, 2),
            new DroneSlot(EEPROM_SLOT, Integer.MAX_VALUE),
            new DroneSlot(Slot.Card, 2),
            new DroneSlot(Slot.Card, 2),
            new DroneSlot(Slot.Card, 2)
        }
    };

    @Override
    public String name() {
        return "drone";
    }

    @Override
    public boolean matches(final ItemStack stack) {
        return stack.getItem() instanceof DroneCaseItem;
    }

    @Override
    public boolean validate(final AssemblerBlockEntity assembler) {
        final int tier = caseTier(assembler);
        final Placement placement = placedItems(assembler, tier);
        if (!placement.valid()) {
            return false;
        }
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEepromCode = false;
        for (final ItemStack stack : placement.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            final DriverItem driver = Driver.driverFor(stack, Drone.class);
            if (driver == null) {
                return false;
            }
            final String slot = driver.slot(stack);
            hasCpu |= Slot.CPU.equals(slot);
            hasMemory |= Slot.Memory.equals(slot);
            hasEepromCode |= EEPROM_SLOT.equals(slot) && hasEepromCode(stack);
        }
        return hasCpu && hasMemory && hasEepromCode;
    }

    @Override
    public boolean canPlaceItem(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
        if (slot == AssemblerBlockEntity.SLOT_TEMPLATE) {
            return matches(stack);
        }
        final DriverItem driver = Driver.driverFor(stack, Drone.class);
        if (driver == null) {
            return false;
        }
        if (isUpgradeSlot(slot)) {
            return Slot.Upgrade.equals(driver.slot(stack)) && canFitAnyDroneSlot(assembler, stack, driver);
        }
        if (isComponentSlot(slot)) {
            return !Slot.Upgrade.equals(driver.slot(stack)) && canFitAnyDroneSlot(assembler, stack, driver);
        }
        return false;
    }

    @Override
    public ItemStack assemble(final AssemblerBlockEntity assembler) {
        final int tier = caseTier(assembler);
        final Placement placement = placedItems(assembler, tier);
        if (!placement.valid()) {
            return ItemStack.EMPTY;
        }
        return ModItems.DRONE.get().assembleFromCase(assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE), placement.items().toArray(ItemStack[]::new));
    }

    @Override
    public double energyRequired(final AssemblerBlockEntity assembler) {
        return TabletAssemblerTemplate.energyForComplexity(complexity(assembler));
    }

    static boolean canPlaceAllDrivers(final int tier, final Iterable<DriverItem> upgradeDrivers, final Iterable<DriverItem> componentDrivers) {
        if (tier < 0) {
            return false;
        }
        final boolean[] upgrades = new boolean[upgradeSlotCount(tier)];
        for (final DriverItem driver : upgradeDrivers) {
            if (!placeDriver(upgrades, driver, UPGRADE_LAYOUTS[normalizeTier(tier)])) {
                return false;
            }
        }
        final boolean[] components = new boolean[componentSlotCount(tier)];
        for (final DriverItem driver : componentDrivers) {
            if (!placeDriver(components, driver, COMPONENT_LAYOUTS[normalizeTier(tier)])) {
                return false;
            }
        }
        return true;
    }

    private static Placement placedItems(final AssemblerBlockEntity assembler, final int tier) {
        if (tier < 0) {
            return new Placement(NonNullList.create(), false);
        }
        final NonNullList<ItemStack> result = NonNullList.withSize(slotCount(tier), ItemStack.EMPTY);
        for (int slot = AssemblerBlockEntity.SLOT_UPGRADE_START; slot < AssemblerBlockEntity.SLOT_UPGRADE_START + AssemblerBlockEntity.UPGRADE_SLOT_COUNT; slot++) {
            if (!place(result, assembler.getItem(slot), tier)) {
                return new Placement(result, false);
            }
        }
        for (int slot = AssemblerBlockEntity.SLOT_COMPONENT_START; slot < AssemblerBlockEntity.SLOT_COMPONENT_START + AssemblerBlockEntity.COMPONENT_SLOT_COUNT; slot++) {
            if (!place(result, assembler.getItem(slot), tier)) {
                return new Placement(result, false);
            }
        }
        return new Placement(result, true);
    }

    private static boolean place(final NonNullList<ItemStack> result, final ItemStack stack, final int tier) {
        if (stack.isEmpty()) {
            return true;
        }
        final DriverItem driver = Driver.driverFor(stack, Drone.class);
        if (driver == null) {
            return false;
        }
        for (int slot = 0; slot < result.size(); slot++) {
            if (result.get(slot).isEmpty() && accepts(slot, stack, driver, tier)) {
                result.set(slot, stack.copy());
                return true;
            }
        }
        return false;
    }

    private static boolean placeDriver(final boolean[] occupied, final DriverItem driver, final DroneSlot[] layout) {
        for (int slot = 0; slot < occupied.length; slot++) {
            if (!occupied[slot] && layout[slot].type().equals(driver.slot(null)) && driver.tier(null) <= layout[slot].tier()) {
                occupied[slot] = true;
                return true;
            }
        }
        return false;
    }

    private static boolean accepts(final int slot, final ItemStack stack, final DriverItem driver, final int tier) {
        final DroneSlot expected = slotAt(tier, slot);
        return expected.type().equals(driver.slot(stack)) && driver.tier(stack) <= expected.tier();
    }

    private static boolean canFitAnyDroneSlot(final AssemblerBlockEntity assembler, final ItemStack stack, final DriverItem driver) {
        final int tier = caseTier(assembler);
        for (int slot = 0; slot < slotCount(tier); slot++) {
            if (accepts(slot, stack, driver, tier)) {
                return true;
            }
        }
        return false;
    }

    private static int caseTier(final AssemblerBlockEntity assembler) {
        final ItemStack stack = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE);
        return stack.getItem() instanceof DroneCaseItem item ? item.tier() : -1;
    }

    private static int slotCount(final int tier) {
        return upgradeSlotCount(tier) + componentSlotCount(tier);
    }

    private static int upgradeSlotCount(final int tier) {
        return UPGRADE_LAYOUTS[normalizeTier(tier)].length;
    }

    private static int componentSlotCount(final int tier) {
        return COMPONENT_LAYOUTS[normalizeTier(tier)].length;
    }

    private static DroneSlot slotAt(final int tier, final int slot) {
        final DroneSlot[] upgrades = UPGRADE_LAYOUTS[normalizeTier(tier)];
        if (slot < upgrades.length) {
            return upgrades[slot];
        }
        final DroneSlot[] components = COMPONENT_LAYOUTS[normalizeTier(tier)];
        final int componentSlot = slot - upgrades.length;
        return componentSlot >= 0 && componentSlot < components.length ? components[componentSlot] : new DroneSlot(Slot.None, -1);
    }

    private static int normalizeTier(final int tier) {
        return Math.max(0, Math.min(2, tier));
    }

    private static int complexity(final AssemblerBlockEntity assembler) {
        int complexity = 0;
        for (int slot = AssemblerBlockEntity.SLOT_UPGRADE_START; slot < AssemblerBlockEntity.CONTAINER_SIZE; slot++) {
            final ItemStack stack = assembler.getItem(slot);
            complexity += TabletAssemblerTemplate.complexityOf(Driver.driverFor(stack, Drone.class), stack);
        }
        return complexity;
    }

    private static boolean hasEepromCode(final ItemStack stack) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        final CompoundTag eepromData = customData.getUnsafe().getCompound(ItemRegistry.EEPROM_DATA_TAG);
        return eepromData.getByteArray(ItemRegistry.EEPROM_CODE_TAG).length > 0;
    }

    private static boolean isUpgradeSlot(final int slot) {
        return slot >= AssemblerBlockEntity.SLOT_UPGRADE_START && slot < AssemblerBlockEntity.SLOT_UPGRADE_START + AssemblerBlockEntity.UPGRADE_SLOT_COUNT;
    }

    private static boolean isComponentSlot(final int slot) {
        return slot >= AssemblerBlockEntity.SLOT_COMPONENT_START && slot < AssemblerBlockEntity.SLOT_COMPONENT_START + AssemblerBlockEntity.COMPONENT_SLOT_COUNT;
    }

    private record DroneSlot(String type, int tier) {
    }

    private record Placement(NonNullList<ItemStack> items, boolean valid) {
    }
}

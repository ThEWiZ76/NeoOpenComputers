package li.cil.oc.common.template;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

final class RobotAssemblerTemplate implements AssemblerTemplate {
    @Override
    public String name() {
        return "robot";
    }

    @Override
    public boolean matches(final ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ComputerCaseBlock;
    }

    @Override
    public boolean validate(final AssemblerBlockEntity assembler) {
        final int tier = caseTier(assembler);
        final Placement placement = placedItems(assembler, tier);
        return placement.valid()
            && hasSlot(placement.items(), Slot.CPU)
            && hasSlot(placement.items(), Slot.Memory)
            && TabletAssemblerTemplate.isComplexityAllowed(tier, cpuTier(placement.items()), complexity(assembler));
    }

    @Override
    public boolean canPlaceItem(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
        if (slot == AssemblerBlockEntity.SLOT_TEMPLATE) {
            return matches(stack);
        }
        final DriverItem driver = Driver.driverFor(stack, Robot.class);
        if (driver == null) {
            return false;
        }
        if (isContainerSlot(slot)) {
            return Slot.Container.equals(driver.slot(stack)) && canFitAnyRobotSlot(assembler, stack, driver);
        }
        if (isUpgradeSlot(slot)) {
            return Slot.Upgrade.equals(driver.slot(stack)) && canFitAnyRobotSlot(assembler, stack, driver);
        }
        if (isComponentSlot(slot)) {
            final String driverSlot = driver.slot(stack);
            return !Slot.Container.equals(driverSlot) && !Slot.Upgrade.equals(driverSlot) && canFitAnyRobotSlot(assembler, stack, driver);
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
        final ItemStack output = new ItemStack(ModBlocks.ROBOT.get().asItem());
        final CompoundTag blockEntityData = new CompoundTag();
        blockEntityData.putInt(RobotBlockEntity.TAG_TIER, tier);
        saveItems(blockEntityData, placement.items());
        BlockItem.setBlockEntityData(output, ModBlockEntities.ROBOT.get(), blockEntityData);
        return output;
    }

    @Override
    public double energyRequired(final AssemblerBlockEntity assembler) {
        return TabletAssemblerTemplate.energyForComplexity(complexity(assembler));
    }

    static boolean canPlaceAllDrivers(final int tier, final Iterable<DriverItem> containerDrivers, final Iterable<DriverItem> componentDrivers) {
        if (tier < 0) {
            return false;
        }
        final boolean[] occupied = new boolean[RobotBlockEntity.slotCount(tier)];
        for (final DriverItem driver : containerDrivers) {
            if (!placeDriver(occupied, driver, tier)) {
                return false;
            }
        }
        for (final DriverItem driver : componentDrivers) {
            if (!placeDriver(occupied, driver, tier)) {
                return false;
            }
        }
        return true;
    }

    private static Placement placedItems(final AssemblerBlockEntity assembler, final int tier) {
        if (tier < 0) {
            return new Placement(NonNullList.create(), false);
        }
        final NonNullList<ItemStack> result = NonNullList.withSize(RobotBlockEntity.slotCount(tier), ItemStack.EMPTY);
        for (int slot = AssemblerBlockEntity.SLOT_CONTAINER_START; slot < AssemblerBlockEntity.SLOT_CONTAINER_START + AssemblerBlockEntity.CONTAINER_SLOT_COUNT; slot++) {
            if (!place(result, assembler.getItem(slot), tier)) {
                return new Placement(result, false);
            }
        }
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
        final DriverItem driver = Driver.driverFor(stack, Robot.class);
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

    private static boolean placeDriver(final boolean[] occupied, final DriverItem driver, final int tier) {
        for (int slot = 0; slot < occupied.length; slot++) {
            if (!occupied[slot] && acceptsDriver(slot, driver, tier)) {
                occupied[slot] = true;
                return true;
            }
        }
        return false;
    }

    private static boolean acceptsDriver(final int slot, final DriverItem driver, final int tier) {
        return RobotBlockEntity.slotType(tier, slot).equals(driver.slot(null))
            && driver.tier(null) <= RobotBlockEntity.slotTier(tier, slot);
    }

    private static boolean accepts(final int slot, final ItemStack stack, final DriverItem driver, final int tier) {
        return RobotBlockEntity.slotType(tier, slot).equals(driver.slot(stack))
            && driver.tier(stack) <= RobotBlockEntity.slotTier(tier, slot);
    }

    private static boolean canFitAnyRobotSlot(final AssemblerBlockEntity assembler, final ItemStack stack, final DriverItem driver) {
        final int tier = caseTier(assembler);
        for (int slot = 0; slot < RobotBlockEntity.slotCount(tier); slot++) {
            if (accepts(slot, stack, driver, tier)) {
                return true;
            }
        }
        return false;
    }

    private static int caseTier(final AssemblerBlockEntity assembler) {
        final ItemStack stack = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE);
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ComputerCaseBlock block) {
            return block.tier();
        }
        return -1;
    }

    private static boolean hasSlot(final NonNullList<ItemStack> items, final String slotType) {
        for (final ItemStack stack : items) {
            final DriverItem driver = Driver.driverFor(stack, Robot.class);
            if (driver != null && slotType.equals(driver.slot(stack))) {
                return true;
            }
        }
        return false;
    }

    private static int cpuTier(final NonNullList<ItemStack> items) {
        int result = 0;
        for (final ItemStack stack : items) {
            final DriverItem driver = Driver.driverFor(stack, Robot.class);
            if (driver instanceof Processor) {
                result += driver.tier(stack);
            }
        }
        return result;
    }

    private static int complexity(final AssemblerBlockEntity assembler) {
        int complexity = 0;
        for (int slot = AssemblerBlockEntity.SLOT_CONTAINER_START; slot < AssemblerBlockEntity.CONTAINER_SIZE; slot++) {
            final ItemStack stack = assembler.getItem(slot);
            complexity += TabletAssemblerTemplate.complexityOf(Driver.driverFor(stack, Robot.class), stack);
        }
        return complexity;
    }

    private static void saveItems(final CompoundTag tag, final NonNullList<ItemStack> items) {
        final ListTag itemTags = new ListTag();
        for (int slot = 0; slot < items.size(); slot++) {
            final ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                final CompoundTag stackTag = ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack)
                    .result()
                    .filter(CompoundTag.class::isInstance)
                    .map(CompoundTag.class::cast)
                    .orElseGet(CompoundTag::new);
                stackTag.putByte("Slot", (byte) slot);
                itemTags.add(stackTag);
            }
        }
        tag.put("Items", itemTags);
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

    private record Placement(NonNullList<ItemStack> items, boolean valid) {
    }
}

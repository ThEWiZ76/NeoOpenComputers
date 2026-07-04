package li.cil.oc.common.template;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.item.MicrocontrollerCaseItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class MicrocontrollerAssemblerTemplate implements AssemblerTemplate {
    private static final String EEPROM_SLOT = "eeprom";

    @Override
    public String name() {
        return "microcontroller";
    }

    @Override
    public boolean matches(final ItemStack stack) {
        return stack.getItem() instanceof MicrocontrollerCaseItem;
    }

    @Override
    public boolean validate(final AssemblerBlockEntity assembler) {
        final NonNullList<ItemStack> items = placedItems(assembler);
        if (items.isEmpty()) {
            return false;
        }
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEepromCode = false;
        for (int slot = 0; slot < items.size(); slot++) {
            final ItemStack stack = items.get(slot);
            final DriverItem driver = Driver.driverFor(stack, MicrocontrollerBlockEntity.class);
            if (driver == null || !accepts(slot, stack, driver, caseTier(assembler))) {
                return false;
            }
            final String driverSlot = driver.slot(stack);
            hasCpu |= Slot.CPU.equals(driverSlot);
            hasMemory |= Slot.Memory.equals(driverSlot);
            hasEepromCode |= EEPROM_SLOT.equals(driverSlot) && hasEepromCode(stack);
        }
        return hasCpu && hasMemory && hasEepromCode;
    }

    @Override
    public boolean canPlaceItem(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
        if (slot == AssemblerBlockEntity.SLOT_TEMPLATE) {
            return matches(stack);
        }
        final DriverItem driver = Driver.driverFor(stack, MicrocontrollerBlockEntity.class);
        if (driver == null) {
            return false;
        }
        if (isUpgradeSlot(slot)) {
            return Slot.Upgrade.equals(driver.slot(stack)) && canFitAnyMicrocontrollerSlot(assembler, stack, driver);
        }
        if (isComponentSlot(slot)) {
            final String driverSlot = driver.slot(stack);
            return (Slot.CPU.equals(driverSlot) || Slot.Memory.equals(driverSlot) || EEPROM_SLOT.equals(driverSlot) || Slot.Card.equals(driverSlot))
                && canFitAnyMicrocontrollerSlot(assembler, stack, driver);
        }
        return false;
    }

    @Override
    public ItemStack assemble(final AssemblerBlockEntity assembler) {
        final int tier = caseTier(assembler);
        final ItemStack output = new ItemStack(blockItemForTier(tier));
        final NonNullList<ItemStack> items = placedItems(assembler);
        final CompoundTag blockEntityData = new CompoundTag();
        saveItems(blockEntityData, items);
        BlockItem.setBlockEntityData(output, ModBlockEntities.MICROCONTROLLER.get(), blockEntityData);
        return output;
    }

    @Override
    public double energyRequired(final AssemblerBlockEntity assembler) {
        return TabletAssemblerTemplate.energyForComplexity(complexity(assembler));
    }

    private static NonNullList<ItemStack> placedItems(final AssemblerBlockEntity assembler) {
        final int tier = caseTier(assembler);
        if (tier < 0) {
            return NonNullList.create();
        }
        final NonNullList<ItemStack> result = NonNullList.withSize(MicrocontrollerBlockEntity.slotCount(tier), ItemStack.EMPTY);
        for (int slot = AssemblerBlockEntity.SLOT_COMPONENT_START; slot < AssemblerBlockEntity.SLOT_COMPONENT_START + AssemblerBlockEntity.COMPONENT_SLOT_COUNT; slot++) {
            place(result, assembler.getItem(slot), tier);
        }
        for (int slot = AssemblerBlockEntity.SLOT_UPGRADE_START; slot < AssemblerBlockEntity.SLOT_UPGRADE_START + AssemblerBlockEntity.UPGRADE_SLOT_COUNT; slot++) {
            place(result, assembler.getItem(slot), tier);
        }
        return result;
    }

    private static boolean place(final NonNullList<ItemStack> result, final ItemStack stack, final int tier) {
        if (stack.isEmpty()) {
            return true;
        }
        final DriverItem driver = Driver.driverFor(stack, MicrocontrollerBlockEntity.class);
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

    private static boolean accepts(final int slot, final ItemStack stack, final DriverItem driver, final int tier) {
        return MicrocontrollerBlockEntity.slotType(tier, slot).equals(driver.slot(stack))
            && driver.tier(stack) <= MicrocontrollerBlockEntity.slotTier(tier, slot);
    }

    private static boolean canFitAnyMicrocontrollerSlot(final AssemblerBlockEntity assembler, final ItemStack stack, final DriverItem driver) {
        final int tier = caseTier(assembler);
        for (int slot = 0; slot < MicrocontrollerBlockEntity.slotCount(tier); slot++) {
            if (accepts(slot, stack, driver, tier)) {
                return true;
            }
        }
        return false;
    }

    private static int caseTier(final AssemblerBlockEntity assembler) {
        final ItemStack stack = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE);
        return stack.getItem() instanceof MicrocontrollerCaseItem item ? item.tier() : -1;
    }

    private static int complexity(final AssemblerBlockEntity assembler) {
        int complexity = 0;
        for (int slot = AssemblerBlockEntity.SLOT_UPGRADE_START; slot < AssemblerBlockEntity.CONTAINER_SIZE; slot++) {
            final ItemStack stack = assembler.getItem(slot);
            complexity += TabletAssemblerTemplate.complexityOf(Driver.driverFor(stack, MicrocontrollerBlockEntity.class), stack);
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

    private static Item blockItemForTier(final int tier) {
        if (tier == 1) {
            return ModBlocks.MICROCONTROLLER_TIER2.get().asItem();
        }
        if (tier >= 3) {
            return ModBlocks.MICROCONTROLLER_CREATIVE.get().asItem();
        }
        return ModBlocks.MICROCONTROLLER_TIER1.get().asItem();
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

    private static boolean isUpgradeSlot(final int slot) {
        return slot >= AssemblerBlockEntity.SLOT_UPGRADE_START && slot < AssemblerBlockEntity.SLOT_UPGRADE_START + AssemblerBlockEntity.UPGRADE_SLOT_COUNT;
    }

    private static boolean isComponentSlot(final int slot) {
        return slot >= AssemblerBlockEntity.SLOT_COMPONENT_START && slot < AssemblerBlockEntity.SLOT_COMPONENT_START + AssemblerBlockEntity.COMPONENT_SLOT_COUNT;
    }
}

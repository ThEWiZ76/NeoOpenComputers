package li.cil.oc.common.gametest;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModInkProviders;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.InkProviders;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.PrinterBlockEntity;
import li.cil.oc.common.item.data.PrintData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class PrinterParityGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 1);

    @GameTest(template = "empty")
    public static void printerReturnsItsConsumedCartridge(GameTestHelper helper) {
        final var printer = printer(helper);
        printer.setItem(1, new ItemStack(ModItems.INK_CARTRIDGE.get())); tick(helper, printer);
        helper.assertTrue(printer.amountInk() == ModInkProviders.UPSTREAM_INK_CARTRIDGE_VALUE, "Cartridge ink amount differs from upstream");
        cartridge(helper, printer); tick(helper, printer);
        helper.assertTrue(printer.amountInk() == ModInkProviders.UPSTREAM_INK_CARTRIDGE_VALUE, "Empty cartridge was consumed as additional ink");
        cartridge(helper, printer); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerCartridgeAndInkSurviveReload(GameTestHelper helper) {
        final var printer = printer(helper); printer.setItem(1, new ItemStack(ModItems.INK_CARTRIDGE.get())); tick(helper, printer);
        final var restored = reload(helper, printer);
        helper.assertTrue(restored.amountInk() == ModInkProviders.UPSTREAM_INK_CARTRIDGE_VALUE, "Reload lost credited ink");
        cartridge(helper, restored); tick(helper, restored); cartridge(helper, restored);
        helper.assertTrue(restored.amountInk() == ModInkProviders.UPSTREAM_INK_CARTRIDGE_VALUE, "Reload credited ink twice"); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerDoesNotConsumeCartridgeWithoutCapacity(GameTestHelper helper) {
        final var printer = printer(helper); balances(printer, 0, PrinterBlockEntity.MAX_INK - ModInkProviders.UPSTREAM_INK_CARTRIDGE_VALUE + 1);
        final int before = printer.amountInk(); printer.setItem(1, new ItemStack(ModItems.INK_CARTRIDGE.get())); tick(helper, printer);
        helper.assertTrue(printer.amountInk() == before && printer.getItem(1).is(ModItems.INK_CARTRIDGE.get()), "Insufficient ink capacity consumed or emptied a cartridge"); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerConsumesOrdinaryDyeOneAtATime(GameTestHelper helper) {
        final var printer = printer(helper); printer.setItem(1, new ItemStack(Items.BLACK_DYE, 3));
        for (int i = 1; i <= 3; i++) {
            tick(helper, printer);
            helper.assertTrue(printer.amountInk() == i * ModInkProviders.UPSTREAM_DYE_VALUE && printer.getItem(1).getCount() == 3 - i, "Dye input was lost or counted twice");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerPreservesStackedAddonInkAndItsContainers(GameTestHelper helper) throws Exception {
        InkProviders.add(PrinterParityGameTests.class.getMethod("stackedInkProvider", ItemStack.class));
        final var printer = printer(helper); final var bottles = new ItemStack(Items.HONEY_BOTTLE, 2);
        final var tag = new CompoundTag(); tag.putBoolean("oc_printer_ink_probe", true); bottles.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        final var area = new AABB(printer.getBlockPos()).inflate(2);
        final var existing = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        printer.setItem(1, bottles); tick(helper, printer);
        helper.assertTrue(printer.amountInk() == 17 && printer.getItem(1).is(Items.HONEY_BOTTLE) && printer.getItem(1).getCount() == 1,
            "Stacked ink fixture did not credit exactly one input or lost the queued remainder");
        final var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> !existing.contains(entity.getUUID()) && entity.getItem().is(Items.GLASS_BOTTLE));
        helper.assertTrue(dropped.size() == 1 && dropped.getFirst().getItem().getCount() == 1, "Stacked ink lost or duplicated its first container");
        tick(helper, printer);
        helper.assertTrue(printer.amountInk() == 34 && printer.getItem(1).is(Items.GLASS_BOTTLE) && printer.getItem(1).getCount() == 1,
            "Last stacked ink item did not return its container to the input slot");
        dropped.forEach(ItemEntity::discard); helper.succeed();
    }

    public static int stackedInkProvider(ItemStack stack) {
        final var data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(Items.HONEY_BOTTLE) && data != null && data.copyTag().getBoolean("oc_printer_ink_probe") ? 17 : 0;
    }

    @GameTest(template = "empty")
    public static void printerQueuesValidInputsWhenBuffersAreFull(GameTestHelper helper) {
        final var printer = printer(helper); balances(printer, PrinterBlockEntity.MAX_MATERIAL, PrinterBlockEntity.MAX_INK);
        helper.assertTrue(printer.canPlaceItem(0, new ItemStack(ModItems.CHAMELIUM.get()))
            && printer.canPlaceItem(1, new ItemStack(ModItems.INK_CARTRIDGE.get())), "Full buffers rejected valid queued input unlike upstream");
        printer.setItem(0, new ItemStack(ModItems.CHAMELIUM.get())); printer.setItem(1, new ItemStack(ModItems.INK_CARTRIDGE.get()));
        tick(helper, printer);
        helper.assertTrue(printer.getItem(0).is(ModItems.CHAMELIUM.get()) && printer.getItem(1).is(ModItems.INK_CARTRIDGE.get()), "Full buffer consumed queued input");
        balancesEqual(helper, printer, PrinterBlockEntity.MAX_MATERIAL, PrinterBlockEntity.MAX_INK); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerHopperOnlyTakesSpentContainersOrOutput(GameTestHelper helper) {
        helper.setBlock(POS.below(), Blocks.HOPPER);
        final HopperBlockEntity hopper = helper.getBlockEntity(POS.below());
        final var printer = printer(helper); printer.setItem(0, new ItemStack(ModItems.CHAMELIUM.get())); printer.setItem(1, new ItemStack(ModItems.INK_CARTRIDGE.get()));
        helper.assertTrue(!HopperBlockEntity.suckInItems(helper.getLevel(), hopper) && hopper.isEmpty(), "Hopper extracted valid printer input instead of leaving it queued");
        printer.setItem(1, new ItemStack(ModItems.INK_CARTRIDGE_EMPTY.get()));
        helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), hopper) && hopper.getItem(0).is(ModItems.INK_CARTRIDGE_EMPTY.get())
            && printer.getItem(0).is(ModItems.CHAMELIUM.get()) && printer.getItem(1).isEmpty(), "Hopper could not recover the spent cartridge"); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerItemCapabilityFollowsUpstreamSidedAccess(GameTestHelper helper) {
        final var printer = printer(helper); balances(printer, PrinterBlockEntity.MAX_MATERIAL, PrinterBlockEntity.MAX_INK);
        final Direction[] sides = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (var side : sides) {
            final var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, printer.getBlockPos(), side);
            helper.assertTrue(handler != null && handler.getSlots() == 3, "Printer item handler unavailable on " + side);
            helper.assertTrue(handler.insertItem(0, new ItemStack(ModItems.CHAMELIUM.get()), false).isEmpty()
                && handler.insertItem(1, new ItemStack(ModItems.INK_CARTRIDGE.get()), false).isEmpty(), "Automation could not queue valid printer input");
            helper.assertTrue(handler.extractItem(0, 1, false).isEmpty() && handler.extractItem(1, 1, false).isEmpty(), "Automation extracted unconsumed input");
            helper.assertTrue(handler.insertItem(2, new ItemStack(Items.STONE), false).is(Items.STONE), "Automation inserted into the output slot");
            printer.setItem(0, ItemStack.EMPTY); printer.setItem(1, new ItemStack(ModItems.INK_CARTRIDGE_EMPTY.get()));
            helper.assertTrue(handler.extractItem(1, 1, false).is(ModItems.INK_CARTRIDGE_EMPTY.get()), "Automation lost a spent cartridge");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerRejoinsExistingCableAfterLateLoad(GameTestHelper helper) {
        helper.setBlock(POS.west(), ModBlocks.CABLE.get());
        final CableBlockEntity cable = helper.getBlockEntity(POS.west());
        final var printer = printer(helper);
        helper.assertTrue(printer.node().network() == cable.node().network() && printer.node().network() != null, "Printer fixture was not initially connected");
        final String address = printer.node().address(); final var restored = reload(helper, printer);
        helper.assertTrue(restored.node().network() != null && restored.node().network() == cable.node().network(), "Loaded printer did not rejoin the existing cable network");
        helper.assertTrue(address.equals(restored.node().address()), "Printer component address changed during load"); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerFinishesCommittedModelAfterEditAndReload(GameTestHelper helper) throws Exception {
        powered(() -> {
            var printer = printer(helper); model(printer, "committed"); balances(printer, 10, 10); call(printer, "commit", 2);
            power(printer, 2); tick(helper, printer);
            helper.assertTrue(printer.isPrinting() && printer.progress() == 50D, "Printer fixture did not begin a paid partial job");
            model(printer, "replacement"); printer = reload(helper, printer);
            power(printer, 2); tick(helper, printer);
            output(helper, printer, "committed", 1); balancesEqual(helper, printer, 9, 9);
            for (int i = 0; i < 3; i++) tick(helper, printer);
            helper.assertTrue(!printer.isActive() && !printer.isPrinting(), "Editing restarted the old committed batch");
            output(helper, printer, "committed", 1);
            printer.removeItemNoUpdate(2); call(printer, "commit", 1); power(printer, 4); tick(helper, printer); tick(helper, printer);
            output(helper, printer, "replacement", 1); balancesEqual(helper, printer, 8, 8);
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerBatchUsesExactMaterialInkAndPartialPower(GameTestHelper helper) throws Exception {
        powered(() -> {
            var printer = printer(helper); model(printer, "batch"); balances(printer, 10, 10); call(printer, "commit", 2);
            for (int i = 0; i < 3; i++) tick(helper, printer);
            helper.assertTrue(printer.isPrinting() && printer.getItem(2).isEmpty(), "Unpowered printer completed its job"); balancesEqual(helper, printer, 9, 9);
            power(printer, 3); tick(helper, printer); tick(helper, printer);
            helper.assertTrue(printer.progress() == 75D, "Partial power did not pay exactly three of four energy units");
            printer = reload(helper, printer); power(printer, 1); tick(helper, printer); output(helper, printer, "batch", 1);
            tick(helper, printer); balancesEqual(helper, printer, 8, 8);
            power(printer, 4); tick(helper, printer); tick(helper, printer);
            output(helper, printer, "batch", 2); balancesEqual(helper, printer, 8, 8);
            helper.assertTrue(!printer.isPrinting() && !printer.isActive() && ((Connector) printer.node()).globalBuffer() == 0D, "Batch count or energy accounting changed");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void printerKeepsPaidOutputWhileFullAcrossReload(GameTestHelper helper) throws Exception {
        powered(() -> {
            var printer = printer(helper); model(printer, "full"); balances(printer, 10, 10);
            final var full = printer.previewStack(); full.setCount(full.getMaxStackSize()); printer.setItem(2, full);
            call(printer, "commit", 1); power(printer, 4); tick(helper, printer); tick(helper, printer);
            helper.assertTrue(printer.isPrinting() && printer.progress() == 100D, "Full output lost the paid pending print"); balancesEqual(helper, printer, 9, 9);
            printer = reload(helper, printer); tick(helper, printer);
            output(helper, printer, "full", full.getMaxStackSize());
            printer.removeItem(2, 1); tick(helper, printer);
            helper.assertTrue(!printer.isPrinting() && !printer.isActive(), "Paid output did not finish when space became available");
            output(helper, printer, "full", full.getMaxStackSize()); balancesEqual(helper, printer, 9, 9);
            helper.assertTrue(((Connector) printer.node()).globalBuffer() == 0D, "Blocked output consumed additional energy");
        }); helper.succeed();
    }

    private static PrinterBlockEntity printer(GameTestHelper helper) { helper.setBlock(POS, ModBlocks.PRINTER.get()); return helper.getBlockEntity(POS); }
    private static void tick(GameTestHelper helper, PrinterBlockEntity printer) { PrinterBlockEntity.serverTick(helper.getLevel(), printer.getBlockPos(), printer.getBlockState(), printer); }
    private static void power(PrinterBlockEntity printer, double value) { ((Connector) printer.node()).changeBuffer(value); }
    private static Object[] call(PrinterBlockEntity printer, String method, Object... arguments) throws Exception { return ((Component) printer.node()).invoke(method, null, arguments); }
    private static void model(PrinterBlockEntity printer, String label) throws Exception {
        call(printer, "reset"); call(printer, "setLabel", label); call(printer, "addShape", 0, 0, 0, 1, 1, 1, "minecraft:stone");
    }
    private static void balances(PrinterBlockEntity printer, int material, int ink) { final var tag = new CompoundTag(); printer.save(tag); tag.putInt("amountMaterial", material); tag.putInt("amountInk", ink); printer.load(tag); }
    private static void balancesEqual(GameTestHelper helper, PrinterBlockEntity printer, int material, int ink) { helper.assertTrue(printer.amountMaterial() == material && printer.amountInk() == ink, "Printer charged the wrong material/ink amounts"); }
    private static void cartridge(GameTestHelper helper, PrinterBlockEntity printer) { helper.assertTrue(printer.getItem(1).is(ModItems.INK_CARTRIDGE_EMPTY.get()) && printer.getItem(1).getCount() == 1, "Printer discarded the consumed ink cartridge's container"); }
    private static void output(GameTestHelper helper, PrinterBlockEntity printer, String label, int count) {
        final var stack = printer.getItem(2); helper.assertTrue(stack.is(ModItems.PRINT.get()) && stack.getCount() == count && label.equals(new PrintData(stack).label()), "Printer emitted the wrong model/count");
    }
    private static PrinterBlockEntity reload(GameTestHelper helper, PrinterBlockEntity printer) {
        final var saved = printer.saveWithFullMetadata(helper.getLevel().registryAccess()); printer.onChunkUnloaded(); printer.setRemoved();
        final var restored = (PrinterBlockEntity) BlockEntity.loadStatic(printer.getBlockPos(), printer.getBlockState(), saved, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(restored); restored.onLoad(); return restored;
    }
    private static void powered(NeoOpenComputersGameTests.ThrowingRunnable action) throws Exception {
        NeoOpenComputersGameTests.withCachedConfig(ModSettings.IGNORE_POWER, false, () ->
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.PRINT_COST, 4D, () ->
                NeoOpenComputersGameTests.withCachedConfig(ModSettings.PRINTER_TICK_AMOUNT, 2D, action)));
    }
}

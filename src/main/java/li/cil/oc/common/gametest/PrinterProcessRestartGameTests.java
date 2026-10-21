package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.PrinterBlockEntity;
import li.cil.oc.common.item.data.PrintData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class PrinterProcessRestartGameTests {
    private static final BlockPos PRINTER = new BlockPos(4096, 80, 4096);
    private static final BlockPos CABLE = PRINTER.west();
    private static final BlockPos RECORD = PRINTER.south(2);

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.restartPrinter")) return List.of();
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalStateException("Printer probe requires restart phase/world");
        return List.of(new TestFunction("printerRestart", "printer_restart_" + phase, "neoopencomputers:empty", 200, 0, true,
            helper -> {
                final var level = helper.getLevel();
                level.setChunkForced(PRINTER.getX() >> 4, PRINTER.getZ() >> 4, true);
                level.setChunkForced(CABLE.getX() >> 4, CABLE.getZ() >> 4, true);
                helper.runAfterDelay(5, () -> {
                    try { run(helper, phase.equals("prepare")); }
                    catch (Exception failure) { throw new AssertionError("Printer restart fixture failed", failure); }
                });
            }));
    }

    private static void run(GameTestHelper helper, boolean prepare) throws Exception {
        final var level = helper.getLevel();
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(PRINTER) && level.isEmptyBlock(CABLE) && level.isEmptyBlock(RECORD), "Printer restart needs a fresh world");
            level.setBlockAndUpdate(CABLE, ModBlocks.CABLE.get().defaultBlockState());
            level.setBlockAndUpdate(PRINTER, ModBlocks.PRINTER.get().defaultBlockState());
            level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(level.getBlockEntity(PRINTER) instanceof PrinterBlockEntity
            && level.getBlockEntity(CABLE) instanceof CableBlockEntity && level.getBlockEntity(RECORD) instanceof ChestBlockEntity, "Saved printer fixture missing");
        final var printer = (PrinterBlockEntity) level.getBlockEntity(PRINTER);
        final var cable = (CableBlockEntity) level.getBlockEntity(CABLE);
        final var record = (ChestBlockEntity) level.getBlockEntity(RECORD);
        final CompoundTag expected;
        if (prepare) {
            final var component = (Component) printer.node();
            component.invoke("setLabel", null, "printer process restart");
            component.invoke("addShape", null, 0, 0, 0, 1, 1, 1, "minecraft:stone"); component.invoke("commit", null, 1);
            printer.setItem(0, new ItemStack(ModItems.CHAMELIUM.get())); printer.setItem(1, new ItemStack(ModItems.INK_CARTRIDGE.get()));
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.IGNORE_POWER, false, () ->
                NeoOpenComputersGameTests.withCachedConfig(ModSettings.PRINT_COST, 4D, () ->
                    NeoOpenComputersGameTests.withCachedConfig(ModSettings.PRINTER_TICK_AMOUNT, 2D, () -> {
                        ((Connector) printer.node()).changeBuffer(2);
                        tick(helper, printer); tick(helper, printer);
                    })));
            expected = new CompoundTag(); expected.putString("address", printer.node().address());
            expected.putInt("material", printer.amountMaterial()); expected.putInt("ink", printer.amountInk());
        } else {
            final var saved = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(saved != null, "Printer checkpoint record missing"); expected = saved.copyTag();
            helper.assertTrue(expected.getString("phase").equals("prepared") && expected.getLong("pid") > 0
                && expected.getLong("pid") != ProcessHandle.current().pid(), "Printer checkpoint did not come from another JVM");
        }
        final var snapshot = new CompoundTag(); printer.save(snapshot);
        helper.assertTrue(printer.isPrinting() && snapshot.getDouble("remaining") == 2D && snapshot.getDouble("total") == 4D
            && printer.getItem(2).isEmpty(), "Partial job/energy/output was not preserved");
        helper.assertTrue(printer.node().network() != null && printer.node().network() == cable.node().network()
            && printer.node().address().equals(expected.getString("address")), "Printer cable connection/address not restored");
        helper.assertTrue(printer.getItem(1).is(ModItems.INK_CARTRIDGE_EMPTY.get()) && printer.getItem(1).getCount() == 1,
            "Spent cartridge was lost across printer restart");
        if (!prepare) {
            ((Connector) printer.node()).changeBuffer(2);
            tick(helper, printer); tick(helper, printer);
            for (int i = 0; i < 4; i++) tick(helper, printer);
            final var output = printer.getItem(2);
            helper.assertTrue(output.is(ModItems.PRINT.get()) && output.getCount() == 1 && new PrintData(output).label().equals("printer process restart")
                && !printer.isPrinting() && !printer.isActive(), "Restored printer did not finish its original job exactly once");
            helper.assertTrue(printer.amountMaterial() == expected.getInt("material") && printer.amountInk() == expected.getInt("ink")
                && ((Connector) printer.node()).globalBuffer() == 0D, "Printer restart charged material, ink or energy twice");
        }
        expected.putString("phase", prepare ? "prepared" : "verified"); expected.putLong("pid", ProcessHandle.current().pid());
        final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(expected)); record.setItem(0, paper); record.setChanged();
        NeoOpenComputers.LOGGER.info("Printer restart probe {} in PID {}; pending job saved normally", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, PrinterBlockEntity printer) {
        PrinterBlockEntity.serverTick(helper.getLevel(), PRINTER, printer.getBlockState(), printer);
    }
}

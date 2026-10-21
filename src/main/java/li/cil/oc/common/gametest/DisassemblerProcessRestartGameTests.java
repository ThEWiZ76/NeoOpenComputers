package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.template.DisassemblerTemplate;
import li.cil.oc.common.template.DisassemblerTemplates;
import li.cil.oc.common.template.DisassemblyResult;
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
public final class DisassemblerProcessRestartGameTests {
    private static final BlockPos MACHINE = new BlockPos(4112, 80, 4096);
    private static final BlockPos CABLE = MACHINE.west();
    private static final BlockPos CHEST = MACHINE.east();

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.restartDisassembler")) return List.of();
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalStateException("Disassembler probe requires restart phase/world");
        return List.of(new TestFunction("disassemblerRestart", "disassembler_restart_" + phase, "neoopencomputers:empty", 200, 0, true, helper -> {
            final var level = helper.getLevel();
            level.setChunkForced(MACHINE.getX() >> 4, MACHINE.getZ() >> 4, true);
            level.setChunkForced(CABLE.getX() >> 4, CABLE.getZ() >> 4, true);
            helper.runAfterDelay(5, () -> {
                try { run(helper, phase.equals("prepare")); }
                catch (Exception failure) { throw new IllegalStateException("Disassembler restart fixture failed", failure); }
            });
        }));
    }

    private static void run(GameTestHelper helper, boolean prepare) throws Exception {
        try (var ignored = DisassemblerTemplates.register(new DisassemblerTemplate() {
            public String name() { return "disassembler_restart_probe"; }
            public boolean matches(ItemStack stack) { return stack.is(Items.DIAMOND); }
            public ItemStack[] disassemble(ItemStack stack) { return new ItemStack[]{new ItemStack(Items.EMERALD, 2)}; }
            public DisassemblyResult plan(ItemStack stack, ItemStack[] ingredients) {
                final var drop = new ItemStack(Items.IRON_INGOT);
                final var tag = new CompoundTag(); tag.putInt("retained", 42); drop.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                return new DisassemblyResult(disassemble(stack), new ItemStack[]{drop});
            }
        })) {
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_ITEM_COST, 6D, () ->
                NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_TICK_AMOUNT, 3D, () ->
                    NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_BREAK_CHANCE, 0D, () -> runConfigured(helper, prepare))));
        }
        helper.succeed();
    }

    private static void runConfigured(GameTestHelper helper, boolean prepare) {
        final var level = helper.getLevel();
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(MACHINE) && level.isEmptyBlock(CABLE) && level.isEmptyBlock(CHEST), "Disassembler restart requires a fresh world");
            level.setBlockAndUpdate(CABLE, ModBlocks.CABLE.get().defaultBlockState());
            level.setBlockAndUpdate(MACHINE, ModBlocks.DISASSEMBLER.get().defaultBlockState());
            level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(level.getBlockEntity(MACHINE) instanceof DisassemblerBlockEntity && level.getBlockEntity(CABLE) instanceof CableBlockEntity
            && level.getBlockEntity(CHEST) instanceof ChestBlockEntity, "Saved disassembler fixture missing");
        final var machine = (DisassemblerBlockEntity) level.getBlockEntity(MACHINE);
        final var cable = (CableBlockEntity) level.getBlockEntity(CABLE);
        final var chest = (ChestBlockEntity) level.getBlockEntity(CHEST);
        final var connector = (Connector) machine.node();
        final CompoundTag expected;
        if (prepare) {
            machine.setItem(0, new ItemStack(Items.DIAMOND)); tick(helper, machine);
            connector.changeBuffer(3); tick(helper, machine);
            machine.setItem(0, new ItemStack(Items.DIAMOND));
            expected = new CompoundTag(); expected.putString("address", machine.node().address());
        } else {
            final var record = chest.getItem(26).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(record != null, "Disassembler checkpoint record missing"); expected = record.copyTag();
            helper.assertTrue(expected.getString("phase").equals("prepared") && expected.getLong("pid") > 0
                && expected.getLong("pid") != ProcessHandle.current().pid(), "Disassembler did not restore another JVM's checkpoint");
        }
        final var saved = machine.saveWithFullMetadata(level.registryAccess());
        helper.assertTrue(saved.getList("oc:queue", 10).size() == 2 && saved.getDouble("oc:buffer") == 3D
            && machine.getItem(0).is(Items.DIAMOND) && machine.getItem(0).getCount() == 1, "Pending paid work or waiting input changed across restart");
        helper.assertTrue(connector.globalBuffer() == 0D && emeralds(chest) == 0 && immediate(helper, chest) == 1,
            "Checkpoint advanced unpaid work or lost/repeated its immediate return");
        helper.assertTrue(machine.node().address().equals(expected.getString("address")) && machine.node().network() != null
            && machine.node().network() == cable.node().network(), "Disassembler cable network/address not restored");
        if (!prepare) {
            helper.assertTrue(!machine.disassemble() && machine.getItem(0).is(Items.DIAMOND), "New input overwrote the restored job");
            connector.changeBuffer(3); tick(helper, machine);
            helper.assertTrue(emeralds(chest) == 1, "Saved partial energy was lost or counted twice");
            connector.changeBuffer(6); tick(helper, machine); tick(helper, machine);
            helper.assertTrue(emeralds(chest) == 2 && immediate(helper, chest) == 1 && machine.getItem(0).is(Items.DIAMOND),
                "First job lost outputs, repeated immediate contents or consumed waiting input early");
            tick(helper, machine);
            helper.assertTrue(machine.getItem(0).isEmpty() && immediate(helper, chest) == 2,
                "Second waiting input did not start/return its own contents exactly once");
            connector.changeBuffer(12);
            for (int i = 0; i < 7; i++) tick(helper, machine);
            final var finished = machine.saveWithFullMetadata(level.registryAccess());
            helper.assertTrue(emeralds(chest) == 4 && immediate(helper, chest) == 2 && machine.isEmpty() && finished.getList("oc:queue", 10).isEmpty()
                && finished.getDouble("oc:buffer") == 0D && connector.globalBuffer() == 0D, "Two jobs did not finish exactly once for their remaining21 energy");
        }
        expected.putString("phase", prepare ? "prepared" : "verified"); expected.putLong("pid", ProcessHandle.current().pid());
        final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(expected)); chest.setItem(26, paper); chest.setChanged();
        NeoOpenComputers.LOGGER.info("Disassembler restart probe {} in PID {}; paid queue, waiting input and immediate returns preserved", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
    }

    private static int emeralds(ChestBlockEntity chest) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).is(Items.EMERALD)) count += chest.getItem(slot).getCount();
        return count;
    }

    private static int immediate(GameTestHelper helper, ChestBlockEntity chest) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).is(Items.IRON_INGOT)) {
            final var data = chest.getItem(slot).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(data != null && data.copyTag().getInt("retained") == 42, "Immediate contents lost their saved item data");
            count += chest.getItem(slot).getCount();
        }
        return count;
    }

    private static void tick(GameTestHelper helper, DisassemblerBlockEntity machine) {
        DisassemblerBlockEntity.serverTick(helper.getLevel(), MACHINE, machine.getBlockState(), machine);
    }
}

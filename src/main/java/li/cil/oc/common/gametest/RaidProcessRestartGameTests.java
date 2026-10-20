package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class RaidProcessRestartGameTests {
    private static final BlockPos RAID = new BlockPos(208, 80, 160);
    private static final BlockPos COMPUTER = RAID.east();
    private static final BlockPos RECORD = RAID.south(2);
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (phase.isEmpty()) return List.of();
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalArgumentException("Unknown RAID restart phase");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            if (level.hasChunkAt(COMPUTER) && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity computer)
                ((Connector) computer.machine().node()).changeBuffer(10000);
        });
        return List.of(new TestFunction("raidRestart", "raid_restart_" + phase, "neoopencomputers:empty", 600, 0, true, helper -> {
            helper.getLevel().setChunkForced(RAID.getX() >> 4, RAID.getZ() >> 4, true);
            helper.runAfterDelay(2, () -> run(helper, phase.equals("prepare")));
        }));
    }
    private static Component filesystem(GameTestHelper helper, RaidBlockEntity raid) {
        final var nodes = raid.onAnalyze(null, Direction.NORTH, 0, 0, 0);
        helper.assertTrue(nodes.length == 1 && nodes[0] instanceof Component, "Process-restored RAID filesystem missing"); return (Component) nodes[0];
    }
    private static void run(GameTestHelper helper, boolean prepare) {
        final var level = helper.getLevel();
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(RAID) && level.isEmptyBlock(COMPUTER) && level.isEmptyBlock(RECORD), "RAID restart requires a fresh world");
            level.setBlockAndUpdate(RAID, ModBlocks.RAID.get().defaultBlockState());
            level.setBlockAndUpdate(COMPUTER, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(level.getBlockEntity(RAID) instanceof RaidBlockEntity && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity
            && level.getBlockEntity(RECORD) instanceof Container, "RAID restart host/checkpoint missing");
        final var raid = (RaidBlockEntity) level.getBlockEntity(RAID); final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(COMPUTER);
        final var record = (Container) level.getBlockEntity(RECORD);
        if (prepare) {
            for (int slot = 0; slot < 3; slot++) raid.setItem(slot, new ItemStack(ModItems.HDD_TIER1.get()));
            for (int turn = 0; turn < 3; turn++) helper.assertTrue(li.cil.oc.common.item.WrenchItem.rotateBlock(level, RAID, false), "RAID restart fixture could not rotate the actual host");
            helper.assertTrue(raid.getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == Direction.WEST, "RAID restart fixture has wrong front");
            try { filesystem(helper, raid).invoke("setLabel", computer.machine(), "restart-raid"); }
            catch (Exception exception) { helper.fail(exception.toString()); }
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
            computer.setItem(4, cpu); computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local eeprom=component.proxy(component.list('eeprom')())
                assert(eeprom.getData()=='', 'RAID computer rebooted')
                local fs
                for address in component.list('filesystem') do
                  local p=component.proxy(address); if p.getLabel()=='restart-raid' then fs=p end
                end
                assert(fs, 'RAID filesystem missing')
                local address=fs.address; local retained=731
                local h=assert(fs.open('raid.bin','wb')); assert(fs.write(h,'r'..string.char(0,255)..'k')); fs.close(h)
                h=assert(fs.open('raid.bin','rb')); assert(fs.read(h,1)=='r')
                eeprom.setData('raid-waiting')
                repeat until computer.pullSignal()=='resume_raid'
                assert(retained==731 and fs.address==address and fs.getLabel()=='restart-raid', 'RAID local/identity/label lost')
                assert(fs.read(h,3)==string.char(0,255)..'k', 'RAID open offset or bytes lost'); fs.close(h)
                eeprom.setData('raid-resumed')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); ((Connector) computer.machine().node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "RAID native restart computer did not start");
        } else {
            final var checkpoint = record.getItem(0).get(DataComponents.CUSTOM_DATA); helper.assertTrue(checkpoint != null, "RAID checkpoint missing");
            final var data = checkpoint.copyTag();
            helper.assertTrue(data.getString("phase").equals("prepared") && data.getLong("pid") > 0 && data.getLong("pid") != ProcessHandle.current().pid(), "RAID checkpoint was not prepared in another JVM");
            helper.assertTrue(data.getString("raid").equals(raid.node().address()) && data.getString("filesystem").equals(filesystem(helper, raid).address())
                && data.getString("computer").equals(computer.machine().node().address()), "RAID process restart changed saved identities");
            helper.assertTrue(data.getString("facing").equals("west") && raid.getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == Direction.WEST,
                "RAID blockstate front was not retained across process restart");
            helper.assertTrue(computer.machine().isRunning() && computer.machine().signal("resume_raid"), "RAID computer did not retain running state");
        }
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(computer).equals(prepare ? "raid-waiting" : "raid-resumed"), "Native RAID continuation failed: " + computer.machine().lastError()))
            .thenExecute(() -> {
                final var data = new CompoundTag(); data.putString("phase", prepare ? "prepared" : "verified"); data.putLong("pid", ProcessHandle.current().pid());
                data.putString("raid", raid.node().address()); data.putString("filesystem", filesystem(helper, raid).address()); data.putString("computer", computer.machine().node().address());
                data.putString("facing", raid.getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).getSerializedName());
                final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setItem(0, paper); record.setChanged();
                li.cil.oc.NeoOpenComputers.LOGGER.info("Native RAID {} in PID {}", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
            }).thenSucceed();
    }
    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

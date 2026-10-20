package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
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

/** Uses only the isolated world selected by the existing explicit restart profile. */
@GameTestHolder("neoopencomputers")
public final class AdapterUpgradeProcessRestartGameTests {
    private static final BlockPos ADAPTER = new BlockPos(192, 80, 160);
    private static final BlockPos COMPUTER = ADAPTER.east();
    private static final BlockPos RECORD = ADAPTER.south(2);
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (phase.isEmpty()) return List.of();
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalArgumentException("Unknown adapter restart phase");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            if (level.hasChunkAt(COMPUTER) && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity computer)
                ((Connector) computer.machine().node()).changeBuffer(10000);
        });
        return List.of(new TestFunction("adapterUpgradeRestart", "adapter_upgrade_restart_" + phase, "neoopencomputers:empty", 600, 0, true,
            helper -> {
                helper.getLevel().setChunkForced(ADAPTER.getX() >> 4, ADAPTER.getZ() >> 4, true);
                helper.runAfterDelay(2, () -> run(helper, phase.equals("prepare")));
            }));
    }
    private static Node database(GameTestHelper helper, AdapterBlockEntity adapter) {
        for (var node : adapter.onAnalyze(null, Direction.NORTH, 0, 0, 0)) if (node.host() instanceof Database) return node;
        helper.fail("Process-restored adapter did not initialize its database"); return null;
    }
    private static void run(GameTestHelper helper, boolean prepare) {
        final var level = helper.getLevel();
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(ADAPTER) && level.isEmptyBlock(COMPUTER) && level.isEmptyBlock(RECORD), "Adapter probe requires a fresh restart world");
            level.setBlockAndUpdate(ADAPTER, ModBlocks.ADAPTER.get().defaultBlockState());
            level.setBlockAndUpdate(COMPUTER, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(level.getBlockEntity(ADAPTER) instanceof AdapterBlockEntity && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity
            && level.getBlockEntity(RECORD) instanceof Container, "Adapter restart host/checkpoint missing");
        final var adapter = (AdapterBlockEntity) level.getBlockEntity(ADAPTER);
        final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(COMPUTER);
        final var record = (Container) level.getBlockEntity(RECORD);
        if (prepare) {
            adapter.setItem(0, new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()));
            ((Database) database(helper, adapter).host()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
            computer.setItem(4, cpu); computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local eeprom=component.proxy(component.list('eeprom')())
                assert(eeprom.getData()=='', 'adapter computer rebooted')
                local db=component.proxy(assert(component.list('database')(), 'adapter database missing'))
                local address=db.address; local hash=assert(db.computeHash(1)); local retained=731
                eeprom.setData('adapter-waiting')
                repeat until computer.pullSignal()=='resume_adapter_upgrade'
                assert(retained==731 and db.address==address and db.computeHash(1)==hash, 'adapter proxy/catalogue lost')
                assert(db.clear(1)==true and db.computeHash(1)==nil, 'restored adapter database is not writable')
                eeprom.setData('adapter-resumed')
                while true do computer.pullSignal() end
                """));
            ((Connector) computer.machine().node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "Adapter native restart computer did not start");
        } else {
            final var checkpoint = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(checkpoint != null, "Adapter restart checkpoint missing"); final var data = checkpoint.copyTag();
            helper.assertTrue(data.getString("phase").equals("prepared") && data.getLong("pid") > 0 && data.getLong("pid") != ProcessHandle.current().pid(), "Adapter checkpoint was not prepared in another JVM");
            helper.assertTrue(data.getString("adapter").equals(adapter.node().address()) && data.getString("database").equals(database(helper, adapter).address())
                && data.getString("computer").equals(computer.machine().node().address()), "Adapter restart changed saved node identities");
            helper.assertTrue(((Database) database(helper, adapter).host()).getStackInSlot(0).is(Items.IRON_INGOT), "Adapter process restart lost database catalogue");
            helper.assertTrue(computer.machine().isRunning() && computer.machine().signal("resume_adapter_upgrade"), "Adapter computer did not retain running state");
        }
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(computer).equals(prepare ? "adapter-waiting" : "adapter-resumed"),
            "Native adapter continuation failed: " + computer.machine().lastError()))
            .thenExecute(() -> {
                if (!prepare) helper.assertTrue(((Database) database(helper, adapter).host()).getStackInSlot(0).isEmpty(), "Native resumed callback did not update the actual adapter database");
                final var data = new CompoundTag(); data.putString("phase", prepare ? "prepared" : "verified"); data.putLong("pid", ProcessHandle.current().pid());
                data.putString("adapter", adapter.node().address()); data.putString("computer", computer.machine().node().address()); data.putString("database", database(helper, adapter).address());
                final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setItem(0, paper); record.setChanged();
                li.cil.oc.NeoOpenComputers.LOGGER.info("Native adapter upgrade {} in PID {}", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
            }).thenSucceed();
    }
    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

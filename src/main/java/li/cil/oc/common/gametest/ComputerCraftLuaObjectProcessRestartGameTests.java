package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Connector;
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

@GameTestHolder("neoopencomputers")
public final class ComputerCraftLuaObjectProcessRestartGameTests {
    private static final BlockPos ADAPTER = new BlockPos(242, 80, 160);
    private static final BlockPos COMPUTER = ADAPTER.west();
    private static final BlockPos FOREIGN = ADAPTER.east();
    private static final BlockPos RECORD = ADAPTER.south(2);

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final var phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (phase.isEmpty() || !net.neoforged.fml.ModList.get().isLoaded("computercraft")) return List.of();
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalArgumentException("Unknown CC object restart phase");
        return Loaded.tests(phase);
    }

    private static final class Loaded {
        static Collection<TestFunction> tests(String phase) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
                final var level = event.getServer().overworld();
                if (level.hasChunkAt(COMPUTER) && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity computer)
                    ((Connector) computer.machine().node()).changeBuffer(10000);
            });
            return List.of(new TestFunction("ccObjectRestart", "cc_luaobject_restart_" + phase, "neoopencomputers:empty", 600, 0, true, helper -> {
                helper.getLevel().setChunkForced(ADAPTER.getX() >> 4, ADAPTER.getZ() >> 4, true);
                helper.runAfterDelay(2, () -> { try { run(helper, phase.equals("prepare")); } catch (Exception failure) { throw new RuntimeException(failure); } });
            }));
        }

        static void run(GameTestHelper helper, boolean prepare) throws Exception {
            final var level = helper.getLevel();
            AutoCloseable fixture = null;
            TestComputerCraftLuaObjectPeripheral peripheral = null;
            if (prepare) {
                helper.assertTrue(level.isEmptyBlock(ADAPTER) && level.isEmptyBlock(COMPUTER) && level.isEmptyBlock(FOREIGN) && level.isEmptyBlock(RECORD), "CC object probe requires a fresh restart world");
                level.setBlockAndUpdate(FOREIGN, Blocks.DISPENSER.defaultBlockState());
                peripheral = new TestComputerCraftLuaObjectPeripheral(level);
                fixture = TestComputerCraftCapabilities.attach(level, FOREIGN, Direction.WEST, peripheral);
                level.setBlockAndUpdate(ADAPTER, ModBlocks.ADAPTER.get().defaultBlockState());
                level.setBlockAndUpdate(COMPUTER, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
                level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
            }
            helper.assertTrue(level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity && level.getBlockEntity(RECORD) instanceof Container, "CC object restart hosts are missing");
            final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(COMPUTER);
            final var record = (Container) level.getBlockEntity(RECORD);
            if (prepare) {
                final var adapter = (AdapterBlockEntity) level.getBlockEntity(ADAPTER);
                adapter.onLoad(); adapter.refreshNeighbors();
                final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
                ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
                computer.setItem(4, cpu); computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
                computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                    local eeprom=component.proxy(component.list('eeprom')())
                    assert(eeprom.getData()=='','CC object computer rebooted')
                    local p=component.proxy(assert(component.list('oc_lua_object_probe')(),'CC object peripheral missing'))
                    local object=p.object().value; local retainedMethod=object.echo; local retained=731
                    assert(retainedMethod('before')=='before')
                    eeprom.setData('cc-object-waiting')
                    repeat until computer.pullSignal()=='resume_cc_object'
                    assert(retained==731,'CC object VM local did not persist')
                    assert(object.echo==nil,'Restored CC object exposed a live method')
                    local ok,reason=pcall(retainedMethod,'after')
                    assert(not ok and tostring(reason):find('no such method',1,true),'Retained foreign callback did not expire: '..tostring(reason))
                    eeprom.setData('cc-object-resumed')
                    while true do computer.pullSignal() end
                    """));
                computer.onLoad(); Network.joinOrCreateNetwork(computer);
                ((Connector) computer.machine().node()).changeBuffer(10000);
                helper.assertTrue(computer.machine().start(), "CC object native computer did not start");
            } else {
                final var checkpoint = record.getItem(0).get(DataComponents.CUSTOM_DATA);
                helper.assertTrue(checkpoint != null, "CC object restart checkpoint missing");
                final var data = checkpoint.copyTag();
                helper.assertTrue(data.getString("phase").equals("prepared") && data.getLong("pid") > 0 && data.getLong("pid") != ProcessHandle.current().pid(), "CC object was not prepared in another JVM");
                helper.assertTrue(data.getString("computer").equals(computer.machine().node().address()), "CC object computer identity changed");
                helper.assertTrue(computer.machine().isRunning() && computer.machine().signal("resume_cc_object"), "CC object VM did not retain its running state");
            }
            final AutoCloseable preparedFixture = fixture;
            final var preparedPeripheral = peripheral;
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(computer).equals(prepare ? "cc-object-waiting" : "cc-object-resumed"), "CC object continuation failed: " + computer.machine().lastError()))
                .thenExecute(() -> {
                    final var data = new CompoundTag(); data.putString("phase", prepare ? "prepared" : "verified"); data.putLong("pid", ProcessHandle.current().pid()); data.putString("computer", computer.machine().node().address());
                    final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setItem(0, paper); record.setChanged();
                    if (preparedPeripheral != null) preparedPeripheral.close();
                    if (preparedFixture != null) try { preparedFixture.close(); } catch (Exception failure) { throw new RuntimeException(failure); }
                    li.cil.oc.NeoOpenComputers.LOGGER.info("Native CC object {} in PID {}", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
                }).thenSucceed();
        }

        private static String marker(ComputerCaseBlockEntity computer) {
            return new String(ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
        }
    }
}

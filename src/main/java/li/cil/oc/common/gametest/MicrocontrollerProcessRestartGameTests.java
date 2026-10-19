package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/** Opt-in real disk/JVM continuation of the isolated microcontroller networks. */
@GameTestHolder(NeoOpenComputers.MODID)
public final class MicrocontrollerProcessRestartGameTests {
    private static final BlockPos HOST = new BlockPos(4, 80, 12);
    private static final BlockPos RECORD = HOST.south(2);
    private static volatile CompoundTag diskState;

    @GameTestGenerator
    public static Collection<TestFunction> microcontrollerRestartProbe() {
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (phase.isEmpty()) return List.of();
        if (!phase.equals("prepare") && !phase.equals("verify")) throw new IllegalArgumentException("Unknown restart phase");
        if (phase.equals("verify")) {
            NeoForge.EVENT_BUS.addListener((ChunkDataEvent.Load event) -> {
                // Async pre-tick observation: only copy saved NBT, never load/query a world.
                if (event.getChunk().getPos().x != 0 || event.getChunk().getPos().z != 0) return;
                final var entities = event.getData().getList("block_entities", Tag.TAG_COMPOUND);
                for (int index = 0; index < entities.size(); index++) {
                    final var data = entities.getCompound(index);
                    if (data.getInt("x") == HOST.getX() && data.getInt("y") == HOST.getY() && data.getInt("z") == HOST.getZ()
                        && data.getString("id").startsWith(NeoOpenComputers.MODID + ":")) diskState = data.copy();
                }
            });
        }
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            if (level != null && level.hasChunkAt(HOST) && level.getBlockEntity(HOST) instanceof MicrocontrollerBlockEntity host) {
                ((Connector) host.machine().node()).changeBuffer(1000);
            }
        });
        return List.of(new TestFunction("processRestart", "microcontrollerrestart_" + phase, "neoopencomputers:empty", 600, 0, true,
            helper -> run(helper, phase.equals("prepare"))));
    }

    private static void run(final GameTestHelper helper, final boolean prepare) {
        final var world = helper.getLevel();
        world.setChunkForced(0, 0, true);
        if (prepare) {
            helper.assertTrue(world.isEmptyBlock(HOST) && world.isEmptyBlock(RECORD), "Microcontroller checkpoint already exists; use a fresh world");
            world.setBlockAndUpdate(HOST, ModBlocks.MICROCONTROLLER_TIER1.get().defaultBlockState());
            world.setBlockAndUpdate(HOST.east(), ModBlocks.CABLE.get().defaultBlockState());
            world.setBlockAndUpdate(HOST.west(), ModBlocks.CABLE.get().defaultBlockState());
            world.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(world.getBlockEntity(HOST) instanceof MicrocontrollerBlockEntity, "Saved microcontroller missing");
        helper.assertTrue(world.getBlockEntity(RECORD) instanceof ChestBlockEntity, "Saved microcontroller record missing");
        final var host = (MicrocontrollerBlockEntity) world.getBlockEntity(HOST);
        final var record = (ChestBlockEntity) world.getBlockEntity(RECORD);
        if (prepare) {
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
            host.setItem(0, cpu);
            host.setItem(1, new ItemStack(ModItems.MEMORY_TIER1.get()));
            host.setItem(3, new ItemStack(ModItems.NETWORK_CARD.get()));
            host.setItem(2, RobotMovementPersistenceGameTests.eeprom("""
                local eeprom = component.proxy(component.list('eeprom')())
                assert(eeprom.getData() == '', 'unexpected reboot')
                local mcu = component.proxy(component.list('microcontroller')())
                local modem = component.proxy(component.list('modem')())
                local retained = {value = 731}
                assert(mcu.setSideOpen(4, false))
                modem.open(731)
                local fs = component.proxy(computer.tmpAddress())
                local file = assert(fs.open('micro-restart.bin', 'w'))
                assert(fs.write(file, 'a' .. string.char(0,255) .. 'z'))
                fs.close(file)
                file = assert(fs.open('micro-restart.bin', 'r'))
                assert(fs.read(file, 1) == 'a')
                eeprom.setData('prepared')
                repeat until computer.pullSignal() == 'micro_after_process_restart'
                assert(retained.value == 731, 'Lua state lost')
                assert(not mcu.isSideOpen(4) and mcu.isSideOpen(5), 'private control proxy/flags lost')
                assert(modem.isOpen(731), 'modem proxy/port lost')
                assert(fs.address == computer.tmpAddress() and fs.read(file, 3) == string.char(0,255) .. 'z', 'file continuation lost')
                fs.close(file)
                eeprom.setData('verified')
                while true do computer.pullSignal() end
                """));
            host.onLoad();
            ((Connector) host.machine().node()).changeBuffer(1000);
            helper.assertTrue(host.toggleMachine(), "Microcontroller did not start");
        } else {
            final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(data != null && data.copyTag().getString("phase").equals("prepared")
                && data.copyTag().getLong("pid") > 0 && data.copyTag().getLong("pid") != ProcessHandle.current().pid(),
                "Microcontroller checkpoint was not prepared by another process");
            helper.assertTrue(diskState != null, "Did not capture microcontroller disk NBT before ticking");
            final var saved = diskState;
            helper.assertTrue(saved.getCompound("oc:snooper").getDouble("buffer") > 0
                && saved.getCompound("oc:machine").getCompound("node").getDouble("buffer") == 0,
                "Disk did not retain energy in the private buffer");
            final byte[] flags = saved.getByteArray("oc:outputs");
            helper.assertTrue(flags.length == 6 && flags[4] == 0 && flags[5] == 1, "Disk output flags missing before ticking");
            assertAddresses(helper, host, data.copyTag());
            helper.assertTrue(host.machine().isRunning(), "Microcontroller lost running state: " + host.machine().lastError());
            helper.assertTrue(host.machine().signal("micro_after_process_restart"), "Microcontroller rejected resume signal");
            NeoOpenComputers.LOGGER.info("Microcontroller restart disk energy/flags captured before ticking in PID {}", ProcessHandle.current().pid());
        }
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(host).equals(prepare ? "prepared" : "verified"),
                "Microcontroller continuation not ready: " + host.machine().lastError()))
            .thenExecute(() -> {
                final var data = new CompoundTag();
                data.putLong("pid", ProcessHandle.current().pid());
                data.putString("phase", prepare ? "prepared" : "verified");
                storeAddresses(host, data);
                final var paper = new ItemStack(Items.PAPER);
                paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                record.setItem(0, paper);
                record.setChanged();
                NeoOpenComputers.LOGGER.info("Microcontroller restart probe {} in PID {}; normal shutdown will save the world",
                    prepare ? "prepared" : "verified", ProcessHandle.current().pid());
            }).thenSucceed();
    }

    private static void storeAddresses(final MicrocontrollerBlockEntity host, final CompoundTag data) {
        data.putString("machine", host.machine().node().address());
        for (final Direction side : Direction.values()) {
            if (side == host.facing()) continue;
            data.putString("plug" + side.ordinal(), host.sidedNode(side).address());
            data.putString("control" + side.ordinal(), host.onAnalyze(null, side, 0, 0, 0)[0].address());
        }
    }

    private static void assertAddresses(final GameTestHelper helper, final MicrocontrollerBlockEntity host, final CompoundTag saved) {
        final var current = new CompoundTag();
        storeAddresses(host, current);
        for (final String key : current.getAllKeys()) helper.assertTrue(current.getString(key).equals(saved.getString(key)), "Address changed: " + key);
        Network.joinOrCreateNetwork(host);
        helper.assertTrue(host.sidedNode(Direction.WEST).network() != host.sidedNode(Direction.EAST).network()
            && host.sidedNode(Direction.WEST).network() != host.machine().node().network(), "Restored networks are not isolated");
    }

    private static String marker(final MicrocontrollerBlockEntity host) {
        return new String(ItemDriverData.dataTag(host.getItem(2)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

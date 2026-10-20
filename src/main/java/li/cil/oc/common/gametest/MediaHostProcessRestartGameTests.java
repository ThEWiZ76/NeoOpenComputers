package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
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
public final class MediaHostProcessRestartGameTests {
    private static final BlockPos INTERNAL = new BlockPos(160, 80, 160);
    private static final BlockPos RACK = new BlockPos(176, 80, 160);
    private record Host(Machine machine, Container hardware, int eeprom, Container media, int mediaSlot) { }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.restartComputerCraftMedia")) return List.of();
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (!List.of("prepare", "verify").contains(phase) || !net.neoforged.fml.ModList.get().isLoaded("computercraft")) throw new IllegalStateException("CC media host probe requires the explicit CC restart profile");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            for (var pos : List.of(INTERNAL, RACK)) if (level.hasChunkAt(pos)) {
                final var entity = level.getBlockEntity(pos);
                final Machine machine = entity instanceof ComputerCaseBlockEntity computer ? computer.machine()
                    : entity instanceof RackBlockEntity rack && rack.getMountable(0) instanceof li.cil.oc.api.internal.Server server ? server.machine() : null;
                if (machine != null) ((Connector) machine.node()).changeBuffer(10000);
            }
        });
        return List.of(test("case", phase, false), test("rack", phase, true));
    }
    private static TestFunction test(String kind, String phase, boolean rack) {
        return new TestFunction("ccMediaHostsRestart", "cc_media_" + kind + "_restart_" + phase, "neoopencomputers:empty", 600, 0, true,
            helper -> run(helper, rack, phase.equals("prepare")));
    }
    private static void run(GameTestHelper helper, boolean rackHost, boolean prepare) {
        final var pos = rackHost ? RACK : INTERNAL;
        helper.getLevel().setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        // Chunk data is present before Forge has run onLoad and created the rack's mountables.
        helper.runAfterDelay(2, () -> runLoaded(helper, rackHost, prepare));
    }
    private static void runLoaded(GameTestHelper helper, boolean rackHost, boolean prepare) {
        final var level = helper.getLevel(); final var pos = rackHost ? RACK : INTERNAL; final var recordPos = pos.south(2);
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(pos) && level.isEmptyBlock(recordPos), "Media host probe requires a fresh world");
            level.setBlockAndUpdate(pos, (rackHost ? ModBlocks.RACK.get() : ModBlocks.COMPUTER_CASE_TIER3.get()).defaultBlockState());
            level.setBlockAndUpdate(recordPos, Blocks.CHEST.defaultBlockState());
        }
        final Host host;
        if (rackHost) {
            helper.assertTrue(level.getBlockEntity(pos) instanceof RackBlockEntity, "Saved media rack missing");
            final var rack = (RackBlockEntity) level.getBlockEntity(pos);
            if (prepare) { rack.setItem(0, new ItemStack(ModItems.SERVER_TIER1.get())); rack.setItem(1, new ItemStack(ModItems.DISK_DRIVE_MOUNTABLE.get())); }
            helper.assertTrue(rack.getMountable(0) instanceof li.cil.oc.api.internal.Server && rack.getMountable(1) instanceof Container, "Saved rack media/server missing");
            final var server = (li.cil.oc.api.internal.Server) rack.getMountable(0);
            host = new Host(server.machine(), (Container) server, 8, (Container) rack.getMountable(1), 0);
            if (prepare) {
                host.hardware.setItem(2, cpu()); host.hardware.setItem(4, new ItemStack(ModItems.MEMORY_TIER1.get()));
                rack.connect(0, -1, Direction.SOUTH); rack.connect(1, -1, Direction.SOUTH);
            }
        } else {
            helper.assertTrue(level.getBlockEntity(pos) instanceof ComputerCaseBlockEntity, "Saved internal media case missing");
            final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(pos);
            host = new Host(computer.machine(), computer, 9, computer, 7);
            if (prepare) { computer.setItem(8, cpu()); computer.setItem(3, new ItemStack(ModItems.MEMORY_TIER1.get())); }
        }
        final var record = (Container) level.getBlockEntity(recordPos);
        if (prepare) {
            host.media.setItem(host.mediaSlot, ComputerCraftMediaGameTests.hostRestartDisk());
            final String lookup = rackHost ? "return component.proxy(component.proxy(assert(component.list('disk_drive')())).media())"
                : "for address in component.list('filesystem') do local p=component.proxy(address); if p.getLabel()=='CC host restart' then return p end end; error('internal disk missing')";
            host.hardware.setItem(host.eeprom, RobotMovementPersistenceGameTests.eeprom("""
                local function media() %s end
                local eeprom = component.proxy(component.list('eeprom')())
                assert(eeprom.getData() == '', 'unexpected media host reboot')
                local fs = media(); local address=fs.address; local retained=731
                local h=assert(fs.open('host.bin','wb')); assert(fs.write(h,'r'..string.char(0,255)..'k')); fs.close(h)
                h=assert(fs.open('host.bin','rb')); assert(fs.read(h,1)=='r')
                eeprom.setData('host-waiting')
                repeat until computer.pullSignal()=='resume_media_host'
                assert(retained==731 and media().address==address, 'media host identity/local lost')
                assert(fs.read(h,3)==string.char(0,255)..'k', 'media host offset/bytes lost'); fs.close(h)
                eeprom.setData('host-resumed')
                while true do computer.pullSignal() end
                """.formatted(lookup)));
            ((Connector) host.machine.node()).changeBuffer(10000);
            helper.assertTrue(host.machine.start(), "Native media host did not start");
        } else {
            final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(data != null && data.copyTag().getString("phase").equals("prepared") && data.copyTag().getLong("pid") > 0
                && data.copyTag().getLong("pid") != ProcessHandle.current().pid(), "Media host checkpoint was not prepared by another JVM");
        }
        helper.startSequence().thenExecute(() -> {
                if (!prepare) {
                    final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag();
                    final String mediaAddress = ItemDriverData.dataTag(host.media.getItem(host.mediaSlot)).getCompound("node").getString("address");
                    helper.assertTrue(data.getString("machine").equals(host.machine.node().address()) && data.getString("media").equals(mediaAddress),
                        "Media host saved identities changed after world initialization: machine " + host.machine.node().address() + " media " + mediaAddress);
                    helper.assertTrue(host.machine.isRunning() && host.machine.signal("resume_media_host"), "Media host did not retain running state: " + host.machine.lastError());
                }
            }).thenWaitUntil(() -> helper.assertTrue(marker(host).equals(prepare ? "host-waiting" : "host-resumed"), "Media host continuation failed: " + host.machine.lastError()))
            .thenExecute(() -> {
                ComputerCraftMediaGameTests.verifyHostRestartDisk(helper, host.media.getItem(host.mediaSlot));
                final var tag = new CompoundTag(); tag.putString("phase", prepare ? "prepared" : "verified"); tag.putLong("pid", ProcessHandle.current().pid());
                tag.putString("machine", host.machine.node().address()); tag.putString("media", ItemDriverData.dataTag(host.media.getItem(host.mediaSlot)).getCompound("node").getString("address"));
                final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); record.setItem(0, paper); record.setChanged();
                li.cil.oc.NeoOpenComputers.LOGGER.info("CC {} media host {} in PID {}", rackHost ? "rack" : "internal case", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
            }).thenSucceed();
    }
    private static ItemStack cpu() {
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class); return cpu;
    }
    private static String marker(Host host) {
        return new String(ItemDriverData.dataTag(host.hardware.getItem(host.eeprom)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

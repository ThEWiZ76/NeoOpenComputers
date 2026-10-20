package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.item.TabletRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class ComputerCraftPortableMediaGameTests {
    private static final String PROGRAM = """
        local function media()
          for address in component.list('filesystem') do
            local p=component.proxy(address); if p.getLabel()=='CC host restart' then return p end
          end
          error('portable CC disk missing')
        end
        local eeprom=component.proxy(component.list('eeprom')())
        assert(eeprom.getData()=='', 'portable computer rebooted')
        local fs=media(); local address=fs.address; local retained=731
        local h=assert(fs.open('host.bin','wb')); assert(fs.write(h,'r'..string.char(0,255)..'k')); fs.close(h)
        h=assert(fs.open('host.bin','rb')); assert(fs.read(h,1)=='r')
        eeprom.setData('portable-waiting')
        repeat until computer.pullSignal()=='resume_portable_media'
        assert(retained==731 and media().address==address, 'portable local/address lost')
        assert(fs.read(h,3)==string.char(0,255)..'k', 'portable file offset/bytes lost'); fs.close(h)
        eeprom.setData('portable-resumed')
        while true do computer.pullSignal() end
        """;
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) return List.of();
        return List.of(new TestFunction("ccPortableMedia", "cc_tablet_media_native_handoff", "neoopencomputers:ae2_export", 600, 0, true, ComputerCraftPortableMediaGameTests::tablet),
            new TestFunction("ccPortableMedia", "cc_robot_media_native_reload", "neoopencomputers:ae2_export", 600, 0, true, ComputerCraftPortableMediaGameTests::robot),
            new TestFunction("ccPortableMedia", "cc_rack_media_module_detach", "neoopencomputers:ae2_export", 100, 0, true, Loaded::rackDetach));
    }
    private static ItemStack cpu() {
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class); return cpu;
    }
    private static String marker(Machine machine) {
        for (var part : machine.host().internalComponents()) if (part.is(ModItems.EEPROM.get())) return new String(ItemDriverData.dataTag(part).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
        return "";
    }
    private static void tablet(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel(); final var item = ModItems.TABLET.get();
        final var stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), new ItemStack(ModItems.DISK_DRIVE.get()), cpu(), new ItemStack(ModItems.MEMORY_TIER1.get()), RobotMovementPersistenceGameTests.eeprom(PROGRAM));
        item.setComponent(stack, 31, ComputerCraftMediaGameTests.hostRestartDisk()); item.setMaxCharge(stack, 10000); item.setCharge(stack, 8000);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        final TabletRuntime[] runtime = {new TabletRuntime(stack, player)};
        helper.assertTrue(item.getCharge(stack) == 8000 && ((Connector) runtime[0].machine().node()).globalBuffer() == 8000, "Creating native CC media changed initial tablet charge");
        helper.assertTrue(runtime[0].start(), "Native CC-media tablet did not start");
        for (int tick = 1; tick < 600; tick++) helper.runAtTickTime(tick, () -> { if (!runtime[0].isClosed()) runtime[0].tick(); });
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(runtime[0].machine()).equals("portable-waiting"), "Tablet CC media failed: " + runtime[0].machine().lastError()))
            .thenExecute(() -> {
                final String address = runtime[0].machine().node().address(); runtime[0].close(true);
                final var decoded = ItemStack.parse(helper.getLevel().registryAccess(), runtime[0].stack().save(helper.getLevel().registryAccess())).orElseThrow();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, decoded); runtime[0] = new TabletRuntime(decoded, player);
                helper.assertTrue(address.equals(runtime[0].machine().node().address()) && runtime[0].machine().isRunning() && runtime[0].machine().signal("resume_portable_media"), "Tablet media handoff lost running identity");
            }).thenWaitUntil(() -> helper.assertTrue(marker(runtime[0].machine()).equals("portable-resumed"), "Tablet CC-media continuation failed: " + runtime[0].machine().lastError()))
            .thenExecute(() -> {
                runtime[0].save(); ComputerCraftMediaGameTests.verifyHostRestartDisk(helper, item.getComponent(runtime[0].stack(), 31)); runtime[0].close(false);
                helper.getLevel().getServer().getPlayerList().remove(player);
            }).thenSucceed();
    }
    private static void robot(GameTestHelper helper) {
        final var world = helper.getLevel(); final var pos = helper.absolutePos(new BlockPos(3, 2, 3)); final var state = ModBlocks.ROBOT.get().defaultBlockState();
        world.setBlockAndUpdate(pos, state); final RobotBlockEntity[] robot = {(RobotBlockEntity) world.getBlockEntity(pos)};
        RobotMovementPersistenceGameTests.installHardware(helper, robot[0], List.of(cpu(), new ItemStack(ModItems.MEMORY_TIER1.get()), RobotMovementPersistenceGameTests.eeprom(PROGRAM), new ItemStack(ModItems.DISK_DRIVE.get())));
        robot[0].onLoad(); helper.assertTrue(robot[0].canPlaceItem(1, ComputerCraftMediaGameTests.hostRestartDisk()), "Robot container rejected native CC media");
        robot[0].setItem(1, ComputerCraftMediaGameTests.hostRestartDisk()); ((Connector) robot[0].machine().node()).changeBuffer(10000);
        helper.assertTrue(robot[0].machine().start(), "Native CC-media robot did not start");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(robot[0].machine()).equals("portable-waiting"), "Robot CC media failed: " + robot[0].machine().lastError()))
            .thenExecute(() -> {
                final var saved = robot[0].saveWithFullMetadata(world.registryAccess()); world.removeBlockEntity(pos);
                robot[0] = new RobotBlockEntity(pos, state); robot[0].setLevel(world); robot[0].loadWithComponents(saved, world.registryAccess()); world.setBlockEntity(robot[0]); robot[0].onLoad();
            }).thenIdle(2).thenExecute(() -> helper.assertTrue(robot[0].machine().isRunning() && robot[0].machine().signal("resume_portable_media"), "Robot media reload did not retain running state"))
            .thenWaitUntil(() -> helper.assertTrue(marker(robot[0].machine()).equals("portable-resumed"), "Robot CC-media continuation failed: " + robot[0].machine().lastError()))
            .thenExecute(() -> { ComputerCraftMediaGameTests.verifyHostRestartDisk(helper, robot[0].getItem(1)); robot[0].machine().stop(); robot[0].clearContent(); }).thenSucceed();
    }

    private static final class Loaded {
        static void rackDetach(GameTestHelper helper) {
            final var hardware = DiskDriveEjectionGameTests.rackHardware(helper); final var context = hardware.server().machine();
            hardware.drive().setItem(0, ComputerCraftMediaGameTests.hostRestartDisk());
            final var fs = DiskDriveEjectionGameTests.rackFilesystem(helper, hardware.drive()); final String address = fs.address();
            final var stack = hardware.drive().getItem(0);
            final var mount = (dan200.computercraft.api.filesystem.WritableMount) stack.getCapability(dan200.computercraft.api.media.MediaCapability.get()).createDataMount(stack, helper.getLevel());
            try {
                ((Connector) fs).changeBuffer(10000); final byte[] bytes = new byte[]{0, (byte) 128, (byte) 255};
                var handle = fs.invoke("open", context, "kept.bin", "wb")[0]; fs.invoke("write", context, handle, bytes); fs.invoke("close", context, handle);
                final var expired = fs.invoke("open", context, "detach.bin", "wb")[0]; fs.invoke("write", context, expired, bytes);
                final var module = hardware.rack().removeItem(1, 1);
                helper.assertTrue(fs.network() == null, "CC rack detach retained child filesystem");
                boolean rejected = false;
                try { fs.invoke("write", context, expired, new byte[]{42}); } catch (Exception exception) { rejected = true; }
                helper.assertTrue(rejected, "Detached CC file handle remained writable");
                mount.delete("detach.bin"); helper.assertTrue(!mount.exists("detach.bin"), "Detached CC native channel remained locked");
                hardware.rack().setItem(1, module);
                // Removing a whole module clears its rack bus mapping, just as in the rack GUI.
                hardware.rack().connect(1, -1, net.minecraft.core.Direction.SOUTH);
                final var drive = (li.cil.oc.common.component.DiskDriveMountableEnvironment) hardware.rack().getMountable(1);
                final var restored = DiskDriveEjectionGameTests.rackFilesystem(helper, drive); ((Connector) restored).changeBuffer(10000);
                helper.assertTrue(address.equals(restored.address()) && Boolean.FALSE.equals(restored.invoke("exists", context, "detach.bin")[0]), "Rack reinsertion changed identity or recreated a deleted file from a stale write handle");
                handle = restored.invoke("open", context, "kept.bin", "rb")[0];
                helper.assertTrue(java.util.Arrays.equals((byte[]) restored.invoke("read", context, handle, 3)[0], bytes), "Rack detach lost native CC disk contents");
                restored.invoke("close", context, handle); hardware.rack().removeItem(1, 1); mount.delete("kept.bin");
            } catch (Exception exception) { helper.fail("Native CC rack detach failed: " + exception.getMessage()); }
            helper.succeed();
        }
    }
}

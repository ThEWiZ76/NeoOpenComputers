package li.cil.oc.common.gametest;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Arrays;

@GameTestHolder("neoopencomputers")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class DiskDriveEjectionGameTests {
    record Hardware(DiskDriveBlockEntity drive, ComputerCaseBlockEntity computer) { }
    record RackHardware(RackBlockEntity rack, li.cil.oc.api.internal.Server server, net.minecraft.world.Container serverItems,
                        li.cil.oc.common.component.DiskDriveMountableEnvironment drive) { }

    static RackHardware rackHardware(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.RACK.get().defaultBlockState());
        final var rack = (RackBlockEntity) helper.getLevel().getBlockEntity(pos); rack.onLoad();
        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER1.get())); rack.setItem(1, new ItemStack(ModItems.DISK_DRIVE_MOUNTABLE.get()));
        final var server = (li.cil.oc.api.internal.Server) rack.getMountable(0);
        rack.connect(0, -1, net.minecraft.core.Direction.SOUTH); rack.connect(1, -1, net.minecraft.core.Direction.SOUTH);
        return new RackHardware(rack, server, (net.minecraft.world.Container) server,
            (li.cil.oc.common.component.DiskDriveMountableEnvironment) rack.getMountable(1));
    }
    static Component rackFilesystem(GameTestHelper helper, li.cil.oc.common.component.DiskDriveMountableEnvironment drive) {
        final var nodes = drive.onAnalyze(null, net.minecraft.core.Direction.NORTH, 0, 0, 0);
        helper.assertTrue(nodes != null && nodes.length == 1 && nodes[0] instanceof Component, "Rack drive has no media filesystem");
        return (Component) nodes[0];
    }

    static Hardware hardware(GameTestHelper helper) {
        final var world = helper.getLevel(); final var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        world.setBlockAndUpdate(pos, ModBlocks.DISK_DRIVE.get().defaultBlockState());
        world.setBlockAndUpdate(pos.west(), ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var drive = (DiskDriveBlockEntity) world.getBlockEntity(pos);
        final var computer = (ComputerCaseBlockEntity) world.getBlockEntity(pos.west());
        drive.onLoad(); computer.onLoad(); Network.joinOrCreateNetwork(computer);
        ((Connector) computer.node()).changeBuffer(10000);
        return new Hardware(drive, computer);
    }
    static Component filesystem(GameTestHelper helper, Hardware hardware) {
        for (var node : hardware.drive.node().reachableNodes()) if (node instanceof Component component && component.name().equals("filesystem")) return component;
        helper.fail("Inserted disk did not expose a filesystem"); return null;
    }

    @GameTest(template = "ae2_export")
    public static void floppySurvivesEveryInventoryRemoval(GameTestHelper helper) throws Exception {
        final var hardware = hardware(helper); final var context = hardware.computer.machine();
        final byte[] expected = new byte[]{0, (byte) 128, (byte) 255};
        for (int path = 0; path < 4; path++) {
            hardware.drive.setItem(0, new ItemStack(ModItems.FLOPPY.get()));
            final var original = filesystem(helper, hardware); final var address = original.address();
            var handle = original.invoke("open", context, "ejected.bin", "wb")[0];
            original.invoke("write", context, handle, expected); original.invoke("close", context, handle);
            final ItemStack removed;
            if (path == 0) removed = hardware.drive.removeItem(0, 1);
            else if (path == 1) removed = hardware.drive.removeItemNoUpdate(0);
            else {
                removed = hardware.drive.getItem(0);
                if (path == 2) hardware.drive.setItem(0, ItemStack.EMPTY); else hardware.drive.clearContent();
            }
            hardware.drive.setItem(0, removed);
            final var restored = filesystem(helper, hardware);
            helper.assertTrue(address.equals(restored.address()), "Disk identity lost during removal path " + path);
            helper.assertTrue(Boolean.TRUE.equals(restored.invoke("exists", context, "ejected.bin")[0]), "Disk contents lost during removal path " + path);
            handle = restored.invoke("open", context, "ejected.bin", "rb")[0];
            helper.assertTrue(Arrays.equals((byte[]) restored.invoke("read", context, handle, 3)[0], expected), "Ejected disk binary data changed");
            restored.invoke("close", context, handle); hardware.drive.clearContent();
        }
        helper.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void rackFloppySurvivesEveryInventoryRemoval(GameTestHelper helper) throws Exception {
        final var hardware = rackHardware(helper); final var drive = hardware.drive; final var context = hardware.server.machine();
        final byte[] expected = new byte[]{0, (byte) 128, (byte) 255};
        for (int path = 0; path < 4; path++) {
            drive.setItem(0, new ItemStack(ModItems.FLOPPY.get()));
            final var original = rackFilesystem(helper, drive); final var address = original.address(); ((Connector) original).changeBuffer(10000);
            var handle = original.invoke("open", context, "ejected.bin", "wb")[0];
            original.invoke("write", context, handle, expected); original.invoke("close", context, handle);
            final ItemStack removed;
            if (path == 0) removed = drive.removeItem(0, 1);
            else if (path == 1) removed = drive.removeItemNoUpdate(0);
            else { removed = drive.getItem(0); if (path == 2) drive.setItem(0, ItemStack.EMPTY); else drive.clearContent(); }
            drive.setItem(0, removed);
            final var restored = rackFilesystem(helper, drive); ((Connector) restored).changeBuffer(10000);
            helper.assertTrue(address.equals(restored.address()), "Rack disk identity lost during removal path " + path);
            helper.assertTrue(Boolean.TRUE.equals(restored.invoke("exists", context, "ejected.bin")[0]), "Rack disk contents lost during removal path " + path);
            handle = restored.invoke("open", context, "ejected.bin", "rb")[0];
            helper.assertTrue(Arrays.equals((byte[]) restored.invoke("read", context, handle, 3)[0], expected), "Rack disk bytes changed");
            restored.invoke("close", context, handle); drive.clearContent();
        }
        helper.succeed();
    }

    @GameTest(template = "ae2_export", timeoutTicks = 600)
    public static void nativeRackServerUsesConfiguredMainBus(GameTestHelper helper) {
        final var hardware = rackHardware(helper);
        hardware.drive.setItem(0, new ItemStack(ModItems.FLOPPY.get()));
        final var filesystem = rackFilesystem(helper, hardware.drive);
        helper.assertTrue(filesystem.canBeSeenFrom(hardware.server.machine().node()), "Configured rack main bus is disconnected from its server machine");
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
        hardware.serverItems.setItem(2, cpu); hardware.serverItems.setItem(4, new ItemStack(ModItems.MEMORY_TIER1.get()));
        hardware.serverItems.setItem(8, RobotMovementPersistenceGameTests.eeprom("""
            local drive = component.proxy(assert(component.list('disk_drive')(), 'rack drive missing'))
            local fs = component.proxy(assert(drive.media()))
            local h = assert(fs.open('rack.bin','wb'))
            assert(fs.write(h,string.char(0,128,255))); fs.close(h)
            h=assert(fs.open('rack.bin','rb')); assert(fs.read(h,3)==string.char(0,128,255)); fs.close(h)
            component.proxy(component.list('eeprom')()).setData('rack-bus-verified')
            while true do computer.pullSignal() end
            """));
        ((Connector) hardware.server.machine().node()).changeBuffer(10000);
        helper.assertTrue(hardware.server.machine().start(), "Native rack computer did not start");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(new String(li.cil.oc.common.item.ItemDriverData.dataTag(hardware.serverItems.getItem(8))
                .getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8).equals("rack-bus-verified"), "Native rack program failed: " + hardware.server.machine().lastError()))
            .thenExecute(() -> {
                hardware.server.machine().stop(); hardware.drive.clearContent();
            }).thenSucceed();
    }

    @GameTest(template = "ae2_export")
    public static void internalCaseStorageSurvivesEveryInventoryRemoval(GameTestHelper helper) throws Exception {
        final var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER3.get().defaultBlockState());
        final var computer = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos); computer.onLoad();
        verifyInternalRemoval(helper, computer, computer.machine(), 7, () -> new ItemStack(ModItems.FLOPPY.get()));
        verifyInternalRemoval(helper, computer, computer.machine(), 5, () -> new ItemStack(ModItems.HDD_TIER1.get()));
        helper.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void internalServerStorageSurvivesEveryInventoryRemoval(GameTestHelper helper) throws Exception {
        final var hardware = rackHardware(helper);
        verifyInternalRemoval(helper, hardware.serverItems, hardware.server.machine(), 6, () -> new ItemStack(ModItems.HDD_TIER1.get()));
        helper.succeed();
    }

    static void verifyInternalRemoval(GameTestHelper helper, net.minecraft.world.Container inventory, li.cil.oc.api.machine.Machine context,
                                      int slot, java.util.function.Supplier<ItemStack> factory) throws Exception {
        final byte[] expected = new byte[]{0, (byte) 128, (byte) 255};
        for (int path = 0; path < 4; path++) {
            inventory.setItem(slot, factory.get()); final var original = internalFilesystem(helper, context, slot); ((Connector) original).changeBuffer(10000);
            final String address = original.address(); var handle = original.invoke("open", context, "internal.bin", "wb")[0];
            original.invoke("write", context, handle, expected); original.invoke("close", context, handle);
            final ItemStack removed;
            if (path == 0) removed = inventory.removeItem(slot, 1);
            else if (path == 1) removed = inventory.removeItemNoUpdate(slot);
            else { removed = inventory.getItem(slot); if (path == 2) inventory.setItem(slot, ItemStack.EMPTY); else inventory.clearContent(); }
            inventory.setItem(slot, removed); final var restored = internalFilesystem(helper, context, slot); ((Connector) restored).changeBuffer(10000);
            helper.assertTrue(address.equals(restored.address()), "Internal storage identity lost in slot " + slot + " removal path " + path);
            helper.assertTrue(Boolean.TRUE.equals(restored.invoke("exists", context, "internal.bin")[0]), "Internal storage contents lost in slot " + slot + " removal path " + path);
            handle = restored.invoke("open", context, "internal.bin", "rb")[0];
            helper.assertTrue(Arrays.equals((byte[]) restored.invoke("read", context, handle, 3)[0], expected), "Internal storage bytes changed");
            restored.invoke("close", context, handle); inventory.clearContent();
        }
    }

    static Component internalFilesystem(GameTestHelper helper, li.cil.oc.api.machine.Machine context, int slot) {
        for (var node : context.node().reachableNodes()) if (node instanceof Component component && component.name().equals("filesystem")
            && context.host().componentSlot(component.address()) == slot) return component;
        helper.fail("Installed storage slot " + slot + " has no filesystem"); return null;
    }
}

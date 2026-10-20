package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import li.cil.oc.common.item.HardDiskDriveItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class RaidStorageGameTests {
    private static final byte[] BYTES = new byte[]{0, (byte) 128, (byte) 255, 42};
    private record Hardware(RaidBlockEntity raid, ComputerCaseBlockEntity computer) { }
    private static Hardware hardware(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(pos.west(), ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.RAID.get().defaultBlockState());
        final var computer = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos.west()); computer.onLoad();
        return new Hardware((RaidBlockEntity) helper.getLevel().getBlockEntity(pos), computer);
    }
    private static Component filesystem(GameTestHelper helper, RaidBlockEntity raid) {
        final var nodes = raid.onAnalyze(null, Direction.NORTH, 0, 0, 0);
        helper.assertTrue(nodes.length == 1 && nodes[0] instanceof Component, "Complete RAID has no filesystem"); return (Component) nodes[0];
    }
    private static void fill(Hardware hardware) {
        for (int slot = 0; slot < 3; slot++) hardware.raid.setItem(slot, new ItemStack(ModItems.HDD_TIER1.get()));
    }
    private static Object write(GameTestHelper helper, Hardware hardware) throws Exception {
        final var fs = filesystem(helper, hardware.raid); ((Connector) hardware.computer.node()).changeBuffer(10000);
        final var context = hardware.computer.machine(); var h = fs.invoke("open", context, "array.bin", "wb")[0];
        fs.invoke("write", context, h, BYTES); fs.invoke("close", context, h);
        h = fs.invoke("open", context, "array.bin", "rb")[0]; fs.invoke("read", context, h, 1); return h;
    }
    private static ItemStack seeded(Hardware hardware) throws Exception {
        final var stack = new ItemStack(ModItems.HDD_TIER1.get()); final var environment = Driver.driverFor(stack).createEnvironment(stack, hardware.raid);
        hardware.computer.node().connect(environment.node()); ((Connector) hardware.computer.node()).changeBuffer(10000);
        final var fs = (Component) environment.node(); final var context = hardware.computer.machine();
        fs.invoke("makeDirectory", context, "old"); final var handle = fs.invoke("open", context, "old/before.bin", "wb")[0];
        fs.invoke("write", context, handle, BYTES); fs.invoke("close", context, handle); environment.save(new CompoundTag()); environment.node().remove(); return stack;
    }
    private static boolean oldFile(Hardware hardware, ItemStack disk) throws Exception {
        final var environment = Driver.driverFor(disk).createEnvironment(disk, hardware.raid);
        try { return Boolean.TRUE.equals(((Component) environment.node()).invoke("exists", hardware.computer.machine(), "old/before.bin")[0]); }
        finally { environment.node().remove(); }
    }
    @GameTest(template = "ae2_export")
    public static void raidFormatsIndividualDisksOnlyWhenComplete(GameTestHelper helper) throws Exception {
        final var hardware = hardware(helper);
        for (int slot = 0; slot < 2; slot++) hardware.raid.setItem(slot, seeded(hardware));
        helper.assertTrue(oldFile(hardware, hardware.raid.getItem(0)), "Incomplete RAID unexpectedly formatted a disk");
        hardware.raid.setItem(2, seeded(hardware));
        for (int slot = 0; slot < 3; slot++) helper.assertTrue(!oldFile(hardware, hardware.raid.getItem(slot)), "Completed RAID retained previous individual HDD data");
        hardware.raid.clearContent(); helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void raidNormalizesRawAndLockedDisks(GameTestHelper helper) throws Exception {
        final var hardware = hardware(helper);
        for (int slot = 0; slot < 3; slot++) {
            final var disk = new ItemStack(ModItems.HDD_TIER1.get()); final var data = HardDiskDriveItem.stackDataTag(disk);
            HardDiskDriveItem.setUnmanaged(data, slot == 0);
            if (slot == 0) {
                final var raw = Driver.driverFor(disk).createEnvironment(disk, hardware.raid);
                hardware.computer.node().connect(raw.node()); ((Connector) hardware.computer.node()).changeBuffer(10000);
                ((Component) raw.node()).invoke("writeByte", hardware.computer.machine(), 1, 42);
                raw.save(new CompoundTag()); raw.node().remove();
            }
            HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(disk), "fixture-owner"); hardware.raid.setItem(slot, disk);
        }
        for (int slot = 0; slot < 3; slot++) helper.assertTrue(!HardDiskDriveItem.isUnmanaged(hardware.raid.getItem(slot)) && !HardDiskDriveItem.isLocked(hardware.raid.getItem(slot)), "RAID kept raw mode or lock on its member HDD");
        final var removed = hardware.raid.removeItem(0, 1); HardDiskDriveItem.setUnmanaged(removed, true);
        final var raw = Driver.driverFor(removed).createEnvironment(removed, hardware.raid);
        try {
            hardware.computer.node().connect(raw.node()); ((Connector) hardware.computer.node()).changeBuffer(10000);
            helper.assertTrue(((Number) ((Component) raw.node()).invoke("readByte", hardware.computer.machine(), 1)[0]).intValue() == 0, "Switching a formatted RAID member back to raw mode resurrected old bytes");
        } finally { raw.node().remove(); }
        hardware.raid.clearContent(); helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void raidUnloadBeforeSaveRetainsFilesIdentityAndOffset(GameTestHelper helper) throws Exception {
        final var hardware = hardware(helper); fill(hardware); final var handle = write(helper, hardware); final var old = filesystem(helper, hardware.raid);
        hardware.raid.onChunkUnloaded(); final var saved = hardware.raid.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var pos = hardware.raid.getBlockPos(); final var state = hardware.raid.getBlockState(); helper.getLevel().removeBlockEntity(pos);
        final var restored = new RaidBlockEntity(pos, state); restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel()); helper.getLevel().setBlockEntity(restored); restored.onLoad();
        final var fs = filesystem(helper, restored); ((Connector) hardware.computer.node()).changeBuffer(10000);
        helper.assertTrue(old.network() == null && old.address().equals(fs.address()), "RAID unload/save lost filesystem identity");
        helper.assertTrue(java.util.Arrays.equals((byte[]) fs.invoke("read", hardware.computer.machine(), handle, 3)[0], new byte[]{(byte) 128, (byte) 255, 42}), "RAID unload/save lost open offset or bytes");
        fs.invoke("close", hardware.computer.machine(), handle); restored.clearContent(); helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void raidLoadDisposesPreviousFilesystem(GameTestHelper helper) throws Exception {
        final var hardware = hardware(helper); fill(hardware); write(helper, hardware); final var old = filesystem(helper, hardware.raid);
        final var saved = hardware.raid.saveWithFullMetadata(helper.getLevel().registryAccess()); hardware.raid.load(saved);
        helper.assertTrue(old.network() == null, "RAID reload left the previous filesystem connected");
        helper.assertTrue(old.address().equals(filesystem(helper, hardware.raid).address()), "RAID reload changed saved filesystem identity"); hardware.raid.clearContent(); helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void raidDiskRemovalDestroysPreviousArray(GameTestHelper helper) throws Exception {
        final var hardware = hardware(helper); fill(hardware);
        for (int path = 0; path < 4; path++) {
            final var handle = write(helper, hardware); final var old = filesystem(helper, hardware.raid); ItemStack disk = hardware.raid.getItem(0);
            if (path == 0) disk = hardware.raid.removeItem(0, 1);
            else if (path == 1) disk = hardware.raid.removeItemNoUpdate(0);
            else if (path == 2) hardware.raid.setItem(0, ItemStack.EMPTY);
            else hardware.raid.clearContent();
            helper.assertTrue(old.network() == null && Boolean.FALSE.equals(old.invoke("exists", hardware.computer.machine(), "array.bin")[0]), "Disk removal disconnected but did not erase the old RAID array");
            boolean closed = false; try { old.invoke("read", hardware.computer.machine(), handle, 1); } catch (Exception exception) { closed = true; }
            helper.assertTrue(closed, "Destroyed RAID retained an open file handle");
            if (path == 3) fill(hardware); else hardware.raid.setItem(0, disk);
            final var fresh = filesystem(helper, hardware.raid);
            helper.assertTrue(!old.address().equals(fresh.address()) && Boolean.FALSE.equals(fresh.invoke("exists", hardware.computer.machine(), "array.bin")[0]), "Reassembled RAID reused old array identity/data");
        }
        hardware.raid.clearContent(); helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void raidSameInstalledStackIsNotAReplacement(GameTestHelper helper) throws Exception {
        final var hardware = hardware(helper); fill(hardware); write(helper, hardware); final var old = filesystem(helper, hardware.raid);
        hardware.raid.setItem(0, hardware.raid.getItem(0));
        helper.assertTrue(filesystem(helper, hardware.raid) == old && Boolean.TRUE.equals(old.invoke("exists", hardware.computer.machine(), "array.bin")[0]), "Setting the identical installed stack destroyed the RAID");
        hardware.raid.clearContent(); helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void raidComparatorTracksCompleteDiskSet(GameTestHelper helper) {
        final var hardware = hardware(helper); final var state = hardware.raid.getBlockState();
        helper.assertTrue(state.hasAnalogOutputSignal() && state.getAnalogOutputSignal(helper.getLevel(), hardware.raid.getBlockPos()) == 0, "Empty RAID lacks upstream comparator output");
        fill(hardware); helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), hardware.raid.getBlockPos()) == 15, "Complete RAID comparator is not 15");
        hardware.raid.removeItem(0, 1); helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), hardware.raid.getBlockPos()) == 0, "Incomplete RAID comparator is not 0");
        hardware.raid.clearContent(); helper.succeed();
    }
}

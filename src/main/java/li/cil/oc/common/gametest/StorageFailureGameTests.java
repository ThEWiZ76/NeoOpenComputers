package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.NativeVolumeStore;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import li.cil.oc.common.item.HardDiskDriveItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class StorageFailureGameTests {
    private static final byte[] BYTES = {97, 0, (byte) 128, (byte) 255, 42};

    @GameTest(template = "empty")
    public static void failedBufferedEjectRetainsMediaAndDirtyWriterUntilRepair(GameTestHelper helper) throws Exception {
        helper.assertTrue(ModSettings.bufferChanges(), "This fixture requires buffered native media");
        final var id = "20261020-0000-4000-8000-000000000006";
        final var root = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers/" + id);
        helper.assertTrue(!Files.exists(root), "Preserve an existing write failure fixture");
        Files.createDirectories(root);
        final var file = root.resolve("probe.bin");
        Files.write(file, new byte[]{1, 2, 3});
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(1000));
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.DISK_DRIVE.get().defaultBlockState());
        final var drive = (DiskDriveBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var stack = new ItemStack(ModItems.FLOPPY.get());
        final var data = HardDiskDriveItem.stackDataTag(stack);
        final var node = new CompoundTag(); node.putString("address", id); data.put("node", node);
        final var fsTag = new CompoundTag(); fsTag.putString(NativeVolumeStore.STORAGE_TAG, id); data.put("fs", fsTag);
        try {
            drive.setItem(0, stack);
            final var component = (Component) drive.onAnalyze(null, null, 0, 0, 0)[0];
            final var fs = ((li.cil.oc.common.FileSystemEnvironment) component.host()).fileSystem();
            final int writer = fs.open("probe.bin", li.cil.oc.api.fs.Mode.Append);
            fs.getHandle(writer).write(new byte[]{4});
            final var beforeEject = drive.getItem(0).copy();
            Files.write(file, new byte[]{8});
            boolean rejected = false;
            try { drive.removeItemNoUpdate(0); } catch (RuntimeException expected) { rejected = true; }
            helper.assertTrue(rejected, "Eject accepted a failed buffered save");
            helper.assertTrue(ItemStack.isSameItemSameComponents(beforeEject, drive.getItem(0)), "Failed eject removed media or published uncommitted item metadata");
            helper.assertTrue(Arrays.equals(new byte[]{8}, Files.readAllBytes(file)), "Failed save overwrote external content");
            fs.getHandle(writer).write(new byte[]{5});
            Files.write(file, new byte[]{1, 2, 3});
            Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(1000));
            final var ejected = drive.removeItemNoUpdate(0);
            helper.assertTrue(ejected.getCount() == 1 && drive.getItem(0).isEmpty(), "Repaired eject lost or duplicated media");
            helper.assertTrue(Arrays.equals(new byte[]{1, 2, 3, 4, 5}, Files.readAllBytes(file)), "Repaired eject lost buffered writes");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(root); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void corruptRawVolumeCannotDiscardItsDiskDriveOrSnapshot(GameTestHelper helper) throws Exception {
        final var id = "20261020-0000-4000-8000-000000000001";
        final var path = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers/" + id + ".bin");
        helper.assertTrue(!Files.exists(path), "Preserve an existing raw failure fixture");
        Files.createDirectories(path.getParent()); Files.write(path, new byte[]{1, 2, 3});
        final var stack = brokenDisk(new ItemStack(ModItems.FLOPPY.get()), id, true);
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.DISK_DRIVE.get().defaultBlockState());
        final var state = helper.getLevel().getBlockState(pos);
        final var original = (DiskDriveBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var items = NonNullList.withSize(1, ItemStack.EMPTY); items.set(0, stack);
        ContainerHelper.saveAllItems(saved, items, helper.getLevel().registryAccess());
        final var diskState = HardDiskDriveItem.stackDataTag(stack).copy(); diskState.putString("fixture:state", "keep host snapshot"); saved.put("disk", diskState);
        try {
            helper.getLevel().removeBlockEntity(pos);
            final var loaded = BlockEntity.loadStatic(pos, state, saved.copy(), helper.getLevel().registryAccess());
            helper.assertTrue(loaded instanceof DiskDriveBlockEntity, "Corrupt raw volume caused Minecraft to discard the disk-drive block entity");
            final var drive = (DiskDriveBlockEntity) loaded;
            drive.setLevel(helper.getLevel()); helper.getLevel().setBlockEntity(drive); drive.onLoad();
            helper.assertTrue(ItemStack.isSameItemSameComponents(stack, drive.getItem(0)), "Failed media mount changed or removed the floppy");
            helper.assertTrue(drive.media(null, null)[0] == null, "Corrupt raw volume supplied an empty replacement component");
            helper.assertTrue(diskState.equals(drive.saveWithFullMetadata(helper.getLevel().registryAccess()).getCompound("disk")), "Failed media mount discarded its deferred host snapshot");
            helper.assertTrue(Arrays.equals(new byte[]{1, 2, 3}, Files.readAllBytes(path)), "Failed raw mount overwrote corrupt bytes");
            final var beforeEject = drive.getItem(0).copy();
            final var ejected = drive.removeItemNoUpdate(0);
            helper.assertTrue(diskState.equals(HardDiskDriveItem.stackDataTag(ejected)), "Ejecting failed media lost its latest host snapshot");
            helper.assertTrue(!HardDiskDriveItem.stackDataTag(beforeEject).contains("fixture:state"), "Failed-media writeback mutated an older item snapshot");
            final var restoredBytes = new byte[ModSettings.floppySize() * 1024]; restoredBytes[0] = 97;
            try (var stream = new GZIPOutputStream(Files.newOutputStream(path))) { stream.write(restoredBytes); }
            drive.setItem(0, ejected);
            final var nodes = drive.onAnalyze(null, null, 0, 0, 0);
            helper.assertTrue(nodes != null && ((Number) ((Component) nodes[0]).invoke("readByte", null, 1)[0]).intValue() == 97, "Repaired raw volume did not recover on remount");
            drive.clearContent();
        } finally { Files.deleteIfExists(path); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingManagedVolumeCannotAbortComputerRestore(GameTestHelper helper) throws Exception {
        final var id = "20261020-0000-4000-8000-000000000002";
        final var path = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers/" + id);
        helper.assertTrue(!Files.exists(path), "Preserve an existing managed failure fixture");
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var original = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        original.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        original.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        original.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"));
        final var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var stack = brokenDisk(new ItemStack(ModItems.HDD_TIER1.get()), id, false);
        final var items = NonNullList.withSize(original.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) items.set(slot, original.getItem(slot).copy());
        items.set(ComputerCaseBlockEntity.SLOT_HDD, stack); ContainerHelper.saveAllItems(saved, items, helper.getLevel().registryAccess());
        final var state = original.getBlockState(); helper.getLevel().removeBlockEntity(pos);
        final var loaded = BlockEntity.loadStatic(pos, state, saved.copy(), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof ComputerCaseBlockEntity, "Missing disk discarded the computer block entity");
        final var computer = (ComputerCaseBlockEntity) loaded;
        computer.setLevel(helper.getLevel());
        try { computer.onLoad(); } catch (RuntimeException failure) { helper.fail("Missing managed disk aborted computer onLoad: " + failure); }
        helper.getLevel().setBlockEntity(computer);
        helper.assertTrue(ItemStack.isSameItemSameComponents(stack, computer.getItem(ComputerCaseBlockEntity.SLOT_HDD)), "Failed computer mount changed its HDD data");
        helper.assertTrue(!Files.exists(path), "Failed computer mount created an empty managed volume");
        final var roundtrip = computer.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var restored = BlockEntity.loadStatic(pos, state, roundtrip.copy(), helper.getLevel().registryAccess());
        helper.assertTrue(restored instanceof ComputerCaseBlockEntity && ItemStack.isSameItemSameComponents(stack, ((ComputerCaseBlockEntity) restored).getItem(ComputerCaseBlockEntity.SLOT_HDD)), "Computer save after failed mount lost its inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingArrayVolumeRetainsRaidMembersAndDeferredState(GameTestHelper helper) throws Exception {
        final var id = "20261020-0000-4000-8000-000000000003";
        final var path = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers/" + id);
        helper.assertTrue(!Files.exists(path), "Preserve an existing RAID failure fixture");
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.RAID.get().defaultBlockState());
        final var original = (RaidBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var items = NonNullList.withSize(3, ItemStack.EMPTY);
        for (int slot = 0; slot < 3; slot++) {
            final var member = new ItemStack(ModItems.HDD_TIER1.get());
            final var env = Driver.driverFor(member).createEnvironment(member, null); Network.joinNewNetwork(env.node());
            try { env.save(new CompoundTag()); } finally { env.node().remove(); }
            items.set(slot, member);
        }
        ContainerHelper.saveAllItems(saved, items, helper.getLevel().registryAccess());
        final var array = new CompoundTag(); final var node = new CompoundTag(); node.putString("address", id); array.put("node", node);
        final var fs = new CompoundTag(); fs.putString(NativeVolumeStore.STORAGE_TAG, id); fs.put("input", new ListTag()); fs.put("output", new ListTag()); array.put("fs", fs);
        saved.put("filesystem", array);
        final var state = original.getBlockState(); helper.getLevel().removeBlockEntity(pos);
        final var loaded = BlockEntity.loadStatic(pos, state, saved.copy(), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof RaidBlockEntity, "Missing array discarded its RAID host");
        final var raid = (RaidBlockEntity) loaded; raid.setLevel(helper.getLevel());
        try {
            try { raid.onLoad(); } catch (RuntimeException failure) { helper.fail("Missing array aborted RAID onLoad: " + failure); }
            helper.getLevel().setBlockEntity(raid);
            for (int slot = 0; slot < 3; slot++) helper.assertTrue(ItemStack.isSameItemSameComponents(items.get(slot), raid.getItem(slot)), "Failed array restore changed member " + slot);
            helper.assertTrue(raid.onAnalyze(null, null, 0, 0, 0)[0] == null && !Files.exists(path), "Missing RAID supplied an empty replacement volume");
            helper.assertTrue(array.equals(raid.saveWithFullMetadata(helper.getLevel().registryAccess()).getCompound("filesystem")), "Failed RAID restore discarded its pending array snapshot");
            Files.createDirectories(path); Files.write(path.resolve("probe.bin"), BYTES); raid.onLoad();
            final var component = (Component) raid.onAnalyze(null, null, 0, 0, 0)[0];
            ((Connector) component).setLocalBufferSize(10000); ((Connector) component).changeBuffer(10000);
            final var reader = component.invoke("open", null, "probe.bin", "rb")[0];
            helper.assertTrue(Arrays.equals(BYTES, (byte[]) component.invoke("read", null, reader, 16)[0]), "Repaired array did not recover its bytes");
            component.invoke("close", null, reader); raid.clearContent();
        } finally { Files.deleteIfExists(path.resolve("probe.bin")); Files.deleteIfExists(path); }
        helper.succeed();
    }

    private static ItemStack brokenDisk(ItemStack stack, String id, boolean raw) {
        HardDiskDriveItem.setUnmanaged(stack, raw);
        final var data = HardDiskDriveItem.stackDataTag(stack); final var node = new CompoundTag(); node.putString("address", id); data.put("node", node);
        if (raw) data.putString(NativeVolumeStore.RAW_STORAGE_TAG, id + ".bin");
        else { final var fs = new CompoundTag(); fs.putString(NativeVolumeStore.STORAGE_TAG, id); data.put("fs", fs); }
        data.putString("oc:lock", "Keep owner"); data.putString("oc:fs.label", "Keep label"); data.putInt("headPos", 5);
        return stack;
    }

    @GameTest(template = "empty")
    public static void unavailableRaidMemberCannotCreateAnEmptyArrayOrLoseItsLock(GameTestHelper helper) throws Exception {
        final var id = "20261020-0000-4000-8000-000000000004";
        final var path = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers/" + id);
        helper.assertTrue(!Files.exists(path), "Preserve an existing member failure fixture");
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.RAID.get().defaultBlockState());
        final var original = (RaidBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var items = NonNullList.withSize(3, ItemStack.EMPTY);
        final var bad = brokenDisk(new ItemStack(ModItems.HDD_TIER1.get()), id, false); items.set(0, bad);
        items.set(1, new ItemStack(ModItems.HDD_TIER1.get())); items.set(2, new ItemStack(ModItems.HDD_TIER1.get()));
        ContainerHelper.saveAllItems(saved, items, helper.getLevel().registryAccess());
        final var state = original.getBlockState(); helper.getLevel().removeBlockEntity(pos);
        final var loaded = (RaidBlockEntity) BlockEntity.loadStatic(pos, state, saved.copy(), helper.getLevel().registryAccess());
        loaded.setLevel(helper.getLevel()); loaded.onLoad(); helper.getLevel().setBlockEntity(loaded);
        helper.assertTrue(loaded.onAnalyze(null, null, 0, 0, 0)[0] == null && !Files.exists(path), "An unavailable member supplied an empty replacement array");
        helper.assertTrue(ItemStack.isSameItemSameComponents(bad, loaded.getItem(0)), "A failed member format changed its original metadata or lock");
        final var removed = loaded.removeItem(0, 1);
        helper.assertTrue(ItemStack.isSameItemSameComponents(bad, removed), "An unavailable RAID member could not be removed intact");
        loaded.setItem(0, new ItemStack(ModItems.HDD_TIER1.get()));
        helper.assertTrue(loaded.onAnalyze(null, null, 0, 0, 0)[0] != null, "Replacing the unavailable member did not recover the array");
        loaded.clearContent(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void corruptRackMediaRetainsSnapshotAndCanBeEjectedIntact(GameTestHelper helper) throws Exception {
        final var id = "20261020-0000-4000-8000-000000000005";
        final var path = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers/" + id + ".bin");
        helper.assertTrue(!Files.exists(path), "Preserve an existing rack failure fixture");
        Files.createDirectories(path.getParent()); Files.write(path, new byte[]{3, 2, 1});
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.RACK.get().defaultBlockState());
        final var host = (li.cil.oc.common.blockentity.RackBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var rackDrive = new li.cil.oc.common.component.DiskDriveMountableEnvironment(host, -1);
        final var source = brokenDisk(new ItemStack(ModItems.FLOPPY.get()), id, true);
        final var saved = new CompoundTag(); rackDrive.save(saved);
        final var items = NonNullList.withSize(1, ItemStack.EMPTY); items.set(0, source);
        ContainerHelper.saveAllItems(saved, items, helper.getLevel().registryAccess());
        final var disk = HardDiskDriveItem.stackDataTag(source).copy(); disk.putString("fixture:state", "rack snapshot"); saved.put("disk", disk);
        try {
            rackDrive.load(saved);
            helper.assertTrue(rackDrive.media(null, null)[0] == null && ItemStack.isSameItemSameComponents(source, rackDrive.getItem(0)), "Corrupt rack media was replaced or changed");
            final var roundtrip = new CompoundTag(); rackDrive.save(roundtrip);
            helper.assertTrue(disk.equals(roundtrip.getCompound("disk")), "Failed rack mount lost its deferred snapshot");
            final var before = rackDrive.getItem(0).copy(); final var ejected = rackDrive.removeItemNoUpdate(0);
            helper.assertTrue(disk.equals(HardDiskDriveItem.stackDataTag(ejected)) && !HardDiskDriveItem.stackDataTag(before).contains("fixture:state"), "Rack ejection lost the pending snapshot or mutated an older copy");
            final var bytes = new byte[ModSettings.floppySize() * 1024]; bytes[0] = 99;
            try (var stream = new GZIPOutputStream(Files.newOutputStream(path))) { stream.write(bytes); }
            rackDrive.setItem(0, ejected);
            final var nodes = rackDrive.onAnalyze(null, null, 0, 0, 0);
            helper.assertTrue(nodes != null && ((Number) ((Component) nodes[0]).invoke("readByte", null, 1)[0]).intValue() == 99, "Repaired rack media did not remount");
            rackDrive.clearContent();
        } finally { rackDrive.node().remove(); Files.deleteIfExists(path); }
        helper.succeed();
    }
}

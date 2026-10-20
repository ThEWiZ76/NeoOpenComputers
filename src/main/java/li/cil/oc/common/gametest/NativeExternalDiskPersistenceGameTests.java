package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NativeExternalDiskPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void writableFloppyRestoresBeforeComputer(GameTestHelper helper) { restores(helper, false, true); }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void writableFloppyRestoresAfterComputer(GameTestHelper helper) { restores(helper, false, false); }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void bundledFloppyRestoresBeforeComputer(GameTestHelper helper) { restores(helper, true, true); }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void bundledFloppyRestoresAfterComputer(GameTestHelper helper) { restores(helper, true, false); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void runningLuaRetainsOpenFilesAfterFailedEjectAndCanRetry(GameTestHelper helper) throws Exception {
        final var id = "20261020-0000-4000-8000-000000000007";
        final var root = helper.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("opencomputers/" + id);
        helper.assertTrue(!java.nio.file.Files.exists(root), "Preserve existing Lua failure fixture");
        java.nio.file.Files.createDirectories(root);
        final var path = root.resolve("probe.bin");
        final byte[] baseline = {97, 0, (byte) 255, 122};
        java.nio.file.Files.write(path, baseline);
        java.nio.file.Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(1000));
        final var casePos = new BlockPos(1, 1, 1);
        final var diskPos = new BlockPos(2, 1, 1);
        helper.setBlock(casePos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(diskPos, ModBlocks.DISK_DRIVE.get());
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(casePos);
        final DiskDriveBlockEntity drive = helper.getBlockEntity(diskPos);
        final var floppy = new ItemStack(ModItems.FLOPPY.get());
        final var data = ItemDriverData.dataTag(floppy);
        final var node = new CompoundTag(); node.putString("address", id); data.put("node", node);
        final var fs = new CompoundTag(); fs.putString(li.cil.oc.common.NativeVolumeStore.STORAGE_TAG, id); data.put("fs", fs);
        drive.setItem(0, floppy);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu);
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom = component.proxy(component.list('eeprom')())
            assert(eeprom.getData() == '', 'unexpected reboot')
            local retained = 731
            local drive = component.proxy(component.list('disk_drive')())
            local fs = component.proxy(assert(drive.media()))
            local reader = assert(fs.open('probe.bin', 'rb'))
            assert(fs.read(reader, 1) == 'a')
            local writer = assert(fs.open('probe.bin', 'ab'))
            assert(fs.write(writer, string.char(42)))
            eeprom.setData('ready')
            repeat local signal = computer.pullSignal() until signal == 'fail_eject'
            local ok, result = pcall(drive.eject)
            assert(not ok or not result, 'failed save ejected media')
            assert(drive.media() == fs.address, 'failed eject detached media')
            assert(fs.read(reader, 3) == string.char(0,255) .. 'z', 'reader lost position or bytes')
            assert(fs.write(writer, string.char(43)), 'writer lost after failure')
            assert(retained == 731, 'local state lost')
            eeprom.setData('failed_safely')
            repeat local signal = computer.pullSignal() until signal == 'retry_eject'
            assert(retained == 731, 'local state lost before retry')
            assert(drive.eject(), 'repaired save could not eject')
            eeprom.setData('recovered')
            while true do computer.pullSignal() end
            """));
        helper.assertTrue(computer.toggleMachine(), "Lua failure computer did not start");
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(marker(computer).equals("ready"), "Lua not ready: " + computer.machine().lastError()))
            .thenExecute(() -> {
                try { java.nio.file.Files.write(path, new byte[]{8}); }
                catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                helper.assertTrue(computer.machine().signal("fail_eject"), "Failure signal rejected");
            })
            .thenWaitUntil(() -> helper.assertTrue(marker(computer).equals("failed_safely") && computer.machine().isRunning(),
                "Lua did not survive failed eject: " + computer.machine().lastError()))
            .thenExecute(() -> {
                helper.assertTrue(drive.getItem(0).getCount() == 1, "Failed Lua eject removed or duplicated media");
                try {
                    helper.assertTrue(java.util.Arrays.equals(new byte[]{8}, java.nio.file.Files.readAllBytes(path)), "Foreign bytes overwritten");
                    java.nio.file.Files.write(path, baseline);
                    java.nio.file.Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(1000));
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                helper.assertTrue(computer.machine().signal("retry_eject"), "Repair signal rejected");
            })
            .thenWaitUntil(() -> helper.assertTrue(marker(computer).equals("recovered") && computer.machine().isRunning(),
                "Lua retry did not recover: " + computer.machine().lastError()))
            .thenExecute(() -> {
                helper.assertTrue(drive.getItem(0).isEmpty(), "Successful Lua eject kept media in its slot");
                final var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(diskPos)).inflate(2));
                helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                    "Successful Lua eject did not create exactly one physical disk");
                try {
                    helper.assertTrue(java.util.Arrays.equals(new byte[]{97, 0, (byte) 255, 122, 42, 43}, java.nio.file.Files.readAllBytes(path)),
                        "Lua retry lost dirty bytes");
                    java.nio.file.Files.delete(path); java.nio.file.Files.delete(root);
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                computer.machine().stop(); drops.getFirst().discard();
            })
            .thenSucceed();
    }

    private static void restores(GameTestHelper helper, boolean bundled, boolean diskFirst) {
        final BlockPos casePos = new BlockPos(1, 1, 1);
        final BlockPos diskPos = new BlockPos(2, 1, 1);
        helper.setBlock(casePos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(diskPos, ModBlocks.DISK_DRIVE.get());
        final ComputerCaseBlockEntity original = helper.getBlockEntity(casePos);
        final DiskDriveBlockEntity drive = helper.getBlockEntity(diskPos);
        final ItemStack floppy = new ItemStack(ModItems.FLOPPY.get());
        if (bundled) {
            final CompoundTag data = new CompoundTag();
            data.putString(ItemRegistry.FLOPPY_FACTORY_ID_TAG, "neoopencomputers:loot/openos");
            floppy.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        drive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, floppy);
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        original.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu);
        original.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        original.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom = component.proxy(component.list('eeprom')())
            assert(eeprom.getData() == '', 'unexpected reboot')
            local drive = component.proxy(component.list('disk_drive')())
            local fs = component.proxy(assert(drive.media()))
            local bundled = %s
            local path = bundled and 'init.lua' or 'resume.bin'
            if not bundled then
              local out = assert(fs.open(path, 'w'))
              assert(fs.write(out, 'a' .. string.char(0,255) .. 'z'))
              fs.close(out)
            end
            local file = assert(fs.open(path, 'r'))
            assert(fs.read(file, 1))
            local expected = assert(fs.read(file, 3))
            assert(fs.seek(file, 'set', 1) == 1)
            eeprom.setData('waiting')
            repeat local signal = computer.pullSignal() until signal == 'continue_probe'
            assert(drive.media() == fs.address, 'external filesystem identity changed')
            assert(fs.read(file, 3) == expected, 'external file handle/offset lost')
            fs.close(file)
            eeprom.setData('restored')
            while true do computer.pullSignal() end
            """.formatted(bundled)));
        helper.assertTrue(original.toggleMachine(), "Computer did not start");
        final ComputerCaseBlockEntity[] loaded = new ComputerCaseBlockEntity[1];
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(marker(original).equals("waiting"), "Not ready: " + original.machine().lastError()))
            .thenIdle(20)
            .thenExecute(() -> {
                final var registries = helper.getLevel().registryAccess();
                final CompoundTag caseData = original.saveWithFullMetadata(registries);
                final CompoundTag diskData = drive.saveWithFullMetadata(registries);
                original.setRemoved();
                drive.setRemoved();
                // Reconstruct both before attaching either: no old disk environment survives.
                final var newCase = BlockEntity.loadStatic(original.getBlockPos(), original.getBlockState(), caseData, registries);
                final var newDisk = BlockEntity.loadStatic(drive.getBlockPos(), drive.getBlockState(), diskData, registries);
                helper.assertTrue(newCase instanceof ComputerCaseBlockEntity && newDisk instanceof DiskDriveBlockEntity, "Snapshot discarded");
                loaded[0] = (ComputerCaseBlockEntity) newCase;
                for (final BlockEntity block : diskFirst ? new BlockEntity[]{newDisk, newCase} : new BlockEntity[]{newCase, newDisk}) {
                    helper.getLevel().setBlockEntity(block);
                    block.onLoad();
                }
                helper.assertTrue(loaded[0].machine().signal("continue_probe"), "Resume signal rejected");
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(loaded[0] != null, "Computer not restored");
                helper.assertTrue(marker(loaded[0]).equals("restored") && loaded[0].machine().isRunning(),
                    "External file did not resume: " + loaded[0].machine().lastError());
            })
            .thenSucceed();
    }

    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(ComputerCaseBlockEntity.SLOT_EEPROM))
            .getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

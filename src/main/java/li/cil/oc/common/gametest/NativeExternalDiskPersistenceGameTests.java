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

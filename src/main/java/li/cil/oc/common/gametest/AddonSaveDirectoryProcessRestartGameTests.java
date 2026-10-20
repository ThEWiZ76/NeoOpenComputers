package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class AddonSaveDirectoryProcessRestartGameTests {
    private static final BlockPos DRIVE = new BlockPos(368, 80, 208);
    private static final BlockPos COMPUTER = DRIVE.east();
    private static final BlockPos RECORD = DRIVE.south(2);
    private static final String ROOT = "addon-save-restart-fixture";
    private static final byte[] PREPARED = {'a', 0, (byte) 128, (byte) 255, 42};
    private static final byte[] RESUMED = {'a', 0, 17, 18, 42};

    @GameTestGenerator public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.restartAddonSaveDirectory")) return List.of();
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalArgumentException("Unknown addon save-directory restart phase");
        final var context = ModLoadingContext.get();
        final var previous = context.getActiveContainer();
        final ItemStack disk;
        try {
            context.setActiveContainer(ModList.get().getModContainerById("neoopencomputers").orElseThrow());
            disk = li.cil.oc.api.Items.registerFloppy(ROOT, DyeColor.ORANGE, () -> FileSystem.fromSaveDirectory(ROOT, 8192, true), false);
        } finally { context.setActiveContainer(previous); }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            if (level.hasChunkAt(COMPUTER) && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity computer)
                ((Connector) computer.machine().node()).changeBuffer(10000);
        });
        return List.of(new TestFunction("addonSaveRestart", "addon_save_directory_restart_" + phase, "neoopencomputers:empty", 600, 0, true, helper -> {
            helper.getLevel().setChunkForced(DRIVE.getX() >> 4, DRIVE.getZ() >> 4, true);
            helper.runAfterDelay(2, () -> run(helper, phase.equals("prepare"), disk));
        }));
    }

    private static void run(GameTestHelper helper, boolean prepare, ItemStack disk) {
        final var level = helper.getLevel();
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(DRIVE) && level.isEmptyBlock(COMPUTER) && level.isEmptyBlock(RECORD), "Addon save-directory probe requires a fresh world");
            level.setBlockAndUpdate(DRIVE, ModBlocks.DISK_DRIVE.get().defaultBlockState());
            level.setBlockAndUpdate(COMPUTER, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(level.getBlockEntity(DRIVE) instanceof DiskDriveBlockEntity && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity
            && level.getBlockEntity(RECORD) instanceof Container, "Addon save-directory hosts/checkpoint missing");
        final var drive = (DiskDriveBlockEntity) level.getBlockEntity(DRIVE);
        final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(COMPUTER);
        final var record = (Container) level.getBlockEntity(RECORD);
        if (prepare) {
            drive.setItem(0, disk.copy()); drive.onLoad();
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
            computer.setItem(4, cpu); computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local eeprom=component.proxy(component.list('eeprom')())
                assert(eeprom.getData()=='','Addon save-directory VM rebooted')
                local drive=component.proxy(assert(component.list('disk_drive')()))
                local fs=component.proxy(assert(drive.media()))
                assert(not fs.isReadOnly(),'Addon save-directory is not writable')
                local w=assert(fs.open('probe.bin','wb')); assert(fs.write(w,string.char(97,0,128,255,42)))
                assert(fs.seek(w,'set',2)==2)
                local r=assert(fs.open('probe.bin','rb')); assert(fs.read(r,1)=='a')
                local retained=731; local address=fs.address
                eeprom.setData('addon-save-waiting')
                repeat until computer.pullSignal()=='resume_addon_save'
                assert(retained==731 and fs.address==address and drive.media()==address,'Addon save-directory continuation/identity lost')
                assert(fs.read(r,4)==string.char(0,128,255,42),'Saved binary input offset lost')
                assert(fs.seek(w,'cur',0)==2,'Saved binary output offset lost')
                assert(fs.write(w,string.char(17,18))); fs.close(w); fs.close(r)
                eeprom.setData('addon-save-resumed')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); ((Connector) computer.machine().node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "Addon save-directory native VM did not start");
        } else {
            final var checkpoint = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(checkpoint != null, "Addon save-directory checkpoint missing");
            final var data = checkpoint.copyTag();
            helper.assertTrue(data.getString("phase").equals("prepared") && data.getLong("pid") > 0 && data.getLong("pid") != ProcessHandle.current().pid(), "Addon save-directory fixture was not prepared in another JVM");
            helper.assertTrue(data.getString("computer").equals(computer.machine().node().address()) && data.getString("drive").equals(drive.node().address()), "Addon save-directory host addresses changed");
            helper.assertTrue(diskBytes(helper, PREPARED), "Saved bytes are absent from the active world's addon directory");
            helper.assertTrue(computer.machine().isRunning() && computer.machine().signal("resume_addon_save"), "Addon save-directory VM did not retain running state");
        }
        final var sequence = helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(computer).equals(prepare ? "addon-save-waiting" : "addon-save-resumed"), "Addon save-directory continuation failed: " + computer.machine().lastError()));
        if (!prepare) {
            sequence.thenExecute(() -> drive.saveWithFullMetadata(level.registryAccess()));
            sequence.thenWaitUntil(() -> helper.assertTrue(diskBytes(helper, RESUMED), "Resumed output was not committed at save"));
        }
        sequence.thenExecute(() -> {
            final var data = new CompoundTag(); data.putString("phase", prepare ? "prepared" : "verified"); data.putLong("pid", ProcessHandle.current().pid());
            data.putString("computer", computer.machine().node().address()); data.putString("drive", drive.node().address());
            final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setItem(0, paper); record.setChanged();
            li.cil.oc.NeoOpenComputers.LOGGER.info("Native addon save directory {} in PID {}", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
        }).thenSucceed();
    }

    private static boolean diskBytes(GameTestHelper helper, byte[] expected) {
        final var path = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers").resolve(ROOT).resolve("probe.bin");
        try { return Files.isRegularFile(path) && Arrays.equals(expected, Files.readAllBytes(path)); }
        catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }

    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(6)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

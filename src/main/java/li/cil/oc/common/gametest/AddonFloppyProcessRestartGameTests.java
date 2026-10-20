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
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class AddonFloppyProcessRestartGameTests {
    private static final BlockPos DRIVE = new BlockPos(304, 80, 160);
    private static final BlockPos COMPUTER = DRIVE.east();
    private static final BlockPos RECORD = DRIVE.south(2);

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.restartAddonFloppy")) return List.of();
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalArgumentException("Unknown addon floppy restart phase");
        final var context = ModLoadingContext.get();
        final var previous = context.getActiveContainer();
        final ItemStack disk;
        try {
            context.setActiveContainer(ModList.get().getModContainerById("neoopencomputers").orElseThrow());
            disk = li.cil.oc.api.Items.registerFloppy("addon-restart-fixture", DyeColor.BLUE, () -> {
                final var fs = FileSystem.fromMemory(4096);
                final var handle = fs.getHandle(fs.open("probe.bin", li.cil.oc.api.fs.Mode.Write));
                handle.write(new byte[]{'a', 0, (byte) 128, (byte) 255, 42}); handle.close();
                return FileSystem.asReadOnly(fs);
            }, false);
        } finally { context.setActiveContainer(previous); }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            if (level.hasChunkAt(COMPUTER) && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity computer)
                ((Connector) computer.machine().node()).changeBuffer(10000);
        });
        return List.of(new TestFunction("addonFloppyRestart", "addon_floppy_restart_" + phase, "neoopencomputers:empty", 600, 0, true, helper -> {
            helper.getLevel().setChunkForced(DRIVE.getX() >> 4, DRIVE.getZ() >> 4, true);
            helper.runAfterDelay(2, () -> run(helper, phase.equals("prepare"), disk));
        }));
    }

    private static void run(GameTestHelper helper, boolean prepare, ItemStack disk) {
        final var level = helper.getLevel();
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(DRIVE) && level.isEmptyBlock(COMPUTER) && level.isEmptyBlock(RECORD), "Addon floppy probe requires a fresh world");
            level.setBlockAndUpdate(DRIVE, ModBlocks.DISK_DRIVE.get().defaultBlockState());
            level.setBlockAndUpdate(COMPUTER, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(level.getBlockEntity(DRIVE) instanceof DiskDriveBlockEntity && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity
            && level.getBlockEntity(RECORD) instanceof Container, "Addon floppy restart hosts/checkpoint missing");
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
                assert(eeprom.getData()=='','Addon floppy computer rebooted')
                local drive=component.proxy(assert(component.list('disk_drive')(),'Disk drive missing'))
                local fs=component.proxy(assert(drive.media(),'Addon floppy filesystem missing'))
                assert(fs.isReadOnly(),'Addon floppy not read-only')
                local h=assert(fs.open('probe.bin','rb')); assert(fs.read(h,1)=='a')
                local retained=731; local address=fs.address
                eeprom.setData('addon-floppy-waiting')
                repeat until computer.pullSignal()=='resume_addon_floppy'
                assert(retained==731 and fs.address==address and drive.media()==address,'Addon floppy VM/identity lost')
                assert(fs.read(h,4)==string.char(0,128,255,42),'Addon floppy read offset/bytes lost'); fs.close(h)
                eeprom.setData('addon-floppy-resumed')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); ((Connector) computer.machine().node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "Addon floppy native computer did not start");
        } else {
            final var checkpoint = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(checkpoint != null, "Addon floppy checkpoint missing");
            final var data = checkpoint.copyTag();
            helper.assertTrue(data.getString("phase").equals("prepared") && data.getLong("pid") > 0 && data.getLong("pid") != ProcessHandle.current().pid(), "Addon floppy was not prepared in another JVM");
            helper.assertTrue(li.cil.oc.api.API.items instanceof ItemRegistry registry && registry.floppyFactory(drive.getItem(0)) != null,
                "Saved addon floppy no longer resolves its factory in the fresh JVM");
            helper.assertTrue(data.getString("factory").equals(factory(disk)) && data.getString("factory").equals(factory(drive.getItem(0))), "Addon floppy registration identity changed");
            helper.assertTrue(data.getString("computer").equals(computer.machine().node().address()) && data.getString("drive").equals(drive.node().address()), "Addon floppy host identity changed");
            helper.assertTrue(computer.machine().isRunning() && computer.machine().signal("resume_addon_floppy"), "Addon floppy computer did not retain running state");
        }
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(computer).equals(prepare ? "addon-floppy-waiting" : "addon-floppy-resumed"), "Addon floppy continuation failed: " + computer.machine().lastError()))
            .thenExecute(() -> {
                final var data = new CompoundTag(); data.putString("phase", prepare ? "prepared" : "verified"); data.putLong("pid", ProcessHandle.current().pid());
                data.putString("factory", factory(drive.getItem(0))); data.putString("computer", computer.machine().node().address()); data.putString("drive", drive.node().address());
                final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setItem(0, paper); record.setChanged();
                li.cil.oc.NeoOpenComputers.LOGGER.info("Native addon floppy {} in PID {}", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
            }).thenSucceed();
    }

    private static String factory(ItemStack disk) { return disk.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG); }
    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(6)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

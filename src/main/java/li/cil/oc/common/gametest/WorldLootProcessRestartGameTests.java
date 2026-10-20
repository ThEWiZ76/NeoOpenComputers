package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class WorldLootProcessRestartGameTests {
    private static final BlockPos DRIVE = new BlockPos(528, 80, 288);
    private static final BlockPos COMPUTER = DRIVE.east();
    private static final BlockPos LEGACY = COMPUTER.east();
    private static final BlockPos RECORD = DRIVE.south(2);

    @GameTestGenerator public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.restartWorldLoot")) return List.of();
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalArgumentException("Unknown world loot restart phase");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            if (level.hasChunkAt(COMPUTER) && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity computer)
                ((Connector) computer.machine().node()).changeBuffer(10000);
        });
        return List.of(new TestFunction("worldLootRestart", "world_loot_native_restart_" + phase, "neoopencomputers:empty", 600, 0, true, helper -> {
            helper.getLevel().setChunkForced(DRIVE.getX() >> 4, DRIVE.getZ() >> 4, true);
            helper.runAfterDelay(2, () -> run(helper, phase.equals("prepare")));
        }));
    }

    private static void run(GameTestHelper helper, boolean prepare) {
        final var level = helper.getLevel();
        helper.assertTrue(sourceBytes(helper), "World loot source files are missing or changed");
        final var definition = ModLootDisks.stacksForCycling().stream().filter(stack -> stack.getHoverName().getString().equals("World restart")).findFirst();
        helper.assertTrue(definition.isPresent(), "World descriptor not loaded in this JVM");
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(DRIVE) && level.isEmptyBlock(COMPUTER) && level.isEmptyBlock(LEGACY) && level.isEmptyBlock(RECORD), "World loot restart requires a fresh world");
            level.setBlockAndUpdate(DRIVE, ModBlocks.DISK_DRIVE.get().defaultBlockState());
            level.setBlockAndUpdate(LEGACY, ModBlocks.DISK_DRIVE.get().defaultBlockState());
            level.setBlockAndUpdate(COMPUTER, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(level.getBlockEntity(DRIVE) instanceof DiskDriveBlockEntity && level.getBlockEntity(LEGACY) instanceof DiskDriveBlockEntity
            && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity && level.getBlockEntity(RECORD) instanceof ChestBlockEntity, "World loot restart hosts/checkpoint missing");
        final var drive = (DiskDriveBlockEntity) level.getBlockEntity(DRIVE);
        final var legacy = (DiskDriveBlockEntity) level.getBlockEntity(LEGACY);
        final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(COMPUTER);
        final var record = (ChestBlockEntity) level.getBlockEntity(RECORD);
        if (prepare) {
            final var disk = generatedDisk(helper, record, definition.orElseThrow());
            drive.setItem(0, disk); drive.onLoad();
            final var legacyDisk = new ItemStack(ModItems.FLOPPY.get());
            final var legacyData = new CompoundTag();
            legacyData.putString("oc:lootPath", "legacy-only"); legacyData.putString("oc:fs.label", "Legacy restart");
            legacyDisk.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));
            legacy.setItem(0, legacyDisk); legacy.onLoad();
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
            computer.setItem(4, cpu); computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local eeprom=component.proxy(component.list('eeprom')())
                assert(eeprom.getData()=='','World loot VM rebooted')
                local media={}
                for address in component.list('disk_drive') do
                  local drive=component.proxy(address)
                  local fs=component.proxy(assert(drive.media(),'World loot filesystem missing'))
                  local label=fs.getLabel()
                  assert(fs.isReadOnly(),'World loot disk is writable')
                  local ok,w=pcall(fs.open,'probe.bin','wb')
                  assert(not ok or not w,'Write-open accepted on world loot source')
                  local h=assert(fs.open('probe.bin','rb'))
                  local first=fs.read(h,1)
                  assert(first==(label=='World restart' and 'a' or string.char(17)),'Wrong world loot source')
                  media[label]={drive=drive,fs=fs,handle=h,address=fs.address}
                end
                assert(media['World restart'] and media['Legacy restart'],'World/legacy disk missing')
                local retained=731
                eeprom.setData('world-loot-waiting')
                repeat until computer.pullSignal()=='resume_world_loot'
                assert(retained==731,'World loot continuation lost')
                for label,m in pairs(media) do
                  assert(m.fs.address==m.address and m.drive.media()==m.address,'World loot component identity changed')
                  assert(m.fs.isReadOnly() and m.fs.getLabel()==label,'World loot metadata changed')
                  assert(m.fs.seek(m.handle,'cur',0)==1,'World loot read offset lost')
                  local expected=label=='World restart' and string.char(0,128,255,42) or string.char(0,255)
                  assert(m.fs.read(m.handle,16)==expected,'World loot read handle/bytes lost')
                  m.fs.close(m.handle)
                end
                eeprom.setData('world-loot-resumed')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); ((Connector) computer.machine().node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "World loot native computer did not start");
        } else {
            final var checkpoint = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(checkpoint != null, "World loot restart checkpoint missing");
            final var data = checkpoint.copyTag();
            helper.assertTrue(data.getString("phase").equals("prepared") && data.getLong("pid") > 0 && data.getLong("pid") != ProcessHandle.current().pid(), "World loot fixture was not prepared in another JVM");
            final var expectedData = definition.orElseThrow().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            final var savedData = drive.getItem(0).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            for (final var key : List.of(ItemRegistry.FLOPPY_FACTORY_ID_TAG, ItemRegistry.FLOPPY_LABEL_TAG, ItemRegistry.FLOPPY_COLOR_TAG)) {
                helper.assertTrue(expectedData.getString(key).equals(savedData.getString(key)), "Saved chest disk lost descriptor field " + key);
            }
            helper.assertTrue(drive.getItem(0).is(ModItems.FLOPPY.get()) && drive.getItem(0).getCount() == 1, "Saved chest disk item/count changed");
            helper.assertTrue(data.getString("computer").equals(computer.machine().node().address()) && data.getString("drive").equals(drive.node().address())
                && data.getString("legacy").equals(legacy.node().address()), "World loot host addresses changed");
            helper.assertTrue(marker(computer).equals("world-loot-waiting"), "World loot continuation already ran; use a fresh prepare/verify pair");
            helper.assertTrue(computer.machine().isRunning() && computer.machine().signal("resume_world_loot"), "World loot VM did not retain running state");
        }
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(computer).equals(prepare ? "world-loot-waiting" : "world-loot-resumed"), "World loot continuation failed: " + computer.machine().lastError()))
            .thenExecute(() -> {
                helper.assertTrue(sourceBytes(helper), "Read-only world loot source files were modified");
                final var data = new CompoundTag(); data.putString("phase", prepare ? "prepared" : "verified"); data.putLong("pid", ProcessHandle.current().pid());
                data.putString("computer", computer.machine().node().address()); data.putString("drive", drive.node().address()); data.putString("legacy", legacy.node().address());
                final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setItem(0, paper); record.setChanged();
                li.cil.oc.NeoOpenComputers.LOGGER.info("Native world loot {} in PID {}", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
            }).thenSucceed();
    }

    private static ItemStack generatedDisk(GameTestHelper helper, ChestBlockEntity chest, ItemStack expected) {
        for (long seed = 1; seed <= 256; seed++) {
            chest.clearContent(); chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON); chest.setLootTableSeed(seed);
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                final var stack = chest.getItem(slot);
                if (ItemStack.isSameItemSameComponents(stack, expected)) {
                    final var result = stack.copyWithCount(1); chest.clearContent(); return result;
                }
            }
        }
        helper.assertTrue(false, "Actual dungeon chest did not generate the world-defined disk");
        return ItemStack.EMPTY;
    }

    private static boolean sourceBytes(GameTestHelper helper) {
        final var loot = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers/loot");
        try {
            return Arrays.equals(Files.readAllBytes(loot.resolve("network/probe.bin")), new byte[]{97, 0, (byte) 128, (byte) 255, 42})
                && Arrays.equals(Files.readAllBytes(loot.resolve("legacy-only/probe.bin")), new byte[]{17, 0, (byte) 255});
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }

    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(6)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

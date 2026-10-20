package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.NativeVolumeStore;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class NativeComputerStorageFailureGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void failedComputerSaveCanRepairAndResumeItsHeap(GameTestHelper helper) throws IOException {
        recovers(helper, false);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void failedComputerUnloadCanRepairAndResumeItsHeap(GameTestHelper helper) throws IOException {
        recovers(helper, true);
    }

    private static void recovers(GameTestHelper helper, boolean unload) throws IOException {
        final var id = "20261020-0000-4000-8000-00000000000" + (unload ? "9" : "8");
        final var root = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers/" + id);
        helper.assertTrue(!Files.exists(root), "Preserve existing computer failure fixture");
        Files.createDirectories(root);
        final var file = root.resolve("probe.bin");
        final byte[] baseline = {97, 0, (byte) 255, 122};
        Files.write(file, baseline); Files.setLastModifiedTime(file, FileTime.fromMillis(1000));
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.COMPUTER_CASE_TIER1.get());
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(pos);
        final var hdd = new ItemStack(ModItems.HDD_TIER1.get());
        final var data = ItemDriverData.dataTag(hdd);
        final var node = new CompoundTag(); node.putString("address", id); data.put("node", node);
        final var fs = new CompoundTag(); fs.putString(NativeVolumeStore.STORAGE_TAG, id); data.put("fs", fs);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu);
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, hdd);
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom = component.proxy(component.list('eeprom')())
            assert(eeprom.getData() == '', 'unexpected reboot')
            local retained = 731
            local fs = component.proxy('%s')
            local reader = assert(fs.open('probe.bin', 'rb'))
            assert(fs.read(reader, 1) == 'a')
            local writer = assert(fs.open('probe.bin', 'ab'))
            assert(fs.write(writer, string.char(42)))
            eeprom.setData('ready')
            repeat local signal = computer.pullSignal() until signal == 'after_failure'
            assert(retained == 731, 'local state lost after failed save')
            assert(fs.read(reader, 1) == string.char(0), 'reader lost after failed save')
            assert(fs.write(writer, string.char(43)), 'writer lost after failed save')
            eeprom.setData('survived')
            repeat local signal = computer.pullSignal() until signal == 'after_restore'
            assert(retained == 731, 'local state lost after repair/reload')
            assert(fs.read(reader, 2) == string.char(255) .. 'z', 'restored reader position lost')
            assert(fs.seek(writer, 'cur', 0) == 6, 'restored writer position lost')
            assert(fs.write(writer, string.char(44)), 'restored writer lost')
            fs.close(reader) fs.close(writer)
            eeprom.setData('restored')
            while true do computer.pullSignal() end
            """.formatted(id)));
        helper.assertTrue(computer.toggleMachine(), "Failure fixture did not start");
        final ComputerCaseBlockEntity[] restored = new ComputerCaseBlockEntity[1];
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(marker(computer).equals("ready"), "Lua not ready: " + computer.machine().lastError()))
            .thenExecute(() -> {
                final var original = computer.getItem(ComputerCaseBlockEntity.SLOT_HDD).copy();
                try { Files.write(file, new byte[]{8}); }
                catch (IOException failure) { throw new UncheckedIOException(failure); }
                boolean rejected = false;
                try {
                    if (unload) computer.onChunkUnloaded();
                    else computer.saveWithFullMetadata(helper.getLevel().registryAccess());
                } catch (RuntimeException expected) { rejected = true; }
                helper.assertTrue(rejected, "Failed computer storage was silently accepted");
                helper.assertTrue(ItemStack.isSameItemSameComponents(original, computer.getItem(ComputerCaseBlockEntity.SLOT_HDD)),
                    "Failed save published uncommitted HDD metadata");
                helper.assertTrue(computer.machine().isRunning(), "Failed save stopped the live heap");
                helper.assertTrue(computer.machine().signal("after_failure"), "Failure continuation signal rejected");
            })
            .thenWaitUntil(() -> helper.assertTrue(marker(computer).equals("survived") && computer.machine().isRunning(),
                "Lua did not survive failed save/unload: " + computer.machine().lastError()))
            .thenExecute(() -> {
                try {
                    helper.assertTrue(Arrays.equals(new byte[]{8}, Files.readAllBytes(file)), "Failed save overwrote foreign bytes");
                    Files.write(file, baseline); Files.setLastModifiedTime(file, FileTime.fromMillis(1000));
                } catch (IOException failure) { throw new UncheckedIOException(failure); }
                computer.onChunkUnloaded();
                helper.assertTrue(!computer.machine().isRunning(), "Repaired unload kept its old VM running");
                final var snapshot = computer.saveWithFullMetadata(helper.getLevel().registryAccess());
                final var loaded = BlockEntity.loadStatic(computer.getBlockPos(), computer.getBlockState(), snapshot, helper.getLevel().registryAccess());
                helper.assertTrue(loaded instanceof ComputerCaseBlockEntity, "Repaired snapshot discarded computer");
                restored[0] = (ComputerCaseBlockEntity) loaded;
                helper.getLevel().setBlockEntity(restored[0]); restored[0].onLoad();
                helper.assertTrue(restored[0].machine().signal("after_restore"), "Restored continuation signal rejected");
            })
            .thenWaitUntil(() -> helper.assertTrue(restored[0] != null && marker(restored[0]).equals("restored") && restored[0].machine().isRunning(),
                "Repaired heap did not restore: " + (restored[0] == null ? "missing" : restored[0].machine().lastError())))
            .thenExecute(() -> {
                restored[0].saveWithFullMetadata(helper.getLevel().registryAccess());
                try {
                    helper.assertTrue(Arrays.equals(new byte[]{97, 0, (byte) 255, 122, 42, 43, 44}, Files.readAllBytes(file)), "Repaired save/reload lost dirty bytes");
                    restored[0].machine().stop(); restored[0].clearContent();
                    Files.delete(file); Files.delete(root);
                } catch (IOException failure) { throw new UncheckedIOException(failure); }
            })
            .thenSucceed();
    }

    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(ComputerCaseBlockEntity.SLOT_EEPROM))
            .getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NativeMicrocontrollerPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeMicrocontrollerDisposesAndRestores(GameTestHelper helper) {
        restores(helper, Reload.DETACHED);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeMicrocontrollerSavesAfterUnload(GameTestHelper helper) {
        restores(helper, Reload.UNLOAD_BEFORE_SAVE);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeMicrocontrollerReattachesSameInstance(GameTestHelper helper) {
        restores(helper, Reload.SAME_INSTANCE);
    }

    private enum Reload { DETACHED, UNLOAD_BEFORE_SAVE, SAME_INSTANCE }

    private static void restores(GameTestHelper helper, Reload mode) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.MICROCONTROLLER_TIER1.get());
        MicrocontrollerBlockEntity original = helper.getBlockEntity(pos);
        ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        original.setItem(0, cpu);
        original.setItem(1, new ItemStack(ModItems.MEMORY_TIER1.get()));
        original.setItem(2, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom = component.proxy(component.list('eeprom')())
            assert(eeprom.getData() == '', 'unexpected reboot')
            local retained = {value = 731}
            local fs = component.proxy(computer.tmpAddress())
            local file = assert(fs.open('resume.bin', 'w'))
            assert(fs.write(file, 'a' .. string.char(0, 255) .. 'z'))
            fs.close(file)
            file = assert(fs.open('resume.bin', 'r'))
            assert(fs.read(file, 1) == 'a')
            eeprom.setData('waiting')
            repeat local signal = computer.pullSignal() until signal == 'continue_probe'
            assert(retained.value == 731, 'local state lost')
            assert(fs.address == computer.tmpAddress(), 'tmp identity lost')
            assert(fs.read(file, 3) == string.char(0, 255) .. 'z', 'file continuation lost')
            fs.close(file)
            eeprom.setData('restored')
            while true do computer.pullSignal() end
            """));
        helper.assertTrue(original.toggleMachine(), "Microcontroller did not start");
        MicrocontrollerBlockEntity[] restored = new MicrocontrollerBlockEntity[1];
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(marker(original).equals("waiting"),
                "Program did not reach checkpoint: " + original.machine().lastError()))
            .thenIdle(20)
            .thenExecute(() -> {
                var registries = helper.getLevel().registryAccess();
                if (mode != Reload.DETACHED) original.onChunkUnloaded();
                var saved = original.saveWithFullMetadata(registries);
                original.setRemoved();
                helper.assertTrue(!original.machine().architecture().isInitialized(), "Removed microcontroller retained native VM");
                helper.assertTrue(!original.machine().isRunning(), "Removed microcontroller still running");
                if (mode == Reload.SAME_INSTANCE) {
                    original.clearRemoved();
                    restored[0] = original;
                } else {
                    var loaded = BlockEntity.loadStatic(original.getBlockPos(), original.getBlockState(), saved, registries);
                    helper.assertTrue(loaded instanceof MicrocontrollerBlockEntity, "Saved microcontroller discarded");
                    restored[0] = (MicrocontrollerBlockEntity) loaded;
                }
                helper.getLevel().setBlockEntity(restored[0]);
                restored[0].onLoad();
                helper.assertTrue(restored[0].machine().isRunning(), "Running state lost");
                restored[0].machine().signal("continue_probe");
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(restored[0] != null, "Microcontroller restoration did not complete");
                helper.assertTrue(marker(restored[0]).equals("restored") && restored[0].machine().isRunning(),
                    "Program did not resume: " + restored[0].machine().lastError() + ", marker=" + marker(restored[0]) +
                        ", uptime=" + restored[0].machine().upTime());
            })
            .thenSucceed();
    }

    private static String marker(MicrocontrollerBlockEntity controller) {
        return new String(ItemDriverData.dataTag(controller.getItem(2))
            .getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

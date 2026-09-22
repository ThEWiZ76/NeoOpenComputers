package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.internal.Server;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.charset.StandardCharsets;

/** Required native architecture continuation test; default LuaJ remains separate. */
@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NativeRackPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRackResumesLocalStateAfterDetachedReload(GameTestHelper helper) {
        resumesAfterReload(helper, """
            local eeprom = component.proxy(component.list("eeprom")())
            assert(eeprom.getData() == "", "program restarted instead of resuming")
            local retained = {value = 731}
            eeprom.setData("waiting")
            repeat local signal = computer.pullSignal() until signal == "continue_probe"
            assert(retained.value == 731)
            eeprom.setData("restored")
            while true do computer.pullSignal() end
            """);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRackResumesOpenFileAtSavedOffset(GameTestHelper helper) {
        resumesOpenFileAfterReload(helper, false);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRackResumesTmpFileAtSavedOffset(GameTestHelper helper) {
        resumesOpenFileAfterReload(helper, true);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRackRetainsUptimeDeadlineAfterReload(GameTestHelper helper) {
        resumesAfterReload(helper, """
            local eeprom = component.proxy(component.list("eeprom")())
            assert(eeprom.getData() == "", "program restarted instead of resuming")
            local started = computer.uptime()
            local deadline = started + 5
            eeprom.setData("waiting")
            repeat local signal = computer.pullSignal() until signal == "continue_probe"
            assert(computer.uptime() >= started + 2.5, "uptime lost before reload")
            assert(computer.uptime() < deadline, "deadline passed while offline")
            repeat computer.pullSignal(deadline - computer.uptime()) until computer.uptime() >= deadline
            eeprom.setData("restored")
            while true do computer.pullSignal() end
            """);
    }

    private static void resumesOpenFileAfterReload(GameTestHelper helper, boolean temporary) {
        resumesAfterReload(helper, """
            local eeprom = component.proxy(component.list("eeprom")())
            assert(eeprom.getData() == "", "program restarted instead of resuming")
            local useTmp = %s
            local tmpAddress = computer.tmpAddress()
            local fs
            for address in component.list('filesystem') do
              if (address == tmpAddress) == useTmp then fs = component.proxy(address); break end
            end
            assert(fs, 'missing test filesystem')
            local file = assert(fs.open('native-continuation.bin', 'w'))
            assert(fs.write(file, 'a' .. string.char(0, 255) .. 'z'))
            fs.close(file)
            file = assert(fs.open('native-continuation.bin', 'r'))
            assert(fs.read(file, 1) == 'a')
            local alias = file
            eeprom.setData("waiting")
            repeat local signal = computer.pullSignal() until signal == "continue_probe"
            if useTmp then assert(fs.address == computer.tmpAddress(), 'tmp address changed') end
            assert(file == alias, 'file proxy identity lost')
            assert(fs.read(file, 3) == string.char(0, 255) .. 'z', 'open file position or contents lost')
            assert(fs.read(file, 1) == nil, 'expected EOF')
            fs.close(file)
            eeprom.setData("restored")
            while true do computer.pullSignal() end
            """.formatted(temporary));
    }

    private static void resumesAfterReload(GameTestHelper helper, String program) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.RACK.get());
        RackBlockEntity rack = helper.getBlockEntity(pos);
        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER1.get()));
        Server original = (Server) rack.getMountable(0);
        Container items = (Container) original;
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(
            cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
        items.setItem(2, cpu);
        items.setItem(4, new ItemStack(ModItems.MEMORY_TIER1.get()));
        items.setItem(6, new ItemStack(ModItems.HDD_TIER1.get()));
        items.setItem(8, RobotMovementPersistenceGameTests.eeprom(program));
        ((Connector) original.machine().node()).changeBuffer(10000);
        helper.assertTrue(original.machine().start(), "Initial rack did not start");
        Server[] loaded = new Server[1];
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(marker(original).equals("waiting"), "Original did not reach checkpoint: " + original.machine().lastError()))
            .thenIdle(50)
            .thenExecute(() -> {
                var registries = helper.getLevel().registryAccess();
                var saved = rack.saveWithFullMetadata(registries);
                rack.setRemoved();
                var restored = BlockEntity.loadStatic(rack.getBlockPos(), rack.getBlockState(), saved, registries);
                helper.assertTrue(restored instanceof RackBlockEntity, "Saved rack was discarded");
                RackBlockEntity replacement = (RackBlockEntity) restored;
                helper.getLevel().setBlockEntity(replacement);
                replacement.onLoad();
                loaded[0] = (Server) replacement.getMountable(0);
                helper.assertTrue(loaded[0].machine().isRunning(), "Running flag was lost");
                loaded[0].machine().signal("continue_probe");
            })
            .thenIdle(60)
            .thenExecute(() -> helper.assertTrue(marker(loaded[0]).equals("restored") && loaded[0].machine().isRunning(),
                "Lua continuation lost: " + loaded[0].machine().lastError() + ", marker=" + marker(loaded[0])))
            .thenSucceed();
    }

    private static String marker(Server server) {
        return new String(ItemDriverData.dataTag(((Container) server).getItem(8))
            .getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}

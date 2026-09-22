package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.LuaArchitecture;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class ArchitectureSwitchGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void luaJCanSelectAndBootNativeArchitecture(GameTestHelper helper) { switches(helper, false); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeCanSelectAndBootLuaJArchitecture(GameTestHelper helper) { switches(helper, true); }

    private static void switches(GameTestHelper helper, boolean fromNative) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.COMPUTER_CASE_TIER1.get());
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        final MutableProcessor processor = (MutableProcessor) Driver.driverFor(cpu);
        processor.setArchitecture(cpu, fromNative ? NativeLuaArchitecture.class : LuaArchitecture.class);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu);
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom = component.proxy(component.list('eeprom')())
            local names = {}
            for _, name in ipairs(computer.getArchitectures()) do names[name] = true end
            assert(names['Lua'] and names['Lua 5.2 (native)'], 'missing selectable architecture')
            if eeprom.getData() == '' then
              assert(computer.getArchitecture() == '%s', 'wrong initial architecture')
              assert(not computer.setArchitecture(computer.getArchitecture()), 'same architecture changed')
              local result, reason = computer.setArchitecture('not-an-architecture')
              assert(result == nil and reason == 'unknown architecture', 'unknown architecture accepted')
              eeprom.setData('switching')
              computer.setArchitecture('%s')
              error('architecture change did not reboot')
            end
            assert(eeprom.getData() == 'switching', 'unexpected repeated reboot')
            assert(computer.getArchitecture() == '%s', 'new architecture did not boot')
            eeprom.setData('done')
            while true do computer.pullSignal() end
            """.formatted(fromNative ? "Lua 5.2 (native)" : "Lua",
                fromNative ? "Lua" : "Lua 5.2 (native)", fromNative ? "Lua" : "Lua 5.2 (native)")));
        final var oldArchitecture = computer.machine().architecture();
        helper.assertTrue(computer.toggleMachine(), "Computer did not start");
        helper.succeedWhen(() -> {
            helper.assertTrue(computer.machine().isRunning(), "Switch failed: " + computer.machine().lastError());
            String marker = new String(ItemDriverData.dataTag(computer.getItem(ComputerCaseBlockEntity.SLOT_EEPROM))
                .getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
            helper.assertTrue(marker.equals("done"), "Switch not complete: " + marker);
            helper.assertTrue(computer.machine().architecture().getClass() == (fromNative ? LuaArchitecture.class : NativeLuaArchitecture.class),
                "CPU metadata changed but running VM did not");
            helper.assertTrue(!oldArchitecture.isInitialized(), "Old architecture remained initialized");
        });
    }
}

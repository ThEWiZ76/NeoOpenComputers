package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class NativeVersionGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void lua53MachinePreservesLocalsAndOpenFile(GameTestHelper helper) { resumes(helper, "5.3"); }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void lua54MachinePreservesLocalsAndOpenFile(GameTestHelper helper) { resumes(helper, "5.4"); }
    @GameTest(template = "empty", timeoutTicks = 2200)
    public static void lua53OpenOsShellSurvivesUnload(GameTestHelper helper) {
        NeoOpenComputersGameTests.nativeVersionOpenOsReload(helper, architecture(helper, "5.3"));
    }
    @GameTest(template = "empty", timeoutTicks = 2200)
    public static void lua54OpenOsShellSurvivesUnload(GameTestHelper helper) {
        NeoOpenComputersGameTests.nativeVersionOpenOsReload(helper, architecture(helper, "5.4"));
    }

    private static Class<? extends Architecture> architecture(GameTestHelper helper, String version) {
        final var found = li.cil.oc.api.Machine.architectures().stream()
            .filter(type -> li.cil.oc.api.Machine.getArchitectureName(type).equals("Lua " + version)).findFirst();
        helper.assertTrue(found.isPresent(), "Missing selectable native Lua " + version + " machine architecture");
        return found.orElseThrow();
    }

    private static void resumes(GameTestHelper helper, String version) {
        final var selected = architecture(helper, version);
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        final var world = helper.getLevel();
        world.setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var original = (ComputerCaseBlockEntity) world.getBlockEntity(pos);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, selected);
        original.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu);
        original.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        final String languageProbe = version.equals("5.4") ? """
            local probe = assert(load("local n = 0; do local resource <close> = setmetatable({}, {__close=function() n=n+1 end}) end; local value <const> = 7; return n,value"))
            local n, value = probe(); assert(n == 1 and value == 7, '5.4 language features missing')
            """ : "";
        original.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
            assert(_VERSION == 'Lua %s', 'wrong VM version: ' .. _VERSION)
            assert(computer.getArchitecture() == 'Lua %s', 'wrong architecture name')
            local operators = assert(load('return 9 // 2, 1 << 4, math.type(7)'))
            local quotient, shift, kind = operators(); assert(quotient == 4 and shift == 16 and kind == 'integer')
            %s
            local eeprom = component.proxy(component.list('eeprom')())
            assert(eeprom.getData() == '', 'unexpected reboot')
            local retained = {value=731}; retained.self = retained
            local fs = component.proxy(computer.tmpAddress())
            local file = assert(fs.open('version.bin', 'w')); assert(fs.write(file, 'a' .. string.char(0,255) .. 'z')); fs.close(file)
            file = assert(fs.open('version.bin', 'r')); assert(fs.read(file, 1) == 'a')
            eeprom.setData('waiting')
            repeat local event = computer.pullSignal() until event == 'continue_version_probe'
            assert(retained.value == 731 and retained.self == retained, 'locals/cycle lost')
            assert(fs.read(file, 3) == string.char(0,255) .. 'z', 'open file offset lost'); fs.close(file)
            eeprom.setData('restored')
            while true do computer.pullSignal() end
            """.formatted(version, version, languageProbe)));
        helper.assertTrue(original.toggleMachine(), "Native version fixture did not start");
        final ComputerCaseBlockEntity[] active = {original};
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(original).equals("waiting"),
            "Version program did not reach checkpoint: " + original.machine().lastError())).thenExecute(() -> {
            original.onChunkUnloaded();
            final var saved = original.saveWithFullMetadata(world.registryAccess());
            original.setRemoved();
            helper.assertTrue(!original.machine().architecture().isInitialized(), "Unload retained native VM");
            active[0] = (ComputerCaseBlockEntity) BlockEntity.loadStatic(pos, original.getBlockState(), saved, world.registryAccess());
            helper.assertTrue(active[0] != null && active[0] != original, "Saved machine was not reconstructed");
            world.setBlockEntity(active[0]); active[0].onLoad();
            helper.assertTrue(active[0].machine().architecture().getClass() == selected, "Saved architecture class changed");
            active[0].machine().signal("continue_version_probe");
        }).thenWaitUntil(() -> helper.assertTrue(marker(active[0]).equals("restored") && active[0].machine().isRunning(),
            "Native version did not resume: " + active[0].machine().lastError())).thenSucceed();
    }

    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(ComputerCaseBlockEntity.SLOT_EEPROM))
            .getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
    private NativeVersionGameTests() {}
}

package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.item.TabletRuntime;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TabletRuntimeGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeTabletResumesAfterRuntimeHandoff(GameTestHelper helper) { lifecycle(helper, true); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeTabletStopsOnOrdinaryEviction(GameTestHelper helper) { lifecycle(helper, false); }

    @GameTest(template = "empty")
    public static void tabletAcceptsItemChargingWithoutReplacingRuntime(GameTestHelper helper) {
        final var item = ModItems.TABLET.get();
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER1.get()), ItemStack.EMPTY);
        item.setMaxCharge(stack, 1000);
        item.setCharge(stack, 500);
        final var runtime = new TabletRuntime(stack, helper.makeMockPlayer(GameType.SURVIVAL));
        try {
            runtime.screen().setPowerState(false);
            helper.assertTrue(item.charge(stack, 200, false) == 200, "Item charge rejected");
            runtime.tick();
            helper.assertTrue(((li.cil.oc.api.network.Connector) runtime.machine().node()).globalBuffer() == 700, "Live battery lost item charging");
            helper.assertTrue(item.getCharge(stack) == 700, "Battery publication overwrote charging");
            helper.assertTrue(item.charge(stack, 50, true) == 50, "Charge simulation failed");
            runtime.tick();
            helper.assertTrue(item.getCharge(stack) == 700, "Simulated charge was applied");
        } finally { runtime.close(false); }
        helper.succeed();
    }

    private static void lifecycle(GameTestHelper helper, boolean preserve) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        final var item = ModItems.TABLET.get();
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), ItemStack.EMPTY,
            cpu, new ItemStack(ModItems.MEMORY_TIER2.get()), RobotMovementPersistenceGameTests.eeprom("""
                local eeprom = component.proxy(component.list('eeprom')())
                assert(eeprom.getData() == '', 'unexpected reboot')
                local retained = {value = 731}
                local fs = component.proxy(computer.tmpAddress())
                local file = assert(fs.open('resume.bin', 'w'))
                assert(fs.write(file, 'a' .. string.char(0,255) .. 'z'))
                fs.close(file)
                file = assert(fs.open('resume.bin', 'r'))
                assert(fs.read(file, 1) == 'a')
                local gpu = component.proxy(component.list('gpu')())
                assert(component.slot(gpu.address) == 4, 'GPU component slot lost')
                local screen = component.list('screen')()
                assert(component.slot(screen) == 0, 'Integrated screen slot lost')
                assert(gpu.bind(screen))
                gpu.set(1, 1, 'tablet ready')
                local tablet = component.proxy(component.list('tablet')())
                assert(type(tablet.getYaw()) == 'number')
                eeprom.setData('waiting')
                repeat local signal = computer.pullSignal() until signal == 'continue_probe'
                assert(retained.value == 731 and fs.address == computer.tmpAddress(), 'state lost')
                assert(fs.read(file, 3) == string.char(0,255) .. 'z', 'file offset lost')
                fs.close(file)
                gpu.set(1, 1, 'tablet restored')
                eeprom.setData('restored')
                while true do computer.pullSignal() end
                """), new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get()), new ItemStack(ModItems.KEYBOARD.get()));
        final TabletRuntime[] runtime = {new TabletRuntime(stack, player)};
        helper.assertTrue(runtime[0].start(), "Tablet did not start");
        for (int tick = 1; tick < 400; tick++) helper.runAtTickTime(tick, () -> runtime[0].tick());
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(marker(runtime[0]).equals("waiting"), "Tablet boot failed: " + runtime[0].machine().lastError()))
            .thenExecute(() -> {
                final TabletRuntime old = runtime[0];
                helper.assertTrue(old.screen().hasKeyboard(), "Tablet keyboard disconnected from screen");
                helper.assertTrue(old.screen().terminalSnapshot().line(0).startsWith("tablet ready"), "Tablet screen did not render boot output");
                final String address = old.machine().node().address();
                final String screenAddress = old.screen().node().address();
                old.close(preserve);
                helper.assertTrue(!old.machine().isRunning() && !old.machine().architecture().isInitialized(), "Disposed tablet retained a VM");
                final ItemStack serialized = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) stack.save(helper.getLevel().registryAccess()));
                runtime[0] = new TabletRuntime(serialized, player);
                helper.assertTrue(address.equals(runtime[0].machine().node().address()), "Tablet computer address changed");
                helper.assertTrue(runtime[0].machine().node().network().node(address) == runtime[0].machine().node(), "Tablet computer address not indexed");
                helper.assertTrue(screenAddress.equals(runtime[0].screen().node().address()), "Tablet screen address changed");
                if (preserve) helper.assertTrue(runtime[0].screen().terminalSnapshot().line(0).startsWith("tablet ready"), "Tablet screen contents lost");
                helper.assertTrue(runtime[0].machine().isRunning() == preserve, "Tablet running state does not match disposal reason");
                if (preserve) helper.assertTrue(runtime[0].machine().signal("continue_probe"), "Resume signal rejected");
            })
            .thenWaitUntil(() -> helper.assertTrue(!preserve || marker(runtime[0]).equals("restored"), "Tablet did not resume: " + runtime[0].machine().lastError()))
            .thenExecute(() -> runtime[0].close(false))
            .thenSucceed();
    }

    private static String marker(TabletRuntime runtime) {
        for (ItemStack component : runtime.internalComponents()) {
            final byte[] data = ItemDriverData.dataTag(component).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG);
            if (data.length > 0) return new String(data, StandardCharsets.UTF_8);
        }
        return "";
    }
}

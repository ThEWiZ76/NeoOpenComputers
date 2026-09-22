package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.item.TabletRuntime;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TabletDimensionGameTests {
    @GameTest(template = "empty", timeoutTicks = 500)
    public static void nativeTabletSurvivesPlayerDimensionRoundTrip(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setNoGravity(true);
        final Vec3 origin = player.position();
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        final ItemStack stack = ModItems.TABLET.get().assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), ItemStack.EMPTY,
            cpu, new ItemStack(ModItems.MEMORY_TIER2.get()), RobotMovementPersistenceGameTests.eeprom("""
                local eeprom = component.proxy(component.list('eeprom')())
                assert(eeprom.getData() == '', 'unexpected reboot')
                local retained = 731
                local fs = component.proxy(computer.tmpAddress())
                local file = assert(fs.open('travel.bin', 'w'))
                assert(fs.write(file, 'a' .. string.char(0,255)))
                fs.close(file)
                file = assert(fs.open('travel.bin', 'r'))
                assert(fs.read(file, 1) == 'a')
                local gpu = component.proxy(component.list('gpu')())
                local screen = component.list('screen')()
                assert(gpu.bind(screen))
                for trip = 1, 2 do
                  gpu.set(1, 1, 'before trip ' .. trip)
                  eeprom.setData('waiting ' .. trip)
                  repeat until computer.pullSignal() == 'dimension_arrived'
                  assert(retained == 730 + trip, 'local state lost')
                  assert(fs.address == computer.tmpAddress(), 'tmp identity changed')
                  assert(fs.read(file, 1) == string.char(trip == 1 and 0 or 255), 'file offset lost')
                  retained = retained + 1
                end
                fs.close(file)
                gpu.set(1, 1, 'round trip complete')
                eeprom.setData('restored')
                while true do computer.pullSignal() end
                """), new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get()), new ItemStack(ModItems.KEYBOARD.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final TabletRuntime[] runtime = {TabletRuntimeRegistry.get(stack, player)};
        helper.assertTrue(runtime[0].start(), "Tablet did not start");
        for (int tick = 1; tick < 500; tick++) helper.runAtTickTime(tick, () -> {
            if (!player.isRemoved()) player.doTick();
        });
        final var sequence = helper.startSequence();
        for (int trip = 1; trip <= 2; trip++) {
            final int step = trip;
            sequence.thenWaitUntil(() -> helper.assertTrue(marker(runtime[0]).equals("waiting " + step), "Tablet did not reach travel checkpoint: " + runtime[0].machine().lastError()))
                .thenExecute(() -> {
                    final TabletRuntime old = runtime[0];
                    final String address = old.machine().node().address();
                    final String screenAddress = old.screen().node().address();
                    final var destination = step == 1 ? helper.getLevel().getServer().getLevel(Level.NETHER) : helper.getLevel();
                    helper.assertTrue(destination != null, "Destination dimension unavailable");
                    final var moved = player.changeDimension(new DimensionTransition(destination,
                        step == 1 ? new Vec3(0.5, 200, 0.5) : origin, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING));
                    helper.assertTrue(moved == player && player.level() == destination, "Player dimension transition failed");
                    // Real inventory processing must notice the new world and perform the handoff.
                    player.doTick();
                    helper.assertTrue(old.isClosed() && !old.machine().architecture().isInitialized(), "Old dimension retained tablet VM");
                    runtime[0] = TabletRuntimeRegistry.get(player.getMainHandItem(), player);
                    helper.assertTrue(runtime[0] != old && runtime[0].world() == destination && runtime[0].machine().isRunning(), "Tablet not restored in destination world");
                    helper.assertTrue(address.equals(runtime[0].machine().node().address()) && screenAddress.equals(runtime[0].screen().node().address()), "Dimension handoff changed component addresses");
                    helper.assertTrue(runtime[0].machine().node().network().node(address) == runtime[0].machine().node(), "Restored machine address not indexed");
                    helper.assertTrue(runtime[0].screen().hasKeyboard(), "Dimension handoff disconnected keyboard");
                    helper.assertTrue(runtime[0].screen().terminalSnapshot().line(0).startsWith("before trip " + step), "Dimension handoff lost screen contents");
                    helper.assertTrue(runtime[0].machine().signal("dimension_arrived"), "Arrival signal rejected");
                });
        }
        sequence.thenWaitUntil(() -> helper.assertTrue(marker(runtime[0]).equals("restored"), "Tablet did not resume after round trip: " + runtime[0].machine().lastError()))
            .thenExecute(() -> {
                helper.assertTrue(runtime[0].screen().terminalSnapshot().line(0).startsWith("round trip complete"), "Restored GPU proxy failed");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.assertTrue(runtime[0].isClosed(), "Logout retained tablet VM");
            }).thenSucceed();
    }

    private static String marker(TabletRuntime runtime) {
        for (ItemStack component : runtime.internalComponents()) {
            final byte[] data = ItemDriverData.dataTag(component).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG);
            if (data.length > 0) return new String(data, StandardCharsets.UTF_8);
        }
        return "";
    }
}

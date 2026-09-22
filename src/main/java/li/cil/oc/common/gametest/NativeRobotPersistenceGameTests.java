package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.LongSupplier;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NativeRobotPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRobotDisposesAndResumes(GameTestHelper helper) { resumes(helper, Reload.DETACHED); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRobotSavesAfterChunkUnload(GameTestHelper helper) { resumes(helper, Reload.UNLOAD_BEFORE_SAVE); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRobotReattachesSameInstance(GameTestHelper helper) { resumes(helper, Reload.SAME_INSTANCE); }

    @GameTest(template = "empty", timeoutTicks = 2000)
    public static void nativeRobotSurvivesRepeatedChunkUnloads(GameTestHelper helper) { resumes(helper, Reload.UNLOAD_BEFORE_SAVE, 20); }

    private enum Reload { DETACHED, UNLOAD_BEFORE_SAVE, SAME_INSTANCE }

    private static void resumes(GameTestHelper helper, Reload mode) {
        resumes(helper, mode, 1);
    }

    private static void resumes(GameTestHelper helper, Reload mode, int reloadCount) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity original = helper.getBlockEntity(pos);
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        RobotMovementPersistenceGameTests.installHardware(helper, original, List.of(cpu,
            new ItemStack(ModItems.MEMORY_TIER1.get()), RobotMovementPersistenceGameTests.eeprom("""
                local robot = component.proxy(component.list('robot')())
                assert(robot.getLightColor() ~= 0x731, 'unexpected reboot')
                local retained = 731
                local fs = component.proxy(computer.tmpAddress())
                local file = assert(fs.open('resume.bin', 'w'))
                assert(fs.write(file, 'a' .. string.char(0,255) .. 'z'))
                fs.close(file)
                file = assert(fs.open('resume.bin', 'r'))
                assert(fs.read(file, 1) == 'a')
                for iteration = 1, %d do
                  robot.setLightColor(0x730 + iteration)
                  repeat local signal = computer.pullSignal() until signal == 'continue_probe'
                  assert(retained == 730 + iteration and fs.address == computer.tmpAddress(), 'state lost')
                  retained = retained + 1
                end
                assert(fs.read(file, 3) == string.char(0,255) .. 'z', 'file continuation lost')
                fs.close(file)
                robot.setLightColor(0x123456)
                while true do computer.pullSignal() end
                """.formatted(reloadCount))));
        original.onLoad();
        original.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(original.toggleMachine(), "Robot did not start");
        final RobotBlockEntity[] loaded = {original};
        final var sequence = helper.startSequence();
        for (int iteration = 1; iteration <= reloadCount; iteration++) {
            final int checkpoint = 0x730 + iteration;
            sequence
            .thenWaitUntil(() -> helper.assertTrue(color(loaded[0]) == checkpoint, "Robot not ready: " + diagnostics(loaded[0])))
            .thenExecute(() -> ((Connector) loaded[0].machine().node()).changeBuffer(10000D))
            .thenIdle(20)
            .thenExecute(() -> {
                final RobotBlockEntity previous = loaded[0];
                var registries = helper.getLevel().registryAccess();
                if (mode != Reload.DETACHED) previous.onChunkUnloaded();
                var saved = previous.saveWithFullMetadata(registries);
                previous.setRemoved();
                helper.assertTrue(!previous.machine().architecture().isInitialized(), "Removed robot retained native VM");
                helper.assertTrue(!previous.machine().isRunning(), "Removed robot still running");
                if (mode == Reload.SAME_INSTANCE) {
                    previous.clearRemoved();
                    loaded[0] = previous;
                } else {
                    var replacement = BlockEntity.loadStatic(previous.getBlockPos(), previous.getBlockState(), saved, registries);
                    helper.assertTrue(replacement instanceof RobotBlockEntity, "Robot snapshot discarded");
                    loaded[0] = (RobotBlockEntity) replacement;
                }
                if (reloadCount > 1) freezeClockBeforePauseExpires(loaded[0].machine());
                helper.getLevel().setBlockEntity(loaded[0]);
                loaded[0].onLoad();
                helper.assertTrue(loaded[0].machine().isRunning(), "Running state lost");
                helper.assertTrue(loaded[0].machine().signal("continue_probe"), "Resume signal rejected");
            });
        }
        sequence
            .thenWaitUntil(() -> {
                helper.assertTrue(loaded[0] != null, "Robot not restored");
                helper.assertTrue(color(loaded[0]) == 0x123456 && loaded[0].machine().isRunning(),
                    "Robot did not resume: " + diagnostics(loaded[0]));
                final ItemStack cargo = loaded[0].getItem(RobotBlockEntity.CARGO_SLOT_START);
                helper.assertTrue(cargo.is(Items.DIAMOND) && cargo.getCount() == 3, "Cargo lost or duplicated");
            })
            .thenSucceed();
    }

    private static int color(RobotBlockEntity robot) { return (Integer) robot.getLightColor(null, null)[0]; }

    private static void freezeClockBeforePauseExpires(Machine machine) {
        try {
            final var deadline = machine.getClass().getDeclaredField("pauseUntilNanos");
            deadline.setAccessible(true);
            // Five ticks before the detached pause expires. Only game time can advance.
            final long frozenTime = deadline.getLong(machine) - 250_000_000L;
            final var clock = machine.getClass().getDeclaredField("nanoTime");
            clock.setAccessible(true);
            clock.set(machine, (LongSupplier) () -> frozenTime);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not freeze detached machine clock", e);
        }
    }

    private static String diagnostics(RobotBlockEntity robot) {
        final Machine machine = robot.machine();
        return "color=" + color(robot) + ", error=" + machine.lastError()
            + ", running=" + machine.isRunning() + ", paused=" + machine.isPaused()
            + ", uptime=" + machine.upTime();
    }
}

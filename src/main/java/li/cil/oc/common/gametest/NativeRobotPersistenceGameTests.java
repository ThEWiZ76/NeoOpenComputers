package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
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

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NativeRobotPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRobotDisposesAndResumes(GameTestHelper helper) { resumes(helper, Reload.DETACHED); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRobotSavesAfterChunkUnload(GameTestHelper helper) { resumes(helper, Reload.UNLOAD_BEFORE_SAVE); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeRobotReattachesSameInstance(GameTestHelper helper) { resumes(helper, Reload.SAME_INSTANCE); }

    private enum Reload { DETACHED, UNLOAD_BEFORE_SAVE, SAME_INSTANCE }

    private static void resumes(GameTestHelper helper, Reload mode) {
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
                robot.setLightColor(0x731)
                repeat local signal = computer.pullSignal() until signal == 'continue_probe'
                assert(retained == 731 and fs.address == computer.tmpAddress(), 'state lost')
                assert(fs.read(file, 3) == string.char(0,255) .. 'z', 'file continuation lost')
                fs.close(file)
                robot.setLightColor(0x123456)
                while true do computer.pullSignal() end
                """)));
        original.onLoad();
        original.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(original.toggleMachine(), "Robot did not start");
        final RobotBlockEntity[] loaded = new RobotBlockEntity[1];
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(color(original) == 0x731, "Robot not ready: " + original.machine().lastError()))
            .thenIdle(20)
            .thenExecute(() -> {
                var registries = helper.getLevel().registryAccess();
                if (mode != Reload.DETACHED) original.onChunkUnloaded();
                var saved = original.saveWithFullMetadata(registries);
                original.setRemoved();
                helper.assertTrue(!original.machine().architecture().isInitialized(), "Removed robot retained native VM");
                helper.assertTrue(!original.machine().isRunning(), "Removed robot still running");
                if (mode == Reload.SAME_INSTANCE) {
                    original.clearRemoved();
                    loaded[0] = original;
                } else {
                    var replacement = BlockEntity.loadStatic(original.getBlockPos(), original.getBlockState(), saved, registries);
                    helper.assertTrue(replacement instanceof RobotBlockEntity, "Robot snapshot discarded");
                    loaded[0] = (RobotBlockEntity) replacement;
                }
                helper.getLevel().setBlockEntity(loaded[0]);
                loaded[0].onLoad();
                helper.assertTrue(loaded[0].machine().isRunning(), "Running state lost");
                helper.assertTrue(loaded[0].machine().signal("continue_probe"), "Resume signal rejected");
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(loaded[0] != null, "Robot not restored");
                helper.assertTrue(color(loaded[0]) == 0x123456 && loaded[0].machine().isRunning(),
                    "Robot did not resume: " + loaded[0].machine().lastError());
                final ItemStack cargo = loaded[0].getItem(RobotBlockEntity.CARGO_SLOT_START);
                helper.assertTrue(cargo.is(Items.DIAMOND) && cargo.getCount() == 3, "Cargo lost or duplicated");
            })
            .thenSucceed();
    }

    private static int color(RobotBlockEntity robot) { return (Integer) robot.getLightColor(null, null)[0]; }
}

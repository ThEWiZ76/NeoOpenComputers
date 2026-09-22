package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NativeDronePersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeDroneDisposesAndResumes(GameTestHelper helper) {
        resumes(helper, false);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeDroneCanSaveAfterRemoval(GameTestHelper helper) {
        resumes(helper, true);
    }

    private static void resumes(GameTestHelper helper, boolean saveAfterRemoval) {
        final DroneEntity original = new DroneEntity(helper.getLevel());
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        original.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        original.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), cpu, new ItemStack(ModItems.MEMORY_TIER1.get()),
            RobotMovementPersistenceGameTests.eeprom("""
                local drone = component.proxy(component.list('drone')())
                assert(drone.getStatusText() == '', 'program rebooted')
                local retained = 731
                local tmp = computer.tmpAddress()
                drone.setStatusText('waiting')
                repeat local signal = computer.pullSignal() until signal == 'continue_probe'
                assert(retained == 731 and tmp == computer.tmpAddress(), 'state lost')
                drone.setStatusText('restored')
                while true do computer.pullSignal() end
                """)), null);
        helper.assertTrue(helper.getLevel().addFreshEntity(original), "Drone did not spawn");
        helper.assertTrue(original.toggleMachine(), "Drone did not start");
        final CompoundTag[] saved = new CompoundTag[1];
        final DroneEntity[] loaded = new DroneEntity[1];
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(status(original).equals("waiting"), "Drone not ready: " + original.machine().lastError()))
            .thenIdle(20)
            .thenExecute(() -> {
                if (!saveAfterRemoval) saved[0] = original.saveWithoutId(new CompoundTag());
                original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                original.onRemovedFromLevel();
                if (saveAfterRemoval) saved[0] = original.saveWithoutId(new CompoundTag());
                helper.assertTrue(!original.machine().architecture().isInitialized(), "Removed drone retained native VM");
                helper.assertTrue(!original.machine().isRunning(), "Removed drone still running");
            })
            .thenIdle(2)
            .thenExecute(() -> {
                loaded[0] = new DroneEntity(helper.getLevel());
                loaded[0].load(saved[0]);
                helper.assertTrue(loaded[0].machine().architecture() instanceof NativeLuaArchitecture, "Drone hardware was not restored before VM load");
                helper.assertTrue(helper.getLevel().addFreshEntity(loaded[0]), "Restored drone did not spawn");
                helper.assertTrue(loaded[0].machine().isRunning(), "Running state lost");
                helper.assertTrue(loaded[0].machine().signal("continue_probe"), "Resume signal rejected");
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(loaded[0] != null, "Drone not restored");
                helper.assertTrue(status(loaded[0]).equals("restored") && loaded[0].machine().isRunning(),
                    "Drone did not resume: " + loaded[0].machine().lastError());
            })
            .thenSucceed();
    }

    private static String status(DroneEntity drone) {
        return (String) drone.getStatusText(null, null)[0];
    }
}

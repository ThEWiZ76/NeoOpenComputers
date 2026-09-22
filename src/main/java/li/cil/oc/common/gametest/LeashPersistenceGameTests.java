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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class LeashPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeDroneRestoresLeashedSheep(GameTestHelper helper) {
        final var original = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        original.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        original.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), cpu, new ItemStack(ModItems.MEMORY_TIER1.get()),
            new ItemStack(ModItems.LEASH_UPGRADE.get()), RobotMovementPersistenceGameTests.eeprom("""
                local drone = component.proxy(component.list('drone')())
                local leash = component.proxy(component.list('leash')())
                assert(drone.getStatusText() == '', 'unexpected reboot')
                assert(leash.leash(5))
                drone.setStatusText('waiting')
                repeat until computer.pullSignal() == 'continue_leash'
                leash.unleash()
                drone.setStatusText('restored')
                while true do computer.pullSignal() end
                """)), null);
        helper.assertTrue(helper.getLevel().addFreshEntity(original), "Drone did not spawn");
        final var sheep = EntityType.SHEEP.create(helper.getLevel());
        helper.assertTrue(sheep != null, "Sheep missing");
        sheep.setNoAi(true);
        sheep.setNoGravity(true);
        sheep.moveTo(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(sheep);
        helper.assertTrue(original.toggleMachine(), "Drone did not start");
        final CompoundTag[] saved = {null};
        final DroneEntity[] loaded = {null};
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(status(original).equals("waiting"), "Leash setup failed: " + original.machine().lastError()))
            .thenExecute(() -> {
                helper.assertTrue(sheep.getLeashHolder() == original, "Leash is not attached to the actual drone entity");
                saved[0] = original.saveWithoutId(new CompoundTag());
                sheep.dropLeash(true, false);
                original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                original.onRemovedFromLevel();
            })
            .thenIdle(2)
            .thenExecute(() -> {
                loaded[0] = new DroneEntity(helper.getLevel());
                loaded[0].load(saved[0]);
                helper.assertTrue(helper.getLevel().addFreshEntity(loaded[0]), "Restored drone did not spawn");
            })
            .thenWaitUntil(() -> helper.assertTrue(sheep.getLeashHolder() == loaded[0], "Saved sheep was not reacquired by the new drone"))
            .thenExecute(() -> helper.assertTrue(loaded[0].machine().signal("continue_leash"), "Resume signal rejected"))
            .thenWaitUntil(() -> helper.assertTrue(status(loaded[0]).equals("restored"), "Old leash proxy failed: " + loaded[0].machine().lastError()))
            .thenExecute(() -> helper.assertTrue(!sheep.isLeashed(), "Restored leash did not release sheep"))
            .thenSucceed();
    }

    private static String status(DroneEntity drone) { return (String) drone.getStatusText(null, null)[0]; }
}

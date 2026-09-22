package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class DroneMovementGameTests {
    @GameTest(template = "empty")
    public static void submergedDroneStopsInWaterAndRestartsDry(GameTestHelper helper) {
        submerged(helper, net.minecraft.world.level.block.Blocks.WATER, net.minecraft.tags.FluidTags.WATER);
    }

    @GameTest(template = "empty")
    public static void submergedDroneStopsInLavaAndRestartsDry(GameTestHelper helper) {
        submerged(helper, net.minecraft.world.level.block.Blocks.LAVA, net.minecraft.tags.FluidTags.LAVA);
    }

    private static void submerged(GameTestHelper helper, net.minecraft.world.level.block.Block fluid,
                                  net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> tag) {
        final var drone = create(helper);
        final var position = drone.position();
        final var block = drone.blockPosition();
        helper.getLevel().setBlockAndUpdate(block, fluid.defaultBlockState());
        helper.assertTrue(drone.toggleMachine(), "Submersion fixture did not start");
        drone.tick();
        helper.assertTrue(drone.isEyeInFluid(tag), "Submersion fixture did not cover drone eyes");
        helper.assertTrue(!drone.machine().isRunning(), "Submerged drone kept running in " + fluid);
        helper.assertTrue(drone.getDeltaMovement().y < 0, "Submerged stopped drone did not fall");
        helper.getLevel().setBlockAndUpdate(block, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        drone.setPos(position.add(0, 2, 0));
        drone.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(drone.toggleMachine(), "Dry drone could not restart");
        drone.tick();
        helper.assertTrue(!drone.isEyeInFluid(tag) && drone.machine().isRunning(), "Dry drone stopped again after restart");
        drone.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droneAcceleratesAndRetainsLateralMomentum(GameTestHelper helper) throws Exception {
        final var drone = create(helper);
        helper.assertTrue(drone.toggleMachine(), "Movement fixture did not start");
        ((Component) drone.node()).invoke("move", drone.machine(), 10D, 0D, 0D);
        final double initialX = drone.getX();
        drone.tick();
        near(helper, drone.getX() - initialX, 0.1, "First acceleration step");
        near(helper, drone.getDeltaMovement().x, 0.08, "Air drag after first step");
        drone.tick();
        near(helper, drone.getX() - initialX, 0.28, "Second acceleration step must retain velocity");
        near(helper, drone.getDeltaMovement().x, 0.144, "Second step drag");
        final double initialZ = drone.getZ();
        drone.setDeltaMovement(new Vec3(0, 0, 0.2));
        drone.tick();
        near(helper, drone.getZ() - initialZ, 0.2, "Lateral impulse disappeared before movement");
        drone.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stoppedDroneFallsAndHonorsNoGravity(GameTestHelper helper) {
        final var drone = create(helper);
        final double initialY = drone.getY();
        drone.tick();
        near(helper, drone.getY() - initialY, -0.05, "Stopped drone must fall");
        near(helper, drone.getDeltaMovement().y, -0.04, "Falling drag");
        drone.setNoGravity(true);
        drone.setDeltaMovement(Vec3.ZERO);
        final double suspendedY = drone.getY();
        drone.tick();
        near(helper, drone.getY(), suspendedY, "NoGravity drone must remain suspended");
        drone.discard();
        helper.succeed();
    }

    private static DroneEntity create(GameTestHelper helper) {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 8, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), new ItemStack(ModItems.CPU_TIER1.get()),
            new ItemStack(ModItems.MEMORY_TIER1.get()), RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end")), null);
        return drone;
    }

    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.000001, message + ": " + actual);
    }
}

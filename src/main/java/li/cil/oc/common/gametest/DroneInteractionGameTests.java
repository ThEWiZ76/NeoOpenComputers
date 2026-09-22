package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class DroneInteractionGameTests {
    @GameTest(template = "empty")
    public static void playerCanTargetDroneAndSendHitSignal(GameTestHelper helper) {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), new ItemStack(ModItems.CPU_TIER1.get()),
            new ItemStack(ModItems.MEMORY_TIER1.get()), RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end")), null);
        helper.getLevel().addFreshEntity(drone);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final Vec3 center = drone.getBoundingBox().getCenter();
        final Vec3 start = center.add(-2, 0, 0);
        final Vec3 end = center.add(2, 0, 0);
        final var hit = ProjectileUtil.getEntityHitResult(helper.getLevel(), player, start, end,
            new AABB(start, end).inflate(0.5), entity -> entity.isPickable() && !entity.isSpectator());
        helper.assertTrue(hit != null && hit.getEntity() == drone, "Normal entity targeting cannot hit drone");
        helper.assertTrue(drone.isPushable(), "Drone cannot be pushed like upstream");
        player.moveTo(drone.getX() - 2, drone.getY() - player.getEyeHeight(), drone.getZ(), 0, 0);
        helper.assertTrue(drone.toggleMachine(), "Drone did not start");
        while (drone.machine().popSignal() != null) { }
        drone.setDeltaMovement(Vec3.ZERO);
        player.attack(drone);
        final var signal = drone.machine().popSignal();
        helper.assertTrue(signal != null && signal.name().equals("hit"), "Player attack did not emit hit signal");
        helper.assertTrue(Math.abs(((Number) signal.args()[0]).doubleValue() + 1) < 0.000001
            && Math.abs(((Number) signal.args()[1]).doubleValue()) < 0.000001
            && Math.abs(((Number) signal.args()[2]).doubleValue()) < 0.000001, "Hit direction does not use upstream x,z,y order");
        helper.assertTrue(signal.args().length == (ModSettings.inputUsername() ? 4 : 3), "Hit signal ignored username setting");
        helper.assertTrue(drone.getDeltaMovement().distanceTo(new Vec3(0.5, 0, 0)) < 0.000001, "Hit did not push drone away from player");
        drone.machine().stop();
        drone.setDeltaMovement(Vec3.ZERO);
        player.attack(drone);
        helper.assertTrue(drone.machine().popSignal() == null && drone.getDeltaMovement().equals(Vec3.ZERO), "Stopped drone reacted as running");
        drone.discard();
        helper.assertTrue(!drone.isPickable(), "Removed drone remains targetable");
        helper.succeed();
    }
}

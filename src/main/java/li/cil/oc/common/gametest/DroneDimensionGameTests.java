package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class DroneDimensionGameTests {
    @GameTest(template = "empty")
    public static void droneDimensionRoundtripPreservesRelativeTargetAndCargo(GameTestHelper helper) throws Exception {
        final var original = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        original.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        final var origin = original.position();
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        original.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), cpu, new ItemStack(ModItems.MEMORY_TIER1.get()),
            new ItemStack(ModItems.INVENTORY_UPGRADE.get()), RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end")), null);
        original.mainInventory().setItem(2, new ItemStack(Items.DIAMOND, 7));
        helper.getLevel().addFreshEntity(original);
        helper.assertTrue(original.toggleMachine(), "Dimension fixture did not start");
        final var offset = new Vec3(3, 2, -4);
        ((Component) original.node()).invoke("move", original.machine(), offset.x, offset.y, offset.z);
        final String address = original.node().address();
        final var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether missing");
        DroneEntity active = original;
        try {
            for (int step = 0; step < 2; step++) {
                final var previous = active;
                final var destination = step == 0 ? nether : helper.getLevel();
                final var target = step == 0 ? new Vec3(0.5, 200, 0.5) : origin;
                active = (DroneEntity) previous.changeDimension(new DimensionTransition(destination, target, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING));
                helper.assertTrue(active != null && active != previous && active.level() == destination, "Drone did not change dimensions");
                helper.assertTrue(active.getTarget().subtract(active.position()).distanceTo(offset) < 0.000001,
                    "Dimension change retained absolute target instead of relative offset");
                helper.assertTrue(previous.isRemoved() && !previous.machine().isRunning()
                    && !previous.machine().architecture().isInitialized(), "Old dimension retained a live drone VM");
                helper.assertTrue(address.equals(active.node().address()), "Dimension change replaced drone address");
                helper.assertTrue(active.mainInventory().getItem(2).is(Items.DIAMOND)
                    && active.mainInventory().getItem(2).getCount() == 7, "Dimension change lost cargo");
            }
            helper.succeed();
        } finally {
            if (active != null) active.discard();
        }
    }
}

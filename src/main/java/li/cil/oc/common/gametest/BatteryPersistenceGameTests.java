package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.component.BatteryUpgradeEnvironment;
import li.cil.oc.common.item.BatteryUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class BatteryPersistenceGameTests {
    @GameTest(template = "empty")
    public static void legacyBatteryChargeMigratesWithoutLosingItemData(GameTestHelper helper) {
        final var item = ModItems.BATTERY_UPGRADE_TIER1.get();
        final var stack = new ItemStack(item);
        final var root = new net.minecraft.nbt.CompoundTag();
        final var legacy = new net.minecraft.nbt.CompoundTag();
        legacy.putDouble("charge", 333);
        root.put("oc:battery", legacy);
        root.putString("unrelated", "keep");
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root));
        final var environment = item.createEnvironment(stack, null);
        helper.assertTrue(((Connector) environment.node()).localBuffer() == 333, "Legacy item charge did not migrate");
        environment.save(new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(item.chargeStored(stack) == 333, "Saving migrated charge changed its value");
        helper.assertTrue(item.charge(stack, 67, false) == 67, "Migrated battery rejected recharge");
        final var restored = item.createEnvironment(stack, null);
        helper.assertTrue(((Connector) restored.node()).localBuffer() == 400, "Migrated connector ignored recharge");
        helper.assertTrue("keep".equals(stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getString("unrelated")),
            "Battery migration changed unrelated item data");
        environment.node().remove();
        restored.node().remove();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void installedSolarPanelRetainsEnergyAndContinuesGenerating(GameTestHelper helper) {
        final var level = helper.getLevel();
        final long previousDayTime = level.getDayTime();
        try {
            level.setDayTime(6000);
            final var pos = new BlockPos(1, 5, 1);
            helper.setBlock(pos, ModBlocks.ROBOT.get());
            RobotBlockEntity robot = helper.getBlockEntity(pos);
            robot.setTier(2);
            RobotMovementPersistenceGameTests.installHardware(helper, robot, List.of(new ItemStack(ModItems.SOLAR_GENERATOR_UPGRADE.get())));
            robot.onLoad();
            final Connector machine = (Connector) robot.machine().node();
            final double before = machine.globalBuffer();
            robot.machine().update();
            final double charged = machine.globalBuffer();
            helper.assertTrue(Math.abs(charged - before - li.cil.oc.common.ModSettings.solarGeneratorEfficiency()) < 0.000001,
                "Installed solar panel did not charge stopped robot");
            final var saved = robot.saveWithFullMetadata(level.registryAccess());
            robot.setRemoved();
            robot = (RobotBlockEntity) BlockEntity.loadStatic(robot.getBlockPos(), robot.getBlockState(), saved, level.registryAccess());
            level.setBlockEntity(robot);
            robot.onLoad();
            final Connector restored = (Connector) robot.machine().node();
            helper.assertTrue(Math.abs(restored.globalBuffer() - charged) < 0.000001, "Solar reload lost stored energy");
            robot.machine().update();
            helper.assertTrue(Math.abs(restored.globalBuffer() - charged - li.cil.oc.common.ModSettings.solarGeneratorEfficiency()) < 0.000001,
                "Restored solar panel did not resume generation");
            helper.succeed();
        } finally {
            level.setDayTime(previousDayTime);
        }
    }

    @GameTest(template = "empty")
    public static void batteryTier1RetainsCharge(GameTestHelper helper) { verify(helper, ModItems.BATTERY_UPGRADE_TIER1.get()); }
    @GameTest(template = "empty")
    public static void batteryTier2RetainsCharge(GameTestHelper helper) { verify(helper, ModItems.BATTERY_UPGRADE_TIER2.get()); }
    @GameTest(template = "empty")
    public static void batteryTier3RetainsCharge(GameTestHelper helper) { verify(helper, ModItems.BATTERY_UPGRADE_TIER3.get()); }

    private static void verify(GameTestHelper helper, BatteryUpgradeItem item) {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.setTier(2);
        RobotMovementPersistenceGameTests.installHardware(helper, robot,
            List.of(new ItemStack(ModItems.UPGRADE_CONTAINER_TIER3.get())));
        robot.onLoad();
        final var stack = new ItemStack(item);
        helper.assertTrue(item.charge(stack, 731, false) == 731, "Battery fixture could not charge");
        robot.setItem(1, stack);
        helper.assertTrue(battery(robot).localBuffer() == 731, "Installed battery lost item charge");
        battery(robot).changeBuffer(-231);
        helper.assertTrue(battery(robot).localBuffer() == 500, "Battery fixture did not consume local energy");
        final double energy = ((Connector) robot.machine().node()).globalBuffer();
        final var address = battery(robot).address();
        for (int repeat = 0; repeat < 2; repeat++) {
            final var saved = robot.saveWithFullMetadata(helper.getLevel().registryAccess());
            robot.setRemoved();
            robot = (RobotBlockEntity) BlockEntity.loadStatic(robot.getBlockPos(), robot.getBlockState(), saved, helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(robot);
            robot.onLoad();
            helper.assertTrue(battery(robot).localBuffer() == 500, "Battery charge changed on reload");
            helper.assertTrue(((Connector) robot.machine().node()).globalBuffer() == energy, "Reload lost or duplicated network energy");
            helper.assertTrue(address.equals(battery(robot).address()), "Battery node identity changed");
        }
        final var removed = robot.removeItemNoUpdate(1);
        helper.assertTrue(item.chargeStored(removed) == 500, "Removed battery item has stale charge");
        helper.assertTrue(item.charge(removed, 100, true) == 100 && item.chargeStored(removed) == 500, "Simulated charging mutated battery");
        helper.assertTrue(item.charge(removed, 100, false) == 100, "Removed battery did not recharge");
        robot.setItem(1, removed);
        helper.assertTrue(battery(robot).localBuffer() == 600, "Reinstalled battery ignored recharging");
        helper.succeed();
    }

    private static Connector battery(RobotBlockEntity robot) {
        for (var node : robot.machine().node().neighbors()) {
            if (node.host() instanceof BatteryUpgradeEnvironment) return (Connector) node;
        }
        throw new AssertionError("Installed battery node missing");
    }
}

package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class AgentBlockComparisonGameTests {
    @GameTest(template = "empty")
    public static void robotComparesSelectedBlockItem(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.onLoad();
        verify(helper, robot, 3, robot.toGlobal(Direction.SOUTH));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droneComparesSelectedBlockItem(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get())), null);
        try { verify(helper, drone, 5, Direction.EAST); }
        finally { drone.discard(); }
        helper.succeed();
    }

    private static void verify(GameTestHelper helper, Agent agent, int side, Direction direction) throws Exception {
        final var target = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(direction);
        final var component = (Component) ((Environment) agent).node();
        final var inventory = agent.mainInventory();
        agent.setSelectedSlot(1);
        helper.getLevel().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        check(helper, agent, side, false);
        inventory.setItem(0, new ItemStack(Items.DIRT, 9));
        inventory.setItem(1, new ItemStack(Items.STONE, 7));
        check(helper, agent, side, true);
        helper.getLevel().setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
        check(helper, agent, side, false);
        inventory.setItem(1, new ItemStack(Items.OAK_LOG, 7));
        helper.getLevel().setBlockAndUpdate(target, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        check(helper, agent, side, true);
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("compare", agent.machine(), side, true)[0]), "Fuzzy block match failed");
        inventory.setItem(1, new ItemStack(Items.REDSTONE, 7));
        helper.getLevel().setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(target, Blocks.REDSTONE_WIRE.defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockState(target).is(Blocks.REDSTONE_WIRE), "Redstone fixture did not remain placed");
        check(helper, agent, side, true);
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 7));
        check(helper, agent, side, false);
        helper.assertTrue(inventory.getItem(0).getCount() == 9 && inventory.getItem(1).getCount() == 7
            && agent.selectedSlot() == 1, "Comparison mutated cargo or selection");
        inventory.setItem(1, ItemStack.EMPTY);
        try {
            component.invoke("compare", agent.machine(), agent instanceof Robot ? 2 : 6);
            throw new AssertionError("Invalid side accepted with empty inventory");
        } catch (IllegalArgumentException expected) { }
    }

    private static void check(GameTestHelper helper, Agent agent, int side, boolean expected) throws Exception {
        final var result = ((Component) ((Environment) agent).node()).invoke("compare", agent.machine(), side);
        helper.assertTrue(Boolean.valueOf(expected).equals(result[0]), "Unexpected block comparison: " + java.util.Arrays.toString(result));
    }
}

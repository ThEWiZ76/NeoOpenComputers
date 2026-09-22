package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class AgentDetectionGameTests {
    @GameTest(template = "empty")
    public static void robotDetectsContentsAndProtection(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.onLoad();
        verify(helper, robot, 3, robot.toGlobal(Direction.SOUTH));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droneDetectsContentsAndProtection(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        try { verify(helper, drone, 5, Direction.EAST); }
        finally { drone.discard(); }
        helper.succeed();
    }

    private static void verify(GameTestHelper helper, Agent agent, int side, Direction direction) throws Exception {
        final var target = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(direction);
        final var world = helper.getLevel();
        world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        check(helper, agent, side, false, "air");
        world.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        check(helper, agent, side, true, "solid");
        world.setBlockAndUpdate(target, Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        check(helper, agent, side, true, "solid");
        world.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
        check(helper, agent, side, false, "liquid");
        java.util.function.Consumer<BlockEvent.BreakEvent> protection = event -> {
            if (event.getLevel() == world && event.getPos().equals(target)) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(protection);
        try {
            check(helper, agent, side, true, "liquid");
            world.setBlockAndUpdate(target, Blocks.SHORT_GRASS.defaultBlockState());
            check(helper, agent, side, true, "replaceable");
        } finally { NeoForge.EVENT_BUS.unregister(protection); }
        check(helper, agent, side, false, "replaceable");
        world.setBlockAndUpdate(target, Blocks.TORCH.defaultBlockState());
        check(helper, agent, side, true, "passable");
        world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        final var sheep = EntityType.SHEEP.create(world);
        sheep.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0, 0);
        world.addFreshEntity(sheep);
        try { check(helper, agent, side, true, "entity"); }
        finally { sheep.discard(); }
        check(helper, agent, side, false, "air");
        try {
            ((Component) ((Environment) agent).node()).invoke("detect", agent.machine(), 6);
            throw new AssertionError("Invalid side accepted");
        } catch (IllegalArgumentException expected) { }
    }

    private static void check(GameTestHelper helper, Agent agent, int side, boolean blocked, String kind) throws Exception {
        final var result = ((Component) ((Environment) agent).node()).invoke("detect", agent.machine(), side);
        helper.assertTrue(Boolean.valueOf(blocked).equals(result[0]) && kind.equals(result[1]),
            "Expected " + blocked + "/" + kind + ", got " + java.util.Arrays.toString(result));
    }
}

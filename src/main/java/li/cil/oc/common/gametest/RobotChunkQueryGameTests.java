package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.block.RobotAfterimageBlock;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class RobotChunkQueryGameTests {
    @GameTest(template = "empty")
    public static void robotGeometryDoesNotLoadMissingOriginChunk(GameTestHelper helper) {
        final var world = helper.getLevel();
        final var pos = new BlockPos(17_600_008, 80, -17_600_008);
        helper.assertTrue(!world.hasChunkAt(pos), "Geometry fixture requires an unloaded origin");
        final var state = ModBlocks.ROBOT.get().defaultBlockState();
        final var expected = new AABB(1D / 16, 0, 1D / 16, 15D / 16, 14D / 16, 15D / 16);
        helper.assertTrue(state.getShape(world, pos).bounds().equals(expected), "Unloaded robot lost fallback outline");
        helper.assertTrue(!world.hasChunkAt(pos), "Robot outline query forced its unloaded origin chunk to load");
        helper.assertTrue(state.getCollisionShape(world, pos).bounds().equals(expected), "Unloaded robot lost fallback collision");
        helper.assertTrue(!world.hasChunkAt(pos), "Robot collision query forced its unloaded origin chunk to load");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotRedstoneDoesNotLoadMissingOriginChunk(GameTestHelper helper) {
        final var world = helper.getLevel();
        final var pos = new BlockPos(17_601_608, 80, -17_600_008);
        helper.assertTrue(!world.hasChunkAt(pos), "Redstone fixture requires an unloaded origin");
        final var state = ModBlocks.ROBOT.get().defaultBlockState();
        for (final var side : Direction.values()) {
            helper.assertTrue(state.getSignal(world, pos, side) == 0 && state.getDirectSignal(world, pos, side) == 0
                && !state.getBlock().canConnectRedstone(state, world, pos, side), "Unloaded robot reported redstone hardware/output");
            helper.assertTrue(!world.hasChunkAt(pos), "Robot redstone query forced its unloaded origin chunk to load");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotAfterimageSkipsUnloadedReaderNeighbors(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final var target = pos.east();
        final var robot = new RobotBlockEntity(target, ModBlocks.ROBOT.get().defaultBlockState());
        robot.setLevel(helper.getLevel());
        final var tag = new CompoundTag(); final var animation = new CompoundTag();
        animation.putLong("from", pos.asLong()); tag.put("oc:animation", animation);
        robot.handleUpdateTag(tag, helper.getLevel().registryAccess());
        final var reads = new java.util.concurrent.atomic.AtomicInteger();
        final var reader = (LevelReader) java.lang.reflect.Proxy.newProxyInstance(LevelReader.class.getClassLoader(), new Class<?>[]{LevelReader.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "hasChunkAt" -> target.equals(args[0]);
                case "getBlockEntity" -> {
                    if (!target.equals(args[0])) throw new IllegalStateException("Afterimage queried an unloaded reader neighbor");
                    reads.incrementAndGet(); yield robot;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
        try {
            helper.assertTrue(RobotAfterimageBlock.findRobot(reader, pos) == robot && reads.get() == 1,
                "Afterimage failed to find its loaded target through LevelReader");
            final var shape = ModBlocks.ROBOT_AFTERIMAGE.get().defaultBlockState().getShape(reader, pos);
            helper.assertTrue(!shape.isEmpty() && shape.bounds().minX == 1D + 1D / 16,
                "Afterimage lost its loaded robot's translated geometry");
        } finally { robot.setRemoved(); }
        helper.succeed();
    }
}

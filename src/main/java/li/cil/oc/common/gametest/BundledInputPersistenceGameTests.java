package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.blockentity.RedstoneIoBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class BundledInputPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 220)
    public static void caseBundledInputsSurviveUnloadAndFreshHostLoad(GameTestHelper helper) throws Exception { restore(helper, ModBlocks.COMPUTER_CASE_TIER2.get().defaultBlockState()); }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void microcontrollerBundledInputsSurviveUnloadAndFreshHostLoad(GameTestHelper helper) throws Exception { restore(helper, ModBlocks.MICROCONTROLLER_TIER2.get().defaultBlockState()); }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void robotBundledInputsSurviveUnloadAndFreshHostLoad(GameTestHelper helper) throws Exception { restore(helper, ModBlocks.ROBOT.get().defaultBlockState()); }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void redstoneIoBundledInputsAndChangesSurviveUnloadAndFreshHostLoad(GameTestHelper helper) throws Exception { restore(helper, ModBlocks.REDSTONE_IO.get().defaultBlockState()); }

    private static void restore(GameTestHelper helper, BlockState state) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) { helper.succeed(); return; }
        final var world = helper.getLevel();
        final var registries = world.registryAccess();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, state);
        final var original = world.getBlockEntity(pos);
        final var host = (RedstoneControllerHost) original;
        final var cleanup = RedstoneCardGameTests.bootComputerCraft(helper, pos.above(), "redstone.setBundledOutput('bottom', 32773)");
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(host.bundledRedstoneInput(Direction.UP, 15) == 15 && host.bundledRedstoneInput(Direction.UP, 0) == 15,
                "Host did not sample the real CC source");
        }).thenExecute(() -> {
            original.onChunkUnloaded();
            final var saved = original.saveWithFullMetadata(registries);
            world.removeBlockEntity(pos);
            final var restored = BlockEntity.loadStatic(pos, state, saved, registries);
            helper.assertTrue(restored != null && restored != original, "Persistence fixture did not create a fresh host");
            restored.setLevel(world);
            final var restoredHost = (RedstoneControllerHost) restored;
            helper.assertTrue(restoredHost.bundledRedstoneInput(Direction.UP, 15) == 15 && restoredHost.bundledRedstoneInput(Direction.UP, 2) == 15,
                "Fresh host lost previously sampled bundled input before its first tick");
            world.setBlockEntity(restored);
            restored.onLoad();
            final var events = new java.util.ArrayList<Object[]>();
            final var listener = new AbstractManagedEnvironment() {
                { setNode(Network.newNode(this, Visibility.Network).create()); }
                @Override public void onMessage(li.cil.oc.api.network.Message message) {
                    if (message.name().equals("computer.signal")) events.add(message.data());
                }
            };
            if (restored instanceof RedstoneIoBlockEntity io) {
                helper.assertTrue(io.node().network() != null, "Restored redstone IO did not rejoin its world network");
                io.node().connect(listener.node());
                helper.assertTrue(io.node().isNeighborOf(listener.node()), "Persistence listener did not connect to the restored IO node");
            }
            try {
                sample(restored, world, pos, state);
                helper.assertTrue(events.isEmpty(), "Unchanged restored input emitted duplicate events");
                final var oldSave = saved.copy();
                oldSave.remove("oc:bundledRedstoneInputs");
                restored.loadWithComponents(oldSave, registries);
                helper.assertTrue(restoredHost.bundledRedstoneInput(Direction.UP, 15) == 0, "Loading an older save retained stale in-memory inputs");
                sample(restored, world, pos, state);
                helper.assertTrue(restoredHost.bundledRedstoneInput(Direction.UP, 15) == 15 && events.isEmpty(), "First sample after an older save emitted a false change");
                restored.loadWithComponents(saved, registries);
                cleanup.run();
                sample(restored, world, pos, state);
                helper.assertTrue(restoredHost.bundledRedstoneInput(Direction.UP, 15) == 0, "Changed source was not sampled after restore");
                if (restored instanceof RedstoneIoBlockEntity) {
                    helper.assertTrue(events.size() == 3, "Saved-to-current transition did not emit exactly three falling colors");
                    for (int index = 0; index < 3; index++) {
                        helper.assertTrue(java.util.Arrays.equals(events.get(index), new Object[]{"redstone_changed", 1, 15, 0, new int[]{0, 2, 15}[index]}),
                            "Restored change lost side, old/new values or color");
                    }
                    sample(restored, world, pos, state);
                    helper.assertTrue(events.size() == 3, "Repeated sampling duplicated restored changes");
                }
            } finally { listener.node().remove(); cleanup.run(); }
        }).thenSucceed();
    }

    private static void sample(BlockEntity blockEntity, net.minecraft.world.level.Level world, BlockPos pos, BlockState state) {
        if (blockEntity instanceof ComputerCaseBlockEntity computer) ComputerCaseBlockEntity.serverTick(world, pos, state, computer);
        else if (blockEntity instanceof MicrocontrollerBlockEntity micro) MicrocontrollerBlockEntity.serverTick(world, pos, state, micro);
        else if (blockEntity instanceof RobotBlockEntity robot) RobotBlockEntity.serverTick(world, pos, state, robot);
        else if (blockEntity instanceof RedstoneIoBlockEntity io) io.updateRedstoneInputs();
    }
}

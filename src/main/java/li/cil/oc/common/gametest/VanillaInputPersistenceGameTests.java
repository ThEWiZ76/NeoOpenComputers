package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.*;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class VanillaInputPersistenceGameTests {
    @GameTest(template = "empty")
    public static void caseKeepsVanillaInputsAcrossUnloadAndFreshLoad(GameTestHelper helper) { restore(helper, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState()); }
    @GameTest(template = "empty")
    public static void microcontrollerKeepsVanillaInputsAcrossUnloadAndFreshLoad(GameTestHelper helper) { restore(helper, ModBlocks.MICROCONTROLLER_TIER1.get().defaultBlockState()); }
    @GameTest(template = "empty")
    public static void redstoneIoKeepsVanillaInputsAndOnlyEmitsRealChangesAfterLoad(GameTestHelper helper) { restore(helper, ModBlocks.REDSTONE_IO.get().defaultBlockState()); }
    @GameTest(template = "empty")
    public static void robotKeepsVanillaInputsAcrossUnloadAndFreshLoad(GameTestHelper helper) { restore(helper, ModBlocks.ROBOT.get().defaultBlockState()); }

    private static void restore(GameTestHelper helper, BlockState state) {
        final var world = helper.getLevel();
        final var registries = world.registryAccess();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, state);
        world.setBlockAndUpdate(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        final var original = world.getBlockEntity(pos);
        final var originalHost = (RedstoneControllerHost) original;
        originalHost.setWakeThreshold(8);
        sample(original);
        helper.assertTrue(originalHost.redstoneInput(Direction.UP) == 15, "Initial input was not sampled");
        original.onChunkUnloaded();
        final var saved = original.saveWithFullMetadata(registries);
        world.removeBlockEntity(pos);
        final var restored = BlockEntity.loadStatic(pos, state, saved, registries);
        helper.assertTrue(restored != null && restored != original, "Fixture did not instantiate a fresh host");
        restored.setLevel(world);
        final var host = (RedstoneControllerHost) restored;
        helper.assertTrue(host.redstoneInput(Direction.UP) == 15, "Fresh host lost its sampled input before first tick");
        world.setBlockEntity(restored);
        restored.onLoad();
        final var events = new java.util.ArrayList<Object[]>();
        final int[] starts = {0};
        final var listener = new AbstractManagedEnvironment() {
            { setNode(Network.newNode(this, Visibility.Network).create()); }
            @Override public void onMessage(li.cil.oc.api.network.Message message) {
                if (message.name().equals("computer.signal")) events.add(message.data());
                if (message.name().equals("computer.start")) starts[0]++;
            }
        };
        if (restored instanceof RedstoneIoBlockEntity io) io.node().connect(listener.node());
        try {
            sample(restored);
            helper.assertTrue(events.isEmpty() && starts[0] == 0, "Unchanged restored input signalled or woke the network");
            final var older = saved.copy();
            older.remove("oc:redstoneInputs");
            restored.loadWithComponents(older, registries);
            helper.assertTrue(host.redstoneInput(Direction.UP) == 0, "Older save retained stale in-memory input");
            sample(restored);
            helper.assertTrue(host.redstoneInput(Direction.UP) == 15 && events.isEmpty() && starts[0] == 0,
                "First sample of older data must not signal or wake");
            restored.loadWithComponents(saved, registries);
            world.removeBlock(pos.above(), false);
            sample(restored);
            helper.assertTrue(host.redstoneInput(Direction.UP) == 0, "Changed input was not sampled");
            if (restored instanceof RedstoneIoBlockEntity) {
                helper.assertTrue(events.size() == 1 && java.util.Arrays.equals(events.getFirst(), new Object[]{"redstone_changed", 1, 15, 0}) && starts[0] == 0,
                    "Falling edge lost history or spuriously woke the network");
                sample(restored);
                helper.assertTrue(events.size() == 1, "Unchanged input duplicated an event");
                world.setBlockAndUpdate(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                sample(restored);
                helper.assertTrue(events.size() == 2 && java.util.Arrays.equals(events.getLast(), new Object[]{"redstone_changed", 1, 0, 15}) && starts[0] == 1,
                    "Real rising edge did not signal and wake exactly once");
            }
            final var malformed = saved.copy();
            malformed.putIntArray("oc:redstoneInputs", new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE});
            restored.loadWithComponents(malformed, registries);
            helper.assertTrue(host.redstoneInput(Direction.DOWN) == 0 && host.redstoneInput(Direction.UP) == 15 && host.redstoneInput(Direction.EAST) == 0,
                "Malformed or short input history was not clamped/reset");
            helper.succeed();
        } finally { listener.node().remove(); }
    }

    private static void sample(BlockEntity entity) {
        final var world = entity.getLevel();
        final var pos = entity.getBlockPos();
        final var state = entity.getBlockState();
        if (entity instanceof ComputerCaseBlockEntity host) ComputerCaseBlockEntity.serverTick(world, pos, state, host);
        else if (entity instanceof MicrocontrollerBlockEntity host) MicrocontrollerBlockEntity.serverTick(world, pos, state, host);
        else if (entity instanceof RobotBlockEntity host) RobotBlockEntity.serverTick(world, pos, state, host);
        else if (entity instanceof RedstoneIoBlockEntity host) host.updateRedstoneInputs();
    }
}

package li.cil.oc.common.gametest;

import li.cil.oc.api.API;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("neoopencomputers")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class ProjectRedGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void projectRedCableCarriesUnsignedStrengthsIntoIo(GameTestHelper helper) throws Exception {
        cable(helper, ModBlocks.COMPUTER_CASE_TIER2.get(), ModBlocks.REDSTONE_IO.get());
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void projectRedCableCarriesUnsignedStrengthsIntoCase(GameTestHelper helper) throws Exception {
        cable(helper, ModBlocks.REDSTONE_IO.get(), ModBlocks.COMPUTER_CASE_TIER2.get());
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void projectRedCableCarriesUnsignedStrengthsIntoMicrocontroller(GameTestHelper helper) throws Exception {
        cable(helper, ModBlocks.ROBOT.get(), ModBlocks.MICROCONTROLLER_TIER2.get());
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void projectRedCableCarriesUnsignedStrengthsIntoRobot(GameTestHelper helper) throws Exception {
        cable(helper, ModBlocks.MICROCONTROLLER_TIER2.get(), ModBlocks.ROBOT.get());
    }

    private static void cable(GameTestHelper helper, net.minecraft.world.level.block.Block source,
                              net.minecraft.world.level.block.Block target) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("projectred_transmission")) { helper.succeed(); return; }
        CableFixture.run(helper, source, target);
    }

    @GameTest(template = "empty")
    public static void projectRedEnablesAdvancedCardWithoutComputerCraft(GameTestHelper helper) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("projectred_transmission")) {
            helper.assertTrue(!Boolean.getBoolean("neoopencomputers.testProjectRed"), "Requested ProjectRed was not loaded");
            helper.succeed();
            return;
        }
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER2.get().defaultBlockState());
        final var host = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var stack = new ItemStack(ModItems.REDSTONE_CARD_TIER2.get());
        final var environment = API.driver.driverFor(stack).createEnvironment(stack, host);
        try {
            helper.assertTrue(((Component) environment.node()).methods().contains("setBundledOutput"),
                "Advanced bundled callbacks missing with ProjectRed installed");
        } finally { environment.node().remove(); }
        helper.succeed();
    }

    private static final class CableFixture {
        static void run(GameTestHelper helper, net.minecraft.world.level.block.Block sourceBlock,
                        net.minecraft.world.level.block.Block targetBlock) throws Exception {
            final var world = helper.getLevel();
            final var source = helper.absolutePos(new BlockPos(1, 2, 1));
            final var cable = source.east();
            final var target = cable.east();
            world.setBlockAndUpdate(source, sourceBlock.defaultBlockState());
            world.setBlockAndUpdate(target, targetBlock.defaultBlockState());
            for (final var pos : java.util.List.of(source, target)) {
                if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.RobotBlockEntity robot) {
                    robot.setTier(1);
                    RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(new ItemStack(ModItems.REDSTONE_CARD_TIER2.get())));
                }
            }
            final var emitter = (li.cil.oc.common.component.RedstoneControllerHost) world.getBlockEntity(source);
            final var receiver = (li.cil.oc.common.component.RedstoneControllerHost) world.getBlockEntity(target);
            for (int color = 0; color < 16; color++) emitter.setBundledRedstoneOutput(Direction.EAST, color, 255 - color * 16);
            final var type = Class.forName("mrtjp.projectred.transmission.WireType");
            final var wire = type.getMethod("newPart").invoke(type.getField("FRAMED_BUNDLED_NEUTRAL").get(null));
            Class.forName("codechicken.multipart.block.TileMultipart").getMethod("addPart", net.minecraft.world.level.Level.class,
                BlockPos.class, Class.forName("codechicken.multipart.api.part.MultiPart")).invoke(null, world, cable, wire);
            helper.startSequence().thenWaitUntil(() -> {
                final var signal = mrtjp.projectred.api.ProjectRedAPI.transmissionAPI.getBundledInput(world, target, Direction.WEST);
                helper.assertTrue(signal != null, "ProjectRed cable is not connected to OC output");
                tick(helper, target);
                for (int color = 0; color < 16; color++) {
                    final int expected = 255 - color * 16;
                    helper.assertTrue((signal[color] & 255) == expected, "Real ProjectRed cable lost output strength on color " + color);
                    helper.assertTrue(receiver.bundledRedstoneInput(Direction.WEST, color) == expected,
                        "OC lost unsigned ProjectRed input strength on color " + color);
                }
            }).thenExecute(() -> {
                for (int color = 0; color < 16; color++) emitter.setBundledRedstoneOutput(Direction.EAST, color, 0);
            }).thenWaitUntil(() -> {
                tick(helper, target);
                for (int color = 0; color < 16; color++) helper.assertTrue(receiver.bundledRedstoneInput(Direction.WEST, color) == 0,
                    "ProjectRed input retained stale power after output cleared");
            }).thenSucceed();
        }

        private static void tick(GameTestHelper helper, BlockPos pos) {
            final var world = helper.getLevel();
            final var state = world.getBlockState(pos);
            final var entity = world.getBlockEntity(pos);
            if (entity instanceof li.cil.oc.common.blockentity.RedstoneIoBlockEntity io) io.updateRedstoneInputs();
            else if (entity instanceof ComputerCaseBlockEntity computer) ComputerCaseBlockEntity.serverTick(world, pos, state, computer);
            else if (entity instanceof li.cil.oc.common.blockentity.MicrocontrollerBlockEntity micro) li.cil.oc.common.blockentity.MicrocontrollerBlockEntity.serverTick(world, pos, state, micro);
            else if (entity instanceof li.cil.oc.common.blockentity.RobotBlockEntity robot) li.cil.oc.common.blockentity.RobotBlockEntity.serverTick(world, pos, state, robot);
        }
    }

    private ProjectRedGameTests() {}
}

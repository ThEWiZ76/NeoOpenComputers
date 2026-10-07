package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Environment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.PowerDistributorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class DebuggerGameTests {
    @GameTest(template = "empty")
    public static void debuggerFollowsSelectedPortAndLogsNetworkTraffic(GameTestHelper helper) {
        final var registered = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "debugger"));
        helper.assertTrue(registered.isPresent(), "Network debugger item is missing");
        final var item = registered.orElseThrow();
        helper.assertTrue(li.cil.oc.api.API.items.get("debugger") != null, "Debugger item API alias is missing");
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, ModBlocks.POWER_DISTRIBUTOR.get().defaultBlockState());
        final var distributor = (PowerDistributorBlockEntity) world.getBlockEntity(pos);
        final var player = new ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(new java.util.UUID(0, 321), "debugger_fixture"), ClientInformation.createDefault());
        final var stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final var environment = (Environment) item;
        try {
            final var east = new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false);
            helper.assertTrue(item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, east)).consumesAction(), "Real player could not attach debugger");
            helper.assertTrue(environment.node().isNeighborOf(distributor.sidedNode(Direction.EAST))
                && !environment.node().isNeighborOf(distributor.sidedNode(Direction.DOWN)), "Debugger selected the default node instead of clicked port");
            distributor.sidedNode(Direction.EAST).sendToReachable("debugger_fixture", "payload", 42);
            final var west = new BlockHitResult(Vec3.atCenterOf(pos), Direction.WEST, pos, false);
            item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, west));
            helper.assertTrue(environment.node().isNeighborOf(distributor.sidedNode(Direction.WEST))
                && !environment.node().isNeighborOf(distributor.sidedNode(Direction.EAST)), "Debugger retained its previous port connection");
            final var fake = new net.neoforged.neoforge.common.util.FakePlayer(world,
                new com.mojang.authlib.GameProfile(new java.util.UUID(0, 322), "debugger_fake"));
            fake.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            helper.assertTrue(!item.useOn(new UseOnContext(fake, InteractionHand.MAIN_HAND, east)).consumesAction()
                && environment.node().isNeighborOf(distributor.sidedNode(Direction.WEST)), "Fake player moved the debugger");
            final var mock = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            mock.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            helper.assertTrue(!item.useOn(new UseOnContext(mock, InteractionHand.MAIN_HAND, east)).consumesAction()
                && environment.node().isNeighborOf(distributor.sidedNode(Direction.WEST)), "Non-server player moved the debugger");
            final var geolyzerPos = pos.above(2);
            world.setBlockAndUpdate(geolyzerPos, ModBlocks.GEOLYZER.get().defaultBlockState());
            final var geolyzer = (Environment) world.getBlockEntity(geolyzerPos);
            item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(geolyzerPos), Direction.UP, geolyzerPos, false)));
            helper.assertTrue(environment.node().isNeighborOf(geolyzer.node())
                && !environment.node().isNeighborOf(distributor.sidedNode(Direction.WEST)), "Debugger did not reconnect to a plain environment");
            final var stonePos = pos.above();
            world.setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
            item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(stonePos), Direction.DOWN, stonePos, false)));
            helper.assertTrue(environment.node().network() == null && stack.getCount() == 1, "Clicking a non-network block did not detach the reusable debugger");
        } finally {
            environment.node().remove();
        }
        helper.succeed();
    }
}

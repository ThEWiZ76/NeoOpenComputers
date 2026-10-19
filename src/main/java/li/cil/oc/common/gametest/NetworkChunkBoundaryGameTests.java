package li.cil.oc.common.gametest;

import li.cil.oc.api.Network;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class NetworkChunkBoundaryGameTests {
    @GameTest(template = "empty")
    public static void microcontrollerInputSamplingDoesNotLoadAdjacentChunk(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var edge = new BlockPos(1_600_015, 80, -1_596_792);
        final var chunk = level.getChunkAt(edge);
        helper.assertTrue(!level.hasChunkAt(edge.east()), "Microcontroller fixture requires an unloaded east chunk");
        try {
            chunk.setBlockState(edge, ModBlocks.MICROCONTROLLER_TIER1.get().defaultBlockState(), false);
            final var host = (li.cil.oc.common.blockentity.MicrocontrollerBlockEntity) chunk.getBlockEntity(edge);
            host.onLoad();
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Microcontroller lifecycle loaded adjacent chunk");
            li.cil.oc.common.blockentity.MicrocontrollerBlockEntity.serverTick(level, edge, host.getBlockState(), host);
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Microcontroller input sampling loaded adjacent chunk");
            host.onChunkUnloaded();
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Microcontroller teardown loaded adjacent chunk");
        } finally {
            chunk.setBlockState(edge, Blocks.AIR.defaultBlockState(), false);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void adapterDiscoveryDoesNotLoadAdjacentChunk(GameTestHelper helper) {
        final var level = helper.getLevel();
        final var edge = new BlockPos(1_600_015, 80, -1_598_392);
        final var chunk = level.getChunkAt(edge);
        net.minecraft.world.level.chunk.LevelChunk neighborChunk = null;
        helper.assertTrue(!level.hasChunkAt(edge.east()), "Adapter fixture requires an unloaded east chunk");
        final boolean inventoryDriver = li.cil.oc.common.ModSettings.enableInventoryDriver();
        li.cil.oc.common.ModSettings.ENABLE_INVENTORY_DRIVER.set(true);
        try {
            chunk.setBlockState(edge, ModBlocks.ADAPTER.get().defaultBlockState(), false);
            final var adapter = (li.cil.oc.common.blockentity.AdapterBlockEntity) chunk.getBlockEntity(edge);
            adapter.onLoad(); adapter.refreshNeighbors();
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Adapter discovery forced an adjacent chunk to load");
            neighborChunk = level.getChunkAt(edge.east());
            neighborChunk.setBlockState(edge.east(), Blocks.CHEST.defaultBlockState(), false);
            li.cil.oc.common.blockentity.AdapterBlockEntity.serverTick(level, edge, adapter.getBlockState(), adapter);
            boolean found = false;
            for (final var node : adapter.node().reachableNodes()) {
                if (node instanceof li.cil.oc.api.network.Component component && component.name().equals("inventory")) found = true;
            }
            helper.assertTrue(found, "Adapter did not discover its neighbor after that chunk loaded normally");
        } finally {
            chunk.setBlockState(edge, Blocks.AIR.defaultBlockState(), false);
            if (neighborChunk != null) neighborChunk.setBlockState(edge.east(), Blocks.AIR.defaultBlockState(), false);
            li.cil.oc.common.ModSettings.ENABLE_INVENTORY_DRIVER.set(inventoryDriver);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableAndScreenDiscoveryDoesNotLoadAdjacentChunk(GameTestHelper helper) {
        final var level = helper.getLevel();
        final var edge = new BlockPos(1_600_015, 80, -1_599_992);
        final var west = edge.west();
        final var screenPos = edge.above(2);
        final var chunk = level.getChunkAt(edge);
        helper.assertTrue(!level.hasChunkAt(edge.east()), "Boundary fixture requires an unloaded east chunk");
        try {
            // Operate in the loaded chunk directly: ordinary placement neighbor
            // notifications are not the network API behavior under test.
            chunk.setBlockState(edge, ModBlocks.CABLE.get().defaultBlockState(), false);
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Placing the boundary cable loaded the adjacent chunk");
            chunk.setBlockState(west, ModBlocks.CABLE.get().defaultBlockState(), false);
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Connecting the west cable loaded the adjacent chunk");
            chunk.setBlockState(screenPos, ModBlocks.SCREEN_TIER1.get().defaultBlockState(), false);
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Placing the boundary screen loaded the adjacent chunk");
            final var cable = (CableBlockEntity) chunk.getBlockEntity(edge);
            final var neighbor = (CableBlockEntity) chunk.getBlockEntity(west);
            final var screen = (ScreenBlockEntity) chunk.getBlockEntity(screenPos);
            cable.onLoad(); neighbor.onLoad();
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Cable onLoad loaded the adjacent chunk");
            screen.onLoad();
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Screen onLoad loaded the adjacent chunk");
            cable.canConnect(Direction.EAST);
            screen.canConnect(Direction.EAST);
            Network.joinOrCreateNetwork(level, edge);
            Network.joinOrCreateNetwork(level, screenPos);
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Discovery or a sided callback forced the adjacent chunk to load");
            helper.assertTrue(cable.node().isNeighborOf(neighbor.node()), "Loaded cable neighbor failed to connect");
            cable.onChunkUnloaded(); screen.onChunkUnloaded();
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Teardown loaded an adjacent chunk");
        } finally {
            chunk.setBlockState(edge, Blocks.AIR.defaultBlockState(), false);
            chunk.setBlockState(west, Blocks.AIR.defaultBlockState(), false);
            chunk.setBlockState(screenPos, Blocks.AIR.defaultBlockState(), false);
        }
        helper.succeed();
    }

    private NetworkChunkBoundaryGameTests() { }
}

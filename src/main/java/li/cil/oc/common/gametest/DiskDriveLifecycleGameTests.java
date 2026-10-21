package li.cil.oc.common.gametest;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real replacement for the Unsafe unit fixture that had no item/driver ownership. */
@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class DiskDriveLifecycleGameTests {
    @GameTest(template = "empty")
    public static void diskDriveReleasesDriveAndMediaNodesOnUnloadAndRemoval(GameTestHelper helper) {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.DISK_DRIVE.get());
        final DiskDriveBlockEntity drive = helper.getBlockEntity(pos);
        drive.setItem(0, new ItemStack(ModItems.FLOPPY.get()));
        Network.joinOrCreateNetwork(drive);
        final Node driveNode = drive.node();
        Node media = null;
        for (var node : driveNode.reachableNodes())
            if (node instanceof Component component && component.name().equals("filesystem")) media = node;
        helper.assertTrue(driveNode.network() != null && media != null && media.network() != null,
            "Cleanup fixture must start with an actual attached media node");
        drive.onChunkUnloaded(); drive.setRemoved();
        helper.assertTrue(driveNode.network() == null && media.network() == null, "Unload/removal leaked a drive or media node");
        final var saved = drive.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.contains("disk") && !saved.getList("Items", 10).isEmpty(), "Late save discarded the unloaded media snapshot");
        helper.assertTrue(driveNode.network() == null && media.network() == null, "Late save reconnected disposed nodes");
        helper.succeed();
    }
}

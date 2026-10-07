package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.CableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class CableGameTests {
    @GameTest(template = "empty")
    public static void cableColorReloadAndIsolation(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var center = place(helper, pos);
        helper.assertTrue(center instanceof Colored, "Cable does not expose its stored color");
        for (final var side : Direction.values()) {
            ((Colored) place(helper, pos.relative(side))).setColor(DyeColor.BLUE.getTextureDiffuseColor());
        }
        ((Colored) center).setColor(DyeColor.RED.getTextureDiffuseColor());
        helper.assertTrue(center.node().network() != null, "Fully isolated colored cable has no network");
        for (final var side : Direction.values()) {
            final var neighbor = (CableBlockEntity) helper.getLevel().getBlockEntity(pos.relative(side));
            helper.assertTrue(center.node().network() != neighbor.node().network(), "Color mismatch connected on " + side);
        }
        final var loaded = new CableBlockEntity(pos, center.getBlockState());
        final var legacy = new CompoundTag();
        legacy.putInt("oc:renderColor", DyeColor.GREEN.getId());
        loaded.loadWithComponents(legacy, helper.getLevel().registryAccess());
        helper.assertTrue(((Colored) loaded).getColor() == DyeColor.GREEN.getTextureDiffuseColor(), "Legacy dye metadata did not migrate");
        legacy.putInt("oc:renderColorRGB", DyeColor.YELLOW.getTextureDiffuseColor());
        loaded.loadWithComponents(legacy, helper.getLevel().registryAccess());
        helper.assertTrue(((Colored) loaded).getColor() == DyeColor.YELLOW.getTextureDiffuseColor(), "RGB did not override legacy color");
        loaded.handleUpdateTag(center.getUpdateTag(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(((Colored) loaded).getColor() == DyeColor.RED.getTextureDiffuseColor(), "Client update lost cable color");
        helper.assertTrue(!center.getUpdateTag(helper.getLevel().registryAccess()).contains("node"), "Client color update leaked server network data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableColorsSplitAndReconnectNetworks(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final var first = place(helper, pos);
        final var middle = place(helper, pos.relative(Direction.EAST));
        final var last = place(helper, pos.relative(Direction.EAST, 2));
        helper.assertTrue(first instanceof Colored, "Cable does not implement color connectivity");
        final var firstColor = (Colored) first;
        final var middleColor = (Colored) middle;
        final var lastColor = (Colored) last;
        firstColor.setColor(DyeColor.RED.getTextureDiffuseColor());
        lastColor.setColor(DyeColor.BLUE.getTextureDiffuseColor());
        helper.assertTrue(first.node().network() == last.node().network(), "Default light gray cable did not bridge different colors");
        final var address = middle.node().address();
        middleColor.setColor(DyeColor.RED.getTextureDiffuseColor());
        helper.assertTrue(first.node().network() == middle.node().network(), "Matching cable colors did not connect");
        helper.assertTrue(first.node().network() != last.node().network(), "Different cable colors remained connected after recoloring");
        Network.joinOrCreateNetwork(helper.getLevel(), last.getBlockPos());
        helper.assertTrue(first.node().network() != last.node().network(), "Joining from the opposite cable bypassed color rules");
        middleColor.setColor(DyeColor.LIGHT_GRAY.getTextureDiffuseColor());
        helper.assertTrue(first.node().network() == last.node().network(), "Restoring wildcard color did not reconnect both networks");
        helper.assertTrue(address.equals(middle.node().address()), "Recoloring changed the cable node address");
        middleColor.setColor(DyeColor.BLUE.getTextureDiffuseColor());
        final var saved = middle.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var loaded = new CableBlockEntity(middle.getBlockPos(), middle.getBlockState());
        loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(((Colored) loaded).getColor() == middleColor.getColor(), "Cable color was lost in NBT");
        helper.assertTrue(address.equals(loaded.node().address()), "Cable address was lost in NBT");
        helper.succeed();
    }

    private static CableBlockEntity place(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.CABLE.get().defaultBlockState());
        final var cable = (CableBlockEntity) helper.getLevel().getBlockEntity(pos);
        Network.joinOrCreateNetwork(helper.getLevel(), pos);
        return cable;
    }
}
